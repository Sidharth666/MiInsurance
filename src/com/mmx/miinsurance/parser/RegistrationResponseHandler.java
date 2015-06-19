package com.mmx.miinsurance.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RegistrationResponseHandler extends DefaultHandler{

	private boolean bCustomerId;
	private String sCustomerId;
	private boolean bFaultStatus;
	private boolean bEndDate;
	private String sFaultStatus;
	private String sEndDate;
	
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(localName.equalsIgnoreCase("primaryCustomerId")){
			bCustomerId = true;
		}
		else if(localName.equalsIgnoreCase("membershipEndDate")){
			bEndDate = true;
		}
		else if(localName.equalsIgnoreCase("faultstring")){
			bFaultStatus = true;
		}
	}
	
	
	@Override
	public void endElement(String uri, String localName, String qName)throws SAXException {
		if(localName.equalsIgnoreCase("primaryCustomerId")){
			bCustomerId = false;
		}
		else if(localName.equalsIgnoreCase("membershipEndDate")){
			bEndDate = false;
		}
		else if(localName.equalsIgnoreCase("faultstring")){
			bFaultStatus = false;
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)throws SAXException {
		if(bCustomerId){
			sCustomerId = new String(ch, start, length);
		}
		else if(bEndDate){
			sEndDate = new String(ch, start, length);
		}
		else if(bFaultStatus){
			sFaultStatus = new String(ch, start, length);
		}
	}
	
	public String getFaultString(){
		return sFaultStatus.trim();
	}
	
	public String getCustomerId(){
		return sCustomerId;
	}


	public String getsEndDate() {
		return sEndDate;
	}
	
}
