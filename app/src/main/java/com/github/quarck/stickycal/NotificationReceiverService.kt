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
import java.util.ArrayList
import java.util.HashMap

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReceiverService : NotificationListenerService(), Handler.Callback
{
	private val handledPackages = arrayOf<String>( "com.google.android.calendar", "com.android.calendar" )

	private var alarm: Alarm? = null

	private val messenger = Messenger(Handler(this))

	private var settings: Settings? = null

	override fun handleMessage(msg: Message): Boolean
	{
		var ret = true

		Lw.d(TAG, "handleMessage, msg=" + msg.what)

		when (msg.what)
		{
			MSG_CHECK_PERMISSIONS -> ret = handleCheckPermissions(msg)

			MSG_LIST_NOTIFICATIONS -> ret = handleListNotifications(msg)

			MSG_RELOAD_SETTINGS ->
			{
				Lw.d(TAG, "Explicit request to reload config")
				update(null, false)
			}
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

	private fun handleListNotifications(msg: Message): Boolean
	{
		Lw.d(TAG, "handleListNotifications")
		try
		{
			val notifications = activeNotifications
			val `val` = arrayOfNulls<String>(notifications.size())

			var idx = 0
			for (notification in notifications)
			{
				Lw.d(TAG, "Sending info about notification " + notification)
				`val`[idx++] = notification.packageName
			}

			reply(msg, Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, `val`))
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception, have no permissions!")
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0))
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

	override fun onCreate()
	{
		super.onCreate()
		Lw.d(TAG, "onCreate()")

		Lw.d(TAG, "AlarmReceiver")
		alarm = Alarm()

		Lw.d(TAG, "Settings")
		settings = Settings(this)
	}

	override fun onDestroy()
	{
		Lw.d(TAG, "onDestroy (??)")
		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder?
	{
		if (intent.getBooleanExtra(configServiceExtra, false))
			return messenger.binder

		return super.onBind(intent)
	}

	fun getTitleAndText(ntf: Notification): Pair<String,String>
	{
		var extras = ntf.extras;

		var title : String = "";
		var text : String = "";

		if (extras != null)
		{
			if ((extras.get(Notification.EXTRA_TITLE) == null && extras.get(Notification.EXTRA_TITLE_BIG) == null)
				|| (extras.get(Notification.EXTRA_TEXT) == null && extras.get(Notification.EXTRA_TEXT_LINES) == null))
			{
			}
			else
			{
				if (extras.get(Notification.EXTRA_TITLE_BIG) != null)
				{
					var bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG) as CharSequence;
					if (bigTitle.length < 40 || extras.get(Notification.EXTRA_TITLE) == null)
						title = bigTitle.toString();
					else
						title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
				}
				else
					title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

				if (extras.get(Notification.EXTRA_TEXT_LINES) != null)
				{
					for (line in extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
					{
						text += line;
						text += "\n\n";
					}
					text = text.trim();
				}
				else
				{
					text = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
				}
			}
		}

		return Pair(title,text)
	}

	fun postNotification(context: Context, ntf: Notification)
	{
		var (title, text) = getTitleAndText(ntf);

		if (title != null && text != null)
		{
			val nextId = ++ nextNotificationId;

			val notification =
				Notification
					.Builder(context)
					.setContentTitle(title)
					.setContentText(text)
					.setSmallIcon(R.drawable.ic_launcher)
					.setPriority(Notification.PRIORITY_HIGH)
					.setContentIntent(ntf.contentIntent)
					.setAutoCancel(true)
					.build()

			var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.notify(NOTIFICATION_TAG, nextId, notification)
		}
	}

	private fun update(notification: StatusBarNotification?, added: Boolean)
	{
		Lw.d(TAG, "update")

		if (notification != null)
		{
			if (added)
			{
				Lw.d(TAG, "Checking notification" + notification)
				val packageName = notification.packageName

				Lw.d(TAG, "Package name is " + packageName)

				if (packageName in handledPackages)
				{
					Lw.d(TAG, "YEP, this is a reminder from the calendar")
					postNotification(this, notification.getNotification());
					cancelNotification(notification.key);
				}
			}
		}
	}

	override fun onNotificationPosted(arg0: StatusBarNotification)
	{
		Lw.d(TAG, "Notification posted: " + arg0)
		update(arg0, true)
	}

	override fun onNotificationRemoved(arg0: StatusBarNotification)
	{
		Lw.d(TAG, "Notification removed: " + arg0)
		update(arg0, false)
	}

	companion object
	{
		val TAG = "Service"

		val configServiceExtra = "configService"

		val MSG_CHECK_PERMISSIONS = 1
		val MSG_NO_PERMISSIONS = 2
		val MSG_LIST_NOTIFICATIONS = 3
		val MSG_RELOAD_SETTINGS = 4
		val MSG_TOGGLE_MUTE = 6

		val NOTIFICATION_TAG = "com.github.quarck.stickycal.ForwardedNotificationTag"

		var nextNotificationId = Consts.notificationIdDynamicFrom

	}
}
