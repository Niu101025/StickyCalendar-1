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
import java.util.LinkedList

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.CalendarContract

class SavedNotifications(context: Context)
	: SQLiteOpenHelper(context, SavedNotifications.DATABASE_NAME, null, SavedNotifications.DATABASE_VERSION)
{
	data class DBNotification(var eventId: Long, var title: String, var text: String)

	private val COLUMNS = arrayOf<String>(KEY_TITLE, KEY_TEXT, KEY_EVENTID)

	override fun onCreate(db: SQLiteDatabase)
	{
		var CREATE_PKG_TABLE =
			"CREATE TABLE $TABLE_NAME ( $KEY_EVENTID INTEGER PRIMARY KEY, $KEY_TITLE TEXT, $KEY_TEXT TEXT )"

		Lw.d(TAG, "Creating DB TABLE using query: " + CREATE_PKG_TABLE)

		db.execSQL(CREATE_PKG_TABLE)

		val CREATE_INDEX = "CREATE UNIQUE INDEX $INDEX_NAME ON $TABLE_NAME ($KEY_EVENTID)"

		Lw.d(TAG, "Creating DB INDEX using query: " + CREATE_INDEX)

		db.execSQL(CREATE_INDEX)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
	{
		Lw.d(TAG, "DROPPING table and index")
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
		db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME)
		this.onCreate(db)
	}

	fun addNotification(notification: DBNotification)
	{
		Lw.d(TAG, "addNotification " + notification.toString())

		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_EVENTID, notification.eventId)
		values.put(KEY_TITLE, notification.title)
		values.put(KEY_TEXT, notification.text)

		db.insert(TABLE_NAME, // table
			null, // nullColumnHack
			values) // key/value -> keys = column names/ values = column
		// values

		db.close()
	}

	fun addNotification(eventId: Long, title: String, text: String)
	{
		addNotification(DBNotification(eventId, title, text))
	}

	val notifications: List<DBNotification>
		get()
		{
			val packages = LinkedList<DBNotification>()

			val query = "SELECT  * FROM " + TABLE_NAME

			val db = this.writableDatabase
			val cursor = db.rawQuery(query, null)

			var pkg: DBNotification? = null
			if (cursor.moveToFirst())
			{
				do
				{
					pkg = DBNotification(
						cursor.getString(0).toLong(),
						cursor.getString(1),
						cursor.getString(2)
						)
					packages.add(pkg)
				} while (cursor.moveToNext())

				cursor.close()
			}

			return packages
		}

	fun deleteNotification(eventId: Long)
	{
		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			KEY_EVENTID + " = ?", // selections
			arrayOf<String>(eventId.toString())) // selections args

		db.close()

		Lw.d(TAG, "deleteNotification ${eventId}")
	}

	fun deleteNotification(ntf: DBNotification)
	{
		deleteNotification(ntf.eventId)
	}

	public fun postAllNotifications(ctx: Context)
	{
		var notifications = this.notifications

		var notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		for (notification in notifications)
		{
			if (notification.eventId !in NotificationReceiverService.notificationIdMap)
			{
				var nextId = ++ nextId;

				var uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, notification.eventId);
				var intent = Intent(Intent.ACTION_VIEW).setData(uri);

				notificationManager.notify(
					"${NotificationReceiverService.NOTIFICATION_TAG};${notification.eventId}",
					nextId,
					Notification
						.Builder(ctx)
						.setContentTitle(notification.title)
						.setContentText(notification.text)
						.setSmallIcon(R.drawable.stat_notify_calendar)
						.setPriority(Notification.PRIORITY_HIGH)
						.setContentIntent(PendingIntent.getActivity(ctx, 0, intent, 0))
						.setAutoCancel(true)
						.build())
			}
		}
	}

	companion object
	{
		private val TAG = "DB"

		private val DATABASE_VERSION = 1

		private val DATABASE_NAME = "Notifications"

		private val TABLE_NAME = "notification"
		private val INDEX_NAME = "eventIdidx"

		private val KEY_EVENTID = "eventId"
		private val KEY_TITLE = "title"
		private val KEY_TEXT = "text"

		private var nextId = 100000000;
	}
}
