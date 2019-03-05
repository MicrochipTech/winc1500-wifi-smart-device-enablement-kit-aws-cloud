package com.amazonaws.mchp.awsprovisionkit.task.net;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

public class ProtocolAdapter {

	public void trySaveSessionKey(byte[] exPubKey64, byte[] random) {
		if (this.m_enableAES)
			return;
		//this.m_sessionKey = ECCrypto.single().getSessionKey(exPubKey64, random);
		if (null != this.m_sessionKey && this.m_sessionKey.length > 0) {
			this.setEnableAES(true);
		}
	}


	byte[] getPackedData(byte cmdId, byte[] parameters) {
		byte[] data = new byte[SF.fiParamStart + parameters.length + SF.lenCRC];
		data[SF.fiSOf] = SF.SOF;
		data[SF.fiCmdID] = cmdId;
		int len = (data.length - SF.fiDataLenH - 1 - SF.lenCRC);
		byte[] bb = MyHelper.intToByte(len);
		data[SF.fiDataLenL] = bb[0];
		data[SF.fiDataLenH] = bb[1];
		data[SF.fiSequence] = this.getSequence();

		for (int i = 0; i < parameters.length; i++) {
			data[SF.fiParamStart + i] = parameters[i];
		}

		// 对数据包做预处理
		data = ProtocolHelper.tryPackage(data, this);
		return data;
	}


	public static MsgData tryParse(byte[] data, byte[] random, ProtocolAdapter protocol) {
		MsgData item = null;
		try {
			data = ProtocolHelper.tryUnPack(data, random, protocol);
			item = new MsgData(data);
			item.tryParse();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			item = new MsgData(data);
			item.setError(e.getMessage());
		}

		return item;
	}


	public byte[] makeDiscoveryReq() {

		byte[] parms = MyHelper.asciiToBytes("Atmel_WiFi_Discovery");
		byte[] data = getPackedData(SF.cmdDiscovery, parms);

		return data;
	}


	public Boolean getEnableAES() {
		return this.m_enableAES;
		// return false;
	}


	public void setEnableAES(Boolean enableAES) {
		this.m_enableAES = enableAES;
	}


	public byte[] getSessionKey() {
		return this.m_sessionKey;
	}


	public byte[] getIV() {
		return this.m_IV;
	}


	private byte m_sequence = 0;

	private Boolean m_enableAES = false;

	private byte[] m_sessionKey = null;

	private byte[] m_IV = null;


	public byte peekSequence() {
		return this.m_sequence;
	}


	public byte getSequence() {
		return ++m_sequence;
	}


	public byte[] makeDiscoveryATC(String wifiSsid, String password, WifiSEC securityType, String uuid) {
		String ssid = wifiSsid; // "mytest81";
		String pwd = password; // "11111111";
		String devName = "kitchen";	// To Do: Hardcord the device name as "light" for test
		byte secType = securityType.getValue();

		byte[] parms = MyHelper.asciiToBytes("CONFIG=0" + ssid + "0" + pwd + "0" + "0" + uuid + "0" + devName);
		int ii = 7;
		int ssidLen = ssid.length();
		parms[ii] = (byte) ssidLen;
		parms[ii + 1 + ssidLen] = (byte) pwd.length();
		parms[ii + 1 + ssidLen + 1 + pwd.length()] = secType;
		parms[ii + 1 + ssidLen + 1 + pwd.length() + 1 ] = (byte)uuid.length();
		parms[ii + 1 + ssidLen + 1 + pwd.length() + 1 + uuid.length() + 1] = (byte)devName.length();

		byte[] data = getPackedData(SF.cmdDiscovery, parms);

		return data;
	}

	public byte[] makeDiscoveryATZ() {

		byte[] parms = MyHelper.asciiToBytes("CONDONE");
		byte[] data = getPackedData(SF.cmdDiscovery, parms);

		return data;
	}

	public ProtocolAdapter() {
		this.m_IV = MyHelper.genRandom(16);
	}

	public byte[] makeQueryMAC() {
		byte[] cid = MyHelper.intToByte(SF.cidMAC);
		// CID=0x00fd, cluster index=0, attribute=
		byte[] parms = new byte[] { cid[0], cid[1], 0, 2 };
		byte[] data = getPackedData(SF.cmdQueryAttr, parms);

		return data;
	}


	public byte[] makeQueryTemp() {
		/*
		 * format = CID=0x0003, cluster index=0, attribute1=[1] attributes
		 */
		byte[] cid = MyHelper.intToByte(SF.cidDevTemp);
		byte[] parms = new byte[] { cid[0], cid[1], 0 };
		byte[] data = getPackedData(SF.cmdQueryCluster, parms);

		return data;
	}

}
