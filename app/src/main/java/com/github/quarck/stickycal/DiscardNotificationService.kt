package com.github.quarck.stickycal

import android.app.IntentService
import android.content.Intent

/**
 * Created by quarck on 19/01/16.
 */
class DiscardNotificationService: IntentService("DiscardNotificationService")
{
	override fun onHandleIntent(intent: Intent?)
	{
		Lw.d(TAG, "onHandleIntent")

		if (intent != null)
		{
			var notificationId = intent.getIntExtra(Consts.INTENT_NOTIFICATION_ID_KEY, -1)
			var eventId = intent.getLongExtra(Consts.INTENT_EVENT_ID_KEY, -1)

			var db = SavedNotifications(this)
			var mgr = NotificationViewManager()

			if (notificationId != -1 && eventId != -1L)
			{
				Lw.d("Removing event id ${eventId} from DB")
				db.deleteNotification(eventId)

				Lw.d("Dismissing notification id ${notificationId}")
				mgr.removeNotification(this, eventId, notificationId)
			}
			else
			{
				Lw.e(TAG, "notificationId=${notificationId}, eventId=${eventId}")
			}
		}
		else
		{
			Lw.e(TAG, "Intent is null!")
		}
	}

	companion object
	{
		val TAG = "DiscardNotificationService"
	}
}