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


package com.amazonaws.mchp.awsprovisionkit.activity;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mchp.awsprovisionkit.model.WiFiSmartDevice;
import com.amazonaws.mchp.awsprovisionkit.model.itemInfo;
import com.amazonaws.mchp.awsprovisionkit.task.json.AwsShadowJsonMsg;
import com.amazonaws.mchp.awsprovisionkit.task.net.WlanAdapter;
import com.amazonaws.mchp.awsprovisionkit.task.ui.*;
import com.amazonaws.mchp.awsprovisionkit.utils.*;
import com.amazonaws.mchp.awsprovisionkit.task.json.AwsJsonMsg;
import com.amazonaws.mchp.awsprovisionkit.service.AwsService;
import com.amazonaws.mchp.awsprovisionkit.R;
import com.amazonaws.mchp.awsprovisionkit.task.ui.SlideListView;
import com.amazonaws.mchp.awsprovisionkit.model.AwsDevice;
import com.amazonaws.mchp.awsprovisionkit.adapter.*;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;

@SuppressLint("HandlerLeak")
public class DeviceListActivity  extends AppCompatActivity implements OnClickListener, SwipeRefreshLayout.OnRefreshListener {

	static final String LOG_TAG = DeviceListActivity.class.getCanonicalName();

	/** The ic FoundDevices */
	private View icFoundDevices;

	/** The tv FoundDevicesListTitle */
	private TextView tvFoundDevicesListTitle;

	/** The slv FoundDevices */
	private SlideListView slvFoundDevices;

	/** The sv ListGroup */
	private ScrollView svListGroup;

	private DeviceListAdapter myadapter;

	private List<AwsDevice> foundDevicesList;

	private static List<String> boundMessage;

	private ProgressDialog progressDialog;

	protected static final int UPDATALIST = 1;
	protected static final int TOAST = 3;
	/* Display progress diaglog */
	protected static final int PROGRESSDIAG = 4;
	protected static final int PROGRESSDIAG_DISMISS = 5;
	protected static final int REFRESH_ICON_DISMISS = 6;
	protected static final int BOUND = 9;

	private String idToken;
	private CognitoUser user;
	private String username;

	private VerticalSwipeRefreshLayout mSwipeLayout;

	private NavigationView nDrawer;
	private DrawerLayout mDrawer;
	private ActionBarDrawerToggle mDrawerToggle;
	private Toolbar toolbar;
    private Integer mPubCounter;
	private deviceListJsonMsgReceiver receiver;
	private WlanAdapter mWifiAdapter = null;
	private String uuid;

	final Context context = this;


	String[] thing_name = new String[10];
	ArrayList<WiFiSmartDevice> thingList = new ArrayList<WiFiSmartDevice>();

	Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATALIST:
					UpdateUI();
					break;

				case BOUND:

					break;

				case PROGRESSDIAG:
					progressDialog.setTitle("Loading");
					progressDialog.setMessage(msg.obj.toString());
					progressDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
					progressDialog.show();
				break;

				case PROGRESSDIAG_DISMISS:
					if (progressDialog.isShowing())
					progressDialog.dismiss();
					break;

