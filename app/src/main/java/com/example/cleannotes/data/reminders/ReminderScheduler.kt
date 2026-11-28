package com.example.cleannotes.data.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cleannotes.MainActivity
import com.example.cleannotes.R
import com.example.cleannotes.model.ReminderConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ReminderScheduler {
	private fun prefs(context: Context) =
		context.getSharedPreferences("scheduled_reminders", Context.MODE_PRIVATE)

	private fun alarm(context: Context, id: Int, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT) =
		PendingIntent.getBroadcast(
			context,
			id,
			Intent(context, ReminderReceiver::class.java).setAction("fire").putExtra("id", id),
			flags or PendingIntent.FLAG_IMMUTABLE,
		)

	fun exactAllowed(context: Context) =
		Build.VERSION.SDK_INT < 31 ||
				context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

	@android.annotation.SuppressLint("ApplySharedPref")
	fun update(context: Context, id: Int, title: String, config: ReminderConfig?) {
		if (
			config != null &&
			prefs(context)
				.getString(
					"config_$id",
					null,
				) == Json.encodeToString(config) &&
			prefs(context).getString("title_$id", null) == title
		)
			return
		cancel(context, id)
		if (config == null) return
		ensureChannel(context, config)
		prefs(context)
			.edit()
			.putString("config_$id", Json.encodeToString(config))
			.putString("title_$id", title)
			.commit()
		val at =
			if (config.at > System.currentTimeMillis()) config.at
			else config.nextAfter(System.currentTimeMillis()) ?: return
		schedule(context, id, at)
	}

	fun cancel(context: Context, id: Int) = cancel(context, listOf(id))

	// Called on IO; durable metadata must be removed before a future broadcast can restore it.
	@android.annotation.SuppressLint("ApplySharedPref")
	fun cancel(context: Context, ids: Collection<Int>) {
		if (ids.isEmpty()) return
		val editor = prefs(context).edit()
		ids.forEach { editor.remove("config_$it").remove("title_$it") }
		editor.commit()
		val manager = context.getSystemService(AlarmManager::class.java)
		val notifications = NotificationManagerCompat.from(context)
		ids.forEach { id ->
			alarm(context, id, PendingIntent.FLAG_NO_CREATE)?.let { manager.cancel(it) }
			notifications.cancel(id)
		}
	}

	private fun schedule(context: Context, id: Int, at: Long) {
		val manager = context.getSystemService(AlarmManager::class.java)
		try {
			if (exactAllowed(context))
				manager.setExactAndAllowWhileIdle(
					AlarmManager.RTC_WAKEUP,
					at,
					alarm(context, id),
				)
			else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(context, id))
		} catch (_: SecurityException) {
			manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(context, id))
		}
	}

	fun restore(context: Context) {
		prefs(context)
			.all
			.keys
			.filter { it.startsWith("config_") }
			.forEach { key ->
				val id = key.removePrefix("config_").toIntOrNull() ?: return@forEach
				val config = config(context, id) ?: return@forEach
				val at =
					if (config.at > System.currentTimeMillis()) config.at
					else config.nextAfter(System.currentTimeMillis()) ?: return@forEach
				schedule(context, id, at)
			}
	}

	private fun config(context: Context, id: Int): ReminderConfig? {
		val stored = prefs(context).getString("config_$id", null) ?: return null
		return runCatching { Json.decodeFromString<ReminderConfig>(stored) }.getOrNull()
	}

	fun ensureChannel(context: Context, config: ReminderConfig): String {
		val channel = "reminder_${config.sound}_${config.vibrate}_${config.highPriority}"
		if (Build.VERSION.SDK_INT >= 26) {
			val c =
				NotificationChannel(
					channel,
					"Recordatorios · ${if (config.sound) "sonido" else "silencio"} · ${if (config.vibrate) "vibración" else "sin vibración"} · ${if (config.highPriority) "prioritarios" else "normales"}",
					if (config.highPriority) NotificationManager.IMPORTANCE_HIGH
					else NotificationManager.IMPORTANCE_DEFAULT,
				)
			c.enableVibration(config.vibrate)
			if (!config.sound) c.setSound(null, null)
			context.getSystemService(NotificationManager::class.java).createNotificationChannel(c)
		}
		return channel
	}

	fun fire(context: Context, id: Int, snooze: Boolean) {
		val config = config(context, id) ?: return
		if (snooze) {
			schedule(
				context,
				id,
				System.currentTimeMillis() + config.snoozeMinutes.coerceIn(1, 180) * 60_000L,
			)
			NotificationManagerCompat.from(context).cancel(id)
			return
		}
		val channel = ensureChannel(context, config)
		val open =
			PendingIntent.getActivity(
				context,
				id,
				Intent(context, MainActivity::class.java)
					.putExtra("noteId", id)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
			)
		val later =
			PendingIntent.getBroadcast(
				context,
				id,
				Intent(context, ReminderReceiver::class.java)
					.setAction("snooze")
					.putExtra("id", id),
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
			)
		val notification =
			NotificationCompat.Builder(context, channel)
				.setSmallIcon(R.drawable.ic_notification)
				.setContentTitle(prefs(context).getString("title_$id", "Recordatorio"))
				.setContentText(config.message)
				.setStyle(NotificationCompat.BigTextStyle().bigText(config.message))
				.setAutoCancel(true)
				.setContentIntent(open)
				.setPriority(
					if (config.highPriority) NotificationCompat.PRIORITY_HIGH
					else NotificationCompat.PRIORITY_DEFAULT
				)
				.setDefaults(
					(if (config.sound) NotificationCompat.DEFAULT_SOUND else 0) or
							(if (config.vibrate) NotificationCompat.DEFAULT_VIBRATE else 0)
				)
				.addAction(0, "Posponer ${config.snoozeMinutes} min", later)
				.build()
		if (
			Build.VERSION.SDK_INT < 33 ||
			context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
			PackageManager.PERMISSION_GRANTED
		) {
			NotificationManagerCompat.from(context).notify(id, notification)
		}
		config.nextAfter(System.currentTimeMillis())?.let { schedule(context, id, it) }
	}
}

class ReminderReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val pending = goAsync()
		Thread {
			try {
				if (
					intent.action in
					listOf(
						Intent.ACTION_BOOT_COMPLETED,
						Intent.ACTION_MY_PACKAGE_REPLACED,
						Intent.ACTION_TIME_CHANGED,
						Intent.ACTION_TIMEZONE_CHANGED,
						AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
					)
				)
					ReminderScheduler.restore(context)
				else
					ReminderScheduler.fire(
						context,
						intent.getIntExtra("id", -1),
						intent.action == "snooze",
					)
			} finally {
				pending.finish()
			}
		}
			.start()
	}
}
