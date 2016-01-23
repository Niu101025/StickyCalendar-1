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
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.CalendarContract

class NotificationViewManager
{
	public fun postNotification(ctx: Context, notification: DBNotification, notificationSettings: NotificationSettingsSnapshot, pendingIntent: PendingIntent?)
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

		var builder = Notification
			.Builder(ctx)
			.setContentTitle(notification.title)
			.setContentText(notification.text)
			.setSmallIcon(R.drawable.stat_notify_calendar)
			.setPriority(Notification.PRIORITY_HIGH)
			.setContentIntent(pIndent)
			.setAutoCancel(!notificationSettings.showDiscardButton)

		if (notificationSettings.showDiscardButton)
		{
			var discardIntent = Intent(ctx, DiscardNotificationService::class.java)
			discardIntent.putExtra(Consts.INTENT_NOTIFICATION_ID_KEY, nextId);
			discardIntent.putExtra(Consts.INTENT_EVENT_ID_KEY, notification.eventId);
			var pDiscardIndent = PendingIntent.getService(ctx, nextId, discardIntent, PendingIntent.FLAG_CANCEL_CURRENT)

			Lw.d("NotificationViewManager: adding pending intent for discard, event id ${notification.eventId}, notificationId ${nextId}")

			var discard = ctx.getString(R.string.discard)
			if (discard == null)
				discard = "DISCARD"

			builder = builder.addAction(
				android.R.drawable.ic_menu_close_clear_cancel,
				discard,
				pDiscardIndent)
		}

		if (notificationSettings.ringtoneUri != null)
		{
			builder = builder.setSound(notificationSettings.ringtoneUri)
		}

		if (notificationSettings.vibraOn)
		{
			builder = builder.setVibrate(longArrayOf(800));
		}

		var newNotification = builder.build()

		notificationManager.notify(
			"${Consts.NOTIFICATION_TAG};${notification.eventId}", nextId, builder.build())
	}

	fun removeNotification(ctx: Context, eventId: Long, notificationId: Int)
	{
		var notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel("${Consts.NOTIFICATION_TAG};${eventId}", notificationId);
	}

	public fun onNotificationRemoved(eventId: Long)
	{
		if (tracker != null)
		{
			tracker!!.deleteByEventId(eventId)
		}
	}

	fun onAccessToNotificationsLost(context: Context)
	{
		val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
		val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

		val notification =
			Notification
				.Builder(context)
				.setContentTitle(context.getString(R.string.access_lost))
				.setContentText(context.getString(R.string.reenable_app))
				.setSmallIcon(R.drawable.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true)
				.build()

		var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(Consts.NOTIFICATION_ID_UPDATED_NEED_PERMISSIONS, notification) // would update if already exists
	}

	fun onNotificationParseError(context: Context, intent: PendingIntent)
	{
		val notification =
			Notification
				.Builder(context)
				.setContentTitle(context.getString(R.string.sticky_cal_error_title))
				.setContentText(context.getString(R.string.sticky_cal_error))
				.setSmallIcon(R.drawable.stat_notify_calendar)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentIntent(intent)
				.setAutoCancel(true)
				.build()

		var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify("${Consts.NOTIFICATION_TAG};", Consts.NOTIFICATION_ID_ERROR, notification)
	}

	companion object
	{
		private var tracker: NotificationIdTracker? = null
	}

}