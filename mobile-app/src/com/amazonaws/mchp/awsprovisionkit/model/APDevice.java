package com.amazonaws.mchp.awsprovisionkit.model;

/**
 * Created by Administrator on 2016/12/20.
 */

public class APDevice {


    private String ssidName;
    private String wifiSec;

    public APDevice setAPSSIDName(String ssidName) {
        this.ssidName = ssidName;
        return this;
    }

    public APDevice setAPSec(String wifiSec) {
        this.wifiSec = wifiSec;
        return this;
    }

    public String getAPSSIDName() {
        return ssidName;
    }

    public String getAPSec() {
        return wifiSec;
    }

}
