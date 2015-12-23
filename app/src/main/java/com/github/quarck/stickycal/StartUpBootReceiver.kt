package com.github.quarck.stickycal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by quarck on 23/12/15.
 */
class StartUpBootReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		var db = SavedNotifications(context)
		db!!.postAllNotifications(context)
	}
}
