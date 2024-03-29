package com.customlayout.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.facebook.react.bridge.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId


class PushNotificationModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    /**
     * Notification templates
     */
    object Templates {
        val COMMON = 0
        val EVENT = 1
        val WEATHER = 2
        val MR = 3
        val UIN = 4
        val JASKLIK = 5
        val SNR = 6
        val ANNUR = 7
        val ASPEK = 8 
        val DOKUPLUS = 9
        val UNHASPAY = 10
        val JAWARAPAY = 11
        val ZTI = 12
        val PURWOREJO = 13
        val HEBAT = 14
        val BARBEKOE = 15
    }

    /**
     * Default activity meta key from android manifest
     */
    companion object {
        val DEFAULT_ACTIVITY = "com.customlayout.notification.default_activity"
    }


    /**
     * Default push notification broadcast receiver class name
     */
    val DEFAULT_BROADCAST_RECEVIER_CLASSNAME = "com.customlayout.notification.PushNotificationBroadcastReceiver"

    /**
     * Meta key from AndroidManifest.xml for default broadcast receiver classname
     */
    val META_KEY_DEFAULT_BROADCAST_RECEVIER_CLASSNAME = "com.customlayout.notification.default_broadcast_receiver"

    /**
     * Returns module that should be used in React Native
     *
     * Ex(js):
     * import {PushNotificationAndroid} from "@cryptoticket/react-native-push-notification"
     *
     * @return React Native module name
     */
    override fun getName(): String {
        return "PushNotificationAndroid"
    }

    /**
     * Returns constants that can be used in React Native
     *
     * Ex(js):
     * import {PushNotificationAndroid} from "@cryptoticket/react-native-push-notification"
     * console.log(PushNotificationAndroid.CHANNEL_IMPORTANCE_NONE);
     *
     * @return map object with constants
     */
    override fun getConstants(): MutableMap<String, Any> {
        val constants = mutableMapOf<String, Any>()
        // notification channel importance levels
        constants.put("CHANNEL_IMPORTANCE_NONE", NotificationManager.IMPORTANCE_NONE)
        constants.put("CHANNEL_IMPORTANCE_MIN", NotificationManager.IMPORTANCE_MIN)
        constants.put("CHANNEL_IMPORTANCE_LOW", NotificationManager.IMPORTANCE_LOW)
        constants.put("CHANNEL_IMPORTANCE_DEFAULT", NotificationManager.IMPORTANCE_DEFAULT)
        constants.put("CHANNEL_IMPORTANCE_HIGH", NotificationManager.IMPORTANCE_HIGH)
        constants.put("CHANNEL_IMPORTANCE_MAX", NotificationManager.IMPORTANCE_MAX)
        // notification priorities (used for compatibility with android <= 7(SDK <= 25))
        constants.put("PRIORITY_MIN", NotificationCompat.PRIORITY_MIN)
        constants.put("PRIORITY_LOW", NotificationCompat.PRIORITY_LOW)
        constants.put("PRIORITY_DEFAULT", NotificationCompat.PRIORITY_DEFAULT)
        constants.put("PRIORITY_HIGH", NotificationCompat.PRIORITY_HIGH)
        constants.put("PRIORITY_MAX", NotificationCompat.PRIORITY_MAX)
        // notification templates
        constants.put("TEMPLATE_COMMON", Templates.COMMON)
        constants.put("TEMPLATE_EVENT", Templates.EVENT)
        constants.put("TEMPLATE_WEATHER", Templates.WEATHER)
        constants.put("TEMPLATE_MR", Templates.MR)
        constants.put("TEMPLATE_UIN", Templates.UIN)
        constants.put("TEMPLATE_JASKLIK", Templates.JASKLIK)
        constants.put("TEMPLATE_SNR", Templates.SNR)
        constants.put("TEMPLATE_ANNUR", Templates.ANNUR)
        constants.put("TEMPLATE_ASPEK", Templates.ASPEK)
        constants.put("TEMPLATE_DOKUPLUS", Templates.DOKUPLUS)
        constants.put("TEMPLATE_UNHASPAY", Templates.UNHASPAY)
        constants.put("TEMPLATE_JAWARAPAY", Templates.JAWARAPAY)
        constants.put("TEMPLATE_ZTI", Templates.ZTI)
        constants.put("TEMPLATE_PURWOREJO", Templates.PURWOREJO)
        constants.put("TEMPLATE_HEBAT", Templates.HEBAT)
        constants.put("TEMPLATE_BARBEKOE", Templates.BARBEKOE)
        return constants
    }

    /**
     * Creates notification channel.
     * Notification channels are required from android 8 (SDK 26).
     * This methods can be called multiple times, channels are not recreated.
     *
     * @param id notification channel id, used on showing push notification
     * @param name notification channel name, displayed in app push notification settings
     * @param desc notification channel description
     * @param importance notification channel importance, the more importance the more chances user will see a notification
     */
    @ReactMethod
    fun createChannel(id: String, name: String, desc: String, importance: Int) {
        // create channel only if API is available in SDK, android >= 8 (SDK >= 26)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                description = desc
            }
            val notificationManager: NotificationManager = reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Returns device FCM token in React Native promise
     *
     * @param promise React Native promise
     */
    @ReactMethod
    fun getDeviceToken(promise: Promise) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    promise.reject("E_FIREBASE_DEVICE_TOKEN", "Unable to retrieve device FCM token")
                }
                val token = task.result?.token
                promise.resolve(token)
            })
    }

    /**
     * Shows push notification
     *
     * @param notificationId notification id, needed in case we would want to modify a notification
     * @param template template id
     * @param channelId notification channel id
     * @param data notification data attributes, for different templates there are different data attributes
     * @param priority notification priority, used for backward compatibility with android <= 7 (SDK <= 25), android >= 8 uses channels
     */
    @ReactMethod
    fun show(notificationId: Int, template: Int, channelId: String, data: ReadableMap, priority: Int = NotificationCompat.PRIORITY_DEFAULT) {

        // prepare pending intent that opens main activity
        val mainIntent = Intent(PushNotificationBroadcastReceiver.Actions.PRESS_ON_NOTIFICATION)
        // if target broadcast receiver exists in AndroidManifest.xml then use it, else use default broadcast receiver
        var defaultBroadcastReceiverClassName = reactApplicationContext.packageManager.getApplicationInfo(reactApplicationContext.packageName, PackageManager.GET_META_DATA).metaData.getString(META_KEY_DEFAULT_BROADCAST_RECEVIER_CLASSNAME)
        if(defaultBroadcastReceiverClassName == null) {
            defaultBroadcastReceiverClassName = DEFAULT_BROADCAST_RECEVIER_CLASSNAME
        }
        mainIntent.component = ComponentName(reactApplicationContext, defaultBroadcastReceiverClassName)
        // add all notification data attributes to intent extra params
        data.entryIterator.forEach {
            mainIntent.putExtra(it.key, if (it.value == null) null else it.value.toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(reactApplicationContext, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        var iconNotification=1;
         if(template == Templates.WEATHER) {
            iconNotification = R.drawable.ic_launcher;
        }
          if(template == Templates.MR) {
            iconNotification = R.drawable.ic_launcher_mr;
        }
          if(template == Templates.UIN) {
            iconNotification = R.drawable.ic_launcher_uin;
        }
        if(template == Templates.JASKLIK) {
            iconNotification = R.drawable.ic_launcher_jasklik;
        }
        if(template == Templates.SNR) {
            iconNotification = R.drawable.ic_launcher_snr;
        }
        if(template == Templates.ANNUR) {
            iconNotification = R.drawable.ic_launcher_annur;
        }
        if(template == Templates.ASPEK) {
            iconNotification = R.drawable.ic_launcher_aspek;
        }
        if(template == Templates.DOKUPLUS) {
            iconNotification = R.drawable.ic_launcher_dokuplus;
        }
        if(template == Templates.UNHASPAY) {
            iconNotification = R.drawable.ic_launcher_unhaspay;
        }
        if(template == Templates.JAWARAPAY) {
            iconNotification = R.drawable.ic_launcher_jawara_pay;
        }
        if(template == Templates.ZTI) {
            iconNotification = R.drawable.ic_launcher_zti;
        }
        if(template == Templates.PURWOREJO) {
            iconNotification = R.drawable.ic_launcher_purworejo;
        }
        if(template == Templates.HEBAT) {
            iconNotification = R.drawable.ic_launcher_hebat;
        }
        if(template == Templates.BARBEKOE) {
            iconNotification = R.drawable.ic_launcher_barbekoe;
        }
        
        
        val builder = NotificationCompat.Builder(reactApplicationContext, channelId)
                .setSmallIcon(iconNotification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setPriority(priority)
                .setOngoing(true)

        // template common push notification
        if(template == Templates.COMMON) {
            builder.apply {
                setContentTitle(data.getString("title"))
                setContentText(data.getString("message"))
            }
        }

        // template event push notification
        if(template == Templates.EVENT) {
            val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.notification_template_event)
            // set title
            if(!data.isNull("title")) {
                remoteViews.setViewVisibility(R.id.textViewTitle, View.VISIBLE)
                remoteViews.setTextViewText(R.id.textViewTitle, data.getString("title"))
            }
            // set message
            if(!data.isNull("message")) {
                remoteViews.setViewVisibility(R.id.textViewMessage, View.VISIBLE)
                remoteViews.setTextViewText(R.id.textViewMessage, data.getString("message"))
            }
            // set event media image
            if(!data.isNull("media")) {
                if(!data.getString("media")!!.isEmpty()) {
                    remoteViews.setViewVisibility(R.id.imageViewMedia, View.VISIBLE)
                    val bitmap = Glide.with(reactApplicationContext).asBitmap().load(data.getString("media")).submit().get()
                    remoteViews.setImageViewBitmap(R.id.imageViewMedia, bitmap)
                }
            }
            // if url param exists open this url in browser on notification click
            if(!data.isNull("url")) {
                if(!data.getString("url")!!.isEmpty()) {
                    val openUrlIntent = Intent(PushNotificationBroadcastReceiver.Actions.OPEN_URL)
                    openUrlIntent.component = ComponentName(reactApplicationContext, defaultBroadcastReceiverClassName)
                    openUrlIntent.putExtra("url", data.getString("url"))
                    val openUrlPendingIntent = PendingIntent.getBroadcast(reactApplicationContext, 0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    builder.setContentIntent(openUrlPendingIntent)
                }
            }
            // on check button click send CLOSE_NOTIFICATION action to broadcast receiver that closes notification
            val closeNotificationIntent = Intent(PushNotificationBroadcastReceiver.Actions.CLOSE_NOTIFICATION)
            closeNotificationIntent.component = ComponentName(reactApplicationContext, defaultBroadcastReceiverClassName)
            closeNotificationIntent.putExtra("id", notificationId)
            val closeNotificationPendingIntent = PendingIntent.getBroadcast(reactApplicationContext, notificationId, closeNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.buttonCheck, closeNotificationPendingIntent)
            // set notification template
            builder.setContent(remoteViews)
        }

        if(template == Templates.WEATHER) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.notification_template_weather)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
          if(template == Templates.MR) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.mr)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }  
        if(template == Templates.UIN) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.uin)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            } 
            if(template == Templates.JASKLIK) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.jasklik)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.SNR) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.snr)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.ANNUR) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.annur)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.ASPEK) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.aspek)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            } 
            if(template == Templates.DOKUPLUS) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.dokuplus)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.UNHASPAY) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.unhaspay)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
             if(template == Templates.JAWARAPAY) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.jawara_pay)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.ZTI) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.zti)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.PURWOREJO) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.purworejo)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.HEBAT) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.hebat)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
            if(template == Templates.BARBEKOE) {
                val remoteViews = RemoteViews(reactApplicationContext.packageName, R.layout.barbekoe)
                remoteViews.setTextViewText(R.id.imsak_time, data.getString("imsak_time"))
                remoteViews.setTextViewText(R.id.Imsak_title, data.getString("Imsak_title"))
                remoteViews.setTextViewText(R.id.subuh_time, data.getString("subuh_time"))
                remoteViews.setTextViewText(R.id.subuh_title, data.getString("subuh_title"))
                remoteViews.setTextViewText(R.id.sunrise_time, data.getString("sunrise_time"))
                remoteViews.setTextViewText(R.id.Sunrise_title, data.getString("Sunrise_title"))
                remoteViews.setTextViewText(R.id.Dzuhur_time, data.getString("Dzuhur_time"))
                remoteViews.setTextViewText(R.id.Dzuhur_title, data.getString("Dzuhur_title"))
                remoteViews.setTextViewText(R.id.Asar_time, data.getString("Asar_time"))
                remoteViews.setTextViewText(R.id.Asar_title, data.getString("Asar_title"))
                remoteViews.setTextViewText(R.id.Maghrib_time, data.getString("Maghrib_time"))
                remoteViews.setTextViewText(R.id.Maghrib_title, data.getString("Maghrib_title"))
                remoteViews.setTextViewText(R.id.Isiya_time, data.getString("Isiya_time"))
                remoteViews.setTextViewText(R.id.Isiya_title, data.getString("Isiya_title"))
                builder.setContent(remoteViews)
            }
        // show notification
        NotificationManagerCompat.from(reactApplicationContext).notify(notificationId, builder.build())
    }

}
