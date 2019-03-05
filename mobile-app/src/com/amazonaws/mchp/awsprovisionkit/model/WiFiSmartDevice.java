package com.amazonaws.mchp.awsprovisionkit.model;

/**
 * Created by Administrator on 2016/12/20.
 */

public class WiFiSmartDevice {


    private String thingID;
    private String deviceName;



    public WiFiSmartDevice setThingID(String thingID) {
        this.thingID = thingID;
        return this;
    }

    public WiFiSmartDevice setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getThingID() {
        return thingID;
    }

    public String getDeviceName() {
        return deviceName;
    }

}
