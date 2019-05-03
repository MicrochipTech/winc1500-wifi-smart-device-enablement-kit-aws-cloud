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
import java.net.UnknownHostException;


public class MsgAdapter extends MsgBase {


	public Boolean IsOverheat = false;


	public MsgAdapter(String plugIp) {
		try {
			this.initSocket(InetAddress.getByName(plugIp));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			MyHelper.e(">>>>----8899 Unknow plugIp: " + plugIp);
			e.printStackTrace();
		}
	}

	public MsgAdapter(InetAddress plugIp) {
		this.initSocket(plugIp);
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
	}

	@Override
	public void close() throws IOException {
		super.close();
	}


	public MsgData tryDiscovery(int step) {
		return tryDiscovery(step, null, null, null, null);
	}


	public MsgData tryDiscovery(int step, String ssid, String password, WifiSEC securityType, String uuid) {
		byte[] data = null;
		switch (step) {
		case 1: // get the mac address from the device
				data = protocol.makeDiscoveryReq();
			break;
		case 2: // send CONFIG=SSID message to configure the network
			if (null == this.getMAC() || this.getMAC().isEmpty()) {
				return buildErrorMsg("The device might be disconnected.");
			}
			data = protocol.makeDiscoveryATC(ssid, password, securityType, uuid);
			break;
		case 3: // send CONDONE message to end the process
			MyHelper.d(">>>>>>>>makeDiscoveryATZ");
			data = protocol.makeDiscoveryATZ();
			break;

		default:
			break;
		}

		MsgData item = doSendAdnRead(data);

		if (step == 3 && !item.hasError()) {
			String mac = this.getMAC();
			if (mac != null) {
				byte[] token = this.Random;
				MyHelper.saveToken(mac, token, true);
			}
			try {
				Thread.sleep(500); // wait 0.5 sec to close the socket
				this.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (step == 1 && !item.hasError()) {
			this.setMAC(item.MAC);
			if (item.MAC == null)
				item.setError("Read MAC address failed");
		}
		return item;
	}


	public MsgData tryGetTemperature() {
		byte[] data = protocol.makeQueryTemp();
		MsgData item = doSendAdnRead(data);

		if (!item.hasError()) {
			this.IsOverheat = item.TempCurrent >= item.TempHigh;
		}

		return item;
	}


}
