package com.android.buzz.main;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.buzz.activity.HomeActivity;
import com.android.buzz.activity.MainActivity;
import com.android.buzz.activity.NotificationActivity;
import com.android.buzz.bluetooth.BluetoothChat;
import com.android.buzz.util.Constants;
import com.android.buzz.util.SharedpreferenceUtility;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	static int notificationcount;
	private static final String TAG = "GCMIntentService";

	private static String msgcount;

	private MainApplication aMainApplication = null;

	public GCMIntentService() {
		// Call extended class Constructor GCMBaseIntentService
		super(Config.GOOGLE_SENDER_ID);
	}

	/**
	 * Method called on device registered
	 **/
	@Override
	protected void onRegistered(Context context, String registrationId) {
		Constants.DEVICEREGID=registrationId;
		//Get Global MainApplication Class object (see application tag in AndroidManifest.xml)
		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();

		Log.i(TAG, "Device registered: regId = " + registrationId);
		aMainApplication.displayMessageOnScreen(context, "Your device registred with GCM");
		// Log.d("NAME", MainActivity4.name);
		// aMainApplication.register(context, MainActivity4.name, MainActivity4.email, registrationId);
	}

	/**
	 * Method called on device unregistred
	 * */
	@Override
	protected void onUnregistered(Context context, String registrationId) {
		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();
		Log.i(TAG, "Device unregistered");
		aMainApplication.displayMessageOnScreen(context, getString(R.string.gcm_unregistered));
		aMainApplication.unregister(context, registrationId);
	}
	/**
	 * Method called on Receiving a new message from GCM server
	 * */
	@Override
	protected void onMessage(Context context, Intent intent) {

		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();
		Log.i(TAG, "Received message");
		String message = intent.getExtras().getString("Message");
		aMainApplication.displayMessageOnScreen(context, message);
		// notifies user
		//if(message.contains("message")){
		//String profile_id=intent.getExtras().getString("profile_id");
		//String user_id=intent.getExtras().getString("user_id");
		//	generateNotification(context, message);
		//}else{
		generateNotification(context, intent);
		//}

	}

	/**
	 * Method called on receiving a deleted message
	 * */
	@Override
	protected void onDeletedMessages(Context context, int total) {

		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();

		Log.i(TAG, "Received deleted messages notification");

		String message = getString(R.string.gcm_deleted, total);
		aMainApplication.displayMessageOnScreen(context, message);
		// notifies user
		generateNotification(context, message);
	}

	/**
	 * Method called on Error
	 * */
	@Override
	public void onError(Context context, String errorId) {

		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();

		Log.i(TAG, "Received error: " + errorId);
		aMainApplication.displayMessageOnScreen(context, getString(R.string.gcm_error, errorId));
	}

	@Override
	protected boolean onRecoverableError(Context context, String errorId) {

		if(aMainApplication == null)
			aMainApplication = (MainApplication) getApplicationContext();

		// log message
		Log.i(TAG, "Received recoverable error: " + errorId);
		aMainApplication.displayMessageOnScreen(context, getString(R.string.gcm_recoverable_error,
				errorId));
		return super.onRecoverableError(context, errorId);
	}

	/**
	 * Create a notification to inform the user that server has sent a message.
	 */
	@SuppressWarnings({ "deprecation", "unused" })
	private static void generateNotification(Context context, Intent receiveIntent) {

		/***variables to get app's current state***/
		/*boolean isBackground=Foreground.get(context).isBackground();
		boolean isForeground=Foreground.get(context).isForeground();*/
		/***********/
		String profile_id = "",user_id = "";
		String message="";
		int icon = R.drawable.ic_launcher;

		/***for ringtone creation ***/
		try {
			if(SharedpreferenceUtility.getInstance(context).getString("soundNotification").equalsIgnoreCase("1") || SharedpreferenceUtility.getInstance(context).getString("soundNotification").equalsIgnoreCase("")){
				try {
					Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
					Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
					r.play();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		long when = System.currentTimeMillis();
		try {
			message = receiveIntent.getExtras().getString("Message");
			if(message.contains("message")){
				profile_id=receiveIntent.getExtras().getString("profile_id");
				user_id=receiveIntent.getExtras().getString("user_id");
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Constants.NOTIFICATIONCOUNT = MainApplication.getPendingNotificationsCount() + 1;
			MainApplication.setPendingNotificationsCount(Constants.NOTIFICATIONCOUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			NotificationManager notificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(icon, message, when);
			Intent notificationIntent=null;

			/***to check application is in background or not ***/
			/*if(isForeground==false||isBackground==false){
				notificationIntent = new Intent(context, MainActivity.class);
				notificationIntent.putExtra("fromBuzzNotification", "yes");
				notificationIntent.putExtra("profile_id", profile_id);
				notificationIntent.putExtra("user_id", user_id);

			}else{
				if(message.contains("message")){
					notificationIntent = new Intent(context, BluetoothChat.class);
					notificationIntent.putExtra("fromBuzzNotification", "yes");
					notificationIntent.putExtra("profile_id", profile_id);
					notificationIntent.putExtra("user_id", user_id);
				}else {
					notificationIntent = new Intent(context, NotificationActivity.class);
					notificationIntent.putExtra("fromBuzzNotification", "yes");
				}
			}*/
            /*************/
			if(message.contains("message")){
				notificationIntent = new Intent(context, BluetoothChat.class);
				notificationIntent.putExtra("fromBuzzNotification", "yes");
				notificationIntent.putExtra("profile_id", profile_id);
				notificationIntent.putExtra("user_id", user_id);
			}else {
				notificationIntent = new Intent(context, NotificationActivity.class);
				notificationIntent.putExtra("fromBuzzNotification", "yes");
			}

			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent intent = PendingIntent.getActivity(context, 0,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			String title = context.getString(R.string.app_name);
			NotificationCompat.Builder mBuilder=null;
			if(message.contains("message")){
				mBuilder =new NotificationCompat.Builder(BluetoothChat.getInstance())
				.setDefaults(notification.defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setTicker(title)
				.setContentIntent(intent)
				.setAutoCancel(true);    	
			}else{
				mBuilder =new NotificationCompat.Builder(NotificationActivity.getInstance())
				.setDefaults(notification.defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setTicker(title)
				.setContentIntent(intent)
				.setAutoCancel(true);
			}
			mBuilder.setNumber(Constants.NOTIFICATIONCOUNT);
			if (message != null) {
				mBuilder.setContentText(message);
			} else {
				mBuilder.setContentText("<missing message content>");
			}
			if (msgcount != null) {
				mBuilder.setNumber(Integer.parseInt(msgcount));
			}
			notification.setLatestEventInfo(context, title, message, intent);
			notificationManager.notify(Constants.NOTIFICATIONCOUNT, notification);    
			try {
				Log.e("dfsgaghkjsahgkjsahglkjhglkjhlkjghkjghkjshgdshkj","akjfhakhgkjahgkjahgkjsahgkjahgkjshgkjshgkjhdskjghsdkjghskjdghskjdh");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Create a notification to inform the user that server has sent a message.
	 */
	@SuppressWarnings({ "deprecation", "unused" })
	private static void generateNotification(Context context, String message){
		int icon = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();

		try {
			Constants.NOTIFICATIONCOUNT = MainApplication.getPendingNotificationsCount() + 1;
			MainApplication.setPendingNotificationsCount(Constants.NOTIFICATIONCOUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			NotificationManager notificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification = new Notification(icon, message, when);
			Intent notificationIntent=null;
			if(message.contains("message")){
				/*notificationIntent = new Intent(context, BluetoothChat.class);
			    	notificationIntent.putExtra("fromBuzzNotification", "yes");*/
				notificationIntent = new Intent(context, NotificationActivity.class);
				notificationIntent.putExtra("fromBuzzNotification", "yes");
			}else {
				notificationIntent = new Intent(context, NotificationActivity.class);
				notificationIntent.putExtra("fromBuzzNotification", "yes");
			}


			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent intent = PendingIntent.getActivity(context, 0,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			String title = context.getString(R.string.app_name);
			NotificationCompat.Builder mBuilder=null;
			if(message.contains("message")){
				mBuilder =new NotificationCompat.Builder(BluetoothChat.getInstance())
				.setDefaults(notification.defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setTicker(title)
				.setContentIntent(intent)
				.setAutoCancel(true);    	
			}else{
				mBuilder =new NotificationCompat.Builder(NotificationActivity.getInstance())
				.setDefaults(notification.defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setTicker(title)
				.setContentIntent(intent)
				.setAutoCancel(true);
			}
			mBuilder.setNumber(Constants.NOTIFICATIONCOUNT);
			if (message != null) {
				mBuilder.setContentText(message);
			} else {
				mBuilder.setContentText("<missing message content>");
			}
			if (msgcount != null) {
				mBuilder.setNumber(Integer.parseInt(msgcount));
			}
			notification.setLatestEventInfo(context, title, message, intent);
			notificationManager.notify(Constants.NOTIFICATIONCOUNT, notification);    
			try {
				Log.e("dfsgaghkjsahgkjsahglkjhglkjhlkjghkjghkjshgdshkj","akjfhakhgkjahgkjahgkjsahgkjahgkjshgkjshgkjhdskjghsdkjghskjdghskjdh");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

