package com.mmx.miinsurance.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.util.Log;

import com.mmx.miinsurance.parser.RegistrationResponseHandler;
import com.mmx.miinsurance.util.ResponseBean;
import com.mmx.miinsurance.util.Utility;

public class CreateRequestHandler {

	private final String _TAG = "CreateRequestHandler";
	private IResponseListener mListener;
	private Thread httpThread;
	private Context mContext;
	private byte[] mReqData;
	private ResponseBean bean;
	
	public CreateRequestHandler(Context ctx, IResponseListener listener){
		mContext = ctx;
		mListener = listener;
		bean = new ResponseBean();
	}
	
	public void requestCustomerRegistration(String firstName, String addrLine1, String emailId, String mobileNumber, String imeiNo1, int mobilePurchaseMonth, int mobilePurchaseYear, String mobileModel){
		makeRequest(firstName, addrLine1, emailId, mobileNumber, imeiNo1, mobilePurchaseMonth, mobilePurchaseYear, mobileModel);
	}
	
	private void makeRequest(final String firstName, final String addrLine1, final String emailId, final String mobileNumber, final String imeiNo1, final int mobilePurchaseMonth, final int mobilePurchaseYear, final String mobileModel){
		httpThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if(null == mListener){
					Log.e(_TAG, "::Returning::Handler Null::Nobody available to listen my response::");
					localCleanUp();
					return;
				}
				if(0 == Utility.getUtilObj().checkNetwork(mContext)){
					bean.setNetworkError("Returning:Network not available");
					mListener.onReceiveResponse(ServerDefine.NETWORK_NOT_AVAILABLE, bean);
					localCleanUp();
					return;
				}
				prepareRequest(firstName, addrLine1, emailId, mobileNumber, imeiNo1, mobilePurchaseMonth, mobilePurchaseYear, mobileModel);
				
				if(null == mReqData){
//					mListener.onReceiveResponse(ServerDefine.REQUEST_NOT_AVAILABLE, "Returning:Request data not available");
					localCleanUp();
					return;
				}
				
				
				String sURL = Utility.getUtilObj().getRegistrationURL();
				URL updateURL;
				URLConnection urlConn;
				HttpsURLConnection httpConn = null;
				try {
					updateURL = new URL(sURL);
					Utility.getUtilObj().trustAllHosts();
					urlConn = updateURL.openConnection();
//					Utility.getUtilObj().makeAllTrusted();
					httpConn = (HttpsURLConnection)urlConn;
					httpConn.setHostnameVerifier(Utility.getUtilObj().DO_NOT_VERIFY);
					if(httpConn != null){
						httpConn.setDoOutput(true);
						httpConn.setDoInput(true);
						httpConn.setConnectTimeout(3*6000);
						httpConn.setReadTimeout(4*6000);
						httpConn.setRequestMethod("POST");
						httpConn.setRequestProperty("Content-Type", "text/xml");
						OutputStream osw = httpConn.getOutputStream();
						osw.write(mReqData);
						osw.flush();

						///** Receive data **///
						InputStream response = null;
						StringBuilder total = new StringBuilder();

						int responseCode = httpConn.getResponseCode();
						if(responseCode == HttpURLConnection.HTTP_OK){
							response = httpConn.getInputStream();
							if (response != null) {
								BufferedReader r = new BufferedReader(new InputStreamReader(response));
								String line;
								while ((line = r.readLine()) != null) {
								    total.append(line);
								}
								handleSuccess(total.toString());
							}else{
								bean.setUnknownServerError("UNKNOWN_SERVER_ERROR");
								mListener.onReceiveResponse(ServerDefine.HTTP_SUCCESS, bean);
							}
						}else{
							response = httpConn.getErrorStream();
							if (response != null) {
								BufferedReader r = new BufferedReader(new InputStreamReader(response));
								String line;
								while ((line = r.readLine()) != null) {
								    total.append(line);
								}
								handleError(total.toString());
							}else{
								bean.setUnknownServerError("UNKNOWN_SERVER_ERROR");
								mListener.onReceiveResponse(ServerDefine.UNKNOWN_SERVER_ERROR, bean);
							}
						}
					}
				} catch (IOException e) {
					Log.e(_TAG, "Exception Occurred:"+e);
					String errorMsg = null;
					if(e.getMessage() != null){
						bean.setErrorMsg(e.getMessage());
					}
					if (e instanceof SocketTimeoutException) {
						mListener.onReceiveResponse(ServerDefine.ERROR_SOCKET_TIMEOUT, bean);
					} else if (e instanceof UnknownHostException) {
						mListener.onReceiveResponse(ServerDefine.ERROR_UNKNOWN_HOST, bean);
					} else if (e instanceof SocketException) {
						mListener.onReceiveResponse(ServerDefine.ERROR_SOCKET_EXCEPTION, bean);
					} else if (e instanceof MalformedURLException) {
						mListener.onReceiveResponse(ServerDefine.ERROR_MALFORMED_URL, bean);
					} else {
						mListener.onReceiveResponse(ServerDefine.ERROR_SOME_IO_EXCEPTION, bean);
					}
				}finally{
					if (httpConn != null) {
						httpConn.disconnect();
						urlConn = null;
						httpConn = null;
						localCleanUp();
					}
				}
			}
		});
		
		httpThread.start();
	}

	private void prepareRequest(String firstName, String addrLine1, String emailId, String mobileNumber, String imeiNo1, int mobilePurchaseMonth, int mobilePurchaseYear, String mobileModel){
		String requestS = getHeader() + getAddressRequest(addrLine1) + getOrderInfo() + getCustomerInfo(firstName, emailId, mobileNumber, imeiNo1, mobilePurchaseMonth, mobilePurchaseYear, mobileModel)+ getFooter();
		//Log.e("PUNEET TEST", "TEST REQUEST: "+requestS);
		mReqData = requestS.getBytes();
	}
	
	private String getHeader(){
		String header =  "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:par=\"http://partner.webservices.common.oasys.oneassist.com\"> "+
		   " <soapenv:Header> "+
		   	"<wsse:Security SOAP-ENV:mustUnderstand=\"1\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"> "+
				"<wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"> "+
					//"<wsse:Username>admin@oneassist.in</wsse:Username> "+ //UAT
					//"<wsse:Password>oasys</wsse:Password> "+				//UAT
					"<wsse:Username>MMXWEBSVC</wsse:Username> "+   //Production
					"<wsse:Password>M!cR0mAx123</wsse:Password> "+				//Production
				"</wsse:UsernameToken> "+
			"</wsse:Security> "+
		"</soapenv:Header> "+
		  "<soapenv:Body> "+
		      "<par:createCustomer> "+
		"<createCustomerRequest> ";
					return header;
	}
	
	private String getAddressRequest(String addrLine1){
		String address = "<customerAddressInfo> <addrLine1>"+addrLine1+"</addrLine1> </customerAddressInfo> ";
		return address;
	}
	
	/**
	 * As informed by saurabh.chandra@oneassist.in Partner BU code in not required on partner code to send at production.
	 * 12 March 2014: toms.varghese@happiestminds.com + saurabh.chandra@oneassist.in, confirmed to send partnerBUCode = 376
	 * @return
	 */
	private String getOrderInfo(){
		//String orderinfo = "<customerOrderInfo> <partnerCode>29</partnerCode> <partnerBUCode>308</partnerBUCode> <paymentMode>COD</paymentMode> <planCode>59</planCode></customerOrderInfo> ";
		String orderinfo = "<customerOrderInfo> <partnerCode>38</partnerCode> <partnerBUCode>376</partnerBUCode> <paymentMode>COD</paymentMode> <planCode>59</planCode></customerOrderInfo> ";
		return orderinfo;
	}
	
	private String getCustomerInfo(String firstName, String emailId, String mobileNumber, String imeiNo1, int mobilePurchaseMonth, int mobilePurchaseYear, String mobileModel){
		String customerInfo =  "<primaryCustomerInfo>"+
		" <firstName>"+firstName+"</firstName>"+
		" <emailId>"+emailId+"</emailId>"+
		" <mobileNumber>"+mobileNumber+"</mobileNumber>"+
		" <mobileOs>AND</mobileOs>"+
		" <imeiNo1>"+imeiNo1+"</imeiNo1>"+
		" <mobilePurchaseMonth>"+mobilePurchaseMonth+"</mobilePurchaseMonth>"+
		" <mobilePurchaseYear>"+mobilePurchaseYear+"</mobilePurchaseYear>"+
		" <mobileMake>MMax</mobileMake>"+
		" <mobileModel>"+mobileModel+"</mobileModel>"
		+" </primaryCustomerInfo> ";
		return customerInfo;
	}
	
	private String getFooter(){
		String footer =  "</createCustomerRequest> </par:createCustomer> </soapenv:Body> </soapenv:Envelope>";
		return footer;
	}
	
	private void localCleanUp(){
		mListener = null;
		mContext = null;
		mReqData = null;
		httpThread = null;
		System.gc();
	}
	
	private void handleSuccess(String successMsg){
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			RegistrationResponseHandler myXmlHandler = new RegistrationResponseHandler();
			xr.setContentHandler(myXmlHandler);

			InputStream in = null;
			in = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + successMsg).getBytes("UTF-8"));
			xr.parse(new InputSource(in));
			bean.setsCustomerId(myXmlHandler.getCustomerId());
			bean.setsEndDate(myXmlHandler.getsEndDate());
			mListener.onReceiveResponse(ServerDefine.HTTP_SUCCESS, bean);
		} catch (Exception e) {
			if(e.getMessage() != null){
//				mListener.onReceiveResponse(ServerDefine.HTTP_SUCCESS, "PARSING_ERROR");	
			}
			e.printStackTrace();
		} 
	}
	
	private void handleError(String errorMsg){
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			RegistrationResponseHandler myXmlHandler = new RegistrationResponseHandler();
			xr.setContentHandler(myXmlHandler);

			InputStream in = null;
			in = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + errorMsg).getBytes("UTF-8"));
			xr.parse(new InputSource(in));
			bean.setsFaultString(myXmlHandler.getFaultString());
			mListener.onReceiveResponse(ServerDefine.SERVER_ERROR, bean);
		} catch (Exception e) {
			if(e.getMessage() != null){
				bean.setParsingError("PARSING_ERROR");
				mListener.onReceiveResponse(ServerDefine.SERVER_ERROR, bean);	
			}
			e.printStackTrace();
		} 
	}
	
}
