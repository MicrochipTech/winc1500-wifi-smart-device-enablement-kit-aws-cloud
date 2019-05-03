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

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;

import com.amazonaws.mchp.awsprovisionkit.base.BaseApp;
//import net.nanmu.view.SingleToast;

import org.apache.http.util.EncodingUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * 帮助类，处理数据转换
 *
 */
public final class MyHelper {


	public static final void printHex(String pre, byte[] data) {
		StringBuilder sb = format2Hex(data);
		d(pre + " 0x" + sb);
	}

	public static final StringBuilder format2Hex(byte[] data) {
		StringBuilder sb = new StringBuilder(bytesToHexText(data));
		for (int i = 2; i < sb.length(); i += 3) {
			sb.insert(i, ' ');
		}
		return sb;
	}


	public static final void v(String msg) {
		if (null != msg)
			Log.v(MyConfig.TAG, msg);
	}


	public static final void i(String msg) {
		if (null != msg)
			Log.i(MyConfig.TAG, msg);
	}


	public static final void d(String msg) {
		if (null != msg)
			Log.d(MyConfig.TAG, msg);
	}


	public static final void e(String msg) {
		if (null != msg)
			Log.e(MyConfig.TAG, msg);
	}


	public static byte[] hexTextToBytes(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}


	private static byte toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c); // 索引=数字
		return b;
	}


	public static final String bytesToHexText(byte[] bArray) {
		return bytesToHexText(bArray, 0, bArray.length);
	}


	public static final String bytesToHexText(byte[] bArray, int start, int length) {
		int len1 = bArray.length - start;
		if (length > len1)
			length = len1;
		StringBuffer sb = new StringBuffer(length);
		String sTemp;
		for (int i = 0; i < length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[start + i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase(Locale.getDefault()));
		}
		return sb.toString();
	}


	public static int byteToInt(byte[] b) {
		int s = 0;
		int len = b.length;
		if (len > 4)
			len = 4;
		for (int i = 0; i < len; i++) {
			int s1 = b[i] & 0xff;
			s |= s1 << (8 * i);
		}

		return s;
	}


	public static byte[] intToByte(int number) {
		int temp = number;
		byte[] b = new byte[4];
		for (int i = 0; i < b.length; i++) {
			b[i] = Integer.valueOf(temp & 0xff).byteValue(); // 将最低位保存在最低位
			temp = temp >> 8; // 向右移8位
		}
		return b;
	}


	public static byte[] longToByte(long number) {
		long temp = number;
		byte[] b = new byte[8];
		for (int i = 0; i < b.length; i++) {
			b[i] = Long.valueOf(temp & 0xff).byteValue();// 将最低位保存在最低位
			temp = temp >> 8; // 向右移8位
		}
		return b;
	}


	public static long byteToLong(byte[] b) {
		long s = 0;
		int len = b.length;
		if (len > 8)
			len = 8;
		for (int i = 0; i < len; i++) {
			long s1 = b[i] & 0xff;
			s |= s1 << (8 * i);
		}

		return s;
	}


	public static byte[] shortToByte(short number) {
		int temp = number;
		byte[] b = new byte[2];
		for (int i = 0; i < b.length; i++) {
			b[i] = Integer.valueOf(temp & 0xff).byteValue();// 将最低位保存在最低位
			temp = temp >> 8; // 向右移8位
		}
		return b;
	}


	public static short byteToShort(byte[] b) {
		short s = 0;
		short s0 = (short) (b[0] & 0xff);// 最低位
		short s1 = (short) (b[1] & 0xff);
		s1 <<= 8;
		s = (short) (s0 | s1);
		return s;
	}

	public static byte[] asciiToBytes(String asciiText) {
		if (null == asciiText)
			return new byte[0];
		byte[] data = EncodingUtils.getAsciiBytes(asciiText);
		return data;
	}



	public static String bytesToAscii(byte[] bArray) {
		return bytesToAscii(bArray, 0, bArray.length);
	}


	public static String bytesToAscii(byte[] bArray, int start, int length) {
		int len1 = bArray.length - start;
		if (length > len1)
			length = len1;
		String s = EncodingUtils.getAsciiString(bArray, start, length);
		return s;
	}



	public static InetAddress getBroadcastAddress() throws IOException {
		WifiManager myWifiManager = (WifiManager) BaseApp.getInstance().getApplicationContext().getSystemService(BaseApp.WIFI_SERVICE);
		DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
		if (myDhcpInfo == null) {
			System.out.println("Could not get broadcast address");
			return null;
		}
		int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}




	public static byte[] subBytes(byte[] src, int start, int len) {
		if ((start + len) > src.length) {
			len = src.length - start;
		}

		if (start >= src.length) {
			start = src.length - 1;
		}

		byte[] sub = new byte[len];
		if (len > 0) {
			System.arraycopy(src, start, sub, 0, len);
		}

		return sub;
	}

	public static byte[] genRandom(int len) {
		byte[] data = SecureRandom.getSeed(len);
		return data;
	}

	private static void saveData(String key, String value) {
		Context ctx = BaseApp.getInstance().getApplicationContext();
		SharedPreferences sp = ctx.getSharedPreferences("ATMEL_CRYPTO_4_DEV", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		if (null == value)
			editor.remove(key);
		else
			editor.putString(key, value);
		editor.commit();
	}

	private static String readData(String key) {
		Context ctx = BaseApp.getInstance().getApplicationContext();
		SharedPreferences sp = ctx.getSharedPreferences("ATMEL_CRYPTO_4_DEV", Context.MODE_PRIVATE);
		String value = sp.getString(key, null);
		return value;
	}

	public static void saveKeys(byte[] pri, byte[] pub) {
		String s_priv = bytesToHexText(pri);
		String s_pub = bytesToHexText(pub);
		saveData("dev_key_public", s_pub);
		saveData("dev_key_private", s_priv);
	}

	public static Object[] readKeys() {
		String spub = readData("dev_key_public");
		String spri = readData("dev_key_private");

		if (null == spub || null == spri)
			return null;
		byte[] bpub = hexTextToBytes(spub);
		byte[] bpri = hexTextToBytes(spri);
		return new Object[] { bpri, bpub };
	}


	public static void saveToken(String mac, byte[] token, boolean isHost) {
		String s1 = (isHost ? "1" : "0") + bytesToHexText(token);
		String key = "mac" + mac;
		saveData(key, s1);
	}

	public static void removeToken(String mac) {
		if (mac == null || mac.isEmpty())
			return;
		String key = "mac" + mac;
		saveData(key, null);
	}


	public static Object[] readToken(String mac) {
		if (null == mac || mac.isEmpty())
			return null;
		String key = "mac" + mac;
		String vv = readData(key);
		if (null == vv)
			return null;
		boolean isHost = vv.charAt(0) == '1';
		byte[] data = hexTextToBytes(vv.substring(1));

		return new Object[] { data, isHost };
	}


	public static String getWifiIpAddress(Boolean isGetRouterIp) {
		try {
			int cc = 0;
			do {
				if (cc++ > 5)
					return null;
				WifiManager wifimanage = (WifiManager) BaseApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
				// check if WiFi is enable or not
				if (!wifimanage.isWifiEnabled()) {
					return null;
				}

				WifiInfo wifiinfo = wifimanage.getConnectionInfo();
				if (!wifiinfo.getSSID().contains("WiFiSmartDevice")) {
					Thread.sleep(500);
					continue;
				}

				String ip = intToIp(wifiinfo.getIpAddress(), isGetRouterIp);

				return ip;
			} while (true);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isLocalIP_OK() {
		String plugIp = getLocalIP();
		if (null == plugIp || plugIp.isEmpty() || plugIp.startsWith("0.0.0")) {
			return false;
		}
		return true;
	}

	public static String getLocalIP() {
		try {
			WifiManager wifimanage = (WifiManager) BaseApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
			if (!wifimanage.isWifiEnabled()) {
				return null;
			}

			WifiInfo wifiinfo = wifimanage.getConnectionInfo();
			String ip = intToIp(wifiinfo.getIpAddress(), false);
			return ip;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	private static String intToIp(int i, Boolean isRouterIp) {
		if (isRouterIp)
			return (i & 0xff) + "." + ((i >> 8) & 0xff) + "." + ((i >> 16) & 0xff) + ".1";
		else
			return (i & 0xff) + "." + ((i >> 8) & 0xff) + "." + ((i >> 16) & 0xff) + "." + ((i >> 24) & 0xff);
	}
}

