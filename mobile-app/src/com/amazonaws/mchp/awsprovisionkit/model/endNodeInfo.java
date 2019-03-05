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