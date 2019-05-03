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

import java.util.zip.CRC32;


public class ProtocolHelper {


	static final int _crcStart = 0;

	static final int _crcDeduct = 4;

	public static byte[] tryEncrypt(byte[] data) {
		if (MyConfig.EnableEncrypt) {

		}
		return data;
	}

	public static byte[] tryPackage(byte[] data, ProtocolAdapter protocol) {
		byte[] result = data;
		//CrcAppend(data);
		//result = doEncrypt(data, protocol);

		return result;
	}


	public static byte[] tryUnPack(byte[] data, byte[] random, ProtocolAdapter protocol) throws Exception {

		byte[] result = data;
		//result = doDecrypt(data, random, protocol);

		//if (!CrcCheck(result)) {
		//	MyHelper.printHex("校验数据包出错，", result);
		//	throw new Exception("CRC校验失败");
		//}

		return result;
	}


	static Boolean CrcCheck(byte[] data) {
		if (!MyConfig.EnableCrcCheck)
			return true; // 未开启CRC校验

		// 长度计算
		int dl = MyHelper.byteToInt(new byte[] { data[SF.fiDataLenL], data[SF.fiDataLenH] });
		if (dl != (data.length - _crcDeduct - SF.fiDataLenH - 1))
			return false;

		byte[] bb = calcCRC32(data, _crcStart, data.length - _crcDeduct);
		int len = data.length - 4;

		for (int i = 0; i < 4; i++) { // 从低位开始校验
			if (bb[i] != data[len + i])
				return false; // CRC结果不同，校验失败返回 false
		}

		return true;
	}


	static void CrcAppend(byte[] data) {
		byte[] b = calcCRC32(data, _crcStart, data.length - _crcDeduct);

		int index = data.length - 4; // 数组结束 = CRC 最低位
		for (int i = 0; i < 4; i++) { // 从CRC低位到高循环赋值
			data[index + i] = b[i];
		}
	}


	public static byte[] calcCRC32(byte[] data, int start, int len) {
		int t1 = data.length - start;
		if (len > t1)
			len = t1;
		CRC32 crc = new CRC32();
		crc.update(data, start, len);
		long v = crc.getValue();
		byte[] b = MyHelper.longToByte(v);
		crc = null;
		return b;
	}

	/**
	 * encrypt packet
	 * 
	 */
	/*
	static byte[] doEncrypt(byte[] data, ProtocolAdapter protocol) {

		if (!MyConfig.EnableEncrypt)
			return data; // 未启用加密、解密

		if (!protocol.getEnableAES())
			return data;

		// 加密数据
		byte[] sessionKey = protocol.getSessionKey();
		byte[] iv = protocol.getIV();
		//byte[] pubKey = ECCrypto.single().DevicePubKey;
		//byte[] data2 = ECCrypto.single().aesEncrypt(data, sessionKey, iv);

		// 1-5b, 2-len, data-len, 16-IV, 64-PubKey, 4-CRC
		data = new byte[data2.length + 1 + 2 + 16 + 64 + 4];
		byte[] len = MyHelper.intToByte(data.length - _crcDeduct - SF.fiDataLenH - 1);
		data[0] = 0x5b;
		data[SF.fiDataLenL] = len[0];
		data[SF.fiDataLenH] = len[1];
		System.arraycopy(data2, 0, data, SF.fiDataLenH + 1, data2.length);
		System.arraycopy(iv, 0, data, SF.fiDataLenH + 1 + data2.length, iv.length);
		System.arraycopy(pubKey, 0, data, SF.fiDataLenH + 1 + data2.length + iv.length, pubKey.length);
		CrcAppend(data);

		return data;
	}
	*/

	/**
	 * decrypt packet
	 * 
	 */
	/*
	static byte[] doDecrypt(byte[] data, byte[] random, ProtocolAdapter protocol) throws Exception {
		if (!MyConfig.EnableEncrypt)
			return data; // 未启用加密、解密

		if (data[0] == SF.SOF)
			return data; // 非密文数据
		else if (data[0] != 0x5b)
			throw new Exception("Unknow data fromat");

		// check CRC
		int dl = MyHelper.byteToInt(new byte[] { data[SF.fiDataLenL], data[SF.fiDataLenH] });
		if (dl != (data.length - _crcDeduct - SF.fiDataLenH - 1))
			throw new Exception("The Encrypt data length not match");

		byte[] bb = calcCRC32(data, _crcStart, data.length - _crcDeduct);
		int len = data.length - 4;

		for (int i = 0; i < 4; i++) { // 从低位开始校验
			if (bb[i] != data[len + i])
				throw new Exception("The Encrypt data CRC Check fail"); // CRC结果不同，校验失败返回
																		// false
		}

		// 解密数据
		int ivStart = data.length - 4 - 16 - 64; // crc4, iv16, pub64
		byte[] bIV = MyHelper.subBytes(data, ivStart, 16);
		byte[] bPK = MyHelper.subBytes(data, ivStart + 16, 64);
		byte[] bb0 = MyHelper.subBytes(data, 3, data.length - 3 - 16 - 64 - 4);
		byte[] bb1 = ECCrypto.single().aesDecryptSelf(bb0, bPK, bIV, random);
		if (bb1[0] != SF.SOF)
			throw new Exception("the descrypted data[0] is not 5A");

		// tack sessionKey
		protocol.trySaveSessionKey(bPK, random);

		int len2 = MyHelper.byteToInt(new byte[] { bb1[SF.fiDataLenL], bb1[SF.fiDataLenH] });
		byte[] raw = MyHelper.subBytes(bb1, 0, len2 + 4 + SF.fiDataLenH + 1);

		return raw;
	}
	*/
}
