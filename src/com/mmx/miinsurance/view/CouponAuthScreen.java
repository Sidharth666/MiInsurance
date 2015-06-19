package com.mmx.miinsurance.view;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.mmx.miinsurance.R;
import com.mmx.miinsurance.util.Utility;

public class CouponAuthScreen extends Activity{
	private static final String _LOG_TAG = "M!Insurance: OTPActivity";
	private Button mBtn_Authenticate;
	private Button mBtn_Regenerate;
	private EditText mET_Authenticate;
	private Dialog mExitAlert;
	private static byte[] key;
	private static final int totalBytesInKey = 32;	//Just change this value to change key size, nothing else: Supported factors: 16,24,32
	private static final String ENCRYPTION_KEY = "MMX_SECRET_KEY";
	private String mHiddenFileName = ".lib";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String checkPassword = readAndDecrypt();
		if ((null != checkPassword) && (0 != checkPassword.trim().length())) {
			if(checkPassword.equalsIgnoreCase("MMX_INSURANCE_AUTHENTICATION_DONE") || checkPassword.equalsIgnoreCase("MMX_REGISTRATION_DONE")){
				Intent regAuth = new Intent(CouponAuthScreen.this, RegisterScreen.class);
				startActivity(regAuth);
				finish();
				return;
			}
		}
		setContentView(R.layout.screen_coupon_auth);
		initializeUI();
		setBtnListeners();
	}
	
	
	private void initializeUI(){
		mBtn_Authenticate = (Button)findViewById(R.id.btn_authenticate);
		mET_Authenticate = (EditText)findViewById(R.id.et_otp);
		mBtn_Regenerate = (Button)findViewById(R.id.btn_regenerate);
	}
	
	
	private void setBtnListeners(){
		mBtn_Authenticate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				validate(mET_Authenticate.getText().toString());
			}
		});
		
		mBtn_Regenerate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent backItn = new Intent(CouponAuthScreen.this, CouponCodeScreen.class);
				backItn.putExtra(Utility.getUtilObj().ITN_BACK_PRESSED_KEY, true);
				startActivity(backItn);
				CouponAuthScreen.this.finish();
			}
		});
	}
	
	private void validate(String passwordEntered) {

		if ((null == passwordEntered) || 0 == passwordEntered.trim().length()) {
			Toast.makeText(CouponAuthScreen.this, "Please enter some Password.", Toast.LENGTH_LONG).show();
		} else {
			String passwd_stored = readAndDecrypt();
			if((null == passwd_stored)|| passwd_stored.trim().length() == 0){
				Toast.makeText(CouponAuthScreen.this, "Fatal error occured, Press back button & re-enter coupon code!", Toast.LENGTH_LONG).show();
			}else if(passwd_stored.trim().equalsIgnoreCase(passwordEntered.trim())){
				encryptAndSave("MMX_INSURANCE_AUTHENTICATION_DONE");
				Intent fireRegister = new Intent(CouponAuthScreen.this, RegisterScreen.class);
				startActivity(fireRegister);
				finish();
			}else{
				Toast.makeText(CouponAuthScreen.this, "Please enter correct Password.", Toast.LENGTH_LONG).show();
			}
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
			Log.e(_LOG_TAG+": Error while encrypting", e.getMessage());
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
	
	@Override
	public void onBackPressed() {
		showExitAlert();
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
				CouponAuthScreen.this.finish();
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
	
	private void emptyUI(){
		mBtn_Authenticate = null;
		mBtn_Regenerate = null;
		mET_Authenticate = null;
		mExitAlert = null;
		key = null;
		mHiddenFileName = null;;
	}
	
	@Override
	protected void onDestroy() {
		emptyUI();
		super.onDestroy();
	}

}
