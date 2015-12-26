package com.github.quarck.stickycal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

class NotificationViewManager
{
	public fun postNotification(ctx: Context, notification: DBNotification, pendingIntent: PendingIntent?)
	{
		synchronized(NotificationManager::class.java)
		{
			if (tracker == null)
				tracker = NotificationIdTracker(ctx)
		}

		var notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		var nextId = tracker!!.getNotificationIdForEventId(notification.eventId)

		var pIndent = pendingIntent
		if (pIndent == null)
		{
			var uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, notification.eventId);
			var intent = Intent(Intent.ACTION_VIEW).setData(uri);
			pIndent = PendingIntent.getActivity(ctx, 0, intent, 0)
		}

		notificationManager.notify(
			"${Consts.NOTIFICATION_TAG};${notification.eventId}",
			nextId,
			Notification
				.Builder(ctx)
				.setContentTitle(notification.title)
				.setContentText(notification.text)
				.setSmallIcon(R.drawable.stat_notify_calendar)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentIntent(pIndent)
				.setAutoCancel(true)
				.build())
	}

	public fun onNotificationRemoved(eventId: Long)
	{
		if (tracker != null)
		{
			tracker!!.deleteByEventId(eventId)
		}
	}

	companion object
	{
		private var tracker: NotificationIdTracker? = null
	}
}