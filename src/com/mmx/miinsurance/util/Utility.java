package com.mmx.miinsurance.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mmx.miinsurance.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

/**
 * Helper class: Singleton Pattern
 */
public class Utility {

	private static Utility mUtilObj;
	private Dialog mErrorAlert;

	public static Utility getUtilObj() {
		if (null == mUtilObj) {
			mUtilObj = new Utility();
		}
		return mUtilObj;
	}

	public final String ITN_BACK_PRESSED_KEY = "ITN_BACK_PRESSED";

	/**
	 * Access network state of phone
	 * 
	 * @param context
	 * @return
	 */
	public int checkNetwork(Context context) {
		ConnectivityManager cm = null;
		if (context != null) {
			cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		if (cm == null) {
			return 0;
		}
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null) {
			return 0;
		} else {
			return 1;
		}
	}

	public String getRegistrationURL() {
		// return "http://182.18.170.232:8080/OASYS/webservice/partner?wsdl";
		// //UAT
		// return "https://www.oneassist.in/OASYS/webservice/partner?wsdl";
		// //PRODUCTION
		return "https://ws.oneassist.in/OASYS/webservice/partner?wsdl";
	}
	
	public String getVCheckURL() {
		return "http://182.74.18.118/vas_app/check_update.php";
//		return "http://192.168.1.21/vas_app/check_update.php";
	}
	
	public String getUpdatedAPKURL() {
		return "http://182.74.18.118/vas_app/MiInsurance.apk";
//		return "http://192.168.1.21/vas_app/MiInsurance.apk";
	}
	
	public void showErrorAlert(String message, final Context context){
		Builder builder = new AlertDialog.Builder(context);
		LinearLayout ll_ExitBox = new LinearLayout(context);
	    ll_ExitBox.setOrientation(LinearLayout.VERTICAL);
	    ll_ExitBox.setBackgroundResource(R.drawable.box_complete);
	    ll_ExitBox.setPadding(30, 50, 25, 50);
	    
	    TextView auth_Msg_Top = new TextView(context);
	    auth_Msg_Top.setTextColor(Color.BLACK);
	    auth_Msg_Top.setText(message);
	    auth_Msg_Top.setTextSize(16);
	    auth_Msg_Top.setPadding(0, 0, 0, 30);
	    auth_Msg_Top.setTextColor(Color.rgb(50, 50, 50));
	    
	    LinearLayout ll_btnParent = new LinearLayout(context);
	    ll_btnParent.setOrientation(LinearLayout.HORIZONTAL);
	    
	    LinearLayout ll_btnOK = new LinearLayout(context);
	    ll_btnOK.setLayoutParams(new LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT ,1));
	    ll_btnOK.setOrientation(LinearLayout.HORIZONTAL);
	    ll_btnOK.setGravity(Gravity.CENTER_HORIZONTAL);
	    
	    Button btnOk = new Button(context);
	    btnOk.setBackgroundResource(R.drawable.ok_btn_selector);
	    
	    btnOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mErrorAlert != null){
					mErrorAlert.cancel();
					mErrorAlert.dismiss();
					((Activity) context).finish();
				}
			}
		});
	    
	    ll_btnOK.addView(btnOk);
	    ll_btnParent.addView(ll_btnOK);
	    ll_ExitBox.addView(auth_Msg_Top);
	    ll_ExitBox.addView(ll_btnParent);
	    
	    builder.setCancelable(true);
	    
	    mErrorAlert = builder.create();
	    mErrorAlert.setCancelable(false);
	    mErrorAlert.show();
	    mErrorAlert.setContentView(ll_ExitBox);
	    mErrorAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}

	public void makeAllTrusted() throws KeyManagementException,

	NoSuchAlgorithmException {

		TrustManager[] trustAllCerts = new TrustManager[] {

		new X509TrustManager() {

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {

			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {

			}

		} };

		SSLContext sc = SSLContext.getInstance("BKS");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	}
	
	// always verify the host - dont check for certificate
	public final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	/**
	 * Trust every server - dont check for any certificate
	 */
	public void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
