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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2016/12/20.
 */

public class endNodeInfo implements Parcelable {

    public String devName;
    public String devType;
    public String macAddr;
    public String info;

    public endNodeInfo() {
        devName = "";
        devType = "";
        macAddr = "";
        info = "";

    }

    public endNodeInfo(Parcel source) {
        // reconstruct from the parcel
        devName = source.readString();
        devType = source.readString();
        macAddr = source.readString();
        info = source.readString();
    }

    public static final Parcelable.Creator<endNodeInfo> CREATOR

            = new Parcelable.Creator<endNodeInfo>() {

        public endNodeInfo createFromParcel(Parcel p) {

            return new endNodeInfo(p);

        }


        public endNodeInfo[] newArray(int size) {

            return new endNodeInfo[size];

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
        dest.writeString(devName);
        dest.writeString(devType);
        dest.writeString(macAddr);
        dest.writeString(info);
    }

    @Override
    public boolean equals(Object o) {
        if ( o == null ) {
            return false;
        }
        return macAddr.equals(((endNodeInfo)o).macAddr);
    }

}