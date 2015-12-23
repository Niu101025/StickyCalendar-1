package com.github.quarck.stickycal

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent

/**
 * Created by quarck on 23/12/15.
 */

fun Notification.getTitleAndText(): Pair<String, String>
{
	var extras = this.extras;

	var title: String = "";
	var text: String = "";

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

	if (title == "" )
		title = "ERORR: Can't get title"

	return Pair(title, text)
}

fun PendingIntent.getIntent(): Intent
{
	try
	{
		var getIntent = PendingIntent::class.java.getDeclaredMethod("getIntent");
		return getIntent.invoke(this) as Intent;
	}
	catch (e: Exception)
	{
		throw IllegalStateException(e);
	}
}

fun Notification.getGooleCalendarEventId(): Long?
{
	var ret: Long? = null;

	try
	{
		var originalIntent = this.contentIntent.getIntent()

		Lw.d(NotificationReceiverService.TAG, "Got original intent: url=${originalIntent.toUri(Intent.URI_INTENT_SCHEME)}")

		ret =
			originalIntent
				.toUri(Intent.URI_INTENT_SCHEME)
				.split(';')
				.filter { x -> x.contains("l.eventid=") }
				.first()
				.split("l.eventid=")
				.last()
				.toLong()

	}
	catch (ex: Exception)
	{
		ret = null
	}

	return ret;
}
