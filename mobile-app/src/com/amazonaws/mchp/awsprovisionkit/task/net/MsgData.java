package com.amazonaws.mchp.awsprovisionkit.task.net;

import java.net.InetAddress;
import java.util.List;

/**
 * 数据包信息
 */
public class MsgData {
	public MsgData() {
	}

	public MsgData(byte[] rawData) {
		this.m_data = rawData;
	}


	private byte[] m_data = null;

	private Boolean m_hasErr = false;

	private String m_error = null;


	private InetAddress m_plugIp = null;


	public InetAddress getPlugIp() {
		return m_plugIp;
	}


	public void setPlugIp(InetAddress ip) {
		this.m_plugIp = ip;
	}


	public void setError(String error) {
		this.m_error = error;
		this.m_hasErr = (error != null && error.length() > 0);
	}


	public String getError() {
		return this.m_error;
	}


	public Boolean hasError() {
		return m_hasErr;
	}


	public byte[] getData() {
		return m_data;
	}


	public int DataLength = 0;

	public byte CmdId = 0;


	public String MAC = null;

	public String ThingID = null;


	public String Message = null;

	public String ErrorCode = null;

	public int TempCurrent = 0;


	public int TempHigh = 0;

	/**
	 * 尝试解析内部的数据包，解析失败时记录错误信息，可以通过getError方法获取到错误信息
	 * 
	 * @return true = success to parse, false = fail to parse
	 */
	public Boolean tryParse() {
		byte[] bb = this.m_data;
		this.CmdId = bb[SF.fiCmdID];
		this.DataLength = MyHelper.byteToInt(new byte[] { bb[SF.fiDataLenL], bb[SF.fiDataLenH], 0, 0x01, 0x00 });
		int parametersLen = bb.length - SF.fiParamStart - SF.lenCRC;

		MyHelper.d("tryParse: CmdId="+ this.CmdId);
		switch (this.CmdId) {
		case SF.cmdDiscoveryR: // response of discovery command
			MyHelper.d("tryParse: log1");
			if (this.DataLength < 13) {
				MyHelper.d("tryParse: log2");
				this.Message = MyHelper.bytesToAscii(bb, SF.fiParamStart, 3);
				if (!this.Message.equals("+ok")) {
					this.setError("返回结果错误");
					MyHelper.d("tryParse: log3");
				}
				break;
			} else {
				//this.DevId = MyHelper.bytesToHexText(bb, SF.fiParamStart, 2);
				this.MAC = MyHelper.bytesToHexText(bb, SF.fiParamStart, 6);
				this.ThingID = MyHelper.bytesToAscii(bb, SF.fiParamStart+6, 40);
				MyHelper.d("MAC=="+bb[9]+" "+bb[10]+" "+bb[11]+" "+bb[12]+" "+bb[13]);


			}
			break;



		case SF.cmdReportErrCode: // error happen
			this.ErrorCode = MyHelper.bytesToHexText(bb, SF.fiParamStart, parametersLen);
			this.setError("Erroed code is " + this.ErrorCode);
			return false;
		case SF.cmdOtauControlRe:
		case SF.cmdOtauDataRe:
			this.Message = MyHelper.bytesToAscii(bb, SF.fiParamStart, 3);
			if (!this.Message.equals("+ok"))
				this.setError("Need [+ok] response");
			break;

		default:
			this.setError("Unknow command:" + this.CmdId);
			return false;
		}

		return true;
	}

}
