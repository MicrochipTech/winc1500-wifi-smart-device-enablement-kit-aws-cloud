/*
 * \file
 *
 * Copyright (c) 2016 Microchip Technology Inc. and its subsidiaries.  You may use this
 * software and any derivatives exclusively with Microchip products.
 *
 *
 * THIS SOFTWARE IS SUPPLIED BY MICROCHIP "AS IS". NO WARRANTIES,
 * WHETHER EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS SOFTWARE,
 * INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY,
 * AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL MICROCHIP BE
 * LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE, INCIDENTAL OR CONSEQUENTIAL
 * LOSS, DAMAGE, COST OR EXPENSE OF ANY KIND WHATSOEVER RELATED TO THE
 * SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP HAS BEEN ADVISED OF THE
 * POSSIBILITY OR THE DAMAGES ARE FORESEEABLE.  TO THE FULLEST EXTENT
 * ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL CLAIMS IN ANY WAY
 * RELATED TO THIS SOFTWARE WILL NOT EXCEED THE AMOUNT OF FEES, IF ANY,
 * THAT YOU HAVE PAID DIRECTLY TO MICROCHIP FOR THIS SOFTWARE.
 */
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
