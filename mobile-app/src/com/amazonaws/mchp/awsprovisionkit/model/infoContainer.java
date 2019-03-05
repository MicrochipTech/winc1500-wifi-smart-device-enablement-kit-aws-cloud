package com.amazonaws.mchp.awsprovisionkit.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 */

public class infoContainer implements Parcelable {

    public String dataType;
    public Integer dataValue;

    public infoContainer() {
        dataType = "";
        dataValue = 0;
    }

    public infoContainer(Parcel source) {
        // reconstruct from the parcel
        dataType = source.readString();
        dataValue = source.readInt();
    }

    public static final Parcelable.Creator<infoContainer> CREATOR

            = new Parcelable.Creator<infoContainer>() {

        public infoContainer createFromParcel(Parcel p) {

            return new infoContainer(p);

        }


        public infoContainer[] newArray(int size) {

            return new infoContainer[size];

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
        dest.writeString(dataType);
        dest.writeInt(dataValue);
    }

    @Override
    public boolean equals(Object o) {
        if ( o == null ) {
            return false;
        }
        return dataType.equals(((infoContainer)o).dataType);
    }

}