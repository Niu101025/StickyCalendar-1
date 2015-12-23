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
import android.content.ContentUris
import android.content.Context
import java.util.ArrayList
import java.util.HashMap

import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.CalendarContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReceiverService : NotificationListenerService(), Handler.Callback
{
	private val handledPackages = arrayOf<String>("com.google.android.calendar", "com.android.calendar")

	private val messenger = Messenger(Handler(this))

	private var settings: Settings? = null

	private var db: SavedNotifications? = null

	override fun handleMessage(msg: Message): Boolean
	{
		var ret = true

		Lw.d(TAG, "handleMessage, msg=" + msg.what)

		when (msg.what)
		{
				MSG_CHECK_PERMISSIONS -> ret = handleCheckPermissions(msg)
				MSG_POST_ALL_NOTIFICATIONS -> ret = handlePostAllNotifications(msg)
		}

		return ret
	}

	private fun handleCheckPermissions(msg: Message): Boolean
	{
		Lw.d(TAG, "handleCheckPermissions")
		try
		{
			var notifications = activeNotifications
			Lw.e(TAG, "Got ${notifications.size} notifications during check")
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception, have no permissions!")
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0))
		}

		return true
	}

	private fun handlePostAllNotifications(msg: Message): Boolean
	{
		db!!.postAllNotifications(this);
		return true;
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


	fun getOurNotificationEventId(notification: StatusBarNotification): Long?
	{
		var ret: Long? = null;

		try
		{
			var tag = notification.tag;
			if (tag != null)
			{
				ret = tag.split(';').last().toLong()
			}
		}
		catch (ex: Exception)
		{
			ret = null
		}
		return ret;
	}


	fun processNotification(context: Context, originalNotification: Notification) : Boolean
	{
		var ret =  true

		var (title, text) = originalNotification.getTitleAndText();

		var eventId = originalNotification.getGooleCalendarEventId()

		var nextId : Int

		if (eventId != null && eventId in notificationIdMap)
		{
			nextId = notificationIdMap[eventId]!!; // we are already displaying this notification -- dont' need to create new notificatoin
		}
		else
		{
			nextId = getNextAvailNotificationId()
		}

		val notification =
			Notification
				.Builder(context)
				.setContentTitle(title)
				.setContentText(text)
				.setSmallIcon(R.drawable.stat_notify_calendar)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentIntent(originalNotification.contentIntent)
				.setAutoCancel(true)
				.build()

		var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify("${NOTIFICATION_TAG};${eventId}", nextId, notification)

		if (eventId != null)
		{
			Lw.d(TAG, "Parsed event id ${eventId}")

			db!!.addNotification(eventId, title, text)
			notificationIdMap[eventId] = nextId;
		}
		else
		{
			ret = false
			Lw.e(TAG, "Warning: wasn't able to get event id for notification ${notification}")
		}

		return ret;
	}

	override fun onNotificationPosted(notification: StatusBarNotification)
	{
		if (notification != null && settings!!.isServiceEnabled)
		{
			Lw.d(TAG, "Checking notification" + notification)
			val packageName = notification.packageName

			Lw.d(TAG, "Package name is " + packageName)

			if (packageName in handledPackages)
			{
				Lw.d(TAG, "This is a reminder from the calendar, key=${notification.key}")

				processNotification(this, notification.getNotification());

				if (settings!!.removeOriginal)
				{
					cancelNotification(notification.key)
				};
			}
			else if (packageName == Consts.packageName)
			{
				var id = notification.id;
				var eventId = getOurNotificationEventId(notification)

				if (eventId != null)
				{
					notificationIdMap[eventId] = id
					Lw.d(TAG, "Our notification id ${id}, eventId ${eventId} was added")
				}
			}
		}
	}

	override fun onNotificationRemoved(notification: StatusBarNotification)
	{
		if (notification != null)
		{
			var tag = notification.tag;
			var pkg = notification.packageName
			if (tag != null && pkg != null
				&& pkg == Consts.packageName
				&& tag.startsWith(NOTIFICATION_TAG))
			{
				var id = notification.id;
				var eventId = getOurNotificationEventId(notification)

				if (eventId != null)
				{
					Lw.d(TAG, "Our notification id ${id}, eventId ${eventId} was removed")
					db!!.deleteNotification(eventId)
					notificationIdMap.remove(eventId)
				}
			}
		}
	}

	private fun getNextAvailNotificationId(): Int
	{
		return ++ nextNotificationId;
	}

	companion object
	{
		val TAG = "Service"

		val configServiceExtra = "configService"

		val MSG_CHECK_PERMISSIONS = 1
		val MSG_NO_PERMISSIONS = 2
		var MSG_POST_ALL_NOTIFICATIONS = 3

		val NOTIFICATION_TAG = "com.github.quarck.stickycal.ForwardedNotificationTag"

		var nextNotificationId = Consts.notificationIdDynamicFrom

		var notificationIdMap = HashMap<Long, Int>(); // map from calendar event id to notification id
	}
}
