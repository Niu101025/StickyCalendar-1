package com.github.quarck.stickycal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartUpBootReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		postCachedNotifications(context)
	}
}
