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

package com.amazonaws.mchp.awsprovisionkit.task.json;


import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import android.os.Parcel;

import com.amazonaws.mchp.awsprovisionkit.model.endNodeInfo;
import com.amazonaws.mchp.awsprovisionkit.model.infoContainer;

/**
 * Created by Administrator on 2016/12/2.
 */



public class AwsJsonMsg implements Parcelable {
    //private static final long serialVersionUID = -7060210544600464481L;
    static final String LOG_TAG = AwsJsonMsg.class.getCanonicalName();
    static final Integer   MAX_ENDNODE_NUMBER = 1;
    //public endNodeInfo nodeInfo;
    public String cmd;
    public String devName;
    public String devType;
    public String macAddr;
    public String macType;

    public int onlineDeviceNum;


    private ArrayList<endNodeInfo> nodeInfo;
    private ArrayList<infoContainer> infoList;


    public static final String   AWS_JSON_COMMAND_ATTR = "command" ;
    public static final String   AWS_JSON_SUBCOMMAND_ATTR = "subcommand" ;
    public static final String   AWS_JSON_ENDNODEINFO_ATTR = "endNodeInfo";
    public static final String   AWS_JSON_DEVNAME_ATTR = "devName";
    public static final String   AWS_JSON_DEVTYPE_ATTR = "devType";
    public static final String   AWS_JSON_MACADDR_ATTR = "macAddr";
    public static final String   AWS_JSON_DATATYPE_ATTR = "dataType";
    public static final String   AWS_JSON_DATAVALUE_ATTR = "value";
    public static final String   AWS_JSON_ONLINEDEVNUM_ATTR = "onlineDevNum";
    public static final String   AWS_JSON_INFO_ATTR = "info";
    public static final String   AWS_JSON_COMMAND_REPORTINFO = "reportInfo" ;
    public static final String   AWS_JSON_COMMAND_REPORTALLINFO = "reportAllInfo" ;
    public static final String   AWS_JSON_COMMAND_REPORTDISCONNECT = "reportDisconnect" ;
    public static final String   AWS_JSON_COMMAND_SEARCH = "search" ;
    public static final String   AWS_JSON_COMMAND_GET = "get" ;
    public static final String   AWS_JSON_COMMAND_UPDATE = "update" ;
    public static final String   AWS_JSON_COMMAND_CONTROL = "control" ;
    public static final String   AWS_JSON_COMMAND_SEARCHRESP = "searchResp" ;
    public static final String   AWS_JSON_SUBCOMMAND_ADDNODE = "addNode" ;
    public static final String   AWS_JSON_SUBCOMMAND_REMOVENODE = "removeNode" ;
    public static final String   AWS_JSON_SUBCOMMAND_GET3DPLOTDATA = "get3dPlotData" ;
    public static final String   AWS_JSON_DEVTYPE_WIFISENSORBOARD = "wifiSensorBoard";

    public static final String   AWS_JSON_DATATYPE_TEMP = "temp";
    public static final String   AWS_JSON_DATATYPE_HUM = "hum";
    public static final String   AWS_JSON_DATATYPE_UV = "uv";
    public static final String   AWS_JSON_DATATYPE_PRESSURE = "pressure";
    public static final String   AWS_JSON_DATATYPE_ONLINE_STATE = "State";

    public static final String   AWS_JSON_DATATYPE_LED_R = "LED_R";
    public static final String   AWS_JSON_DATATYPE_LED_G = "LED_G";
    public static final String   AWS_JSON_DATATYPE_LED_B = "LED_B";
    public static final String   AWS_JSON_DATATYPE_LED_INTENSITY = "LED_INTENSITY";

    public static final String   AWS_JSON_DATATYPE_LED_STATE = "Light";
    public static final String   AWS_JSON_DATATYPE_BUTTON_1 = "BUTTON_1";
    public static final String   AWS_JSON_DATATYPE_BUTTON_2 = "BUTTON_2";
    public static final String   AWS_JSON_DATATYPE_BUTTON_3 = "BUTTON_3";



    public AwsJsonMsg(Parcel source) {
        // reconstruct from the parcel
        cmd = source.readString();
        devName = source.readString();
        devType = source.readString();
        macAddr = source.readString();
        macType = source.readString();
        onlineDeviceNum = source.readInt();

        nodeInfo = new ArrayList<endNodeInfo>();
        source.readTypedList(nodeInfo, endNodeInfo.CREATOR);
        infoList = new ArrayList<infoContainer>();
        source.readTypedList(infoList, infoContainer.CREATOR);

    }

