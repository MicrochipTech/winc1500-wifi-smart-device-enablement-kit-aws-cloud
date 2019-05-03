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

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * WiFi Control
 *
 */
public class WlanAdapter {
	public static ScanResult getSelectedWlan() {
		return cSelectedWlan.get();
	}
	private static boolean scanWaitingFlag = false;

	public static void setSelectedWlan(ScanResult sr) {
		if (null != cSelectedWlan) {
			cSelectedWlan.clear();
			cSelectedWlan = null;
		}

		cSelectedWlan = new WeakReference<ScanResult>(sr);
	}

	private static WeakReference<ScanResult> cSelectedWlan;

	WeakReference<Activity> mWeak;
	WifiManager mWifi;
	WifiReceiver mReceiver = null;
	Object mSyncNetworkChange = null;
	Object mSyncWifiScaning = null;
	Object mSyncWifiChange = null;
	public List<ScanResult> mWifiList;
	String mTargetSSID = null;
	final static int TIMEOUT = 8000;

	public WlanAdapter(Activity activity) {
		this.mSyncNetworkChange = new Object();
		this.mSyncWifiScaning = new Object();
		this.mSyncWifiChange = new Object();
		this.mWeak = new WeakReference<Activity>(activity);
		this.mTargetSSID = "";
	}

	void ensureWifi() {
		Activity act = this.mWeak.get();
		if (act == null)
			return;

		if (null == mWifi) {
			this.mWifi = (WifiManager) act.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			this.doRegister();
		}

		if (!this.mWifi.isWifiEnabled()) {
			this.mWifi.setWifiEnabled(true);
			this.waitingWifiChange();
		}
	}

