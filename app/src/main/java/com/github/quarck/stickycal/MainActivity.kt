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

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton

class MainActivity : Activity()
{
	private var serviceClient: ServiceClient? = null

	private var settings: Settings? = null

	private var toggleButtonEnableService : ToggleButton? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Lw.d("main activity created")

		Lw.d(TAG, "onCreateView")

		settings = Settings(this)

		setContentView(R.layout.activity_main)

		toggleButtonEnableService = findViewById(R.id.toggleButtonEnableService) as ToggleButton
		toggleButtonEnableService!!.isChecked = settings!!.isServiceEnabled
	}

	public fun OnBtnEnableServiceClick(v: View)
	{
		Lw.d("saveSettingsOnClickListener.onClick()")

		saveSettings()

		(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
			.cancel(Consts.NOTIFICATION_ID_UPDATED)

		serviceClient!!.checkPermissions()
	}

	public fun OnTextViewWhyClick(v: View) = showRationale(R.string.rationale)

	public fun OnTextViewWhy2Click(v: View) = showRationale(R.string.rationale2)

	public fun OnTextViewCreditsClick(v: View) = startActivity(Intent.parseUri(imageCreditUri, 0))

	public fun OnTextViewKotlinClick(v: View) = startActivity(Intent.parseUri(kotlinUri, 0))

	public fun OnButtonTestClick(v: View)
	{
		var db = SavedNotifications(this)

		var dbNotification = db.addNotification(232323232, "Test Notification", "This is a test notificaiton")

		NotificationViewManager().postNotification(
			this,
			dbNotification,
			settings!!.showDiscardButton,
			settings!!.ringtoneURI,
			null
		)
	}

	private fun showRationale(textId: Int)
	{
		val builder = AlertDialog.Builder(this)
		builder
			.setMessage(textId)
			.setCancelable(false)
			.setPositiveButton("OK", {x, y -> })
		builder.create().show()
	}


	private fun onNoPermissions()
	{
		Lw.d(TAG, "onNoPermissions()!!!")

		toggleButtonEnableService!!.isChecked = false
		settings!!.isServiceEnabled = false

		val builder = AlertDialog.Builder(this)
		builder
			.setMessage(R.string.application_has_no_access)
			.setCancelable(false)
			.setPositiveButton(R.string.open_settings) {
				x, y ->
					val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
					startActivity(intent)
				}
			.setNegativeButton(R.string.cancel) {
				DialogInterface, Int -> finish()
			}

		// Create the AlertDialog object and return it
		builder.create().show()
	}

	private fun saveSettings()
	{
		Lw.d(TAG, "Saving current settings")

		settings!!.isServiceEnabled = toggleButtonEnableService!!.isChecked
		settings!!.removeOriginal = true
	}

	public override fun onStart()
	{
		Lw.d(TAG, "onStart()")
		super.onStart()

		serviceClient = ServiceClient({onNoPermissions()})

		if (serviceClient != null)
		{
			Lw.d(TAG, "binding service")
			serviceClient!!.bindService(applicationContext)
		}
		else
		{
			Lw.d(TAG, "onStart(): failed to create ServiceClient()")
		}

		postCachedNotifications(applicationContext)
	}

	public override fun onStop()
	{
		Lw.d(TAG, "onStop()")
		serviceClient!!.unbindService(applicationContext)
		super.onStop()
	}

	public override fun onPause()
	{
		Lw.d(TAG, "onPause")
		super.onPause()
	}

	public override fun onResume()
	{
		Lw.d(TAG, "onResume")

		super.onResume()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean
	{
		menuInflater.inflate(R.menu.main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		val id = item.itemId
		if (id == R.id.action_settings)
		{
			val intent = Intent(this, SettingsActivity::class.java)
			startActivity(intent)
		}
		return super.onOptionsItemSelected(item)
	}

	companion object
	{
		private val TAG = "MainActivity"
		var imageCreditUri = "http://cornmanthe3rd.deviantart.com/"
		var kotlinUri = "https://kotlinlang.org/"
	}
}
