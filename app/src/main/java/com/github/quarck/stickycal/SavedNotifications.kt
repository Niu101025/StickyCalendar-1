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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

data class DBNotification(var eventId: Long, var title: String, var text: String)

class SavedNotifications(context: Context)
	: SQLiteOpenHelper(context, SavedNotifications.DATABASE_NAME, null, SavedNotifications.DATABASE_VERSION)
{
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

		if (oldVersion != newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
			db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME)
			this.onCreate(db)
		}
	}

	fun addNotification(notification: DBNotification)
	{
		Lw.d(TAG, "addNotification " + notification.toString())

		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_EVENTID, notification.eventId)
		values.put(KEY_TITLE, notification.title)
		values.put(KEY_TEXT, notification.text)

		try {
			db.insertOrThrow(TABLE_NAME, // table
				null, // nullColumnHack
				values) // key/value -> keys = column names/ values = column
			// values
		}
		catch (ex: android.database.sqlite.SQLiteConstraintException)
		{
			Lw.d(TAG, "This entry (${notification.eventId}) is already in the DB, updating!")

			val values = ContentValues()
			values.put(KEY_TITLE, notification.title)
			values.put(KEY_TEXT, notification.text)

			db.update(TABLE_NAME, // table
				values, // column/value
				KEY_EVENTID + " = ?", // selections
				arrayOf<String>(notification.eventId.toString())) // selection args
		}

		db.close()
	}

	fun updateNotification(notification: DBNotification)
	{
		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_TITLE, notification.title)
		values.put(KEY_TEXT, notification.text)

		db.update(TABLE_NAME, // table
			values, // column/value
			KEY_EVENTID + " = ?", // selections
			arrayOf<String>(notification.eventId.toString())) // selection args

		db.close()
	}

	fun addNotification(eventId: Long, title: String, text: String): DBNotification
	{
		var ret = DBNotification(eventId, title, text)
		addNotification(ret)
		return ret
	}

	fun getNotification(eventId: Long): DBNotification?
	{
		val db = this.readableDatabase

		val cursor = db.query(TABLE_NAME, // a. table
			COLUMNS, // b. column names
			" $KEY_EVENTID = ?", // c. selections
			arrayOf<String>(eventId.toString()), // d. selections args
			null, // e. group by
			null, // f. h aving
			null, // g. order by
			null) // h. limit

		var notification: DBNotification? = null

		if (cursor != null && cursor.count >= 1)
		{
			cursor.moveToFirst()
			notification = DBNotification(cursor.getString(0).toLong(), cursor.getString(1), cursor.getString(2))
		}

		cursor?.close()

		return notification
	}

	val notifications: List<DBNotification>
		get()
		{
			val ret = LinkedList<DBNotification>()

			val query = "SELECT  * FROM " + TABLE_NAME

			val db = this.readableDatabase
			val cursor = db.rawQuery(query, null)

			var ntfy: DBNotification? = null
			if (cursor.moveToFirst())
			{
				do
				{
					ntfy = DBNotification(
						cursor.getString(0).toLong(),
						cursor.getString(1),
						cursor.getString(2)
						)
					ret.add(ntfy)
				} while (cursor.moveToNext())

				cursor.close()
			}

			return ret
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


		private val COLUMNS = arrayOf<String>(KEY_EVENTID, KEY_TITLE, KEY_TEXT)
	}
}
