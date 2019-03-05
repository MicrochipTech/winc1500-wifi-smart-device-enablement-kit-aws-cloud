package com.amazonaws.mchp.awsprovisionkit.adapter;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import  com.amazonaws.mchp.awsprovisionkit.model.AwsDevice;
import  com.amazonaws.mchp.awsprovisionkit.R;
import com.amazonaws.mchp.awsprovisionkit.task.json.AwsJsonMsg;

@SuppressLint("InflateParams")
public class DeviceListAdapter extends BaseAdapter {

	protected static final int UNBOUND = 99;

	Handler handler = new Handler();

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	Context context;
	List<AwsDevice> deviceList;

	public DeviceListAdapter(Context context, List<AwsDevice> deviceList) {
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

		private TextView tvDeviceMac, tvDeviceStatus, tvDeviceName;

		private RelativeLayout delete2;

		private ImageView imgRight,imgLeft;

		private LinearLayout llLeft;

		public LinearLayout getLlLeft() {
			if (null == llLeft) {
				llLeft = (LinearLayout) view.findViewById(R.id.llLeft);
			}
			return llLeft;
		}

		public ImageView getImgRight() {
			if (null == imgRight) {
				imgRight = (ImageView) view.findViewById(R.id.imgRight);
			}
			return imgRight;
		}

		public RelativeLayout getDelete2() {
			if (null == delete2) {
				delete2 = (RelativeLayout) view.findViewById(R.id.delete2);
			}
			return delete2;
		}

		public TextView getTvDeviceMac() {
			if (null == tvDeviceMac) {
				tvDeviceMac = (TextView) view.findViewById(R.id.tvDeviceMac);
			}
			return tvDeviceMac;
		}

		public TextView getTvDeviceStatus() {
			if (null == tvDeviceStatus) {
				tvDeviceStatus = (TextView) view.findViewById(R.id.tvDeviceStatus);
			}
			return tvDeviceStatus;
		}

		public TextView getTvDeviceName() {
			if (null == tvDeviceName) {
				tvDeviceName = (TextView) view.findViewById(R.id.tvDeviceName);
			}
			return tvDeviceName;
		}

		public ImageView getImgLeft()
		{
			if ( null == imgLeft)
			{
				imgLeft = (ImageView) view.findViewById(R.id.imgLeft);
			}
			return imgLeft;

		}

	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		Holder holder;

		if (view == null) {
			view = LayoutInflater.from(context).inflate(
					R.layout.item_gateway_list, null);
			holder = new Holder(view);
			view.setTag(holder);
		} else {
			holder = (Holder) view.getTag();
		}
		final AwsDevice device = deviceList.get(position);
		holder.getTvDeviceStatus().setText("Remote");
		holder.getTvDeviceName().setText(device.getDeviceName());
		holder.getTvDeviceMac().setText(device.getMacAddr());

		holder.getImgLeft().setImageResource(R.drawable.winc1500_secure_wifi_board);

		holder.getDelete2().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Message message = new Message();
				message.what = UNBOUND;
				message.obj = device.getMacAddr().toString();
				handler.sendMessage(message);
			}
		});
		return view;
	}
}
