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

import android.net.wifi.ScanResult;
import android.text.TextUtils;


public enum WifiSEC {

	OPEN((byte) 0x01), WPA((byte) 0x02), WEP((byte) 0x03), X802_1((byte) 0x04), EAP((byte) 0x05);

	private final byte value;

	WifiSEC(byte i) {
		this.value = i;
	}

	public byte getValue() {
		return value;
	}

	public static WifiSEC valueOf(int value) {
		switch (value) {
		case 1:
			return OPEN;
		case 2:
			return WPA;
		case 3:
			return WEP;
		case 4:
			return X802_1;
		case 5:
			return EAP;
		default:
			return null;
		}
	}

	public static WifiSEC getScanResultSEC(ScanResult sr) {
		if (null != sr) {
			String capabilities = sr.capabilities;
			if (!TextUtils.isEmpty(capabilities)) {

				if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
					return WifiSEC.WPA;
				} else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
					return WifiSEC.WEP; // WEP
				} else if (capabilities.contains("EAP") || capabilities.contains("eap")) {
					return WifiSEC.EAP;
				} else if (capabilities.contains("X802_1") || capabilities.contains("x802_1")) {
					return WifiSEC.X802_1;
				} else {
					return WifiSEC.OPEN;
				}
			}
		}
		return WifiSEC.OPEN;
	}
}
