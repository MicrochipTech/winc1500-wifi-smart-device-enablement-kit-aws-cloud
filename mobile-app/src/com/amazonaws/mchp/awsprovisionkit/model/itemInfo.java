package com.amazonaws.mchp.awsprovisionkit.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2016/12/20.
 */

public class itemInfo implements Parcelable {

    public String value;
    public String item;


    public itemInfo() {
        value = "";
        item = "";
    }
    public itemInfo(String item, String val) {
        this.value = val;
        this.item = item;
    }

    public itemInfo(Parcel source) {
        // reconstruct from the parcel
        value = source.readString();
        item = source.readString();

    }

    public static final Parcelable.Creator<itemInfo> CREATOR

            = new Parcelable.Creator<itemInfo>() {

        public itemInfo createFromParcel(Parcel p) {

            return new itemInfo(p);

        }


        public itemInfo[] newArray(int size) {

            return new itemInfo[size];

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
        dest.writeString(value);
        dest.writeString(item);
    }

    @Override
    public boolean equals(Object o) {
        if ( o == null ) {
            return false;
        }
        return item.equals(((itemInfo)o).item);
    }

}
