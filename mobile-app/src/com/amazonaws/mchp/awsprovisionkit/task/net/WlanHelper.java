package com.amazonaws.mchp.awsprovisionkit.task.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

/**
 * WifConfig
 *
 */
public class WlanHelper {
	private static final int MAX_PRIORITY = 99999;
	private static final String BSSID_ANY = "any";
	public static final WlanSecurities ConfigSec = WlanSecurities.newInstance();


	public static Boolean isConnectedTo(String ssid, WifiManager wifi) {
		WifiInfo connInfo = wifi.getConnectionInfo();
		if (ssid.equals(connInfo.getSSID()) || connInfo.getSSID().equals("\"" + ssid + "\""))
			return true; // 已连接此WIFI
		return false;
	}


	public static void openWlanSetting(Context ctx) {
		Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
		ctx.startActivity(intent);
	}

	/**
	 * Get configured network information
	 *
	 */
	public static WifiConfiguration getWifiConfiguration(ScanResult scanResult, WifiManager wifiMgr) {
		if (null == scanResult)
			return null;

		final String ssid = scanResult.SSID + "";
		final String ssidQuoted = convertToQuotedString(ssid);
		if (ssid.length() == 0) {
			return null;
		}

		final String bssid = scanResult.BSSID;
		String security = ConfigSec.getScanResultSecurity(scanResult);

		final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();

		for (final WifiConfiguration config : configurations) {
			if (ssid.equals(config.SSID) || ssidQuoted.equals(config.SSID)) {
				if (config.BSSID == null || BSSID_ANY.equals(config.BSSID) || bssid == null
						|| bssid.equals(config.BSSID)) {
					final String configSecurity = ConfigSec.getWifiConfigurationSecurity(config);
					if (security.equals(configSecurity)) {
						return config;
					}
				}
			}
		}
		return null;
	}

	public static WifiConfiguration getWifiConfiguration(final WifiManager wifiMgr,
			final WifiConfiguration configToFind, String security) {
		final String ssid = configToFind.SSID;
		if (ssid.length() == 0) {
			return null;
		}

		final String bssid = configToFind.BSSID;

		if (security == null) {
			security = ConfigSec.getWifiConfigurationSecurity(configToFind);
		}

		final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();

		for (final WifiConfiguration config : configurations) {
			if (config.SSID == null || !ssid.equals(config.SSID)) {
				continue;
			}
			if (config.BSSID == null || BSSID_ANY.equals(config.BSSID) || bssid == null || bssid.equals(config.BSSID)) {
				final String configSecurity = ConfigSec.getWifiConfigurationSecurity(config);
				if (security.equals(configSecurity)) {
					return config;
				}
			}
		}
		return null;
	}

	public static Boolean connectNewConfig(WifiManager wifiMgr, ScanResult scanResult, String password) {
		MyHelper.d(">>>> WIFI - connectNewConfig In ");
		final String security = ConfigSec.getScanResultSecurity(scanResult);
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = convertToQuotedString(scanResult.SSID);
		config.BSSID = scanResult.BSSID;
		ConfigSec.setupSecurity(config, security, password);
		MyHelper.d(">>>> WIFI - connectNewConfig, security = "+security);

		int id = -1;
		try {
			id = wifiMgr.addNetwork(config);
			MyHelper.d(">>>> WIFI - connectNewConfig,  id = "+ id);
		} catch (NullPointerException e) {
		}
		if (id == -1) {
			MyHelper.d(">>>> id= -1, addNetwork fail");
			return null;
		}

		wifiMgr.enableNetwork(id, true);
		/*
		wifiMgr.disconnect();
		MyHelper.d(">>>> enableNetwork ");
		wifiMgr.enableNetwork(id, true);
		MyHelper.d(">>> Reconnect");
		wifiMgr.reconnect();

		return true;
		*/

		if (!wifiMgr.saveConfiguration()) {
			MyHelper.d(">>>> saveConfiguration");
			return null;
		}

		///config = getWifiConfiguration(scanResult, wifiMgr);
		///MyHelper.d(">>>> WIFI - connectNewConfig, SSID = "+config.SSID);
		///MyHelper.d(">>>> WIFI - connectNewConfig, build version = "+Build.VERSION.SDK_INT);
		///Boolean rr = connectToConfiguredNetwork(wifiMgr, config);
		///return rr;
		return true;
	}

	public static boolean connectToConfiguredNetwork(final WifiManager wifiMgr, WifiConfiguration config) {
		final String security = ConfigSec.getWifiConfigurationSecurity(config);

		// We have to retrieve the WifiConfiguration after save.
		config = getWifiConfiguration(wifiMgr, config, security);
		if (config == null) {
			MyHelper.d(">>>> WIFI - connectToConfiguredNetwork, config=NULL");
			return false;
		}

		// Disable others, but do not save.
		// Just to force the WifiManager to connect to it.
		MyHelper.d(">>>> WIFI - connectToConfiguredNetwork, config.networkId="+ config.networkId);
		MyHelper.d(">>>> WIFI - connectToConfiguredNetwork, config.SSID="+ config.SSID);
		MyHelper.d(">>>> WIFI - connectToConfiguredNetwork, config.priority="+ config.priority);
		if (!wifiMgr.enableNetwork(config.networkId, true)) {
			return false;
		}

		// reassociate ? wifiMgr.reassociate() :
		MyHelper.d(">>>> WIFI - reconnect.");
		final boolean connect = wifiMgr.reconnect();
		if (!connect) {
			return false;
		}
		MyHelper.d(">>>> WIFI - reconnect success.");
		return true;
	}

	private static int shiftPriorityAndSave(final WifiManager wifiMgr) {
		final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
		sortByPriority(configurations);
		final int size = configurations.size();
		for (int i = 0; i < size; i++) {
			final WifiConfiguration config = configurations.get(i);
			config.priority = i;
			wifiMgr.updateNetwork(config);
		}
		wifiMgr.saveConfiguration();
		return size;
	}

	private static void sortByPriority(final List<WifiConfiguration> configurations) {
		java.util.Collections.sort(configurations, new Comparator<WifiConfiguration>() {

			@Override
			public int compare(WifiConfiguration object1, WifiConfiguration object2) {
				return object1.priority - object2.priority;
			}
		});
	}


	public static boolean isWifiApEnabled(WifiManager wifiManager) {
		try {
			Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
			method.setAccessible(true);
			return (Boolean) method.invoke(wifiManager);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}


	public static boolean closeWifiAp(WifiManager wifiManager) {
		boolean ret = false;
		if (isWifiApEnabled(wifiManager)) {
			try {
				Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
				method.setAccessible(true);
				WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
				Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class,
						boolean.class);
				ret = (Boolean) method2.invoke(wifiManager, config, false);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	private static int getMaxPriority(final WifiManager wifiManager) {
		final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
		int pri = 0;
		for (final WifiConfiguration config : configurations) {
			if (config.priority > pri) {
				pri = config.priority;
			}
		}
		return pri;
	}

	static String convertToQuotedString(String string) {
		if (TextUtils.isEmpty(string)) {
			return "";
		}

		final int lastPos = string.length() - 1;
		if (lastPos > 0 && (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
			return string;
		}

		return "\"" + string + "\"";
	}
}
