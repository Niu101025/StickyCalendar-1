package com.github.quarck.stickycal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppUpdatedBroadcastReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		val mainActivityIntent = Intent(context, MainActivity::class.java)
		val pendingMainActivityIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, 0)

		val notification =
			Notification
				.Builder(context)
				.setContentTitle(context.getString(R.string.app_updated))
				.setContentText(context.getString(R.string.reenable_app))
				.setSmallIcon(R.drawable.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentIntent(pendingMainActivityIntent)
				.setAutoCancel(true)
				.build()

		var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(Consts.notificationIdUpdated, notification) // would update if already exists
	}
}
