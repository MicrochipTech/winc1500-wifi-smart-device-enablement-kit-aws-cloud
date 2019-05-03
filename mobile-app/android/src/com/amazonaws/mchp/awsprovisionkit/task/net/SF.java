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

/**
 * Static Final
 */
public final class SF {
	/**
	 * SOF value = {@value}
	 */
	public static final byte SOF = 0x5a;

	/**
	 * Format Index SOF, SOF的索引位置
	 */
	public static final byte fiSOf = 0;

	/**
	 * Data Length Low position
	 */
	public static final byte fiDataLenL = 1;

	/**
	 * Data Length High position
	 */
	public static final byte fiDataLenH = 2;

	/**
	 * packet Index position
	 */
	public static final byte fiSequence = 3;

	/**
	 * Command ID position
	 */
	public static final byte fiCmdID = 4;

	/**
	 * Parameters position
	 */
	public static final byte fiParamStart = 9;

	/**
	 * CRC lenght
	 */
	public static final byte lenCRC = 4;


	/**
	 * discovery command
	 */
	public static final byte cmdDiscovery = 0x01;

	/**
	 * discovery response command
	 */
	public static final byte cmdDiscoveryR = (byte) 0x81;

	/**
	 * command report command
	 */
	public static final byte cmdReportErrCode = (byte) 0x9c;

	public static final byte cmdQueryAttr = (byte) 0x11;

	public static final byte cmdQueryCluster = (byte) 0x12;

	public static final byte cmdOtauControlRe = (byte) 0xA9;

	public static final byte cmdOtauDataRe = (byte) 0xA8;

	public static final int cidDevTemp = 0x0003;

	public static final int cidMAC = 0x00FD;


}
