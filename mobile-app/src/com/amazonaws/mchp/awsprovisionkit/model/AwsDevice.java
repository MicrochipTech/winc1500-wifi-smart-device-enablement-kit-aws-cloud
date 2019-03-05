package com.amazonaws.mchp.awsprovisionkit.model;


/**
 * Created by Administrator on 2016/12/2.
 */

public class AwsDevice {
    private String deviceName;
    private String macAddr;
    private String devType;
    private String thingName;

    public String getDeviceName() {
        return deviceName;
    }
    public String getMacAddr(){return macAddr;}
    public String getDevType(){return devType;}
    public String getThingName(){return thingName;}

    public AwsDevice setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public AwsDevice setMacAddr(String macAddr) {
        this.macAddr = macAddr;
        return this;
    }

    public AwsDevice setDevType(String devType) {
        this.devType = devType;
        return this;
    }

    public AwsDevice setThingName(String thingName) {
        this.thingName = thingName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if ( o == null || !(o instanceof  AwsDevice)) {
            return false;
        }
        if(deviceName == null &&  ((AwsDevice) o).getDeviceName() == null ) {
            return true;
        }
        if(deviceName != null) {
            return deviceName.equals(((AwsDevice)o).getDeviceName());
        }
        return false;
    }
}
