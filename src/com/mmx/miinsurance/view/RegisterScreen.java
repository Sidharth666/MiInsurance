package com.mmx.miinsurance.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.mmx.miinsurance.R;
import com.mmx.miinsurance.server.CreateRequestHandler;
import com.mmx.miinsurance.server.IResponseListener;
import com.mmx.miinsurance.server.ServerDefine;
import com.mmx.miinsurance.util.ResponseBean;

public class RegisterScreen extends Activity implements IResponseListener{
	
	private static final String _LOG_TAG = "M!Insurance: RegisterActivity";
	private Dialog mExitErrorAlert; // I am using single object, so that only one Alert exist at a time
	private EditText mET_Name;
	private EditText mET_Address;
	private EditText mET_Email;
	private EditText mET_Mobile;
	private TextView mTV_DOP;
	private TextView mTV_Disclaimer;
	private TextView mTV_Expand;
	private TextView mTV_TermsCond;
	private CheckBox mCB_AgreeTerms;
	private Button mRegisterDetails;
	private WebView webView;
	private String mSt_Name;
	private String mSt_Address;
	private String mSt_Email;
	private String mSt_Mobile;
	private String mSt_IMEI;
	private String mSt_DOP;
	private String mSt_Model;
	private boolean mB_DisclaimerAgreed;
	private boolean mB_DisclaimerExpanded;
	 /** Constant EMAIL_ADDRESS_PATTERN, from Pattern.java */
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern
            .compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@"
                    + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                    + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+");
	
