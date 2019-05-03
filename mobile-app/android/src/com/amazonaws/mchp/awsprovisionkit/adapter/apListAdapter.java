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

package com.amazonaws.mchp.awsprovisionkit.adapter;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mchp.awsprovisionkit.model.APDevice;
import  com.amazonaws.mchp.awsprovisionkit.R;

@SuppressLint("InflateParams")
public class apListAdapter extends BaseAdapter {

	protected static final int UNBOUND = 99;

	Handler handler = new Handler();

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	Context context;
	List<APDevice> deviceList;

	public apListAdapter(Context context, List<APDevice> deviceList) {
		super();
		this.context = context;
		this.deviceList = deviceList;
	}

	@Override
	public int getCount() {
		return deviceList.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	private class Holder {
		View view;

		public Holder(View view) {
			this.view = view;
		}

		private TextView tvSSIDName;

		private RelativeLayout delete2;




		public RelativeLayout getDelete2() {
			if (null == delete2) {
				delete2 = (RelativeLayout) view.findViewById(R.id.delete2);
			}
			return delete2;
		}

		public TextView getSSIDName() {
			if (null == tvSSIDName) {
				tvSSIDName = (TextView) view.findViewById(R.id.tvSSIDName);
			}
			return tvSSIDName;
		}


	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		Holder holder;

		if (view == null) {
			view = LayoutInflater.from(context).inflate(
					R.layout.item_ap_list, null);
			holder = new Holder(view);
			view.setTag(holder);
		} else {
			holder = (Holder) view.getTag();
		}
		final APDevice device = deviceList.get(position);
		holder.getSSIDName().setText(device.getAPSSIDName());


		return view;
	}
}
