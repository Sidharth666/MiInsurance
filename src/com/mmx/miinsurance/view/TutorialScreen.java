package com.mmx.miinsurance.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.mmx.miinsurance.R;
import com.mmx.miinsurance.util.Utility;

public class TutorialScreen extends FragmentActivity {

	private PagerAdapter mPagerAdapter;
	private Dialog mExitAlert;
	PackageInfo pInfo;
	private Context context;
	private ProgressDialog mUpdateDialog;
	private ProgressDialog mDownloadDialog;
	private static final String APK_NAME = "MiInsurance.apk";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_tutorial);
		context = this;
		if (Utility.getUtilObj().checkNetwork(this) == 1) {
			new UpdateCheck().execute();
		} else {
//			Utility.getUtilObj().showErrorAlert("No Network Available. Please check your network settings",context);
		}

		initialisePaging();
	}
	/*
	 * Check server for Version number and update if a new version is available
	 */
	private class UpdateCheck extends AsyncTask<String, Void, StringBuilder> {

		@Override
		protected void onPreExecute() {
			showRegistrationProgress();
		}

		@Override
		protected StringBuilder doInBackground(String... params) {
			String sURL = Utility.getUtilObj().getVCheckURL();
			URL updateURL;
			URLConnection urlConn;
			HttpURLConnection httpConn = null;
			StringBuilder total = null;

			try {
				updateURL = new URL(sURL);
				urlConn = updateURL.openConnection();
				httpConn = (HttpURLConnection) urlConn;
				if (httpConn != null) {
					httpConn.setDoOutput(true);
					httpConn.setDoInput(true);
					httpConn.setConnectTimeout(3 * 6000);
					httpConn.setReadTimeout(4 * 6000);
					httpConn.setRequestMethod("GET");

					// /** Receive data **///
					InputStream response = null;
					total = new StringBuilder();
					int responseCode = httpConn.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						response = httpConn.getInputStream();
						if (response != null) {
							BufferedReader r = new BufferedReader(new InputStreamReader(response));
							String line;
							while ((line = r.readLine()) != null) {
								total.append(line);
							}
							
						} else {
							response = httpConn.getErrorStream();
							if (response != null) {
								BufferedReader r = new BufferedReader(new InputStreamReader(response));
								String line;
								while ((line = r.readLine()) != null) {
									total.append(line);
								}
							} else {
								total.append("UNKNOWN_SERVER_ERROR");
							}
							Utility.getUtilObj().showErrorAlert(total.toString(),context);
						}
						response.close();
					}

				}
			} catch (Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Utility.getUtilObj().showErrorAlert("Some error occured. Please try again.",context);						
					}
				});
				
				e.printStackTrace();
			}

			return total;
		}

		@Override
		protected void onPostExecute(StringBuilder result) {
			dismissRegistrationProgress();
			if(result.toString() != ""){
				String[] appInfo = result.toString().split(",");
				String updatedVersion = appInfo[1].substring(8);
				try {
					pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					String currentVersion = pInfo.versionName;
					if (!(updatedVersion.equalsIgnoreCase(currentVersion))) {
						new AlertDialog.Builder(context).setTitle("Update Required").setMessage("Please update to continue...").setPositiveButton("Update",
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog,int which) {
												if (Utility.getUtilObj().checkNetwork(context) == 1) {
													downloadUpdatedApk();
												} else {
//													Utility.getUtilObj().showErrorAlert("No Network Available. Please check your network settings",context);
												}

											}
										})
								.setNegativeButton("Quit",new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog,int which) {
												finish();
											}
										}).setCancelable(false).show();
					}
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
			}else{
				Utility.getUtilObj().showErrorAlert("Some error occured. Please try again.",context);
			}
		}
	}

	protected void downloadUpdatedApk() {
		new DownloadApk().execute();
	}

	private void showRegistrationProgress() {
		if (null == mUpdateDialog) {
			mUpdateDialog = new ProgressDialog(context);
			mUpdateDialog.setCancelable(false);
			mUpdateDialog.setMessage("Please wait...");
		}
		mUpdateDialog.show();
	}

	private void dismissRegistrationProgress() {
		if ((null != mUpdateDialog) && (mUpdateDialog.isShowing())) {
			mUpdateDialog.dismiss();
		}
	}
	
	private void showDownloadProgress(){
			mDownloadDialog = new ProgressDialog(context);
			mDownloadDialog.setCancelable(false);
			mDownloadDialog.setMessage("Downloading file..");
			mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDownloadDialog.show();
	}
	/*
	 * Download updated apk to sd card and install.
	 */

	private class DownloadApk extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			showDownloadProgress();
