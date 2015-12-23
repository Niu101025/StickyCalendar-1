package com.github.quarck.stickycal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartUpBootReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		var db = SavedNotifications(context)
		if (db != null)
			db!!.postAllNotifications(context)
	}
}
