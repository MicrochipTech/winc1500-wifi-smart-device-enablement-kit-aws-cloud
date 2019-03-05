package com.amazonaws.mchp.awsprovisionkit.task.net;

import com.amazonaws.mchp.awsprovisionkit.model.DeviceModel;

/**
 * 常量定义
 */
public final class MyConfig {

	static {
		S_NEW_LINE = System.getProperty("line.separator", "\n");
	}

	/**
	 * New Line symbol
	 */
	public static final String S_NEW_LINE;


	/**
	 * TCP port
	 */
	public static final int PLUG_TCP_PORT = 8899;

	/**
	 * UDP port
	 */
	public static final int PLUG_UDP_PORT = 8898;


	public static final String TAG = "microchip.sp";

	/**
	 * CRC flag
	 */
	public static final Boolean EnableCrcCheck = true;

	/**
	 * Encrypt flag
	 */
	public static final Boolean EnableEncrypt = true;



	/**
	 * Device connect fail
	 */
	public static final String ERR_ConnectDevFail = "Connect device fail, please try again.";

	/**
	 * Send Provision Data to Device Fail
	 */
	public static final String ERR_SendProvDataFail = "Send Provision data to device get fail, please try again.";

	public static final String ERR_ScanAPFail = "Fail to Scan target AP";
	/**
	 * Execution Success
	 */
	public static final String Success_ConnectDev = " Connect Device Success";
	public static final String Success_SendProvData = " Send Provision Data Success";
	public static final String Success = "SUCCESS";

	/**
	 * Execution Success
	 */
	public static final String Fail = "Fail";


}