	private DatePickerDialog mDatePickerDialog;
	private DatePickerDialog.OnDateSetListener mDateSetListener;
	private int mYear;
	private int mMonth;
	private int mDay;
	private Context con;
	private ProgressDialog mRegistrationDialog;
	private CreateRequestHandler mHandler;
	private Handler mUIHandler;
	private static byte[] key; // = {'t','h','i','s','I','s','A','S','e','c','r','e','t','K','e','y'};// "thisIsASecretKey";
	private static final int totalBytesInKey = 32;	//Just change this value to change key size, nothing else: Supported factors: 16,24,32
	private static final String ENCRYPTION_KEY = "MMX_SECRET_KEY";
	private String mHiddenFileName = ".lib";
	private String mFileName = ".libResp";
	private ResponseBean message;
	PackageInfo pInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_register);
		con = this;
		String checkStatus = readAndDecrypt();
		
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String version = pInfo.versionName;
		int versioncode = pInfo.versionCode;
		if(null == checkStatus){
			initializeUI();
			setTermsConditionExpansion();
			setDisclaimerExpansion();
			initializeDatePicker();
			initializeUIHandler();
			setRegisterBtnListener();
		}else if(checkStatus.equalsIgnoreCase("MMX_REGISTRATION_DONE")){
			ResponseBean resp = readRespFromPhone();
			showThanksAlert(resp);
		}else{
			initializeUI();
			setTermsConditionExpansion();
			setDisclaimerExpansion();
			initializeDatePicker();
			initializeUIHandler();
			setRegisterBtnListener();	
		}
	}
	
	private void setTermsConditionExpansion() {

		mTV_TermsCond.setPaintFlags(mTV_TermsCond.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mTV_TermsCond.setText("Terms & Conditions");
		mTV_TermsCond.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showTermsCondAlert();
			}
		});
	
	}

	protected void showTermsCondAlert() {

		String URL = "file:///android_asset/m!insurance.html";
//		M-Insurance Terms and Conditions_abridged-testing.html";
		
		Builder builder = new AlertDialog.Builder(this);
		
	    builder.setCancelable(false);
	    
	    mExitErrorAlert = builder.create();
	    mExitErrorAlert.show();
	    mExitErrorAlert.setContentView(R.layout.dialog_terms_condition);
	    Button btn_Back = (Button) mExitErrorAlert.findViewById(R.id.btn_back);
	    
	    webView = (WebView) mExitErrorAlert.findViewById(R.id.webView);
	    WebSettings webSettings = webView.getSettings(); 
	    webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN); 
	    webView.loadUrl(URL);
	    
	    btn_Back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mExitErrorAlert.dismiss();
				
			}
		});
	    
	    mExitErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		
	}
	
	private void initializeUIHandler(){
		mUIHandler = new Handler(){
			public void handleMessage(Message msg) {
				dismissRegistrationProgress();
				int what = msg.what;
				message = (ResponseBean) msg.obj;
				if (ServerDefine.HTTP_SUCCESS == what) {
					/*if(message.getParsingError().equalsIgnoreCase("PARSING_ERROR") || message.getUnknownServerError().equalsIgnoreCase("UNKNOWN_SERVER_ERROR")){
						showSuccessAlert("Dear "+mSt_Name+".\nLooks like you are facing a problem.\nPlease call us on 1800 407 333 333 and we will be happy to help you", -1);
					}else{*/
						
						saveRespToPhone(message);
//						Toast.makeText(RegisterScreen.this, readRespFromPhone().getsCustomerId(), Toast.LENGTH_LONG).show();
						if (readRespFromPhone()!=null) {
							encryptAndSave("MMX_REGISTRATION_DONE");
						}
						showSuccessAlert(message, 1);
//					}"Dear "+mSt_Name+".\nWelcome to OneAssist.\nYour OneAssist customer id is "+message.getsCustomerId()+ message.getsEndDate()
				}else {
					if (ServerDefine.NETWORK_NOT_AVAILABLE == what){
						message.setErrorMsg("No Data connection found.");
					}else if(ServerDefine.REQUEST_NOT_AVAILABLE == what){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you are facing a problem.\nPlease call us on 1800 407 333 333 and we will be happy to help you");
						mET_Name.setText("");
						mET_Address.setText("");
						mET_Email.setText("");
						mET_Mobile.setText("");
						mTV_DOP.setText("");
					}else if((ServerDefine.ERROR_MALFORMED_URL == what) || (ServerDefine.ERROR_UNKNOWN_HOST == what)){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you are facing a problem.\nPlease call us on 1800 407 333 333 and we will be happy to help you");
					}else if(ServerDefine.ERROR_SOCKET_TIMEOUT == what){
						message.setErrorMsg("Connection timed out. Please try again.");
					}else if(message.getsFaultString().equalsIgnoreCase("BP.INVALID.PRIMARY.FIRST.NAME")){
						message.setErrorMsg("Dear Customer, Looks like you have entered an invalid Name.\nPlease enter name with only alphabets & try again.");
						mET_Name.setText("");
					}else if(message.getsFaultString().equalsIgnoreCase("BP.INVALID.PRIMARY.EMAIL.ID")){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you have entered an invalid Email Id.\nPlease enter a valid email_id & try again.");
						mET_Email.setText("");
					}else if(message.getsFaultString().equalsIgnoreCase("BP.INVALID.PRIMARY.MOBILE.NO")){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you have entered an invalid mobile number.\nPlease enter a valid 10 digit mobile number & try again.");
						mET_Mobile.setText("");
					}else if(message.getsFaultString().equalsIgnoreCase("BP.INVALID.ADDRESS.LINE1")){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you have entered an invalid address.\nPlease enter a valid address less than 500 characters & try again.");
						mET_Address.setText("");
					}else if((message.getsFaultString().equalsIgnoreCase("BP.INVALID.PRIMARY.MOBILE.PURCHASE.YEAR")) || (message.getsFaultString().equalsIgnoreCase("BP.INVALID.PRIMARY.MOBILE.PURCHASE.MONTH"))){
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you have selected an invalid Handset purchase date.\nPlease select valid date(month-year) & try again.");
						mTV_DOP.setText("");
					}else {
						message.setErrorMsg("Dear "+mSt_Name+".\nLooks like you are facing a problem.\nPlease call us on 1800 407 333 333 and we will be happy to help you");
					}
					showErrorAlert(message.getErrorMsg());
				}
			};
		};
	}
	
	private void showRegistrationProgress(){
		if(null == mRegistrationDialog){
			mRegistrationDialog = new ProgressDialog(RegisterScreen.this);
			mRegistrationDialog.setCancelable(false);
			mRegistrationDialog.setMessage("Please wait...");
		}
		mRegistrationDialog.show();
	}
	
	private void dismissRegistrationProgress(){
		if((null != mRegistrationDialog) && (mRegistrationDialog.isShowing())){
			mRegistrationDialog.dismiss();
		}
	}
	
	private void setRegisterBtnListener(){
		mRegisterDetails.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setValues();
				validateValues();
			}
		});
	}

	private void setValues(){
		mSt_Name = mET_Name.getText().toString();
		mSt_Address = mET_Address.getText().toString();
		mSt_Email = mET_Email.getText().toString();
		mSt_Mobile = mET_Mobile.getText().toString();
		mSt_DOP = mTV_DOP.getText().toString();
		mB_DisclaimerAgreed = mCB_AgreeTerms.isChecked();
		presetValues();
	}
	
	private void validateValues(){
		if((null == mSt_Name) || (0 == mSt_Name.trim().length())){
			Toast.makeText(RegisterScreen.this, "Please enter name.", Toast.LENGTH_LONG).show();
		}else if(!validateName(mSt_Name)){
			Toast.makeText(RegisterScreen.this, "Name must contain only letters/ alphabets.", Toast.LENGTH_LONG).show();
		}else if((null == mSt_Address) || (0 == mSt_Address.trim().length())){
			Toast.makeText(RegisterScreen.this, "Please enter address.", Toast.LENGTH_LONG).show();
		}else if((null == mSt_Email) || (mSt_Email.trim().length() == 0) || !validateEmail(mSt_Email)){
			Toast.makeText(RegisterScreen.this, "Please enter a valid email id.", Toast.LENGTH_SHORT).show();
		}else if((null == mSt_Mobile) || (mSt_Mobile.trim().length() != 10)){
			Toast.makeText(RegisterScreen.this, "Please enter a valid 10 digit mobile number.", Toast.LENGTH_SHORT).show();
		}else if((null == mSt_DOP) || (mSt_DOP.trim().length() == 0)){
			Toast.makeText(RegisterScreen.this, "Please select handset purchase month.", Toast.LENGTH_SHORT).show();
		}else if(!validateDobAgainstToday()){
			Toast.makeText(RegisterScreen.this, "Month entered cannot be greater than current month", Toast.LENGTH_LONG).show();
		}else if(!mB_DisclaimerAgreed){
			Toast.makeText(RegisterScreen.this, "Please agree terms & conditions, to register.", Toast.LENGTH_SHORT).show();
		}else{
			makeRegisterRequest();	
		}
	}
	
	private void makeRegisterRequest(){
		showRegistrationProgress();
		mHandler = new CreateRequestHandler(this, this);
		mHandler.requestCustomerRegistration(mSt_Name, mSt_Address, mSt_Email, mSt_Mobile, mSt_IMEI, (mMonth+1),mYear, mSt_Model);
	}
	
	private boolean validateDobAgainstToday(){
		boolean valid = true;
		final Calendar c = Calendar.getInstance();
		int currentYear = c.get(Calendar.YEAR);
		int currentMonth = c.get(Calendar.MONTH);
		if(mYear > currentYear){
			valid = false;
		}else if(mYear == currentYear){
			if(mMonth > currentMonth){
				valid = false;
			}
		}
		return valid;
	}
	
	private boolean validateName(String name){
		String pattern= "^[a-zA-Z ]*$";
        if(name.matches(pattern)){
            return true;
        }
        return false;
	}
	
	private boolean validateEmail(String email){
		return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
	}
	
	/**
	 * This method corresponds to showing and performing actions launched from DatePicker
	 */
	private void initializeDatePicker() {
		mTV_DOP.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDatePickerDialog = null;
				mDatePickerDialog = customDatePicker();
				mDatePickerDialog.show();
				mDatePickerDialog.setCancelable(true);
			}
		});
		currentDate();
		
		mDateSetListener = new DatePickerDialog.OnDateSetListener() {

			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mYear = year;
				mMonth = monthOfYear;
				mDay = dayOfMonth;
				updateDisplay();
			}
		};
	}
	

    private DatePickerDialog customDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
        try {
            Field[] datePickerDialogFields = dpd.getClass().getDeclaredFields();
            for (Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals("mDatePicker")) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker = (DatePicker) datePickerDialogField.get(dpd);
                    Field datePickerFields[] = datePickerDialogField.getType().getDeclaredFields();
                    for (Field datePickerField : datePickerFields) {
                        if ("mDayPicker".equals(datePickerField.getName())|| "mDaySpinner".equals(datePickerField.getName())) {
                            datePickerField.setAccessible(true);
                            Object dayPicker = new Object();
                            dayPicker = datePickerField.get(datePicker);
                            ((View) dayPicker).setVisibility(View.GONE);
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return dpd;
    }
	
	private void currentDate() {
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Date update on first load and date picker change.
	 */
	private void updateDisplay() {
		if(mTV_DOP != null){
			mTV_DOP.setText(new StringBuilder()
			// Month is 0 based so add 1
			.append(mMonth + 1).append("/").append(mYear).append(""));	
		}
	}
	
	private void setDisclaimerExpansion(){
		
		mTV_Expand.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mB_DisclaimerExpanded){
					mTV_Disclaimer.setMaxLines(4);
					mTV_Expand.setText("[+Expand]");
					mB_DisclaimerExpanded = false;
				}else{
					mTV_Disclaimer.setMaxLines(100);
					mTV_Expand.setText("[-Collapse]");
					mB_DisclaimerExpanded = true;
				}
			}
		});
	}
	
	private void initializeUI(){
		mET_Name = (EditText)findViewById(R.id.et_name);
		mET_Address = (EditText)findViewById(R.id.et_address);
		mET_Email = (EditText)findViewById(R.id.et_email);
		mET_Mobile = (EditText)findViewById(R.id.et_mobile);
		mTV_TermsCond = (TextView) findViewById(R.id.tv_TermsCond);
		mTV_DOP = (TextView)findViewById(R.id.tv_DOP);
		mTV_Disclaimer = (TextView)findViewById(R.id.tv_disclaimerText);
		mTV_Expand = (TextView)findViewById(R.id.tv_Expand);
		mCB_AgreeTerms = (CheckBox)findViewById(R.id.cb_AgreeTerms);
		mRegisterDetails = (Button)findViewById(R.id.btn_register);
	}
	
	private void presetValues() {
		mSt_IMEI = getIMEI();
		if ((null == mSt_IMEI) || (0 == mSt_IMEI.trim().length())) {
			mSt_IMEI = "";
		}

		mSt_Model = getDeviceName();
		if (0 == mSt_Model.trim().length()) {
			mSt_Model = "Micromax";
		}
	}
	
	public String getDeviceName() {
		String model = Build.MODEL;
		if((null != model)){
			if((model.trim().length() > 20)){
				model = model.trim().substring(0, 20);
			}
			return model.toUpperCase();	
		}else{
			return "";
		}
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
		
		if((null != IMEI) && (IMEI.trim().length() > 16)){
			IMEI = IMEI.trim().substring(0, 16);
		}
		return IMEI;
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
	
	private void saveRespToPhone(ResponseBean dataObj) {
		FileOutputStream fos = null;
		ObjectOutputStream oos;
		try {
			fos = new FileOutputStream(new File(getFilesDir(),"")+File.separator+mFileName);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(dataObj);
			oos.flush();
			oos.close();
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
				Log.e(_LOG_TAG, "::::::readFromPhone:::Exception: inner:"+e.getMessage());
			}
		} catch (Exception e) {
			Log.e(_LOG_TAG, "::::::readFromPhone:::Exception: outer:"+e.getMessage());
		}
		if(stringBuffer != null){
			return stringBuffer.toString();	
		}
		return null;
	}
	
	
	private ResponseBean readRespFromPhone() {
		
		ObjectInputStream input;
		ResponseBean resp = null;
		
		try {
			 
			   input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(),"")+File.separator+mFileName)));
			   resp = (ResponseBean) input.readObject();
			   input.close();
			   return resp;
	 
		   } catch (Exception e) {
			Log.e(_LOG_TAG, "::::::readFromPhone:::Exception: outer:"+e.getMessage());
		}
		
		return resp;
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
	
	
	@Override
	public void onBackPressed() {
		showExitAlert();
	}
	
	private void showThanksAlert(ResponseBean message){
		Builder builder = new AlertDialog.Builder(this);
	    
	    builder.setCancelable(false);
	    
	    mExitErrorAlert = builder.create();
	    mExitErrorAlert.show();
	    mExitErrorAlert.setContentView(R.layout.alert_thanks);
	    
	    TextView custName = (TextView) mExitErrorAlert.findViewById(R.id.tv_custidnum);
	    custName.setText(""+message.getsCustomerId());
	    TextView custId = (TextView) mExitErrorAlert.findViewById(R.id.tv_validuptodate);
	    custId.setText(""+message.getsEndDate());
	    
	    Button btnOk = (Button) mExitErrorAlert.findViewById(R.id.btnOk);
	    btnOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				if(mExitErrorAlert != null){
					mExitErrorAlert.cancel();
					mExitErrorAlert.dismiss();
				}
				RegisterScreen.this.finish();
			
			}
		});
	    
	    mExitErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	private void showSuccessAlert(final ResponseBean message, final int toFinish){
		Builder builder = new AlertDialog.Builder(this);
	    builder.setCancelable(false);
	    
	    mExitErrorAlert = builder.create();
	    mExitErrorAlert.show();
	    mExitErrorAlert.setContentView(R.layout.alert_success);
	    
	    TextView custName = (TextView) mExitErrorAlert.findViewById(R.id.tv_custname);
	    custName.setText("Dear "+mSt_Name);
	    TextView custId = (TextView) mExitErrorAlert.findViewById(R.id.tv_custid);
	    custId.setText(""+message.getsCustomerId());
	    
	    Button btnOk = (Button) mExitErrorAlert.findViewById(R.id.btnOk);
	    btnOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mExitErrorAlert != null){
					mExitErrorAlert.cancel();
					mExitErrorAlert.dismiss();
				}
				if(toFinish == 1){
					Intent intent = new Intent(RegisterScreen.this, RegistrationSuccessScreen.class);
					intent.putExtra("MSP_ID", message.getsCustomerId());
					intent.putExtra("MSP_DATE", message.getsEndDate());
					startActivity(intent);
					finish();
				}
			}
		});
	    mExitErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	private void showErrorAlert(String message){
		Builder builder = new AlertDialog.Builder(this);
		LinearLayout ll_ExitBox = new LinearLayout(this);
	    ll_ExitBox.setOrientation(LinearLayout.VERTICAL);
	    ll_ExitBox.setBackgroundResource(R.drawable.box_complete);
	    ll_ExitBox.setPadding(30, 50, 25, 50);
	    
	    TextView auth_Msg_Top = new TextView(this);
	    auth_Msg_Top.setTextColor(Color.BLACK);
	    auth_Msg_Top.setText(message);
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
				if(mExitErrorAlert != null){
					mExitErrorAlert.cancel();
					mExitErrorAlert.dismiss();
				}
			}
		});
	    
	    ll_btnOK.addView(btnOk);
	    ll_btnParent.addView(ll_btnOK);
	    ll_ExitBox.addView(auth_Msg_Top);
	    ll_ExitBox.addView(ll_btnParent);
	    
	    builder.setCancelable(true);
	    
	    mExitErrorAlert = builder.create();
	    mExitErrorAlert.show();
	    mExitErrorAlert.setContentView(ll_ExitBox);
	    mExitErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
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
				if(mExitErrorAlert != null){
					mExitErrorAlert.cancel();
					mExitErrorAlert.dismiss();
				}
				RegisterScreen.this.finish();
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
				if(mExitErrorAlert != null){
					mExitErrorAlert.cancel();
					mExitErrorAlert.dismiss();
				}
			}
		});
	    
	    ll_btnCancel.addView(btnCancel);
	    
	    ll_btnParent.addView(ll_btnOK);
	    ll_btnParent.addView(ll_btnCancel);
	    
	    ll_ExitBox.addView(auth_Msg_Top);
	    ll_ExitBox.addView(ll_btnParent);
	    
	    builder.setCancelable(true);
	    
	    mExitErrorAlert = builder.create();
	    mExitErrorAlert.show();
	    mExitErrorAlert.setContentView(ll_ExitBox);
	    mExitErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	@Override
	protected void onDestroy() {
		emptyUI();
		super.onDestroy();
	}
	
	private void emptyUI(){
		mExitErrorAlert = null;
		mET_Name = null;
		mET_Address = null;
		mET_Email = null;
		mET_Mobile = null;
		mTV_DOP = null;
		mTV_Disclaimer = null;
		mTV_Expand = null;
		mCB_AgreeTerms = null;
		mRegisterDetails = null;
		
		mSt_Name = null;
		mSt_Address = null;
		mSt_Email = null;
		mSt_Mobile = null;
		mSt_IMEI = null;
		mSt_DOP = null;
		mSt_Model = null;
		mDatePickerDialog = null;
		mDateSetListener = null;
		mRegistrationDialog = null;
		mHandler = null;
		mUIHandler = null;
		key = null;
		mHiddenFileName = null;;
		System.gc();
	}


	@Override
	public void onReceiveResponse(int responseCode, ResponseBean msg) {
		Message messg = new Message();
		messg.what = responseCode;
		messg.obj = msg;
		mUIHandler.sendMessage(messg);
	}
}
