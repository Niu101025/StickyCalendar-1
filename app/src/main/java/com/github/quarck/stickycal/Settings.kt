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

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.preference.PreferenceManager

fun SharedPreferences?.setBoolean(key: String, value: Boolean)
{
	if (this != null)
	{
		val editor = this.edit()
		editor.putBoolean(key, value)
		editor.commit()
	}
}
data class NotificationSettingsSnapshot(
	val showDiscardButton: Boolean,
	val ringtoneUri: Uri?,
	val vibraOn: Boolean )

class Settings(ctx: Context) {
	private var context: Context? = null

	private var prefs: SharedPreferences? = null

	var isServiceEnabled: Boolean
		get() = prefs!!.getBoolean(IS_ENABLED_KEY, false)
		set(value) = prefs.setBoolean(IS_ENABLED_KEY, value)

	var removeOriginal: Boolean
		get() = prefs!!.getBoolean(REMOVE_ORIGINAL_KEY, false)
		set(value) = prefs.setBoolean(REMOVE_ORIGINAL_KEY, value)

	var showDiscardButton: Boolean
		get() = prefs!!.getBoolean(IS_DISCARD_ENABLED_KEY, false)
		set(value) = prefs.setBoolean(IS_DISCARD_ENABLED_KEY, value)

	var showSnoozeButton: Boolean
		get() = prefs!!.getBoolean(IS_SNOOZE_ENABLED_KEY, false)
		set(value) = prefs.setBoolean(IS_SNOOZE_ENABLED_KEY, value)

	var delayNotificationSwap: Boolean
		get() = prefs!!.getBoolean(DELAY_NOTIFICATION_SWAP_KEY, false)
		set(value) = prefs.setBoolean(DELAY_NOTIFICATION_SWAP_KEY, value)

	var playReminderSound: Boolean
		get() = prefs!!.getBoolean(PLAY_REMINDER_SOUND_KEY, false)
		set(value) = prefs.setBoolean(PLAY_REMINDER_SOUND_KEY, value)

	var vibraOn: Boolean
		get() = prefs!!.getBoolean(VIBRA_KEY, false)
		set(value) = prefs.setBoolean(VIBRA_KEY, value)

	var forwardToPebble: Boolean
		get() = prefs!!.getBoolean(FORWARD_TO_PEBBLE_KEY, false)
		set(value) = prefs.setBoolean(FORWARD_TO_PEBBLE_KEY, value)

	val ringtoneURI: Uri?
		get() {
			var notification: Uri? = null

			if (playReminderSound) {
				try {
					val uriValue = prefs!!.getString(RINGTONE_KEY, "")

					if (uriValue != null && !uriValue.isEmpty())
						notification = Uri.parse(uriValue)
				}
				catch (e: Exception) {
					e.printStackTrace()
				}
				finally {
					if (notification == null)
						notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
				}
			}

			return notification
		}

	val notificationSettingsSnapshot: NotificationSettingsSnapshot
		get() = NotificationSettingsSnapshot(showDiscardButton, ringtoneURI, vibraOn)

	init
	{
		context = ctx
		prefs = PreferenceManager.getDefaultSharedPreferences(context)
	}

	companion object
	{
		private val IS_ENABLED_KEY = "pref_key_is_enabled"
		private val REMOVE_ORIGINAL_KEY = "remove_original"
		private val IS_DISCARD_ENABLED_KEY = "pref_key_enable_discard_button"
		private val IS_SNOOZE_ENABLED_KEY = "pref_key_enable_snooze_button"
		private val DELAY_NOTIFICATION_SWAP_KEY = "delay_notification_swap"

		private val PLAY_REMINDER_SOUND_KEY = "play_reminder_sound"
		private val RINGTONE_KEY = "pref_key_ringtone"

		private val VIBRA_KEY = "vibra_on"

		private val FORWARD_TO_PEBBLE_KEY = "forward_to_pebble"
	}

}
