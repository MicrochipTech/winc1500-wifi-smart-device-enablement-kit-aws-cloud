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
