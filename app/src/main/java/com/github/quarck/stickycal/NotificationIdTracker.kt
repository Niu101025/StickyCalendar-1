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

class NotificationIdTracker(context: Context)
: SQLiteOpenHelper(context, NotificationIdTracker.DATABASE_NAME, null, NotificationIdTracker.DATABASE_VERSION)
{
	data class Entry(var eventId: Long, var notificationId: Int)

	override fun onCreate(db: SQLiteDatabase)
	{
		var CREATE_PKG_TABLE =
			"CREATE TABLE $TABLE_NAME ( $KEY_EVENTID INTEGER PRIMARY KEY, $KEY_NOTIFICATIONID INTEGER )"

		Lw.d(TAG, "Creating DB TABLE using query: " + CREATE_PKG_TABLE)

		db.execSQL(CREATE_PKG_TABLE)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
	{
		Lw.d(TAG, "DROPPING table and index")

		if (oldVersion != newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
			this.onCreate(db)
		}
	}

	fun dropAll()
	{
		Lw.d(TAG, "dropAll")

		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			null, // selections
			null) // selections args

		db.close()
	}

	private fun addEntry(entry: Entry)
	{
		Lw.d(TAG, "addEntry " + entry.toString())

		val db = this.writableDatabase

		val values = ContentValues()
		values.put(KEY_EVENTID, entry.eventId)
		values.put(KEY_NOTIFICATIONID, entry.notificationId)

		try {
			db.insertOrThrow(TABLE_NAME, null, values)
		}
		catch (ex: Exception)
		{
			val values = ContentValues()
			values.put(KEY_NOTIFICATIONID, entry.notificationId)

			db.update(TABLE_NAME, // table
				values, // column/value
				KEY_EVENTID + " = ?", // selections
				arrayOf<String>(entry.eventId.toString())) // selection args
		}

		db.close()
	}

	private fun getEntryByEventId(eventId: Long): Entry?
	{
		val db = this.readableDatabase

		Lw.d(TAG, "getNotificationIdForEventId ${eventId}")

		val cursor = db.query(
			TABLE_NAME, // a. table
			arrayOf<String>(KEY_EVENTID, KEY_NOTIFICATIONID),
			" $KEY_EVENTID = ?", // c. selections
			arrayOf<String>(eventId.toString()), // d. selections args
			null, // e. group by
			null, // f. h aving
			null, // g. order by
			null) // h. limit

		var ret: Entry? = null

		if (cursor != null && cursor.count >= 1)
		{
			cursor.moveToFirst()

			ret = Entry(
				cursor.getString(0).toLong(),
				cursor.getString(1).toInt()
			)
		}

		cursor?.close()

		return ret
	}


	private fun nextNotificationId(): Int
	{
		var ret = 0;

		val db = this.writableDatabase

		Lw.d(TAG, "nextNotificationId")

		val query = "SELECT MAX($KEY_NOTIFICATIONID) FROM " + TABLE_NAME

		val cursor = db.rawQuery(query, null)

		if (cursor.moveToFirst())
		{
			try
			{
				ret = cursor.getString(0).toInt() + 1
			}
			catch (ex: Exception)
			{
				ret = 0;
			}

			cursor.close()
		}

		if (ret == 0)
			ret = Consts.NOTIFICATION_ID_DYNAMIC_FROM;

		return ret
	}

	// Would return existing notification id if it is in the database, otherwise
	// would create a new one
	fun getNotificationIdForEventId(eventId: Long) : Int
	{
		var entry = getEntryByEventId(eventId)

		if (entry == null)
		{
			entry = Entry(eventId, nextNotificationId())
			addEntry(entry)
		}

		Lw.d(TAG, "getNotificationIdForEventId: returning ${entry.notificationId} for event ${eventId}")

		return entry.notificationId;
	}

	val entries: List<Entry>
		get()
		{
			val ret = LinkedList<Entry>()

			val query = "SELECT * FROM " + TABLE_NAME

			val db = this.writableDatabase
			val cursor = db.rawQuery(query, null)

			var ntfy: Entry? = null
			if (cursor.moveToFirst())
			{
				do
				{
					ntfy = Entry(
						cursor.getString(0).toLong(),
						cursor.getString(1).toInt()
					)
					ret.add(ntfy)
				} while (cursor.moveToNext())

				cursor.close()
			}

			return ret
		}

	fun deleteByEventId(eventId: Long)
	{
		Lw.d(TAG, "deleteByEventId ${eventId}")

		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			KEY_EVENTID + " = ?", // selections
			arrayOf<String>(eventId.toString())) // selections args

		db.close()
	}

	fun deleteByNotificationId(notificationId: Int)
	{
		Lw.d(TAG, "deleteByNotificationId ${notificationId}")

		val db = this.writableDatabase

		db.delete(TABLE_NAME, // table name
			KEY_NOTIFICATIONID + " = ?", // selections
			arrayOf<String>(notificationId.toString())) // selections args

		db.close()
	}

	companion object
	{
		private val TAG = "DB"

		private val DATABASE_VERSION = 1

		private val DATABASE_NAME = "NotificationIds"

		private val TABLE_NAME = "idmap"

		private val KEY_EVENTID = "eventId"
		private val KEY_NOTIFICATIONID = "nId"
	}
}