				case TOAST:
					Toast.makeText(DeviceListActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
					break;

				case REFRESH_ICON_DISMISS:
					mSwipeLayout.setRefreshing(false);
					break;
			}
		};
	};


	private void sendScanThingIDCmd(){

		Intent intent_arg = new Intent(DeviceListActivity.this, AwsService.class);
		intent_arg.putExtra(ServiceConstant.CognitoUuid,uuid);
		intent_arg.setAction(ServiceConstant.ScanThingID);
		startService(intent_arg);
	}

	protected  void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		///setTheme(R.style.CognitoAppTheme);

		setContentView(R.layout.activity_device_list);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		foundDevicesList = new ArrayList<AwsDevice>();
		boundMessage = new ArrayList<String>();
		mWifiAdapter = new WlanAdapter(this);
		progressDialog = new ProgressDialog(context);
		receiver = new deviceListJsonMsgReceiver();

		Intent intent = getIntent();
		uuid = intent.getStringExtra(ServiceConstant.CognitoUuid);
		idToken = intent.getStringExtra("idToken");
		if (idToken != null) {
			Log.d(LOG_TAG, "Debug: idtoken=" + idToken);
			uuid = CognitoJWTParser.getClaim(idToken, "sub");
		}
		Log.d(LOG_TAG, "Debug: user uuid = " + uuid);
		username = AppHelper.getCurrUser();
		user = AppHelper.getPool().getUser(username);

		sendScanThingIDCmd();

		initView();
		initEvent();

		mSwipeLayout.post(new Runnable() {
			@Override
			public void run() {
				mSwipeLayout.setRefreshing(true);
			}
		});

		onRefresh();
	}


	@Override
	protected void onResume() {
		super.onResume();
        IntentFilter filter = new IntentFilter(ServiceConstant.CloudStatus);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(ServiceConstant.JSONMsgReport);
		filter.addAction(ServiceConstant.JSONShadowMsgReport);
		filter.addAction(ServiceConstant.ThingIDListReport);
        registerReceiver(receiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();
		boundMessage.clear();

		mPubCounter = 0;
		handler.removeCallbacks(publishShadowGetCmd);

        unregisterReceiver(receiver);

	}

	// Perform the action for the selected navigation item
	private void performAction(MenuItem item) {
		// Close the navigation drawer
		Log.d(LOG_TAG, "Debug: item="+item.getItemId());
		mDrawer.closeDrawers();

		// Find which item was selected
		switch(item.getItemId()) {
			case R.id.nav_user_sign_out:
				// Start sign-out
				user.signOut();

				Intent intent = new Intent();
				if(username == null)
					username = "";
				intent.putExtra("name",username);
				setResult(RESULT_OK, intent);
				finish();
				break;

			case R.id.nav_about:
				// For the inquisitive
				Intent aboutAppActivity = new Intent(this, AboutApp.class);
				startActivity(aboutAppActivity);
				break;

		}
	}
	// Private methods
	// Handle when the a navigation item is selected
	private void setNavDrawer() {
		nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				performAction(item);
				return true;
			}
		});
	}

	private void initView() {
		svListGroup = (ScrollView) findViewById(R.id.svListGroup);
		icFoundDevices = findViewById(R.id.icFoundDevices);
		slvFoundDevices = (SlideListView) icFoundDevices.findViewById(R.id.slideOnlineListView);
		tvFoundDevicesListTitle = (TextView) icFoundDevices.findViewById(R.id.tvListViewTitle);
		tvFoundDevicesListTitle.setText("Device Kit");
		mSwipeLayout = (VerticalSwipeRefreshLayout) findViewById(R.id.id_swipe_ly);
		mSwipeLayout.setOnRefreshListener(this);
		mSwipeLayout.setProgressViewOffset(true, getResources().getDimensionPixelSize(R.dimen.refresher_offset),
				getResources().getDimensionPixelSize(R.dimen.refresher_offset_end));



		// Set toolbar for this screen
		toolbar = (Toolbar) findViewById(R.id.main_toolbar);
		toolbar.setTitle("");
		TextView main_title = (TextView) findViewById(R.id.main_toolbar_title);
		main_title.setText("Device List");
		setSupportActionBar(toolbar);


		// Set navigation drawer for this screen
		mDrawer = (DrawerLayout) findViewById(R.id.devicelist_drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this,mDrawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
		mDrawer.addDrawerListener(mDrawerToggle);
		mDrawerToggle.syncState();
		nDrawer = (NavigationView) findViewById(R.id.nav_devicelist_view);
		setNavDrawer();

		View navigationHeader = nDrawer.getHeaderView(0);
		TextView navHeaderSubTitle = (TextView) navigationHeader.findViewById(R.id.textViewNavUserSub);
		navHeaderSubTitle.setText(username);


	}

	private void initEvent() {

		slvFoundDevices.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {


				Intent activity_intent = new Intent(DeviceListActivity.this, WiFiSmartDeviceCtrlActivity.class);
				Bundle extras = new Bundle();
				extras.putString(ServiceConstant.DevMacAddr, foundDevicesList.get(position).getMacAddr());
				extras.putString(ServiceConstant.ThingName,foundDevicesList.get(position).getThingName());
				extras.putString(ServiceConstant.DevName,foundDevicesList.get(position).getDeviceName());

				activity_intent.putExtras(extras);
				startActivity(activity_intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.devicelist_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
			case android.R.id.home:
				break;
			case R.id.action_QR_code:

				Intent activity_intent = new Intent(DeviceListActivity.this, NetworkProvisionStageOneActivity.class);
				activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
				startActivity(activity_intent);


				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void UpdateUI() {

		svListGroup.setVisibility(View.VISIBLE);


		if (foundDevicesList.isEmpty()) {
			slvFoundDevices.setVisibility(View.GONE);
		} else {
			myadapter = new DeviceListAdapter(this, foundDevicesList);
			slvFoundDevices.setAdapter(myadapter);
			slvFoundDevices.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exitBy2Click();

		}
		return false;
	}


	private static Boolean isExit = false;

	public void exitBy2Click() {
		Timer tExit = null;
			isExit = true;
			tExit = new Timer();
			tExit.schedule(new TimerTask() {
				@Override
				public void run() {
					isExit = false; // 取消退出
				}
			}, 2000);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

	}

	public Handler getMyHandler() {

		return handler;
	}


	public void onRefresh() {
		handler.sendEmptyMessageDelayed(REFRESH_ICON_DISMISS, 10000);

		foundDevicesList.clear();
		UpdateUI();

        mPubCounter = 1;    // Publish Search command for 1 time
        handler.post(publishShadowGetCmd);

	}


    private Runnable publishShadowGetCmd= new Runnable() {
        public void run() {

            if (mPubCounter > 0) {
                mPubCounter --;

				Log.d(LOG_TAG, "Debug: [publishShadowGetCmd] thingList.size="+thingList.size());


				for (int j = 0; j<thingList.size(); j++) {
					if (thingList.get(j).getThingID() != null) {
						final Intent subscribe_intent = new Intent(DeviceListActivity.this, AwsService.class);
						subscribe_intent.putExtra(ServiceConstant.AWSThingName, thingList.get(j).getThingID());
						subscribe_intent.setAction(ServiceConstant.JSONMsgShadowGet);
						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								// Do something after 1s = 1000ms

								startService(subscribe_intent);
							}
						}, j*500);
					}
				}


				///handler.postDelayed(this, 2500);        // send every 2.5 second
            }
        }
    };

	private class deviceListJsonMsgReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(ServiceConstant.CloudStatus)) {

				String connMessage = intent.getStringExtra(ServiceConstant.CloudStatusConn);

				if (connMessage.equals("Connected")) {
					Message message = new Message();
					message.what = PROGRESSDIAG_DISMISS;
					handler.sendMessage(message);

					onRefresh();

					Intent subscribe_intent = new Intent(DeviceListActivity.this, AwsService.class);
					subscribe_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
					subscribe_intent.setAction(ServiceConstant.ScanThingID);
					startService(subscribe_intent);
				}
				else if (connMessage.equals("Re-Connecting")) {
				}

			} else if (intent.getAction().equals(ServiceConstant.JSONMsgReport)) {
				AwsJsonMsg jsonMsgObj = intent.getParcelableExtra(ServiceConstant.JSONMsgObject);
				if (jsonMsgObj != null) {
					Log.d(LOG_TAG, "Debug: Receive JSON message");
					jsonMsgObj.printDebugLog();
					int i, deviceExist=0;

					AwsDevice router;
					if (jsonMsgObj.getCmd().equals(AwsJsonMsg.AWS_JSON_COMMAND_SEARCHRESP)) {
						for (i = 0; i < foundDevicesList.size(); i++) {
							if (foundDevicesList.get(i).getMacAddr().equals(jsonMsgObj.getMacAddr())) {
								deviceExist = 1;
							}
						}
						if (deviceExist == 0) {
							router = new AwsDevice();
							if (jsonMsgObj.getDevType().equals(AwsJsonMsg.AWS_JSON_DEVTYPE_WIFISENSORBOARD)) {
								Log.d(LOG_TAG, "Debug: getDeviceName=" + jsonMsgObj.getMacAddr());
								router.setDeviceName(jsonMsgObj.getDeviceName());
							} else
								router.setDeviceName(jsonMsgObj.getDevType());

							router.setMacAddr(jsonMsgObj.getMacAddr());
							router.setDevType(jsonMsgObj.getDevType());
							foundDevicesList.add(router);
							handler.sendEmptyMessage(UPDATALIST);
						}
					}
				}
			}else if (intent.getAction().equals(ServiceConstant.JSONShadowMsgReport)) {

				handler.sendEmptyMessage(REFRESH_ICON_DISMISS);
				AwsShadowJsonMsg jsonShadowMsgObj = intent.getParcelableExtra(ServiceConstant.JSONShadowMsgObject);
				if (jsonShadowMsgObj != null) {
					Log.d(LOG_TAG, "Receive AWS Shadow JSON message");
					jsonShadowMsgObj.printDebugLog();
					ArrayList<itemInfo> report_info_shadow = jsonShadowMsgObj.getReportInfo();
					for (int i=0; i<report_info_shadow.size(); i++){
						Log.d(LOG_TAG, "Debug:" + report_info_shadow.get(i).item + "=" + report_info_shadow.get(i).value);
					}
					String topic = intent.getStringExtra(ServiceConstant.MQTTChannelName);
					Log.d(LOG_TAG, "Topic:" + topic);
					String[] split = topic.split("/");

					boolean found = false;
					for (int i=0; i< foundDevicesList.size(); i++)
					{
						if (foundDevicesList.get(i).getThingName().equals(split[2]))
						{
							found = true;
							break;
						}
					}

					if (found == false) {
						AwsDevice router = new AwsDevice();

						router.setDeviceName("WiFi Smart Device");    //default
						for (int i = 0; i < thingList.size(); i++) {
							if (split[2].equals(thingList.get(i).getThingID()))
								if (thingList.get(i).getDeviceName().equals("null")) {
									router.setDeviceName("WiFi Smart Device");
								}
								else
								{
									router.setDeviceName(thingList.get(i).getDeviceName());
								}

						}
						for (int i=0; i<report_info_shadow.size(); i++){
							if (report_info_shadow.get(i).item.equals(AwsJsonMsg.AWS_JSON_MACADDR_ATTR))
								router.setMacAddr(report_info_shadow.get(i).value);
						}


						//thing id
						router.setDevType(split[2]);
						router.setThingName(split[2]);
						foundDevicesList.add(router);
						handler.sendEmptyMessage(UPDATALIST);
					}

				}
			}
			else if (intent.getAction().equals(ServiceConstant.ThingIDListReport)) {
				Log.d(LOG_TAG, "Receive ThingIDListReport");
				thingList.clear();
				int j = 0;
				// clear thing_name
				for (int i = 0; i< thing_name.length; i++) {
					thing_name[i] = null;
				}
				Bundle b = intent.getExtras();
				if (null != b) {
					ArrayList<String> arr = b.getStringArrayList(ServiceConstant.ThingIdList);
					for (int i=0; i< arr.size(); i++)
					{
						if (arr.get(i).contains("thingName")){
							WiFiSmartDevice thing = new WiFiSmartDevice();
							String[] split = arr.get(i).split(":");


							String[] IDSplit = split[0].split("=");
							thing_name[j] = IDSplit[1];
							thing.setThingID(IDSplit[1]);

							String[] NameSplit = split[1].split("=");
							thing.setDeviceName(NameSplit[1]);
							Log.d(LOG_TAG, "Added thing_ID :: " + thing.getThingID()+" thing_name :: " + thing.getDeviceName());

							thingList.add(thing);
							j++;
						}

					}
					Log.d(LOG_TAG, "Passed Array List :: " + arr);
				}
				onRefresh();

			}


		}
	}
}
