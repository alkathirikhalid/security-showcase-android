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

object HandleNotifications {

	val ONGOING_NOTIFICATION_ID = getRandomNumber()
	val SMALL_ICON = R.drawable.ic_security
	val STOP_ACTION_ICON = R.drawable.ic_adjust_black_24dp

	/** PendingIntent to stop the service.  */
	private fun getStopServicePI(context: Service): PendingIntent {
		val piStopService: PendingIntent
		run {
			val iStopService = LongRunningIntentBuilder(context).setCommand(LongRunningCommand.STOP).build()
			piStopService = PendingIntent.getService(context, getRandomNumber(), iStopService, 0)
		}
		return piStopService
	}

	/** Get pending intent to launch the activity.  */
	private fun getLaunchActivityPI(context: Service): PendingIntent {
		val piLaunchMainActivity: PendingIntent
		run {
			val iLaunchMainActivity = Intent(context, LoginActivity::class.java)
			piLaunchMainActivity = PendingIntent.getActivity(context, getRandomNumber(), iLaunchMainActivity, 0)
		}
		return piLaunchMainActivity
	}

	//
	// Pre O specific.
	//

	@TargetApi(25)
	object PreO {

		fun createNotification(context: Service) {
			// Create Pending Intents.
			val piLaunchMainActivity = getLaunchActivityPI(context)
			val piStopService = getStopServicePI(context)
			// Action to stop the service.
			val stopAction = NotificationCompat.Action.Builder(
				STOP_ACTION_ICON,
				getNotificationStopActionText(context),
				piStopService)
				.build()

			// Create a notification.
			val mNotification = NotificationCompat.Builder(context)
				.setContentTitle(getNotificationTitle(context))
				.setContentText(getNotificationContent(context))
				.setSmallIcon(SMALL_ICON)
				.setContentIntent(piLaunchMainActivity)
				.addAction(stopAction)
				.setStyle(NotificationCompat.BigTextStyle())
				.build()

			context.startForeground(ONGOING_NOTIFICATION_ID, mNotification)
		}
	}

	private fun getNotificationContent(context: Service): String {
		return "notificationContentPlaceholder"
	}

	private fun getNotificationTitle(context: Service): String {
		return "notificationTitlePlaceholder"
	}

	//
	// O Specific.
	//

	@TargetApi(26)
	object O {

		val CHANNEL_ID = getRandomNumber().toString()

		fun createNotification(context: Service) {
			val channelId = createChannel(context)
			val notification = buildNotification(context, channelId)
			context.startForeground(ONGOING_NOTIFICATION_ID, notification)
		}

		private fun buildNotification(context: Service, channelId: String): Notification {
			// Create Pending Intents.
			val piLaunchMainActivity = getLaunchActivityPI(context)
			val piStopService = getStopServicePI(context)
			// Action to stop the service.
			val stopAction = Notification.Action.Builder(
				STOP_ACTION_ICON,
				getNotificationStopActionText(context),
				piStopService)
				.build()

			// Create a notification.
			return Notification.Builder(context, channelId)
				.setContentTitle(getNotificationTitle(context))
				.setContentText(getNotificationContent(context))
				.setSmallIcon(SMALL_ICON)
				.setContentIntent(piLaunchMainActivity)
				.setActions(stopAction)
				.setStyle(Notification.BigTextStyle())
				.build()
		}

		private fun createChannel(context: Service): String {
			// Create a channel.
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			val channelName = "Playback channel"
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, importance)
			notificationManager.createNotificationChannel(notificationChannel)
			return CHANNEL_ID
		}
	}

	private fun getNotificationStopActionText(context: Service): String {
		return "stopActionTextPlaceholder"
	}
} // end class HandleNotifications.

fun getRandomNumber(): Int {
	return Random().nextInt(100000)
}
