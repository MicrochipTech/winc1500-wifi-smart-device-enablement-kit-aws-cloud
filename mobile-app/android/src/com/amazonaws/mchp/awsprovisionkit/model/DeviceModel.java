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

package com.amazonaws.mchp.awsprovisionkit.model;

import java.io.IOException;
import java.io.Serializable;

import com.amazonaws.mchp.awsprovisionkit.task.net.MsgAdapter;
import com.amazonaws.mchp.awsprovisionkit.task.net.MsgBase;
import com.amazonaws.mchp.awsprovisionkit.task.net.WifiSEC;


public class DeviceModel extends BaseModel implements Serializable {
	/**
	 * 
	 */

	private static final long serialVersionUID = 1L;

	private int id;
	private String deviceType;// 设备型号
	private String deviceName;// 设备名称
	private String deviceSSID;// 设备 SSID --无效
	private String devicePWD;// 设备密码 --无效
	private String deviceID;// 设备 ID
	private String deviceMAC;// 设备 mac
	private String deviceIP;// 设备 IP
	private String deviceStandard;// 插座标准
	private String deviceVersion;// 设备版本号
	private String deviceSerialNo;// 设备序列号

	private float deviceActiveEnergy;// 设备累计电量
	private String highestTemperature;// 最高温度
	private String lowestTemperature;// 最低温度

	private boolean isResourId;// 是否资源id
	private String iconPath;// 本地文件
	private int resId;// 资源id


	private boolean isOverheat;// 是否温度过高


	private WifiSEC deviceSecureType;

	private boolean isOnLine;


	private String itemTitle;

	private int itemImageID;

	private boolean isCheck;// 是否要批量删除

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDeviceSSID() {
		return deviceSSID;
	}

	public void setDeviceSSID(String deviceSSID) {
		this.deviceSSID = deviceSSID;
	}

	public String getDevicePWD() {
		return devicePWD;
	}

	public void setDevicePWD(String devicePWD) {
		this.devicePWD = devicePWD;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public String getDeviceMAC() {
		return deviceMAC;
	}

	public void setDeviceMAC(String deviceMAC) {
		this.deviceMAC = deviceMAC;
	}

	public String getDeviceIP() {
		return deviceIP;
	}

	public void setDeviceIP(String deviceIP) {
		this.deviceIP = deviceIP;
	}

	public WifiSEC getDeviceSecureType() {
		return deviceSecureType;
	}

	public void setDeviceSecureType(WifiSEC deviceSecureType) {
		this.deviceSecureType = deviceSecureType;
	}

	public boolean isOnLine() {
		return isOnLine;
	}

	public void setOnLine(boolean isOnLine) {
		this.isOnLine = isOnLine;
	}

	public String getDeviceStandard() {
		return deviceStandard;
	}

	public void setDeviceStandard(String deviceStandard) {
		this.deviceStandard = deviceStandard;
	}

	public String getDeviceVersion() {
		return deviceVersion;
	}

	public void setDeviceVersion(String deviceVersion) {
		this.deviceVersion = deviceVersion;
	}

	public String getDeviceSerialNo() {
		return deviceSerialNo;
	}

	public void setDeviceSerialNo(String deviceSerialNo) {
		this.deviceSerialNo = deviceSerialNo;
	}

	public float getDeviceActiveEnergy() {
		return deviceActiveEnergy;
	}

	public void setDeviceActiveEnergy(float deviceActiveEnergy) {
		this.deviceActiveEnergy = deviceActiveEnergy;
	}

	public String getHighestTemperature() {
		return highestTemperature;
	}

	public void setHighestTemperature(String highestTemperature) {
		this.highestTemperature = highestTemperature;
	}

	public String getLowestTemperature() {
		return lowestTemperature;
	}

	public void setLowestTemperature(String lowestTemperature) {
		this.lowestTemperature = lowestTemperature;
	}

	public MsgAdapter tryGetMsgAdapter() {
		MsgAdapter ma = MsgBase.getMsgAdapter(this.deviceMAC);
		if (null == ma && this.deviceIP != null && !this.deviceIP.isEmpty())
			ma = MsgBase.getMsgAdapter(this.deviceIP);
		if (this.deviceIP == null && null != ma && ma.getPlugIp() != null) {
			this.setDeviceIP(ma.getPlugIp().toString());
		}

		if (null != ma && !this.getDeviceMAC().equalsIgnoreCase(ma.getMAC())) {
			try {
				ma.close();
			} catch (IOException e) {
			}
			ma = null;
		}
		this.isOnLine = ma != null;
		return ma;
	}

	public String getItemTitle() {
		return itemTitle;
	}

	public void setItemTitle(String itemTitle) {
		this.itemTitle = itemTitle;
	}

	public int getItemImageID() {
		return itemImageID;
	}

	public void setItemImageID(int itemImageID) {
		this.itemImageID = itemImageID;
	}

	public String getIconPath() {
		return iconPath;
	}

	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}

	public boolean isResourId() {
		return isResourId;
	}

	public void setResourId(boolean isResourId) {
		this.isResourId = isResourId;
	}

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public boolean isOverheat() {
		return isOverheat;
	}

	public void setOverheat(boolean isOverheat) {
		this.isOverheat = isOverheat;
	}

	public boolean isCheck() {
		return isCheck;
	}

	public void setCheck(boolean isCheck) {
		this.isCheck = isCheck;
	}

}
