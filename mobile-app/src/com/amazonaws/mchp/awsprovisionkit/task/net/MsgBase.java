package com.amazonaws.mchp.awsprovisionkit.task.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.amazonaws.mchp.awsprovisionkit.base.BaseApp;
import android.content.Intent;
import android.os.Bundle;


public abstract class MsgBase implements Closeable {

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
		MyHelper.d("MsgBase finalize");
		this.dispose();
	}

	protected void dispose() {
		try {
			if (null != this.m_socket) {
				this.m_socket.close();
				MyHelper.d(">>>>----8899 close socket, ip=" + this.getPlugIp() + ", mac=" + this.getMAC());
			}
		} catch (Exception e) {
		} finally {
			this.m_socket = null;
		}

		try {

			if (null != this.m_plugIp) {
				String sip = this.m_plugIp.toString();
				PlugList.remove(sip);
				String mac = this.getMAC();

				if (null == mac || mac.isEmpty() || null != MyHelper.readToken(mac)) {
					PlugUdpDely.remove(sip);
				}

				if (mac != null && !mac.isEmpty()) {
					PlugCache.remove(mac);

				}
			}
		} catch (Exception e) {
		} finally {
			// this.m_macAddr = null;
			// this.m_plugIp = null;
		}
	}


	protected final void printIn(byte[] data) {
		// MyHelper.d(">>>>----8899 in from [" + this.getPlugIp() + "]" +
		// data.length);
	}


	protected final void printOut(byte[] data) {
		// MyHelper.d(">>>>----8899 out to [" + this.getPlugIp() + "]" +
		// data.length);
	}

	protected final static HashMap<String, MsgAdapter> PlugList = new HashMap<String, MsgAdapter>();
	protected final static HashMap<String, MsgAdapter> PlugCache = new HashMap<String, MsgAdapter>();
	protected final static HashMap<String, Object> SharedCache = new HashMap<String, Object>();
	protected final static ConcurrentLinkedQueue<InetAddress> PlugUdpQQ = new ConcurrentLinkedQueue<InetAddress>();
	protected final static HashMap<String, Integer> PlugUdpDely = new HashMap<String, Integer>();



	public static MsgAdapter getMsgAdapter(String ipOrMac) {
		return MsgMulticast.single().getAdapter(ipOrMac);
	}

	synchronized public static int getCasedAdapterCount() {
		return PlugList.size();
	}

	synchronized public static Collection<MsgAdapter> getCasedAdapters() {
		return PlugList.values();
	}

	@Override
	synchronized public void close() throws IOException {
		MyHelper.d("MsgBase close");
		this.dispose();
	}





	protected void initSocket(InetAddress plugIp) {

		this.m_plugIp = plugIp;
		MyHelper.d(">>>>initSocket, ip ="+m_plugIp);
		try {
			m_socket = new Socket(m_plugIp, MyConfig.PLUG_TCP_PORT);
			m_socket.setKeepAlive(true);
			m_socket.setSoLinger(true, 0);
			m_socket.setTcpNoDelay(true);
		} catch (Exception e) {
			String err = ">>>>----8899 build socket failed,";
			if (null != e)
				err += e.getMessage();
			MyHelper.e(err);
		}
	}

	protected String m_macAddr = null;


	protected InetAddress m_plugIp = null;


	protected Socket m_socket = null;

	public InetAddress getPlugIp() {
		return m_plugIp;
	}

	public String getMAC() {
		return this.m_macAddr;
	}

	protected void setMAC(String mac) {
		this.m_macAddr = mac;
	}

	Socket getSocket() throws UnknownHostException, IOException {
		if (null != m_socket) {
			m_socket.setSoTimeout(TIMEOUT);
		}

		return m_socket;
	}

	final int TIMEOUT = 4000;


	synchronized MsgData doSendAdnRead(byte[] data) {
		String err = null;
		MsgData item = null;
		String ipp = "LOCAL-IP=" + MyHelper.getLocalIP() + ",Plug MAC.IP=" + this.getMAC() + this.getPlugIp();

		try {
			Socket socket = getSocket();
			InputStream in = null;
			OutputStream out = null;
			if (null == socket || socket.isClosed() || !socket.isConnected()) {
				this.close();
				err = "Device Offline";
			} else {
				ipp += ", {LOCAL-PORT=" + socket.getLocalPort() + "}";
				in = socket.getInputStream();
				out = socket.getOutputStream();
				printOut(data);
				out.write(data);
				byte[] ret = new byte[1024];
				int len = in.read(ret);

				if (len > 0) {
					ret = MyHelper.subBytes(ret, 0, len);
					item = ProtocolAdapter.tryParse(ret, this.Random, this.protocol);
					if (null != item) {
						printIn(item.getData());
					}
				} else {
					this.close();
					err = "socket error: read data length = 0";
				}
			}
		} catch (Exception e) {
			err = "Network error, ";
			if (null != e) {
				err += e.getMessage();
				e.printStackTrace();
				MyHelper.e(">>>>-doSendAdnRead Exception err="+err);
			}
			try {
				this.close();
			} catch (Exception e2) {
			}
		} finally {
			if (err != null) {
				item = buildErrorMsg(err);
			}
			if (null == item) {
				item = buildErrorMsg("Unknow error");
			}
			if (item.hasError()) {
				MyHelper.e(">>>>----8899 IO ERR: " + ipp + MyConfig.S_NEW_LINE + item.getError());
			}
		}

		return item;
	}

	public MsgBase() {
		protocol = new ProtocolAdapter();
		Random = MyHelper.genRandom(32);
	}


	protected MsgData buildErrorMsg(String error) {
		MsgData md = new MsgData();
		md.setError(error);
		return md;
	}


	protected ProtocolAdapter protocol = null;
	protected byte[] Random = null;

}
