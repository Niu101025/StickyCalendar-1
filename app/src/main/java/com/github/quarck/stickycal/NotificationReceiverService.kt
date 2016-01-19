/*
 * Copyright (c) 2015, Sergey Parshin, s.parshin@outlook.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.quarck.stickycal

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class NotificationReceiverService : NotificationListenerService(), Handler.Callback
{
	private val handledPackages = arrayOf<String>("com.google.android.calendar", "com.android.calendar")

	private val messenger = Messenger(Handler(this))

	private var settings: Settings? = null

	private var db: SavedNotifications? = null

	override fun handleMessage(msg: Message): Boolean
	{
		Lw.d(TAG, "handleMessage, msg=${msg.what}")

		var ret = true

		if (msg.what == MSG_CHECK_PERMISSIONS)
			ret = handleCheckPermissions(msg)
		else if (msg.what == MSG_CHECK_PERMISSIONS_AFTER_UPDATE)
			ret = handleCheckPermissionsAndNotification(msg)

		return ret
	}


	private fun handleCheckPermissions(msg: Message): Boolean
	{
		if (!checkPermissions())
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0))

		return true
	}

	private fun handleCheckPermissionsAndNotification(msg: Message): Boolean
	{
		if (!checkPermissions() && settings!!.isServiceEnabled)
		{
			notificationMgr!!.onAccessToNotificationsLost(this)
		}

		return true
	}

	private fun reply(msgIn: Message, msgOut: Message)
	{
		try
		{
			msgIn.replyTo.send(msgOut)
		}
		catch (e: RemoteException)
		{
			e.printStackTrace()
		}
	}

	private fun checkPermissions(): Boolean
	{
		var ret = false
		Lw.d(TAG, "checkPermissions")
		try
		{
			var notifications = activeNotifications
			Lw.e(TAG, "Got ${notifications.size} notifications during check")
			ret = true
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception, have no permissions!")
		}

		return ret
	}


	override fun onCreate()
	{
		super.onCreate()
		settings = Settings(this)
		db = SavedNotifications(this)
	}

	override fun onDestroy()
	{
		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder?
	{
		if (intent.getBooleanExtra(configServiceExtra, false))
			return messenger.binder

		return super.onBind(intent)
	}


	fun processNotification(context: Context, originalNotification: Notification) : Boolean
	{
		var canRemoveOriginal = true

		var ret =  true

		var (title, text) = originalNotification.getTitleAndText();

		var eventId = originalNotification.getGooleCalendarEventId()

		if (title != "" && eventId != null)
		{
			Lw.d(TAG, "Parsed event: id:${eventId}, title=${title}")

			// Only process if we have full set of data we have to have
			var dbEntry = db!!.addNotification(eventId, title, text)
			notificationMgr.postNotification(context, dbEntry, settings!!.showDiscardButton, originalNotification.contentIntent)
		}
		else if (originalNotification.isGoogleCalendarReminder())
		{
			// We can't properly handle Google Calendar Reminder notifications,
			// and there is no reason for this as well - Calendar Reminders would stay
			// until dismissed (unlike Events)
			Lw.d(TAG, "Not handling Reminder notification ${title}")
			canRemoveOriginal = false;
		}
		else
		{
			canRemoveOriginal = false
			notificationMgr!!.onNotificationParseError(this, originalNotification.contentIntent)
		}

		return canRemoveOriginal;
	}

	override fun onNotificationPosted(notification: StatusBarNotification)
	{
		if (notification != null && settings != null && settings!!.isServiceEnabled)
		{
			val packageName = notification.packageName

			if (packageName in handledPackages)
			{
				Lw.d(TAG, "Event from the calendar, key=${notification.key}")

				var canRemoveOriginal = processNotification(this, notification.getNotification());

				if (settings!!.removeOriginal && canRemoveOriginal)
				{
					cancelNotification(notification.key)
				};
			}
		}
	}

	override fun onNotificationRemoved(notification: StatusBarNotification)
	{
		if (notification != null)
		{
			var tag = notification.tag;
			var pkg = notification.packageName

			if (tag != null && pkg != null &&
				pkg == Consts.OUR_PACKAGE_NAME
				&& tag.startsWith(Consts.NOTIFICATION_TAG))
			{
				var id = notification.id;
				var eventId = notification.getOurNotificationEventId()

				if (eventId != null)
				{
					Lw.d(TAG, "Our notification id ${id}, eventId ${eventId} was removed")

					if (db != null)
					{
						if (!settings!!.showDiscardButton)
						{
							Lw.d(TAG, "Not showing discard button - so simply removing the notification")
							notificationMgr.onNotificationRemoved(eventId)
							db!!.deleteNotification(eventId)
						}
						else
						{
							var dbEntry = db!!.getNotification(eventId)
							if (dbEntry != null)
							{
								Lw.d(TAG, "Discard button is active and DB entry is not null - re-posting notification")
								notificationMgr.postNotification(this, dbEntry, settings!!.showDiscardButton,
									notification.notification.contentIntent)
							}
							else
							{
								Lw.d(TAG, "Discard button is active and DB entry is null - notification was dismissed")
								notificationMgr.onNotificationRemoved(eventId)
							}
						}
					}

				}
			}
		}
	}

	companion object
	{
		val TAG = "Service"

		val configServiceExtra = "configService"

		val MSG_CHECK_PERMISSIONS = 1
		val MSG_NO_PERMISSIONS = 2
		var MSG_CHECK_PERMISSIONS_AFTER_UPDATE = 3

		var notificationMgr = NotificationViewManager()
	}
}
