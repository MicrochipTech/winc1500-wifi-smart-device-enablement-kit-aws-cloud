package com.amazonaws.mchp.awsprovisionkit.task.json;


import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import android.os.Parcel;

import com.amazonaws.mchp.awsprovisionkit.model.itemInfo;


public class AwsShadowJsonMsg implements Parcelable {
    //private static final long serialVersionUID = -7060210544600464481L;
    static final String LOG_TAG = AwsShadowJsonMsg.class.getCanonicalName();
    static final Integer   MAX_ENDNODE_NUMBER = 1;
    //public endNodeInfo nodeInfo;

    public String val;


    private String[] report_item = {
            "macAddr",
            "hum",
            "temp",
            "uv",
            "pressure",
            "COUNT",
            "BUTTON_1",
            "BUTTON_2",
            "BUTTON_3",
            "LED_R",
            "LED_G",
            "LED_B",
            "LED_INTENSITY",
            "Light",
            "State",
    };


    public ArrayList<itemInfo> report_info;
    public ArrayList<itemInfo> desire_info;



    public static final String   AWS_SHADOW_JSON_STATE_ATTR = "state" ;
    public static final String   AWS_SHADOW_JSON_REPORT_ATTR = "reported" ;
    public static final String   AWS_SHADOW_JSON_DESIRED_ATTR = "desired" ;
    public static final String   AWS_SHADOW_JSON_METADATA_ATTR = "metadata" ;
    public static final String   AWS_SHADOW_JSON_TIMESTAMP_ATTR = "timestamp" ;

    public static final String   AWS_JSON_COMMAND_GENERATE_DESIRE_MSG = "generate_desire_msg" ;
    public static final String   AWS_JSON_COMMAND_ATTR = "cmd";
    public static final String   AWS_JSON_COMMAND_GET = "get" ;


    public AwsShadowJsonMsg() {


        report_info = new ArrayList<itemInfo>();
        desire_info = new ArrayList<itemInfo>();

    }

    public AwsShadowJsonMsg(Parcel source) {
        // reconstruct from the parcel
        report_info = new ArrayList<itemInfo>();
        source.readTypedList(report_info, itemInfo.CREATOR);
        desire_info = new ArrayList<itemInfo>();
        source.readTypedList(desire_info, itemInfo.CREATOR);

    }

    public static final Parcelable.Creator<AwsShadowJsonMsg> CREATOR
            = new Parcelable.Creator<AwsShadowJsonMsg>() {
        public AwsShadowJsonMsg createFromParcel(Parcel p) {
            return new AwsShadowJsonMsg(p);
        }

        public AwsShadowJsonMsg[] newArray(int size) {
            return new AwsShadowJsonMsg[size];
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
        dest.writeTypedList(report_info);
        dest.writeTypedList(desire_info);
    }

    public ArrayList<itemInfo> getReportInfo(){
        return report_info;
    }
    public ArrayList<itemInfo> getDesireInfo(){
        return desire_info;
    }

    public AwsShadowJsonMsg parseJsonMsg(String msg) {
        int i;
        Log.d(LOG_TAG, "AwsShadowJsonMsg -->" + msg);

        JSONObject state_msg, report_msg;

        try {
            JSONObject obj = new JSONObject(msg);
            state_msg = obj.getJSONObject(AWS_SHADOW_JSON_STATE_ATTR);
            report_msg = state_msg.getJSONObject(AWS_SHADOW_JSON_REPORT_ATTR);

            for(i=0; i< report_item.length; i++) {
                if (report_msg.has(report_item[i])) {
                    val = report_msg.getString(report_item[i]);
                    itemInfo node = new itemInfo();
                    node.item = report_item[i];
                    node.value = val;
                    report_info.add(node);
                }
            }
            for(i=0; i< report_info.size(); i++){
                Log.d(LOG_TAG, report_info.get(i).item + " :" + report_info.get(i).value);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "report string is in error format  " );
        }

        try {
            JSONObject obj = new JSONObject(msg);
            state_msg = obj.getJSONObject(AWS_SHADOW_JSON_STATE_ATTR);
            report_msg = state_msg.getJSONObject(AWS_SHADOW_JSON_DESIRED_ATTR);

            for(i=0; i< report_item.length; i++) {
                if (report_msg.has(report_item[i])) {
                    val = report_msg.getString(report_item[i]);
                    itemInfo node = new itemInfo();
                    node.item = report_item[i];
                    node.value = val;
                    desire_info.add(node);
                }
            }
            for(i=0; i< desire_info.size(); i++){
                Log.d(LOG_TAG, "Desire:" + desire_info.get(i).item + ":" + desire_info.get(i).value);
            }



        } catch (JSONException e) {
            Log.e(LOG_TAG, "desire string is in error format  " );
        }


        return this;
    }

    // ToDo
    public String generateJsonMsg(String msg) {

        JSONObject mainJObj = new JSONObject();
        JSONObject jObj_lv2 = new JSONObject();
        JSONObject jObj_lv3 = new JSONObject();

        if (msg.equals(AWS_JSON_COMMAND_GENERATE_DESIRE_MSG)){
            try {
                for (int i = 0; i<desire_info.size(); i++) {
                    if (desire_info.get(i).item.equals("LED_R") || desire_info.get(i).item.equals("LED_G") || desire_info.get(i).item.equals("LED_B") || desire_info.get(i).item.equals("LED_INTENSITY") || desire_info.get(i).item.equals("Light")) // special handling for winc1500 secure wifi board of Masters
                        jObj_lv3.put(desire_info.get(i).item, Integer.valueOf(desire_info.get(i).value));
                    else
                        jObj_lv3.put(desire_info.get(i).item, desire_info.get(i).value); // only update 1 device info, so hardcode to 0
                }

                jObj_lv2.put("desired", jObj_lv3);

                mainJObj.put("state", jObj_lv2);

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


        return mainJObj.toString();
    }

    public void printDebugLog() {
        int i;

        for(i=0; i< report_info.size(); i++){
            Log.d(LOG_TAG, "Debug Report " + report_info.get(i).item + ":" + report_info.get(i).value);
        }
        for(i=0; i< desire_info.size(); i++){
            Log.d(LOG_TAG, "Debug Desire " + desire_info.get(i).item + ":" + desire_info.get(i).value);
        }


        return;
    }



}
