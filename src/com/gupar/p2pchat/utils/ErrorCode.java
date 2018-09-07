package com.gupar.p2pchat.utils;

public class ErrorCode {
	public static final int ERROR_CODE_ACCOUNT_AUTH_OK = 1;
	public static final int ERROR_CODE_ACCOUNT_NOT_MATCH = 0;
	public static final int ERROR_CODE_ACCOUNT_NOT_EXIST = -1;
	public static final int ERROR_CODE_ACCOUNT_ALREADY_LOGIN = -2;
	public static final int ERROR_CODE_ACCOUNT_OTHER_LOGIN = -3;
	public static final int ERROR_CODE_PACKET_ERROR = -4;
	public static final int ERROR_CODE_PACKET_UNKONWN = -5;
	public static final int ERROR_CODE_SERVER_BUSY = -6;
	public static final int ERROR_CODE_SERVER_SHUTDOWN = -7;
	public static final int ERROR_CODE_CLIENT_TIMEOUT = -8;

	public static final int ERROR_CODE_BAD_APP = -11;
	public static final int ERROR_CODE_INVALID_ENCKEYLENGTH = -12;
	public static final int ERROR_CODE_INVALID_ENCNONCELENGTH = -13;
	public static final int ERROR_CODE_HACKED_CLIENT = -14;
}