    public static final Parcelable.Creator<AwsJsonMsg> CREATOR
            = new Parcelable.Creator<AwsJsonMsg>() {
        public AwsJsonMsg createFromParcel(Parcel p) {
            return new AwsJsonMsg(p);
        }

        public AwsJsonMsg[] newArray(int size) {
            return new AwsJsonMsg[size];
        }
    };

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Storing the Student data to Parcel object
     **/
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cmd);
        dest.writeString(devName);
        dest.writeString(devType);
        dest.writeString(macAddr);
        dest.writeString(macType);
        dest.writeInt(onlineDeviceNum);
        dest.writeTypedList(nodeInfo);
        dest.writeTypedList(infoList);
    }

    public String getCmd() {
        return cmd;
    }
    public String getDeviceName() {
        return devName;
    }
    public String getDevType(){return devType;}
    public String getMacAddr(){return macAddr;}


    public AwsJsonMsg parseJsonMsg(String msg) {
        int i;
        Log.d(LOG_TAG, "AwsJsonMsg -->" + msg);

        try {
            JSONObject obj = new JSONObject(msg);
            cmd = obj.getString(AWS_JSON_COMMAND_ATTR);
            Log.d(LOG_TAG, "command :" + cmd);
            if (cmd.equals(AWS_JSON_COMMAND_REPORTINFO))
            {
                devType = obj.getString(AWS_JSON_DEVTYPE_ATTR);
                if (devType.equals(AWS_JSON_DEVTYPE_WIFISENSORBOARD))   // wifiSensorBoard
                {

                    for (i=0; i<obj.getJSONArray(AWS_JSON_INFO_ATTR).length(); i++) {
                        infoContainer singleInfoObj = new infoContainer();
                        singleInfoObj.dataType = obj.getJSONArray(AWS_JSON_INFO_ATTR).getJSONObject(i).getString(AWS_JSON_DATATYPE_ATTR);
                        singleInfoObj.dataValue = obj.getJSONArray(AWS_JSON_INFO_ATTR).getJSONObject(i).getInt(AWS_JSON_DATAVALUE_ATTR);
                        infoList.add(singleInfoObj);
                    }
                }
                else {  //gateway
                    endNodeInfo singleNodeInfo = new endNodeInfo();
                    singleNodeInfo.devName = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(0).getString(AWS_JSON_DEVNAME_ATTR);
                    singleNodeInfo.devType = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(0).getString(AWS_JSON_DEVTYPE_ATTR);
                    singleNodeInfo.macAddr = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(0).getString(AWS_JSON_MACADDR_ATTR);
                    singleNodeInfo.info = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(0).getString(AWS_JSON_INFO_ATTR);
                    nodeInfo.add(singleNodeInfo);
                }
            }
            else if (cmd.equals(AWS_JSON_COMMAND_SEARCHRESP))
            {
                macAddr = obj.getString(AWS_JSON_MACADDR_ATTR);
                devType = obj.getString(AWS_JSON_DEVTYPE_ATTR);
                if (devType.equals(AWS_JSON_DEVTYPE_WIFISENSORBOARD))
                    devName = obj.getString(AWS_JSON_DEVNAME_ATTR);
            }
            else if (cmd.equals(AWS_JSON_COMMAND_REPORTALLINFO))
            {
                macAddr = obj.getString(AWS_JSON_MACADDR_ATTR);
                devType = obj.getString(AWS_JSON_DEVTYPE_ATTR);


                if (devType.equals(AWS_JSON_DEVTYPE_WIFISENSORBOARD))   // wifiSensorBoard
                {

                    for (i=0; i<obj.getJSONArray(AWS_JSON_INFO_ATTR).length(); i++) {
                        infoContainer singleInfoObj = new infoContainer();
                        singleInfoObj.dataType = obj.getJSONArray(AWS_JSON_INFO_ATTR).getJSONObject(i).getString(AWS_JSON_DATATYPE_ATTR);
                        singleInfoObj.dataValue = obj.getJSONArray(AWS_JSON_INFO_ATTR).getJSONObject(i).getInt(AWS_JSON_DATAVALUE_ATTR);
                        infoList.add(singleInfoObj);
                    }
                }
                else {  // gateway
                    onlineDeviceNum = obj.getInt(AWS_JSON_ONLINEDEVNUM_ATTR);
                    for (i = 0; i < obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).length(); i++) {
                        endNodeInfo singleNodeInfo = new endNodeInfo();
                        singleNodeInfo.devName = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_DEVNAME_ATTR);
                        singleNodeInfo.devType = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_DEVTYPE_ATTR);
                        singleNodeInfo.macAddr = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_MACADDR_ATTR);
                        singleNodeInfo.info = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_INFO_ATTR);
                        nodeInfo.add(singleNodeInfo);
                    }
                }
            }
            else if (cmd.equals(AWS_JSON_COMMAND_REPORTDISCONNECT))
            {

                for (i=0; i<obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).length(); i++) {
                    endNodeInfo singleNodeInfo = new endNodeInfo();
                    singleNodeInfo.devName = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_DEVNAME_ATTR);
                    singleNodeInfo.devType = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_DEVTYPE_ATTR);
                    singleNodeInfo.macAddr = obj.getJSONArray(AWS_JSON_ENDNODEINFO_ATTR).getJSONObject(i).getString(AWS_JSON_MACADDR_ATTR);
                    nodeInfo.add(singleNodeInfo);
                }
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "report string is in error format  " );
        }
        return this;
    }

    // ToDo
    public String generateJsonMsg(String msg) {

        JSONObject mainJObj = new JSONObject();
        JSONObject jObj = new JSONObject();
        JSONArray jArray = new JSONArray();

        if (msg.equals(AWS_JSON_COMMAND_SEARCH)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_SEARCH);
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        else if (msg.equals(AWS_JSON_COMMAND_GET)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_GET);
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        else if (msg.equals(AWS_JSON_COMMAND_UPDATE)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_UPDATE);
                if (devType.equals(AWS_JSON_DEVTYPE_WIFISENSORBOARD))
                {
                    for (int i=0; i<infoList.size(); i++) {
                        jObj.put(AWS_JSON_DATATYPE_ATTR, infoList.get(i).dataType);       // only update 1 device info, so hardcode to 0
                        jObj.put(AWS_JSON_DATAVALUE_ATTR, infoList.get(i).dataValue);
                        jArray.put(jObj);
                    }
                    mainJObj.put(AWS_JSON_INFO_ATTR, jArray);
                }
                else    //gateway
                {
                    jObj.put(AWS_JSON_MACADDR_ATTR, nodeInfo.get(0).macAddr);       // only update 1 device info, so hardcode to 0
                    jObj.put(AWS_JSON_INFO_ATTR, nodeInfo.get(0).info);
                    jArray.put(jObj);
                    mainJObj.put(AWS_JSON_ENDNODEINFO_ATTR, jArray);
                }
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        else if (msg.equals(AWS_JSON_SUBCOMMAND_ADDNODE)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_CONTROL);
                mainJObj.put(AWS_JSON_SUBCOMMAND_ATTR, AWS_JSON_SUBCOMMAND_ADDNODE);
                mainJObj.put(AWS_JSON_MACADDR_ATTR, macType+macAddr);
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        else if (msg.equals(AWS_JSON_SUBCOMMAND_REMOVENODE)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_CONTROL);
                mainJObj.put(AWS_JSON_SUBCOMMAND_ATTR, AWS_JSON_SUBCOMMAND_REMOVENODE);
                mainJObj.put(AWS_JSON_MACADDR_ATTR, macType+macAddr);
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        else if (msg.equals(AWS_JSON_SUBCOMMAND_GET3DPLOTDATA)){
            try {
                mainJObj.put(AWS_JSON_COMMAND_ATTR, AWS_JSON_COMMAND_CONTROL);
                mainJObj.put(AWS_JSON_SUBCOMMAND_ATTR, AWS_JSON_SUBCOMMAND_GET3DPLOTDATA);
                mainJObj.put(AWS_JSON_MACADDR_ATTR, macAddr);
            }
            catch ( JSONException e){
                e.printStackTrace();
            }
        }
        return mainJObj.toString();
    }

    public void printDebugLog() {

        endNodeInfo singleNode;
        infoContainer singleInfo;
        Log.d(LOG_TAG, "!!!!! DevType :" + devType);
        Log.d(LOG_TAG, "!!!!! macAddr :" + macAddr);
        for (int i=0; i< nodeInfo.size(); i++) {
            singleNode = nodeInfo.get(i);
            Log.d(LOG_TAG, "!!!!! Node DevName :" + singleNode.devName);
            Log.d(LOG_TAG, "!!!!! Mode DevType :" + singleNode.devType);
            Log.d(LOG_TAG, "!!!!! Node macAddr :" + singleNode.macAddr);
            Log.d(LOG_TAG, "!!!!! Node info :" + singleNode.info);
        }
        for (int i=0; i< infoList.size(); i++) {
            singleInfo = infoList.get(i);
            Log.d(LOG_TAG, "!!!!! dataType :" + singleInfo.dataType);
            Log.d(LOG_TAG, "!!!!! value :" + singleInfo.dataValue);
        }
        return;
    }



}
