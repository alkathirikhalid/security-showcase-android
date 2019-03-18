package cz.kotox.securityshowcase.login.biservice

import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.databinding.ObservableInt
import cz.kotox.securityshowcase.login.R
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LongRunningService : Service() {
	private var mNM: NotificationManager? = null

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private val NOTIFICATION = R.string.local_service_started

	val MAX_TIME_SEC = TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES)

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private val mBinder = LocalBinder()

	private var serviceIsStarted: Boolean = false

	private var executor: ScheduledExecutorService? = null

	var timeRunningSec = ObservableInt(0)

	companion object {
		/**
		 * If a call is made to [.commandStart] without firing an explicit Intent to put this
		 * service in a started state (which happens in [.onClick]), then fire the explicit
		 * intent with [Command.START] which actually ends up calling [.startJob]
		 * again and this time, does the work of creating the executor.
		 *
		 *
		 * Next, you would move this service into the foreground, which you can't do unless this
		 * service is in a started state.
		 */
		@TargetApi(Build.VERSION_CODES.O)
		fun moveToStartedState(context: Context) {
			val intent = LongRunningIntentBuilder(context).setCommand(LongRunningCommand.START).build()
			if (isPreAndroidO()) {
				Timber.d("moveToStartedState: Running on Android N or lower - startService(intent)")
				context.startService(intent)
			} else {
				Timber.d("moveToStartedState: Running on Android O - startForegroundService(intent)")
				context.startForegroundService(intent)
			}
		}
	}

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	inner class LocalBinder : Binder() {
		internal val service: LongRunningService
			get() = this@LongRunningService
	}

	override fun onCreate() {
		mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

		// Display a notification about us starting.  We put an icon in the status bar.
		//showNotification()
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		val containsCommand = LongRunningIntentBuilder.containsCommand(intent)
		Timber.d("onStartCommand: Service in [%s] state. commandId: [%s]. startId: [%s]",
			if (serviceIsStarted) "STARTED" else "NOT STARTED",
			if (containsCommand) LongRunningIntentBuilder.getCommand(intent) else "N/A", startId)
		serviceIsStarted = true
		routeIntentToCommand(intent)
		return Service.START_NOT_STICKY
	}

	override fun onDestroy() {
		// Cancel the persistent notification.
		mNM!!.cancel(NOTIFICATION)

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show()
	}

	override fun onBind(intent: Intent): IBinder {
		return mBinder
	}

	fun toggleService(): Boolean {
		return if (serviceIsStarted) {
			Timber.d("onClick: calling commandStop()");
			stopJob()
			false
		} else {
			Timber.d("onClick: calling commandStart()");
			startJob()
			true
		}
	}

	fun startJob() {
		if (!serviceIsStarted) {
			moveToStartedState(this)
			return
		}

		if (executor == null) {
			timeRunningSec.set(0)

			if (isPreAndroidO()) {
				ServiceHandler.PreO.startForeground(this)
			} else {
				ServiceHandler.O.startForeground(this)
			}

			executor = Executors.newSingleThreadScheduledExecutor()
			val runnable = Runnable { recurringTask() }
			executor?.scheduleWithFixedDelay(runnable, 0, 2, TimeUnit.SECONDS)
			Timber.d("commandStart: starting executor")
		} else {
			Timber.d("commandStart: do nothing")
		}
	}

	/**
	 * This method can be called directly, or by firing an explicit Intent with [ ][Command.STOP].
	 */
	private fun stopJob() {
		stopForeground(true)
		stopSelf()
		serviceIsStarted = false
		executor?.shutdown()
		executor = null
	}

//	/**
//	 * Show a notification while this service is running.
//	 */
//	private fun showNotification() {
//		// In this sample, we'll use the same text for the ticker and the expanded notification
//		val text = getText(R.string.local_service_started)
//
//		// The PendingIntent to launch our activity if the user selects this notification
//		val contentIntent = PendingIntent.getActivity(this, 0,
//			Intent(this, LoginActivity::class.java), 0)
//
//		// Set the info for the views that show in the notification panel.
//		val notification = Notification.Builder(this)
//			.setSmallIcon(R.drawable.ic_security)  // the status icon
//			.setTicker(text)  // the status text
//			.setWhen(System.currentTimeMillis())  // the time stamp
//			.setContentTitle(getText(R.string.local_service_label))  // the label of the entry
//			.setContentText(text)  // the contents of the entry
//			.setContentIntent(contentIntent)  // The intent to send when the entry is clicked
//			.build()
//
//		// Send the notification.
//		mNM!!.notify(NOTIFICATION, notification)
//	}
//
//	//note that it is just a notification (not a notification of both service)
//	private fun showNotification2(): Notification {
//		val builder = NotificationCompat.Builder(this)
//		builder.setContentTitle("BothService running indefinitely")
//			.setSmallIcon(R.drawable.ic_security)
//		val n = builder.build()
//		val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//		mNotificationManager.notify(1, n)
//		return n
//	}

	private fun routeIntentToCommand(intent: Intent?) {
		if (intent != null) {

			// process command
			if (LongRunningIntentBuilder.containsCommand(intent)) {
				processCommand(LongRunningIntentBuilder.getCommand(intent))
			}

			// process message
			if (LongRunningIntentBuilder.containsMessage(intent)) {
				processMessage(LongRunningIntentBuilder.getMessage(intent))
			}
		}
	}

	private fun processCommand(command: LongRunningCommand) {
		try {
			when (command) {
				LongRunningCommand.START -> startJob()
				LongRunningCommand.STOP -> stopJob()
				else -> throw UnsupportedOperationException("Invalid command state: $command")
			}
		} catch (e: Exception) {
			Timber.e(e, "processCommand: exception")
		}
	}

	private fun processMessage(message: String?) {
		try {
			Timber.d(String.format("doMessage: message from client: '%s'", message))
		} catch (e: Exception) {
			Timber.e(e, "processMessage: exception")
		}
	}

	/** This method runs in a background thread, not the main thread, in the executor  */
	private fun recurringTask() {
		//if (isCharging()) {
		// Reset the countdown timer.
		//timeRunningSec = 0
		//} else {
		// Run down the countdown timer.
		timeRunningSec.set(timeRunningSec.get().inc())
		Timber.d("time [$timeRunningSec]")

		if (isPreAndroidO()) {
			ServiceHandler.PreO.updateNotification(this, timeRunningSec.get())
		} else {
			ServiceHandler.O.updateNotification(this, timeRunningSec.get())
		}

		if (timeRunningSec.get() >= 30/*MAX_TIME_SEC*/) {
//				// Timer has run out.
			stopJob()
			Timber.d("recurringTask: commandStop()")
//				}

		} else {
			// Timer has not run out, do nothing.
			//d(TAG, "recurringTask: normal");
		}
		//}

//		mHandler?.post(
//			Runnable { /*updateTile()*/ })
	}

	private fun isCharging(): Boolean {
		val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
		val batteryStatus = applicationContext.registerReceiver(null, intentFilter)
		val status = batteryStatus!!.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
		return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
	}

}

private fun isPreAndroidO() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O