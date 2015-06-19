package com.mmx.miinsurance.server;

public interface ServerDefine {
	
	public static final int HTTP_SUCCESS = 200;
	public static final int NETWORK_NOT_AVAILABLE = -2304;
	public static final int REQUEST_NOT_AVAILABLE = -2305;
	public static final int ERROR_SOCKET_TIMEOUT = -2306;
	public static final int ERROR_UNKNOWN_HOST = -2307;
	public static final int ERROR_SOCKET_EXCEPTION = -2308;
	public static final int ERROR_MALFORMED_URL = -2309;
	public static final int ERROR_SOME_IO_EXCEPTION = -2310;
	public static final int UNKNOWN_SERVER_ERROR = -2311;
	public static final int SERVER_ERROR = 500;
}
