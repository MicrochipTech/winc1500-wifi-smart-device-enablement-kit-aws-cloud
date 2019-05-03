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

import android.net.wifi.WifiConfiguration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;

import com.amazonaws.mchp.awsprovisionkit.base.MyThreadPool;


public class MsgMulticast extends MsgBase {

	private MsgMulticast() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		super.close();
		this.tryStopUdp();
	}

	static MsgMulticast m_instance = null;


	public static MsgMulticast single() {
		if (null == m_instance) {
			m_instance = new MsgMulticast();
		}
		return m_instance;
	}


	DatagramSocket udpListener = null;

	Boolean bUdpFinish = false;

	Boolean bUdpStarted = false;


	public void tryStopUdp() {
		if (bUdpFinish)
			return;

		bUdpFinish = true;
		if (null != udpListener) {
			try {
				udpListener.close();
			} catch (Exception e) {
			}
		}

		udpListener = null;
		bUdpStarted = false;
		isMonitoring = false;
		MyHelper.d("UDP-8897 stoped.");
		clearCachePlugs();
	}


	Boolean isMonitoring = false;






	public void clearCachePlugs() {
		for (Object sip : PlugList.keySet().toArray()) {
			MsgAdapter ma = PlugList.get(sip);
			if (null != ma)
				try {
					ma.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		PlugCache.clear();
		PlugUdpDely.clear();
		PlugUdpQQ.clear();

		MyHelper.d(">>>>----0000 ~~~~ runFinalization + GC");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
		System.runFinalization();
		System.gc();
	}






	synchronized public MsgAdapter getAdapter(String ipOrMac) {
		if (null == ipOrMac)
			return null;
		MsgAdapter ma = PlugCache.get(ipOrMac);
		if (null == ma)
			ma = PlugList.get(ipOrMac);
		return ma;
	}



	synchronized public MsgAdapter popAdapter(String ip) {
		MsgAdapter ma = PlugList.get(ip);
		if (null != ma)
			try {
				ma.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return ma;
	}

	/**
	 * 
	 * @param ma
	 */
	synchronized public void popAdapter(MsgAdapter ma) {
		if (null != ma)
			popAdapter(ma.getPlugIp().toString());
	}


	public final ArrayList<MsgData> tryBroadcase() {
		ArrayList<MsgData> result = new ArrayList<MsgData>();
		try {
			byte[] data = protocol.makeQueryMAC();
			InetAddress ip = MyHelper.getBroadcastAddress();

			DatagramPacket pack = new DatagramPacket(data, data.length, ip, MyConfig.PLUG_UDP_PORT);
			MulticastSocket ms = new MulticastSocket();
			ms.setLoopbackMode(false);
			ms.send(pack);
			ms.setSoTimeout(TIMEOUT);
			int max = 32;
			while (max-- > 0) {
				byte[] rdata = new byte[64];
				DatagramPacket rdp = new DatagramPacket(rdata, rdata.length);
				ms.receive(rdp);
				rdata = rdp.getData();

				InetAddress ip1 = rdp.getAddress();
				MyHelper.d(ip1.toString());
				MsgData md = new MsgData(rdata);
				md.setPlugIp(ip1);
				md.tryParse();
				result.add(md);
			}

			ms.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			MyHelper.e(e.getMessage());
		}

		return result;
	}

	MsgAdapter discovery = null;

	public MsgData tryDiscovery1() {
		MsgData result = null;
		try {
			int cc1 = 8;
			while (cc1-- > 0) {
				String ip = MyHelper.getWifiIpAddress(true);
				MyHelper.d(">> tryDiscovery1 ip = " + ip);
				if (null == ip || ip.isEmpty() || ip.startsWith("0.0.0")) {
					Thread.sleep(500);
					continue;
				}

				if (null != discovery) {
					discovery.close();
					discovery = null;
				}

				discovery = new MsgAdapter(ip);
				result = discovery.tryDiscovery(1);
				if (result.hasError()) {
                    MyHelper.e(">>>>>>tryDiscovery1  result.hasError is ture");
					discovery.close();
					discovery = null;
					Thread.sleep(200);
					continue;
				} else {

					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public MsgData tryDiscovery2(WifiConfiguration apInfo, String password, String uuid) {
		if (null == this.discovery) {
			MsgData md = new MsgData();
			md.setError("The device might be disconnected.");
			return md;
		}

		WifiSEC sec = WifiSEC.OPEN;
		if (apInfo.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
		{
			sec = WifiSEC.WPA;
		}
		else if (apInfo.wepKeys[0] != null)
		{
			sec = WifiSEC.WEP;
		}

		String[] item = apInfo.SSID.split("\"");
		String ssid = item[1];
		MsgData md2 = discovery.tryDiscovery(2, ssid, password, sec, uuid);
		if (null != md2 && !md2.hasError()) {
			MyHelper.d(">>>>>>>>>>>>>tryDiscovery(3)");
			discovery.tryDiscovery(3);// send CONDONE message to gateway to finish the provision
		}
		return md2;
	}

	public MsgData tryDiscovery2(String ssid, String password, String sec, String uuid) {
		if (null == this.discovery) {
			MsgData md = new MsgData();
			md.setError("The device might be disconnected.");
			return md;
		}

		WifiSEC wifiSec = WifiSEC.OPEN;
		if (sec.contains("WPA"))
		{
			wifiSec = WifiSEC.WPA;
		}
		else if (sec.contains("WEP"))
		{
			wifiSec = WifiSEC.WEP;
		}

		MsgData md2 = discovery.tryDiscovery(2, ssid, password, wifiSec, uuid);
		if (null != md2 && !md2.hasError()) {
			MyHelper.d(">>>>>>>>>>>>>tryDiscovery(3)");
			discovery.tryDiscovery(3);// send CONDONE message to gateway to finish the provision
		}
		return md2;
	}

	public MsgData tryDiscovery2(String ssid, String password, WifiSEC wifiSEC, String uuid) {
		if (null == this.discovery) {
			MsgData md = new MsgData();
			md.setError("The device might be disconnected.");
			return md;
		}
		MsgData md2 = discovery.tryDiscovery(2, ssid, password, wifiSEC, uuid);
		if (null != md2 && !md2.hasError()) {
			discovery.tryDiscovery(3);// send CONDONE message to gateway to finish the provision
		}
		return md2;
	}


}
