package com.mmx.miinsurance.util;

import java.io.Serializable;

public class ResponseBean implements Serializable{
	
	private String sCustomerId;
	private String sEndDate;
	private String sFaultString;
	private String errorMsg;
	private String parsingError;
	private String unknownServerError;
	private String networkError;
	
	public String getsCustomerId() {
		return sCustomerId;
	}
	public void setsCustomerId(String sCustomerId) {
		this.sCustomerId = sCustomerId;
	}
	public String getsEndDate() {
		return sEndDate;
	}
	public void setsEndDate(String sEndDate) {
		this.sEndDate = sEndDate;
	}
	public String getsFaultString() {
		return sFaultString;
	}
	public void setsFaultString(String sFaultString) {
		this.sFaultString = sFaultString;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public String getParsingError() {
		return parsingError;
	}
	public void setParsingError(String parsingError) {
		this.parsingError = parsingError;
	}
	public String getUnknownServerError() {
		return unknownServerError;
	}
	public void setUnknownServerError(String unknownServerError) {
		this.unknownServerError = unknownServerError;
	}
	public String getNetworkError() {
		return networkError;
	}
	public void setNetworkError(String networkError) {
		this.networkError = networkError;
	}

}
