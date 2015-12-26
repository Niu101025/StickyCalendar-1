package com.github.quarck.stickycal

import android.content.Context

fun postCachedNotifications(context: Context)
{
	var db = SavedNotifications(context)
	var mgr = NotificationViewManager()

	for (notification in db.notifications)
	{
		mgr.postNotification(context, notification, null)
	}
}