//			showRegistrationProgress();
		}

		@Override
		protected String doInBackground(String... params) {
			int count = 0;
			try {
				URL url = new URL(Utility.getUtilObj().getUpdatedAPKURL());
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setDoOutput(true);
				c.connect();

				
				int lenghtOfFile = c.getContentLength();
				
				String PATH = Environment.getExternalStorageDirectory()+ "/download/";
				File file = new File(PATH);
				file.mkdirs();
				File outputFile = new File(file, APK_NAME);
				FileOutputStream fos = new FileOutputStream(outputFile);
				InputStream is ;
				int responseCode = c.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					is = c.getInputStream();

					byte[] buffer = new byte[1024];
					int total = 0;
					while ((count = is.read(buffer)) != -1) {
						total += count;
						int progress = (int)(total*100)/lenghtOfFile;
			            publishProgress(""+progress);
						fos.write(buffer, 0, count);
					}
					fos.close();
					is.close();
				}else {
					StringBuilder total = new StringBuilder();;
					is = c.getErrorStream();
					if (is != null) {

						BufferedReader r = new BufferedReader(new InputStreamReader(is));
						String line;
						while ((line = r.readLine()) != null) {
							total.append(line);
						}
					}else{
						total.append("UNKNOWN_SERVER_ERROR");
					}
					Utility.getUtilObj().showErrorAlert(total.toString(),context);
				}
				

				Intent promptInstall = new Intent(Intent.ACTION_VIEW);
				promptInstall.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory()+ "/download/"+ APK_NAME)),
								"application/vnd.android.package-archive");
				promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(promptInstall);

			} catch (final Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Utility.getUtilObj().showErrorAlert(e.getMessage(),context);						
					}
				});
				
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... progress) {
			mDownloadDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(String result) {
			mDownloadDialog.dismiss();
		}

	}

	/**
	 * Initialise the fragments to be paged
	 */
	private void initialisePaging() {

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, TuteScreenOne.class.getName()));
		fragments.add(Fragment.instantiate(this, TuteScreenTwo.class.getName()));
		fragments.add(Fragment.instantiate(this,TuteScreenThree.class.getName()));
		this.mPagerAdapter = new com.mmx.miinsurance.uihelper.PagerAdapter(super.getSupportFragmentManager(), fragments);
		ViewPager pager = (ViewPager) super.findViewById(R.id.viewpager);
		pager.setAdapter(this.mPagerAdapter);
	}

	@Override
	protected void onDestroy() {
		mPagerAdapter = null;
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		showExitAlert();
	}

	private void showExitAlert() {
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
		ll_btnOK.setLayoutParams(new LayoutParams(0,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		ll_btnOK.setOrientation(LinearLayout.HORIZONTAL);
		ll_btnOK.setGravity(Gravity.CENTER_HORIZONTAL);

		Button btnOk = new Button(this);
		btnOk.setBackgroundResource(R.drawable.ok_btn_selector);

		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mExitAlert != null) {
					mExitAlert.cancel();
					mExitAlert.dismiss();
				}
				TutorialScreen.this.finish();
			}
		});

		ll_btnOK.addView(btnOk);

		LinearLayout ll_btnCancel = new LinearLayout(this);
		LinearLayout.LayoutParams rightBtnParams = new LayoutParams(0,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		rightBtnParams.setMargins(30, 0, 0, 0);

		ll_btnCancel.setLayoutParams(rightBtnParams);
		ll_btnCancel.setOrientation(LinearLayout.HORIZONTAL);
		ll_btnCancel.setGravity(Gravity.CENTER_HORIZONTAL);

		Button btnCancel = new Button(this);
		btnCancel.setBackgroundResource(R.drawable.cancel_btn_selector);

		btnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mExitAlert != null) {
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
		mExitAlert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}
	
	

}
