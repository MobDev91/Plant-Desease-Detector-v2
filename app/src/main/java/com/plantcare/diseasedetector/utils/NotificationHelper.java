package com.plantcare.diseasedetector.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.plantcare.diseasedetector.MainActivity;
import com.plantcare.diseasedetector.R;

import java.util.Calendar;

/**
 * Helper class for managing notifications and reminders
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // Notification Channel IDs
    public static final String CHANNEL_REMINDERS = "plant_care_reminders";
    public static final String CHANNEL_DISEASE_ALERTS = "disease_alerts";
    public static final String CHANNEL_GENERAL = "general_notifications";

    // Notification IDs
    public static final int NOTIFICATION_WATERING_REMINDER = 1001;
    public static final int NOTIFICATION_FERTILIZER_REMINDER = 1002;
    public static final int NOTIFICATION_PEST_CHECK_REMINDER = 1003;
    public static final int NOTIFICATION_DISEASE_ALERT = 2001;
    public static final int NOTIFICATION_BACKUP_COMPLETE = 3001;

    private Context context;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannels();
    }

    /**
     * Create notification channels for Android O and above
     */
    public void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Plant Care Reminders Channel
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Plant Care Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            remindersChannel.setDescription("Reminders for watering, fertilizing, and plant care tasks");
            remindersChannel.enableVibration(true);
            remindersChannel.setShowBadge(true);

            // Disease Alerts Channel
            NotificationChannel alertsChannel = new NotificationChannel(
                    CHANNEL_DISEASE_ALERTS,
                    "Disease Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Urgent alerts about plant diseases and health issues");
            alertsChannel.enableVibration(true);
            alertsChannel.enableLights(true);
            alertsChannel.setShowBadge(true);

            // General Notifications Channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_LOW
            );
            generalChannel.setDescription("General app notifications and updates");
            generalChannel.setShowBadge(false);

            // Register channels
            notificationManager.createNotificationChannel(remindersChannel);
            notificationManager.createNotificationChannel(alertsChannel);
            notificationManager.createNotificationChannel(generalChannel);

            Log.d(TAG, "Notification channels created");
        }
    }

    /**
     * Schedule a watering reminder
     */
    public void scheduleWateringReminder(String plantName, int hourOfDay, int minute) {
        scheduleRepeatingReminder(
                NOTIFICATION_WATERING_REMINDER,
                "Time to Water Your Plants! 💧",
                "Don't forget to water your " + plantName + ". Keep them healthy and hydrated!",
                hourOfDay,
                minute,
                AlarmManager.INTERVAL_DAY
        );
    }

    /**
     * Schedule a fertilizer reminder
     */
    public void scheduleFertilizerReminder(String plantName, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, 9); // 9 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If the date has passed this month, schedule for next month
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1);
        }

        scheduleNotification(
                NOTIFICATION_FERTILIZER_REMINDER,
                "Fertilizer Time! 🌱",
                "Time to fertilize your " + plantName + ". Give them the nutrients they need!",
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 30 // Monthly
        );
    }

    /**
     * Schedule a pest check reminder
     */
    public void schedulePestCheckReminder(int hourOfDay, int minute) {
        scheduleRepeatingReminder(
                NOTIFICATION_PEST_CHECK_REMINDER,
                "Weekly Pest Check 🔍",
                "Time for your weekly plant inspection. Look for signs of pests or diseases!",
                hourOfDay,
                minute,
                AlarmManager.INTERVAL_DAY * 7 // Weekly
        );
    }

    /**
     * Send disease alert notification
     */
    public void sendDiseaseAlert(String diseaseName, String plantName, float confidence) {
        String title = "Disease Detected! ⚠️";
        String message = String.format("Possible %s detected in %s (%.1f%% confidence). Take action immediately!",
                diseaseName, plantName, confidence * 100);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DISEASE_ALERTS)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_info, "View Details", pendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_DISEASE_ALERT, builder.build());
        Log.d(TAG, "Disease alert notification sent");
    }

    /**
     * Send backup completion notification
     */
    public void sendBackupCompleteNotification(boolean success, String message) {
        String title = success ? "Backup Complete ✅" : "Backup Failed ❌";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                .setSmallIcon(success ? R.drawable.ic_health_check : R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_BACKUP_COMPLETE, builder.build());
    }

    /**
     * Schedule a repeating reminder
     */
    private void scheduleRepeatingReminder(int notificationId, String title, String message,
                                           int hourOfDay, int minute, long repeatInterval) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleNotification(notificationId, title, message, calendar.getTimeInMillis(), repeatInterval);
    }

    /**
     * Schedule a notification
     */
    private void scheduleNotification(int notificationId, String title, String message,
                                      long triggerTime, long repeatInterval) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (repeatInterval > 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, repeatInterval, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, "Notification scheduled: " + title + " at " + triggerTime);
    }

    /**
     * Cancel a scheduled notification
     */
    public void cancelNotification(int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
        notificationManager.cancel(notificationId);

        Log.d(TAG, "Notification cancelled: " + notificationId);
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        cancelNotification(NOTIFICATION_WATERING_REMINDER);
        cancelNotification(NOTIFICATION_FERTILIZER_REMINDER);
        cancelNotification(NOTIFICATION_PEST_CHECK_REMINDER);
        notificationManager.cancelAll();
        Log.d(TAG, "All notifications cancelled");
    }

    /**
     * Send a general notification
     */
    public void sendGeneralNotification(String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.ic_plant_care)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * BroadcastReceiver for handling scheduled notifications
     */
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra("notification_id", 0);
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");

            if (title != null && message != null) {
                showScheduledNotification(context, notificationId, title, message);
            }
        }

        private void showScheduledNotification(Context context, int notificationId, String title, String message) {
            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String channelId = getChannelForNotification(notificationId);
            int iconResource = getIconForNotification(notificationId);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(iconResource)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // Add action buttons based on notification type
            if (notificationId == NOTIFICATION_WATERING_REMINDER) {
                Intent doneIntent = new Intent(context, NotificationActionReceiver.class);
                doneIntent.setAction("WATERING_DONE");
                PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                        context, 0, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(R.drawable.ic_health_check, "Mark Done", donePendingIntent);
            }

            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        }

        private String getChannelForNotification(int notificationId) {
            switch (notificationId) {
                case NOTIFICATION_DISEASE_ALERT:
                    return CHANNEL_DISEASE_ALERTS;
                case NOTIFICATION_WATERING_REMINDER:
                case NOTIFICATION_FERTILIZER_REMINDER:
                case NOTIFICATION_PEST_CHECK_REMINDER:
                    return CHANNEL_REMINDERS;
                default:
                    return CHANNEL_GENERAL;
            }
        }

        private int getIconForNotification(int notificationId) {
            switch (notificationId) {
                case NOTIFICATION_WATERING_REMINDER:
                    return R.drawable.ic_plant_care;
                case NOTIFICATION_FERTILIZER_REMINDER:
                    return R.drawable.ic_health_check;
                case NOTIFICATION_PEST_CHECK_REMINDER:
                    return R.drawable.ic_search;
                case NOTIFICATION_DISEASE_ALERT:
                    return R.drawable.ic_warning;
                default:
                    return R.drawable.ic_plant_care;
            }
        }
    }

    /**
     * BroadcastReceiver for handling notification actions
     */
    public static class NotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("WATERING_DONE".equals(action)) {
                // Handle watering completion
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_WATERING_REMINDER);

                // Send confirmation notification
                NotificationHelper helper = new NotificationHelper(context);
                helper.sendGeneralNotification("Great Job! 🌱", "Watering task completed. Your plants thank you!");

                Log.d(TAG, "Watering task marked as complete");
            }
        }
    }

    /**
     * Create a plant care reminder schedule
     */
    public void createPlantCareSchedule(String plantType) {
        // Schedule different reminders based on plant type
        switch (plantType.toLowerCase()) {
            case "tomato":
                scheduleWateringReminder("tomatoes", 7, 0); // 7 AM daily
                schedulePestCheckReminder(10, 0); // 10 AM weekly
                break;
            case "apple":
                scheduleWateringReminder("apple trees", 6, 30); // 6:30 AM
                scheduleFertilizerReminder("apple trees", 1); // 1st of each month
                break;
            default:
                scheduleWateringReminder("plants", 8, 0); // 8 AM daily
                schedulePestCheckReminder(9, 0); // 9 AM weekly
                break;
        }
    }

    /**
     * Send plant health tips notification
     */
    public void sendPlantHealthTip() {
        String[] tips = {
                "💡 Tip: Check soil moisture before watering. Stick your finger 1-2 inches into the soil.",
                "💡 Tip: Rotate your plants weekly to ensure even growth and light exposure.",
                "💡 Tip: Remove dead or yellowing leaves to prevent disease spread.",
                "💡 Tip: Group plants with similar water needs together for easier care.",
                "💡 Tip: Use lukewarm water when watering - cold water can shock plant roots."
        };

        String randomTip = tips[(int) (Math.random() * tips.length)];
        sendGeneralNotification("Plant Care Tip", randomTip);
    }

    /**
     * Schedule weekly plant health tips
     */
    public void scheduleWeeklyTips() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If Sunday has passed this week, schedule for next week
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, WeeklyTipReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 9999, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    /**
     * BroadcastReceiver for weekly tips
     */
    public static class WeeklyTipReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationHelper helper = new NotificationHelper(context);
            helper.sendPlantHealthTip();
        }
    }
}