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

import java.util.LinkedList

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SavedNotifications(context: Context)
	: SQLiteOpenHelper(context, SavedNotifications.DATABASE_NAME, null, SavedNotifications.DATABASE_VERSION)
{

	data class Notification(var notificationTitle: String, var notificationText: String, var eventId : Long)

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

	fun addNotification(notification: Notification)
	{
		Lw.d(TAG, "addNotification " + notification.toString())

		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_EVENTID, notification.eventId)
		values.put(KEY_TITLE, notification.notificationTitle)
		values.put(KEY_TEXT, notification.notificationText)

		db.insert(TABLE_NAME, // table
			null, // nullColumnHack
			values) // key/value -> keys = column names/ values = column
		// values

		db.close()
	}

	val notifications: List<Notification>
		get()
		{
			val packages = LinkedList<Notification>()

			val query = "SELECT  * FROM " + TABLE_NAME

			val db = this.writableDatabase
			val cursor = db.rawQuery(query, null)

			var pkg: Notification? = null
			if (cursor.moveToFirst())
			{
				do
				{
					pkg = Notification(
						cursor.getString(1),
						cursor.getString(2),
						cursor.getString(0).toLong()
						)
					packages.add(pkg)
				} while (cursor.moveToNext())

				cursor.close()
			}

			return packages
		}

	fun deleteNotification(ntf: Notification)
	{
		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			KEY_EVENTID + " = ?", // selections
			arrayOf<String>(ntf.eventId.toString())) // selections args

		db.close()

		Lw.d(TAG, "deleteNotification " + ntf.toString())
	}

	companion object
	{
		private val TAG = "DB"

		val DEFAULT_REMIND_INTERVAL = 5 * 60

		private val DATABASE_VERSION = 5

		private val DATABASE_NAME = "Notifications"

		private val TABLE_NAME = "notification"
		private val INDEX_NAME = "eventIdidx"

		// private static final String KEY_ID = "id";
		private val KEY_TITLE = "title"
		private val KEY_TEXT = "text"
		private val KEY_EVENTID = "eventId"
	}
}
