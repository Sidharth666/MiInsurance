package com.mmx.miinsurance.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.mmx.miinsurance.R;

public class RegistrationSuccessScreen extends Activity{
	
	private Dialog mExitAlert;
	private String custId,custEndDate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_thanks);
		Intent intent = getIntent();
		custId = intent.getExtras().getString("MSP_ID");
		custEndDate = intent.getExtras().getString("MSP_DATE");
		
		TextView tv_CustId = (TextView) findViewById(R.id.tv_custidnum);
		TextView tv_CustDate = (TextView) findViewById(R.id.tv_validuptodate);
		tv_CustId.setText(custId);
		tv_CustDate.setText(custEndDate);
		
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
				RegistrationSuccessScreen.this.finish();
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
	
}