	public void unRegister() {
		Activity context = this.mWeak.get();
		if (null == context || null == this.mReceiver)
			return;
		try {
			MyHelper.d(">>>> unRegister receiver");
			context.unregisterReceiver(this.mReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doRegister() {
		Context context = this.mWeak.get();
		if (null == context)
			return;

		if (null == this.mReceiver) {
			MyHelper.d(">>>> create receiver");
			this.mReceiver = new WifiReceiver();
		}

		context.registerReceiver(this.mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		context.registerReceiver(this.mReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		context.registerReceiver(this.mReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
	}

	public List<ScanResult> tryGetWifiList() {
		Calendar cPlan = Calendar.getInstance();
		cPlan.add(Calendar.SECOND, 1);
		Activity act = this.mWeak.get();
		if (act == null) {
			this.mWifi = null;
			return null;
		}
		this.mWifiList = null;
		try {
			this.ensureWifi();
			this.mWifi.startScan();
			this.waitingWifiScaned();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//try {
		//	Thread.sleep(10000);
		//} catch (Exception e) {
		//}


		if (cPlan.after(Calendar.getInstance()))
			this.waitingWifiScaned(800);

		MyHelper.d(">>>> return scan result");

		return this.mWifiList;
	}

	/**
	 * remove current network from the list of configured network
	 *
	 * @return success=true, fail=false
	 */
	public void removeCurrentAP() {

		this.ensureWifi();
		WifiInfo connInfo = this.mWifi.getConnectionInfo();

		int networkId = connInfo.getNetworkId();
		MyHelper.d(">>>> [removeCurrentAP] remove networkId " + networkId);
		this.mWifi.removeNetwork(networkId);



		return;
	}
	/**
	 * get the current connected AP information
	 *
	 * @return success=true, fail=false
	 */
	public WifiConfiguration getCurrentAPInfo() {

		this.ensureWifi();
		WifiInfo connInfo = this.mWifi.getConnectionInfo();

		int networkId = connInfo.getNetworkId();

		final List<WifiConfiguration> configurations = this.mWifi.getConfiguredNetworks();

		for (final WifiConfiguration config : configurations) {
			if (networkId == config.networkId) {
				return config;
			}
		}
		MyHelper.e("[getCurrentAPInfo] cannot get current AP Info from the WifiConfiguration pool...");
		return null;
	}

	/**
	 * try connect AP
	 *
	 * @return success=true, fail=false
	 */
	public Boolean tryConnectWlan(WifiConfiguration apInfo) {
		this.mWifi.disconnect();

		if (apInfo == null) {
			this.mTargetSSID = "";
			return false;
		}

		try {
			this.ensureWifi();
			if (WlanHelper.isConnectedTo(apInfo.SSID, this.mWifi))
				return true; // already connect to target AP

			MyHelper.d(">>>> disconnect the WiFi connection");
			this.mWifi.disconnect();

			int tryTimes = 2;
			MyHelper.d(">>>> try connect to AP:"+ apInfo.SSID);
			while (tryTimes-- > 0) {

				Boolean r2 = WlanHelper.connectToConfiguredNetwork(this.mWifi, apInfo);
				if (!r2)
					break;

				this.waitingNetworkChange();
				if (WlanHelper.isConnectedTo(apInfo.SSID, this.mWifi))
					MyHelper.d(">>>> connect success to AP:" + apInfo.SSID);
				return true;
			}
			MyHelper.d(">>>> fail to connect AP:"+ apInfo.SSID);


		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ensureConnected(apInfo.SSID))
				return true;
		}

		return false;
	}

	public boolean connectToNewWifi(String ssid)
	{
		boolean ret;
		MyHelper.d(">>>> connectToKnownWifi Enter");
		this.mTargetSSID = '"' + ssid + '"';

		Activity act = this.mWeak.get();
		if (act == null)
			return false;

		WifiManager wifiManager = (WifiManager) act.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		
		
final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();

		for (final WifiConfiguration config : configurations) {
			MyHelper.d( "# configure ssid =" + config.SSID);
			if (this.mTargetSSID.equals(config.SSID) ) {
				ret = wifiManager.removeNetwork(config.networkId);
				MyHelper.d( "# removeNetwork return " + ret);
			}
		}
		
		WifiConfiguration wifiConfiguration = new WifiConfiguration();

		wifiConfiguration.SSID='"'+ssid+'"';
		wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);


		int res = wifiManager.addNetwork(wifiConfiguration);
		MyHelper.d( "# addNetwork returned " + res);
		boolean b = wifiManager.enableNetwork(res, true);
		MyHelper.d( "# enableNetwork returned " + b);
		wifiManager.setWifiEnabled(true);

		boolean changeHappen = wifiManager.saveConfiguration();
		if (res != -1 && changeHappen) {
			MyHelper.d("# Change happen: " + ssid);
		} else {
			MyHelper.d( "# Change NOT happen");
		}

		this.waitingNetworkChange();
		if (WlanHelper.isConnectedTo(ssid, this.mWifi)) {
			MyHelper.d(">>>> connect success to " + ssid);
			return true;
		}
		else {
			MyHelper.d(">>>> connect fail to " + ssid);
			return false;
		}
	}
	/**
	 * try connect AP
	 * 
	 * @return success=true, fail=false
	 */
	public Boolean tryConnectWlan(final ScanResult scanResult, String password) {
		if (scanResult == null) {
			this.mTargetSSID = "";
			return false;
		}

		this.mTargetSSID = '"' + scanResult.SSID + '"';
		Activity act = this.mWeak.get();
		if (act == null) {
			return false;
		}

		try {
			this.ensureWifi();
			if (WlanHelper.isConnectedTo(scanResult.SSID, this.mWifi))
				return true; // already connect to target AP

			this.mWifi.disconnect();


			WifiConfiguration network = null;

			network = WlanHelper.getWifiConfiguration(scanResult, this.mWifi);


			if (null != network) {
				network = WlanHelper.getWifiConfiguration(scanResult, this.mWifi);
				if (null != network) {
					MyHelper.d(">>>> try02 remove configed network ID=" + network.networkId);
					this.mWifi.removeNetwork(network.networkId);
					this.mWifi.saveConfiguration();
					MyHelper.d(">>>> try02 remove configed SSID=" + scanResult.SSID);
				}
			}


			Boolean rr = WlanHelper.connectNewConfig(this.mWifi, scanResult, password);


			MyHelper.d(">>>> try03 connect new SSID=" + scanResult.SSID);
			if (rr) {
				this.waitingNetworkChange(10000);
				if (WlanHelper.isConnectedTo(scanResult.SSID, this.mWifi)) {
					MyHelper.d(">>>> try03 connect success to" + scanResult.SSID);
					return true;
				}

			}

			//rr = WlanHelper.connectNewConfig(this.mWifi, scanResult, password);
			this.mWifi.disconnect();
			network = WlanHelper.getWifiConfiguration(scanResult, this.mWifi);
			MyHelper.d(">>>> try04 connect SSID = " + network.SSID);
			rr = WlanHelper.connectToConfiguredNetwork(this.mWifi, network);
			if (rr) {
				this.waitingNetworkChange(10000);
				if (WlanHelper.isConnectedTo(scanResult.SSID, this.mWifi)) {
					MyHelper.d(">>>> try04 connect success to" + scanResult.SSID);
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ensureConnected(scanResult.SSID))
				return true;
		}

		return false;
	}

	Boolean ensureConnected(String ssid) {
		try {
			int cc1 = 10, cc2 = 1;
			while (cc1-- > 0) {
				waitingNetworkChange(500);
				if (WlanHelper.isConnectedTo(ssid, this.mWifi)) {
					if (cc2-- > 0)
						continue;
					else
						return true;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private class WifiReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			String action = intent.getAction();
			MyHelper.d(">>>> WIFI_RECEIVER = " + action);
			switch (action) {
			case WifiManager.WIFI_STATE_CHANGED_ACTION:
				if (mWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
					MyHelper.d(">>>>----001 WIF state = " + mWifi.getWifiState());
					synchronized (mSyncWifiChange) {
						mSyncWifiChange.notify();
					}
				}
				break;
			case WifiManager.NETWORK_STATE_CHANGED_ACTION:
				WifiInfo wfconn = mWifi.getConnectionInfo();
				MyHelper.d(">>>>----WIFI network = " + wfconn.getSSID());
				if (null != wfconn && mTargetSSID.equals(wfconn.getSSID())) {
					MyHelper.d(">>>>----002 WIFI network = " + wfconn.getSSID());
					synchronized (mSyncNetworkChange) {
						mSyncNetworkChange.notify();
					}
				}
				break;
			case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:

				if (scanWaitingFlag)
					mWifiList = mWifi.getScanResults();

				if (null != mWifiList) {
					MyHelper.d(">>>>----003 WIFI Scan result = " + mWifiList.size());
					synchronized (mSyncWifiScaning) {

						mSyncWifiScaning.notify();
					}
				}
				break;
			}
		}
	}


	void waitingNetworkChange() {
		waitingNetworkChange(TIMEOUT);
	}


	void waitingNetworkChange(int timeout) {
		try {
			MyHelper.d(">>>> WIFI - wait conn...");
			synchronized (this.mSyncNetworkChange) {
				this.mSyncNetworkChange.wait(timeout);
			}
			MyHelper.d(">>>> WIFI - conn finish.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	void waitingWifiScaned() {
		waitingWifiScaned(TIMEOUT);
	}


	void waitingWifiScaned(int timeout) {
		try {
			MyHelper.d(">>>> WIFI - wait scan...");
			scanWaitingFlag = true;
			synchronized (this.mSyncWifiScaning) {
				this.mSyncWifiScaning.wait(timeout);
			}
			scanWaitingFlag = false;
			MyHelper.d(">>>> WIFI - scan finish.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void waitingWifiChange() {
		try {
			MyHelper.d(">>>> WIFI - wait WiFi enable...");
			synchronized (this.mSyncWifiChange) {
				this.mSyncWifiChange.wait(TIMEOUT);
			}
			MyHelper.d(">>>> WIFI - wifi enable finish.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
