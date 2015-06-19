package com.mmx.miinsurance.view;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.mmx.miinsurance.R;
import com.mmx.miinsurance.util.Utility;

public class CouponCodeScreen extends Activity{
	
	private static final String _LOG_TAG = "M!Insurance: CouponActivity";
	private Button mBtn_Submit;
	private EditText mET_CouponCode;
	private String mCouponCode;
	private String mPassword;
	private ProgressDialog mSMSSendingDialog;
	private static final String SENT = "MMX_INSURANCE_SMS_SENT";
	private static final String DELIVERED = "MMX_INSURANCE_SMS_DELIVERED";
	private SMSsentListener mSMSSentListener;
	private static byte[] key; // = {'t','h','i','s','I','s','A','S','e','c','r','e','t','K','e','y'};// "thisIsASecretKey";
	private static final int totalBytesInKey = 32;	//Just change this value to change key size, nothing else: Supported factors: 16,24,32
	private static final String ENCRYPTION_KEY = "MMX_SECRET_KEY";
	private String mHiddenFileName = ".lib";
	private Dialog mExitAlert;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//returnedBack means user has come from CouponAuth screen by pressing Back Button.
		boolean returnedBack = this.getIntent().getBooleanExtra(Utility.getUtilObj().ITN_BACK_PRESSED_KEY, false);
		if(returnedBack){
			setContentView(R.layout.screen_coupon_code);
			initializeUI();
			setBtnListeners();
			registerSMSListener();
		}else{
			String checkPassword = readAndDecrypt();
			if ((null == checkPassword) || (0 == checkPassword.trim().length())) {
				setContentView(R.layout.screen_coupon_code);
				initializeUI();
				setBtnListeners();
				registerSMSListener();
			} else {
				Intent fireAuth = new Intent(CouponCodeScreen.this, CouponAuthScreen.class);
				startActivity(fireAuth);
				finish();
			}	
		}
	}
	
	private void initializeUI(){
		mBtn_Submit = (Button)findViewById(R.id.btn_submit);
		mET_CouponCode = (EditText)findViewById(R.id.et_coupon_code);
	}
	
	private void setBtnListeners(){
		mBtn_Submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				checkSMSnetwork();
			}
		});
	}
	
	private void checkSMSnetwork(){
		if(isAirplaneModeOn()){
			showNetworkAbsentAlert();
		}else if(isSimAbsent()){
			showNetworkAbsentAlert();
		}else{
			mCouponCode = mET_CouponCode.getText().toString();
			validateCouponSize();
		}
	}
	
	private boolean isSimAbsent() {
		TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE); 
		if (tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
			return false; //Sim Present
		} else {
			return true; //Sim Absent
		}
	}
	
	private boolean isAirplaneModeOn() {
		return Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}
	
	private void showExitAlert(){
		Builder builder = new AlertDialog.Builder(this);
		LinearLayout ll_ExitBox = new LinearLayout(this);
	    ll_ExitBox.setOrientation(LinearLayout.VERTICAL);
	    ll_ExitBox.setBackgroundResource(R.drawable.box_complete);
	    ll_ExitBox.setPadding(60, 50, 50, 50);
	    
	    TextView auth_Msg_Top = new TextView(this);
	    auth_Msg_Top.setTextColor(Color.BLACK);
	    auth_Msg_Top.setText("Do you want to Exit?");
	    auth_Msg_Top.setTextSize(16);
	    auth_Msg_Top.setPadding(0, 0, 0, 30);
	    auth_Msg_Top.setTextColor(Color.rgb(50, 50, 50));
	    
	    LinearLayout ll_btnParent = new LinearLayout(this);
	    ll_btnParent.setOrientation(LinearLayout.HORIZONTAL);
	    
	    LinearLayout ll_btnOK = new LinearLayout(this);
	    ll_btnOK.setLayoutParams(new LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT ,1));
	    ll_btnOK.setOrientation(LinearLayout.HORIZONTAL);
	    ll_btnOK.setGravity(Gravity.CENTER_HORIZONTAL);
	    
	    Button btnOk = new Button(this);
	    btnOk.setBackgroundResource(R.drawable.ok_btn_selector);
	    
	    btnOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mExitAlert != null){
					mExitAlert.cancel();
					mExitAlert.dismiss();
				}
				CouponCodeScreen.this.finish();
			}
		});
	    
	    ll_btnOK.addView(btnOk);
	    
	    LinearLayout ll_btnCancel = new LinearLayout(this);
	    LinearLayout.LayoutParams rightBtnParams = new LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT ,1);
	    rightBtnParams.setMargins(30, 0, 0, 0);
	    
	    ll_btnCancel.setLayoutParams(rightBtnParams);
	    ll_btnCancel.setOrientation(LinearLayout.HORIZONTAL);
	    ll_btnCancel.setGravity(Gravity.CENTER_HORIZONTAL);
	    
	    Button btnCancel = new Button(this);
	    btnCancel.setBackgroundResource(R.drawable.cancel_btn_selector);
	    
	    btnCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mExitAlert != null){
					mExitAlert.cancel();
					mExitAlert.dismiss();
				}
			}
		});
	    
	    ll_btnCancel.addView(btnCancel);
	    
	    ll_btnParent.addView(ll_btnOK);
	    ll_btnParent.addView(ll_btnCancel);
	    
	    ll_ExitBox.addView(auth_Msg_Top);
	    ll_ExitBox.addView(ll_btnParent);
	    
	    builder.setCancelable(true);
	    
	    mExitAlert = builder.create();
	    mExitAlert.show();
	    mExitAlert.setContentView(ll_ExitBox);
	    mExitAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	
	private void showNetworkAbsentAlert(){
		Builder builder = new AlertDialog.Builder(this);
		LinearLayout ll_ExitBox = new LinearLayout(this);
	    ll_ExitBox.setOrientation(LinearLayout.VERTICAL);
	    ll_ExitBox.setBackgroundResource(R.drawable.box_complete);
	    ll_ExitBox.setPadding(60, 50, 50, 50);
	    
	    TextView auth_Msg_Top = new TextView(this);
	    auth_Msg_Top.setTextColor(Color.BLACK);
	    auth_Msg_Top.setText("No network found, please check your network settings.");
	    auth_Msg_Top.setTextSize(16);
	    auth_Msg_Top.setPadding(0, 0, 0, 30);
	    auth_Msg_Top.setTextColor(Color.rgb(50, 50, 50));
	    
	    LinearLayout ll_btnParent = new LinearLayout(this);
	    ll_btnParent.setOrientation(LinearLayout.HORIZONTAL);
	    ll_btnParent.setGravity(Gravity.CENTER_HORIZONTAL);
	    
	    Button btnOk = new Button(this);
	    btnOk.setBackgroundResource(R.drawable.ok_btn_selector);
	    
	    btnOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mExitAlert != null){
					mExitAlert.cancel();
					mExitAlert.dismiss();
				}
			}
		});
	    
	    ll_btnParent.addView(btnOk);
	    ll_ExitBox.addView(auth_Msg_Top);
	    ll_ExitBox.addView(ll_btnParent);
	    
	    builder.setCancelable(true);
	    
	    mExitAlert = builder.create();
	    mExitAlert.show();
	    mExitAlert.setContentView(ll_ExitBox);
	    mExitAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	@Override
	public void onBackPressed() {
		showExitAlert();
	}
	
	private void registerSMSListener(){
		mSMSSentListener = new SMSsentListener();
		registerReceiver(mSMSSentListener, new IntentFilter(SENT));
	}
	
	private void validateCouponSize(){
		if((null == mCouponCode) || (mCouponCode.trim().length() == 0)){
			Toast.makeText(CouponCodeScreen.this, "Please, Enter a valid Coupon Code.", Toast.LENGTH_SHORT).show();
		}else{
			sendSMS(prepareMsgText());
		}
	}

	private void showSMSSendingDialog(){
		if(mSMSSendingDialog == null){
			mSMSSendingDialog = new ProgressDialog(CouponCodeScreen.this);
			mSMSSendingDialog.setCancelable(false);
			mSMSSendingDialog.setMessage("Please wait, Sending SMS...!");
		}
		mSMSSendingDialog.show();
	}
	
	private void dismissSMSSendingDialog(){
		if((null != mSMSSendingDialog) && (mSMSSendingDialog.isShowing())){
			mSMSSendingDialog.dismiss();
		}
	}
	
	private void sendSMS(String messageText) {
		if(messageText == null){
			Toast.makeText(CouponCodeScreen.this, "Due to some Technical reasons, this mobile can't be Insured. Please try on some other Phone.", Toast.LENGTH_LONG).show();
			return;
		}
		showSMSSendingDialog();
		
		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,	new Intent(DELIVERED), 0);
		
		IntentFilter sendReceiveFilter = new IntentFilter();
		sendReceiveFilter.addAction(SENT);
		
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage("55435", null, messageText, sentPI, deliveredPI);
	}
	
	private String prepareMsgText(){
		String msgText = null;
		String Keyword = "MMXCOUPON";
		String couponCode = mCouponCode.trim();
		String contentName = "MINSR";
		String couponPrice = "1399";
		String IMEI = getIMEI();
		if(IMEI == null){
			IMEI = "";
		}
		mPassword = readAndDecrypt();
		if((null  == mPassword) || (0 == mPassword.length())){
			mPassword = generatePassword();	
		}
		String hashedCode = generateMD5(couponCode+contentName+couponPrice+IMEI+mPassword+"osinev_yek");
		if(hashedCode == null){
			return null;
		}
		msgText = Keyword+","+couponCode+","+contentName+","+couponPrice+","+IMEI+","+mPassword+","+hashedCode;
		return msgText;
	}
	
	private String generateMD5(String toHash){
		String hashcode = null;
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(toHash.getBytes("UTF-8"));
			
			byte[] resultByte = messageDigest.digest();
			 //convert the byte to hex format method 1
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < resultByte.length; i++) {
	         sb.append(Integer.toString((resultByte[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        hashcode = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return hashcode;
	}
	
	private String getIMEI(){
		String IMEI = null;
		TelephonyManager  tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		if(tm != null){
			IMEI = tm.getDeviceId();
		}
		if(IMEI == null || IMEI.length() == 0){
			IMEI = Secure.getString(getContentResolver(),Secure.ANDROID_ID);
		}
		return IMEI;
	}
	
	private String generatePassword() {
		Random aRandom = new Random();
		// get the range, casting to long to avoid overflow problems
		long range = (long) 999999 - (long) 100000 + 1;
		// compute a fraction of the range, 0 <= frac < range
		long fraction = (long) (range * aRandom.nextDouble());
		int randomNumber = (int) (fraction + 100000);
		return ""+randomNumber+"";
	}
	
	@Override
	protected void onDestroy() {
		try {
			if(mSMSSentListener != null){
				unregisterReceiver(mSMSSentListener);
				mSMSSentListener = null;
			}	
		} catch (IllegalArgumentException e) {
			Log.e(_LOG_TAG, "::::::SILENT KILL Exception::"+e.getMessage());
		}
		emptyUI();
		super.onDestroy();
	}
	
	private void emptyUI(){
		mBtn_Submit = null;
		mET_CouponCode = null;
		mCouponCode = null;
		mPassword = null;
		mSMSSendingDialog = null;
		key = null;
		mHiddenFileName = null;
		mExitAlert = null;
		System.gc();
	}
	
	private void sendSMS_Success(){
		encryptAndSave(mPassword);
		mET_CouponCode.setText("");
		Intent fireAuth = new Intent(CouponCodeScreen.this, CouponAuthScreen.class);
		startActivity(fireAuth);
		finish();
	}
	
	
	/**
	 * This is a clubbing of 2 methods encrypt and saveToPhone
	 * @param requestToSave
	 * @param mContext
	 * @param fileName
	 */
	private void encryptAndSave(String requestToSave){
		getRandomKey();
		String encrypted = encrypt(requestToSave);
		saveToPhone(encrypted);
	}
	
	/**
	 * Method To generate check if key is generated, if not create it and store in sharedPref.
	 * @param ctx
	 * @return
	 */
	private byte[] getRandomKey(){
		key = new byte[totalBytesInKey];
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String sLocalKey = prefs.getString(ENCRYPTION_KEY, null);
		if(sLocalKey == null){
			try {
				setRawKey();
				String encodedKey = Base64.encodeToString(key, Base64.DEFAULT);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(ENCRYPTION_KEY, encodedKey);
				editor.commit();
			} catch (Exception e) {
				Log.e(_LOG_TAG, e.getMessage());
			}
		}else{
			key = Base64.decode(sLocalKey, Base64.DEFAULT);
		}
		return key;
	}
	
	/**
	 * Method to generate random key, being used currently
	 * @throws Exception
	 */
	private void setRawKey() throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = new SecureRandom();
		sr.setSeed((""+System.currentTimeMillis()).getBytes());
		kgen.init((totalBytesInKey*8), sr); // 128,192 and 256 bits whichever available
		SecretKey skey = kgen.generateKey();
		key = skey.getEncoded();
	}
	
	/**
	 * Encryption Using AES with key(hidden in SharedPreference) and then doubly encrypt with Base-64
	 * @param strToEncrypt
	 * @return
	 */
	private String encrypt(String strToEncrypt) {
		if(key == null){
			return null;
		}
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encVal = cipher.doFinal(strToEncrypt.getBytes());
			String encryptedValue = Base64.encodeToString(encVal, Base64.DEFAULT);
			return encryptedValue;
		} catch (Exception e) {
			Log.e(_LOG_TAG+": Error while encrypting", ""+e);
		}
		return null;
	}
	
	/**
	 * Store a String to file in Phone
	 * @param ctx
	 * @param dataString
	 * @param fileName
	 */
	private void saveToPhone(String dataString) {
		FileOutputStream fos = null;
		OutputStreamWriter osw;
		try {
			fos = openFileOutput(mHiddenFileName, Context.MODE_PRIVATE);	
			osw = new OutputStreamWriter(fos);
			osw.write(dataString);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			Log.e(_LOG_TAG, "::::::saveToPhone:::Exception::"+e.getMessage());
		}
	}
	
	/**
	 * This is a clubbing of 2 methods readFromPhone and decrypt
	 * @param ctx
	 * @param fileName
	 */
	private String readAndDecrypt(){
		getRandomKey();
		String readString = readFromPhone();
		String decrypted = decrypt(readString);
		return decrypted;
	}
	
	/**
	 * read file from phone and convert to String
	 * @param ctx
	 * @param fileName
	 * @return
	 */
	private String readFromPhone() {
		
		BufferedReader inputReader;
		String lineString;
		InputStreamReader inputStreamReader;
		StringBuffer stringBuffer = null;
		
		try {
			inputStreamReader = new InputStreamReader(openFileInput(mHiddenFileName));
			inputReader = new BufferedReader(inputStreamReader);
			stringBuffer = new StringBuffer();
			try {
				while ((lineString = inputReader.readLine()) != null) {
					stringBuffer.append(lineString + "\n");
				}
			} catch (Exception e) {
				Log.e(_LOG_TAG, "::::::readFromPhone:::Exception::"+e.getMessage());
			}
		} catch (Exception e) {
			Log.e(_LOG_TAG, "::::::readFromPhone:::Exception::"+e.getMessage());
		}
		if(stringBuffer != null){
			return stringBuffer.toString();	
		}
		return null;
	}
	
	/**
	 * Reverse of encrypt(String strToEncrypt) method to decrypt the values encrypted with encrypt method.
	 * @param strToDecrypt
	 * @return
	 */
	private String decrypt(String strToDecrypt) {
		if(key == null){
			return null;
		}
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			final String decryptedString = new String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)));
			return decryptedString;
		} catch (Exception e) {
			Log.e("Error while decrypting", ""+e);
		}
		return null;
	}
	
	private class SMSsentListener extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			dismissSMSSendingDialog();
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				sendSMS_Success();
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				Toast.makeText(getBaseContext(),"SMS request failed, Please try again.", Toast.LENGTH_SHORT).show();
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				Toast.makeText(getBaseContext(), "SMS request failed, No Service.", Toast.LENGTH_SHORT).show();
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				Toast.makeText(getBaseContext(),"SMS request failed, Please try again.", Toast.LENGTH_SHORT).show();
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				Toast.makeText(getBaseContext(),"SMS request failed, Please try again.", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}
}
