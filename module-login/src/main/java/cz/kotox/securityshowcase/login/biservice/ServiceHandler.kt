package cz.kotox.securityshowcase.login.biservice

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import cz.kotox.securityshowcase.login.LoginActivity
import cz.kotox.securityshowcase.login.R
import java.util.Random

object ServiceHandler {

	val ONGOING_NOTIFICATION_ID = getRandomNumber()
	private val SMALL_ICON = R.drawable.ic_security
	private val STOP_ACTION_ICON = R.drawable.ic_adjust_black_24dp

	/** PendingIntent to stop the service.  */
	private fun getStopServiceIntent(context: Service): PendingIntent {
		val stopIntnet: PendingIntent
		run {
			val iStopService = LongRunningIntentBuilder(context).setCommand(LongRunningCommand.STOP).build()
			stopIntnet = PendingIntent.getService(context, getRandomNumber(), iStopService, 0)
		}
		return stopIntnet
	}

	private fun getLaunchUiIntent(context: Service): PendingIntent {
		val launchUiIntent: PendingIntent
		run {
			val activityIntent = Intent(context, LoginActivity::class.java)
			launchUiIntent = PendingIntent.getActivity(context, getRandomNumber(), activityIntent, 0)
		}
		return launchUiIntent
	}

	@TargetApi(25)
	object PreO {

		fun startForeground(service: Service) {
			startForeground(service, NotificationCompat.Builder(service))
		}

		fun updateNotification(service: Service, progress: Int) {
			updateProgress(service, NotificationCompat.Builder(service), progress)
		}
	}

	@TargetApi(26)
	object O {

		private val CHANNEL_ID = getRandomNumber().toString()

		fun startForeground(service: Service) {
			startForeground(service, NotificationCompat.Builder(service, createChannel(service)))
		}

		fun updateNotification(service: Service, progress: Int) {
			updateProgress(service, NotificationCompat.Builder(service, createChannel(service)), progress)
		}

		private fun createChannel(context: Service): String {
			// Create a channel.
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			val channelName = "MyLongRunning channel"
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, importance)
			notificationManager.createNotificationChannel(notificationChannel)
			return CHANNEL_ID
		}
	}

	private fun getNotificationContent(context: Service): String {
		return "notificationContentPlaceholder"
	}

	private fun getNotificationTitle(context: Service): String {
		return "notificationTitlePlaceholder"
	}

	private fun getNotificationStopActionText(context: Service): String {
		return "stopActionTextPlaceholder"
	}

	private fun startForeground(service: Service, builder: NotificationCompat.Builder) {
		val launchUiIntent = getLaunchUiIntent(service)
		val stopIntent = getStopServiceIntent(service)
		val stopAction = buildStopAction(service, stopIntent)
		val notification = buildNotification(builder, service, launchUiIntent, stopAction)
		service.startForeground(ONGOING_NOTIFICATION_ID, notification)
	}

	private fun updateProgress(service: Service, builder: NotificationCompat.Builder, progress: Int) {
		val launchUiIntent = getLaunchUiIntent(service)
		val stopIntent = getStopServiceIntent(service)
		val stopAction = buildStopAction(service, stopIntent)
		builder.setProgress(30, progress, false)
		val notification = buildNotification(builder, service, launchUiIntent, stopAction)
		val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
	}

	private fun buildStopAction(context: Service, stopIntent: PendingIntent): NotificationCompat.Action = NotificationCompat.Action.Builder(
		STOP_ACTION_ICON,
		getNotificationStopActionText(context),
		stopIntent)
		.build()

	private fun buildNotification(
		builder: NotificationCompat.Builder,
		context: Service,
		contentIntent: PendingIntent,
		stopAction: NotificationCompat.Action): Notification {
		return builder
			.setContentTitle(getNotificationTitle(context))
			.setContentText(getNotificationContent(context))
			.setSmallIcon(SMALL_ICON)
			.setContentIntent(contentIntent)
			.addAction(stopAction)
			.setStyle(NotificationCompat.BigTextStyle())
			.build()
	}
}

fun getRandomNumber(): Int {
	return Random().nextInt(100000)
}
