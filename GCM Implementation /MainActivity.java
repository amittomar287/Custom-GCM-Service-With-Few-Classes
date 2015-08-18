package com.android.buzz.activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.buzz.bluetooth.XmppConnection;
import com.android.buzz.database.DatabaseAdapter;
import com.android.buzz.facebook.FacebookHandler;
import com.android.buzz.facebook.FacebookHandler.SendEntityTask;
import com.android.buzz.linkedin.Config;
import com.android.buzz.linkedin.LinkedinHandler;
import com.android.buzz.main.GCMIntentService;
import com.android.buzz.main.MainApplication;
import com.android.buzz.main.R;
import com.android.buzz.pojo.ProfileInfo;
import com.android.buzz.util.BleUtils;
import com.android.buzz.util.Constants;
import com.android.buzz.util.SharedpreferenceUtility;
import com.android.buzz.util.Util;
import com.android.buzz.webAPI.WebServiceDetails;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;
import com.google.android.gcm.GCMRegistrar;
import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.gson.JsonObject;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;
import com.linkedin.platforms.APIHelper;
import com.linkedin.platforms.LISession;
import com.linkedin.platforms.LISessionManager;

/**
 * This is the Launcher login screen containing different login options
 * @author Canopus
 *
 */
public class MainActivity extends FragmentActivity implements OnClickListener,LocationListener{

