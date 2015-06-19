package com.mmx.miinsurance.server;

import com.mmx.miinsurance.util.ResponseBean;

public interface IResponseListener {

	public void onReceiveResponse(int responseCode, ResponseBean msg);
	
}