	private LoginButton loginBtn;
	private Button linkedIn,mBuzzLogin;
	private FacebookHandler mFBHandler;
	public ProgressDialog mProgressDialog;
	private TextView mTxtV_twr;
	private LocationManager mLocationManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String address;
	private static int REQUEST_CONNECT_DEVICE=101;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	private static final String host = "api.linkedin.com";
	private static final String url="https://api.linkedin.com/v1/people/~:(id,first-name,last-name,maiden-name,email-address,public-profile-url,picture-url,headline,location,industry)";
	//private static final String topCardUrl = "https://" + host + "/v1/people/~:(first-name,last-name,public-profile-url)";
	public static final String PACKAGE_MOBILE_SDK_SAMPLE_APP = "com.android.buzz.main";
	private DefaultHttpClient client;
	private HttpPost request;
	private HttpResponse response;
	private MultipartEntity entity;
	private String mMultipartImageUrl="";
	private ProgressDialog progressDialog;
	private String BOUNDARY;
	private String xmppUserName="";
	private String xmppEmail="";
	private String xmppPass="";
	private ProfileInfo info;
	private String fromNotificationClick="";
	private MainApplication aController;

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/**
		 * This is for linkedin login method to allow it to run on UI thread
		 */
		if( Build.VERSION.SDK_INT >= 9){
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy); 
		}

		/**GCM* */
		getPushNotificationId();

		final int bleStatus =BleUtils.getInstance().getBleStatus(getBaseContext());
		switch (bleStatus) {
		case Constants.STATUS_BLE_NOT_AVAILABLE:
			break;
		case Constants.STATUS_BLUETOOTH_NOT_AVAILABLE:
			Util.getInstance().showToast("status bluetooth not available", MainActivity.this);
			break;
		default:
			mBluetoothAdapter = BleUtils.getInstance().getBluetoothAdapter(getBaseContext());
			Constants.CURRENT_DEVICE_BLE= mBluetoothAdapter.getAddress();
			break;
		}
		loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
		loginBtn.setBackgroundResource(R.drawable.facebook_button);
		loginBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		mBuzzLogin = (Button) findViewById(R.id.buzzlogin);
		linkedIn = (Button) findViewById(R.id.linkedinlogin);
		mTxtV_twr = (TextView) findViewById(R.id.txtv_without_reg);

		linkedIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				LISessionManager.getInstance(getApplicationContext()).init(MainActivity.this, buildScope(), new AuthListener() {
					@Override
					public void onAuthSuccess() {
						setUpdateState();
						getProfileDataLinkedin();
					}
					@Override
					public void onAuthError(LIAuthError error) {
						setUpdateState();
						Toast.makeText(getApplicationContext(), "Auth failed " + error.toString(), Toast.LENGTH_LONG).show();
					}
				}, true);
			}
		});
		loginBtn.setOnClickListener(this);
		mBuzzLogin.setOnClickListener(this);
		mTxtV_twr.setOnClickListener(this);
		if(HomeActivity.mCurrentLocation==null)
			HomeActivity.mCurrentLocation=getLocation();
		if(HomeActivity.mCurrentLocation==null){
			Toast.makeText(this, "Please First disable then enable GPS..", Toast.LENGTH_LONG).show();
		}
	}

	protected void onStop() {
		super.onStop();
		try {
			unregisterReceiver(mHandleMessageReceiver);	
		} catch (Exception e) {
			e.getMessage();
		}

	};
	
	public void getPushNotificationId() {
		try {
			String strDeviceToken = "";
			// Make sure the device has the proper dependencies.
			GCMRegistrar.checkDevice(this);
			// Make sure the manifest permissions was properly set
			GCMRegistrar.checkManifest(this);
			registerReceiver(mHandleMessageReceiver,
					new IntentFilter(com.android.buzz.main.Config.DISPLAY_MESSAGE_ACTION));
			// Get GCM registration id

			Constants.DEVICEREGID= GCMRegistrar.getRegistrationId(this);
			if (Constants.DEVICEREGID.equalsIgnoreCase("")) {
				// Register with GCM
				GCMRegistrar.register(MainActivity.this,com.android.buzz.main.Config.GOOGLE_SENDER_ID);
			} else {
				// Device is already registered on GCM Server
				if (GCMRegistrar.isRegisteredOnServer(this)) {
					// Skips registration.
					Toast.makeText(getApplicationContext(), "Already registered with GCM Server", Toast.LENGTH_LONG).show();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String newMessage = "";
			try {
				newMessage = intent.getExtras().getString(com.android.buzz.main.Config.EXTRA_MESSAGE);
				ComponentName comp = new ComponentName(context.getPackageName(),
						GCMIntentService.class.getName());
				// Waking up mobile if it is sleeping
				aController.acquireWakeLock(getApplicationContext(),intent.setComponent(comp));
				// Releasing wake lock
				aController.releaseWakeLock();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * Get Profile information through linkedin 
	 * */
	private void getProfileDataLinkedin() {
		try {
			APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
			apiHelper.getRequest(MainActivity.this, url, new ApiListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onApiSuccess(ApiResponse s) {
					try {
						JSONObject jobj=new JSONObject(s.toString());
						String jsonResponseStr=jobj.getString("responseData");
						JSONObject json=new JSONObject(jsonResponseStr);

						String firstName="",headline="",id="",industry="",lastName="",location="";
						String cityName="",proPictureUrl="",publicProfileUrl="",userName="";
						if(json.optString("firstName")!=null){
							firstName=json.optString("firstName");
						}else{
							firstName="";
						}
						if(json.optString("headline")!=null){
							headline=json.optString("headline");
						}else{
							headline="";
						}
						if(json.optString("id")!=null){
							id=json.optString("id");
						}else{
							id="";
						}
						if(json.optString("industry")!=null){
							industry=json.optString("industry");
						}else{
							industry="";
						}
						if(json.optString("lastName")!=null){
							lastName=json.optString("lastName");
							userName=firstName+" "+lastName;
						}else{
							lastName="";
							userName=firstName;
						}
						if(json.optString("location")!=null){
							location=json.optString("location");
							JSONObject job=new JSONObject(location);
							cityName=job.getString("name");
						}else{
							cityName="";
							location="";
						}
						if(json.optString("pictureUrl")!=null){
							proPictureUrl=json.optString("pictureUrl");
						}else{
							proPictureUrl="";
						}
						if(json.optString("publicProfileUrl")!=null){
							publicProfileUrl=json.optString("publicProfileUrl");
						}else{
							publicProfileUrl="";
						}

						info=new ProfileInfo();
						info.setName(userName);
						info.setAbout(headline);
						info.setCurrentJobDescription(industry);
						info.setCity(cityName);
						info.setProfile_pic(proPictureUrl);
						//info.setLinkedinBrowserLink(publicProfileUrl);
						/**declarations of multipart entities**/
						client = new DefaultHttpClient();
						//authkey = session.getAuthKey();
						request = new HttpPost(WebServiceDetails.LOGIN_SIGNUP_URL);
						response = null;
						BOUNDARY= "--eriksboundry--";
						request.setHeader("Content-Type", "multipart/form-data; boundary="+BOUNDARY);
						entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE,BOUNDARY,Charset.defaultCharset());
						request.setHeader("Accept", "application/json");
						request.setHeader("Content-Type", "multipart/form-data; boundary="+BOUNDARY);
						try {
							entity.addPart("facebook_id", new StringBody("0"));
							entity.addPart("linkedin_id", new StringBody(publicProfileUrl));
							entity.addPart("is_login", new StringBody("0"));
							entity.addPart("name", new StringBody(userName));
							entity.addPart("job_description", new StringBody(industry));
							entity.addPart("interests", new StringBody(""));
							entity.addPart("marital_status", new StringBody(""));
							entity.addPart("age", new StringBody(""));
							entity.addPart("gender", new StringBody(""));
							entity.addPart("skill", new StringBody(""));
							entity.addPart("device_token", new StringBody(Constants.DEVICEREGID));
							entity.addPart("ble_code", new StringBody(Constants.CURRENT_DEVICE_BLE));
							entity.addPart("education", new StringBody(""));
							entity.addPart("language", new StringBody(""));
							entity.addPart("email_id", new StringBody(""));
							entity.addPart("password", new StringBody("123"));
							entity.addPart("about_me", new StringBody(headline));
							entity.addPart("date_of_birth", new StringBody(""));

							entity.addPart("buzz_line", new StringBody(""));
							String lat=null,longi=null;
							if(HomeActivity.mCurrentLocation!=null){
								lat=HomeActivity.mCurrentLocation.getLatitude()+"";
								longi=HomeActivity.mCurrentLocation.getLongitude()+"";
							}
							if(lat==null){
								lat="0.0";
							}
							if(longi==null){
								longi="0.0";
							}
							entity.addPart("latitude", new StringBody(lat));
							entity.addPart("longitude", new StringBody(longi));
							entity.addPart("hometown", new StringBody(cityName));
							if(!info.getProfile_pic().equalsIgnoreCase("")){
								URL imageURL = new URL(info.getProfile_pic());
								InputStream in = (InputStream) imageURL.getContent();
								Bitmap  bitmap = BitmapFactory.decodeStream(in);
								/**variable to send multipart image url**/
								ByteArrayOutputStream stream = new ByteArrayOutputStream();
								bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
								byte[] byteArray = stream.toByteArray();
								ByteArrayBody bab = new ByteArrayBody(byteArray, "image/jpeg" , "image.png");
								entity.addPart("profile_picture", bab);
								/***/
							}
							entity.addPart("music", new StringBody(""));
							entity.addPart("device_type", new StringBody("android"));
							//ByteArrayOutputStream bytes = new ByteArrayOutputStream();
							//entity.writeTo(bytes);
						//	String content = bytes.toString();
							new SendEntityTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,entity);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				@Override
				public void onApiError(LIApiError error) {
					Toast.makeText(getApplicationContext(), "failed " + error.toString(), Toast.LENGTH_LONG).show();                
				}
			});	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * call multipart entity method in asyntask new registration
	 * */
	public class SendEntityTask extends android.os.AsyncTask<MultipartEntity, String, String>{
		String jsonResponse="";
		MultipartEntity multipartEntity;
		private JSONObject data;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			try {
				//Util.getInstance().showProgressDialog("Please wait..", EditProfileActivity.this);
				progressDialog = new ProgressDialog(MainActivity.this);      
				progressDialog.setIndeterminate(true);
				progressDialog.setMessage("Loading...");
				progressDialog.show();
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
		@Override
		protected String doInBackground(MultipartEntity... params) {
			try {
				multipartEntity=params[0];
				jsonResponse=uploadFileNew(multipartEntity);
				return jsonResponse;
			} catch (Exception e) {
				e.printStackTrace();
				return jsonResponse;
			}
		}
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try {
				String myString=result.toString();
				data = new JSONObject(result.toString());
				JSONArray jsonMainNode = data
						.optJSONArray("signUp");
				int lengthJsonArr = jsonMainNode.length();

				/****** Get Object for each JSON node. ***********/
				JSONObject jsonChildNode = jsonMainNode
						.getJSONObject(0);
				String fff=jsonChildNode.getString("message");
				String status=jsonChildNode.getString("status");
				if(status.equalsIgnoreCase("success")){

					/******* Fetch node values **********/
					String 	userName=jsonChildNode.getString("name");
					String userId = jsonChildNode.getString("user_id");
					String EmailId=jsonChildNode.optString("email_id");
					String password = jsonChildNode.getString("password");
					SharedpreferenceUtility.getInstance(MainActivity.this).
					putString(Constants.LOGIN_USER_ID,userId);
					try {
						ConnectXMPP(SharedpreferenceUtility.getInstance(MainActivity.this).getString(Constants.LOGIN_USER_ID),EmailId,"123");
					} catch (Exception e) {
						e.printStackTrace();
					}
					String latitude = jsonChildNode.getString("latitude");
					String about_me=jsonChildNode.getString("about_me");
					String longitude = jsonChildNode.getString("longitude");
					String hometown = jsonChildNode.optString("hometown");
					String Interests = jsonChildNode.optString("interests");
					String education = jsonChildNode.optString("education");

					String profilePic = jsonChildNode.getString("profile_picture");

					String ln_id = jsonChildNode.optString("linkedin_id");
					String skill=jsonChildNode.optString("skill");
					String age=jsonChildNode.optString("age");
					String gender=jsonChildNode.optString("gender");
					String facebookid=jsonChildNode.optString("facebook_id");
					String language=jsonChildNode.optString("language");
					String MaritialStatus=jsonChildNode.getString("marital_status");
					String jobDescription=jsonChildNode.getString("job_description");
					String Accesstaken=jsonChildNode.optString("access_token");
					String 	birthday=jsonChildNode.getString("date_of_birth");
					//String buzzLine=jsonChildNode.optString("buzz_line");
					//String currentMusic=jsonChildNode.getString("music");
					putResponseDataInSharedPref(userName,birthday,MaritialStatus,education,hometown,jobDescription,profilePic,about_me,EmailId,ln_id);
					DatabaseAdapter database = new DatabaseAdapter(MainActivity.this);
					database.open();
					HashMap<String, String> profile = new HashMap<String, String>();
					profile.put(DatabaseAdapter.KEY_NAME,userName);
					profile.put(DatabaseAdapter.KEY_USER_ID,userId);
					profile.put(DatabaseAdapter.KEY_USER_EMAIL,EmailId);
					profile.put(DatabaseAdapter.KEY_LATITUDE,latitude);
					profile.put(DatabaseAdapter.KEY_LONGITUDE,longitude);
					profile.put(DatabaseAdapter.KEY_USER_IMAGE,profilePic);
					profile.put(DatabaseAdapter.KEY_LINKEDIN_ID,ln_id);
					profile.put(DatabaseAdapter.KEY_USER_SKILLS,skill);
					profile.put(DatabaseAdapter.KEY_USER_AGE,age);
					profile.put(DatabaseAdapter.KEY_USER_GENDER,gender);
					profile.put(DatabaseAdapter.KEY_FACEBOOK_ID,facebookid);
					profile.put(DatabaseAdapter.KEY_USER_LANGUAGES, language);
					profile.put(DatabaseAdapter.KEY_USER_STATUS,MaritialStatus);
					profile.put(DatabaseAdapter.KEY_USER_JOB,jobDescription);
					profile.put(DatabaseAdapter.KEY_USER_INTERESTS,Interests);
					profile.put(DatabaseAdapter.KEY_USER_LOCATION,"");
					profile.put(DatabaseAdapter.KEY_USER_EDUCATION,education);
					profile.put(DatabaseAdapter.KEY_USER_LOGIN_VIA,"registration");
					profile.put(DatabaseAdapter.KEY_HOMETOWN,hometown);
					profile.put(DatabaseAdapter.KEY_PASSWORD,password);

					database.InsertDataIntoContactTable(profile);
					database.close();

					SharedpreferenceUtility.getInstance(MainActivity.this).
					putString(Constants.SHRD_KEY_ACCESS_TOKEN,Accesstaken);

					SharedpreferenceUtility.getInstance(MainActivity.this).
					putString(Constants.LOGIN_USER_NAME,userName);
					SharedpreferenceUtility.getInstance(MainActivity.this).
					putBoolean(Constants.SHRD_KEY_LOGGEDIN,true);
					SharedpreferenceUtility.getInstance(MainActivity.this).
					putString(Constants.LOGIN_USER_PASS,password);
					//Util.getInstance().cancleProgressDialog(EditProfileActivity.this);
					progressDialog.cancel();
					//callToHome();
				}else{

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}catch (Exception e) {
				e.printStackTrace();
			}finally{

			}
		}
	}
	/**
	 * save data when first time registration 
	 * */
	private void putResponseDataInSharedPref(String userName, String birthday,
			String maritialStatus, String education, String hometown,
			String jobDescription, String profilePic,String about_me,String emailId,String linkedinId) {
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("username",userName);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("birth",birthday);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("status",maritialStatus);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("edu",education);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("hometown",hometown);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("jobDescription",jobDescription);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("profilePic",profilePic);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("aboutMe",about_me);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("email_id",emailId);
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("loginFrom","linkedin");
		SharedpreferenceUtility.getInstance(MainActivity.this).
		putString("linkedinId",linkedinId);


	}
	/**
	 * Request using multipart entity
	 * **/
	public  String  uploadFileNew(MultipartEntity entity){
		String mResultString = "" ;

		try {
			request.setEntity(entity);
			response = client.execute(request);
			StatusLine statusLine =response.getStatusLine();
			String reason=statusLine.getReasonPhrase();
			int statusCode =statusLine.getStatusCode();
			if(statusCode==200){
				try {
					mResultString = EntityUtils.toString(response.getEntity());	
				} catch (Exception e) {
					e.printStackTrace();
				}

			}else{
				Log.d("Status Code !=200", "Status code not equal to 200");
			}


		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return mResultString;
	}
	/**
	 * this method is used to register user on XMPP server and authenticate user  
	 * */
	private void ConnectXMPP(String name,String email,String pass) {
		xmppUserName=name;
		xmppEmail=email;
		xmppPass=pass;
		Thread t = new Thread(new Runnable() {
			private CharSequence text = "Registration sucessfull";
			private CharSequence text8 = "Registration Failed";
			@Override
			public void run() {
				XmppConnection.getConnection();
				try {
					try {
						//com.android.buzz.util.Util.getInstance().showProgressDialog("Registering ..", mContext);
						progressDialog = new ProgressDialog(MainActivity.this);      
						progressDialog.setIndeterminate(true);
						progressDialog.setMessage("Loading...");
						progressDialog.show();
					} catch (Exception e) {
						e.printStackTrace();
					}
					XmppConnection.getConnection().connect();
					AccountManager accountManager = XmppConnection.getConnection().getAccountManager();
					Map<String, String> attributes = new HashMap<String, String>();
					if (xmppUserName.equals("") && xmppUserName.equals("")
							&& xmppEmail.equals("") && xmppPass.equals("")) {
						CharSequence text = "None of the Flied can be Empty";
						Toast.makeText(MainActivity.this, text,
								Toast.LENGTH_SHORT).show();
					} else {
						attributes.put("Username", xmppUserName);
						attributes.put("Name", xmppUserName);
						attributes.put("Email", xmppEmail);
						attributes.put("Password", xmppPass);
						accountManager.createAccount(xmppUserName, xmppPass,attributes);
						XmppConnection.getConnection().disconnect();
					}
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to Register in as "+xmppUserName );
					XmppConnection.getConnection().disconnect();
				}
				try {
					XmppConnection.getConnection();
					XmppConnection.getConnection().connect();
					XmppConnection.getConnection().login(xmppUserName, xmppPass);
					Log.i("XMPPChatDemoActivity","Logged in as " + XmppConnection.getConnection().getUser());
					Presence presence = new Presence(Presence.Type.available);
					XmppConnection.getConnection().sendPacket(presence);
					XmppConnection.getConnection().disconnect();
					try {
						Intent gotoProfile=new Intent(MainActivity.this,HomeActivity.class);
						gotoProfile.putExtra("profileInfofromREG", info);
						MainActivity.this.startActivity(gotoProfile);
						progressDialog.cancel();
						//Util.getInstance().cancleProgressDialog(mContext);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to log in as "+ xmppUserName);
					Log.e("XMPPChatDemoActivity", ex.toString());
				}
			}
		});
		t.start();
	}
	private Session.StatusCallback statusCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (state.isOpened()) {
				Log.d("MainActivity", "Facebook session opened.");
			} else if (state.isClosed()) {
				Log.d("MainActivity", "Facebook session closed.");
			}
		}
	};
	/**
	 * set up session manager and store access token 
	 * */
	private void setUpdateState() {
		LISessionManager sessionManager = LISessionManager.getInstance(getApplicationContext());
		LISession session = sessionManager.getSession();
		boolean accessTokenValid = session.isValid();
	}
	/**
	 * make build scope to set permission 
	 * */  
	private static Scope buildScope() {
		return Scope.build(Scope.R_BASICPROFILE, Scope.W_SHARE);
	}

	private Location getLocation() {

		Location location = null;
		try {
			mLocationManager = (LocationManager)this 
					.getSystemService(this.LOCATION_SERVICE);

			// getting GPS status
			boolean isGPSEnabled = mLocationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			boolean isNetworkEnabled = mLocationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				// no network provider is enabled
			} else {
				//  this.canGetLocation = true;
				if (isNetworkEnabled) {
					mLocationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							0,
							20000,this);
					Log.d("Network", "Network Enabled");
					if (mLocationManager != null) {
						location = mLocationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						//double ss = location.getAltitude();
						if (location != null) {
							HomeActivity.mCurrentLocation=location;
						}
					}
				}
				// if GPS Enabled get lat/long using GPS Services
				if (isGPSEnabled) {
					if (location == null) {
						mLocationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								20000, 0, this);
						Log.d("GPS", "GPS Enabled");
						if (mLocationManager != null) {
							location = mLocationManager
									.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (location != null) {
								HomeActivity.mCurrentLocation=location;
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return location;


	}

	/**
	 * Here we handle the results from various domain like facebook,linkedin;
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(mFBHandler!=null)
			mFBHandler.updateFBUIHandler(requestCode, resultCode, data);
		/**
		 * Checking if logged in with facebook and then switching to homescreen on successfull login
		 */
		if(FacebookHandler.FBREQUESTCODE==requestCode && FacebookHandler.FBRESULTCODE==resultCode){
			/*	Intent homeIntent = new Intent(this, ProfileActivity.class);
			startActivity(homeIntent);
			finish();*/
		}
		try {
			LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.fb_login_button:
			mFBHandler = new FacebookHandler(MainActivity.this,loginBtn);
			//finish();
			break;
			//case R.id.linkedinlogin:
			//new LinkedInLogin().execute();
			//finish();
			//break;
		case R.id.buzzlogin:
			Intent regIntent = new Intent(this, LoginActivity.class);
			startActivity(regIntent);
			//finish();
			break;
		case R.id.txtv_without_reg:
			Intent homeIntent = new Intent(this, HomeActivity.class);
			homeIntent.putExtra("fromWithoutRegistration", true);
			startActivity(homeIntent);
			finish();
			break;
		default:
			break;
		}
	}
	/**
	 * Asyncally call the LindedInHandler class after the dialog is starts showing
	 * @author Canopus
	 *
	 */
	private class LinkedInLogin extends AsyncTask<String, Integer, String>{
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(
					MainActivity.this);
			// set progress dialog
			mProgressDialog.setMessage("Loading...");
			mProgressDialog.setCancelable(true);
			mProgressDialog.show();
			super.onPreExecute();
		}
		@Override
		protected String doInBackground(String... params) {
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
			new LinkedinHandler(MainActivity.this,mProgressDialog);
			super.onPostExecute(result);
		}
	}
	/*private void linkedInLogin() {
		ProgressDialog progressDialog = new ProgressDialog(
				MainActivity.this);
		CookieSyncManager.createInstance(this);

		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie(); 

		LinkedinDialog d = new LinkedinDialog(MainActivity.this,progressDialog);
		d.show();

		d.setVerifierListener(new OnVerifyListener() {
			@SuppressLint("NewApi")
			public void onVerify(String verifier) {
				try {
					Log.i("LinkedinSample", "verifier: " + verifier);

					accessToken = LinkedinDialog.oAuthService
							.getOAuthAccessToken(LinkedinDialog.liToken,
									verifier);
					LinkedinDialog.factory.createLinkedInApiClient(accessToken);
					client = factory.createLinkedInApiClient(accessToken);
					// client.postNetworkUpdate("Testing by Mukesh!!! LinkedIn wall post from Android app");
					Log.i("LinkedinSample",
							"ln_access_token: " + accessToken.getToken());
					Log.i("LinkedinSample",
							"ln_access_token: " + accessToken.getTokenSecret());
					//Person p = client.getProfileForCurrentUser();
					com.google.code.linkedinapi.schema.Person p = 	 client.getProfileForCurrentUser(EnumSet.of(
							ProfileField.ID, ProfileField.FIRST_NAME,
							ProfileField.PHONE_NUMBERS, ProfileField.LAST_NAME,
							ProfileField.HEADLINE, ProfileField.INDUSTRY,
							ProfileField.PICTURE_URL, ProfileField.DATE_OF_BIRTH,
							ProfileField.LOCATION_NAME, ProfileField.MAIN_ADDRESS,
								ProfileField.LOCATION_COUNTRY));
					Log.e("create access token secret", client.getAccessToken()
							.getTokenSecret());
					if(p!=null) {
						// get data

						String name = ((com.google.code.linkedinapi.schema.Person) p).getFirstName();
						Toast.makeText(getApplicationContext(), name, 1).show();
					}

				} catch (Exception e) {
					Log.i("LinkedinSample", "error to get verifier");
					e.printStackTrace();
				}
			}
		});

		// set progress dialog
		progressDialog.setMessage("Loading...");
		progressDialog.setCancelable(true);
		progressDialog.show();
	}
	 */


	/**
	 * Asyncally call the LindedInHandler class after the dialog is starts showing
	 * @author Canopus
	 *
	 */
	@Override
	public void onLocationChanged(Location location) {
		if(location!=null)
			HomeActivity.mCurrentLocation=location;

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}
}