/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 */

package com.amazonaws.mchp.awsprovisionkit.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mchp.awsprovisionkit.base.MyThreadPool;
import android.os.AsyncTask;
import com.amazonaws.mchp.awsprovisionkit.service.AwsService;
import com.amazonaws.mchp.awsprovisionkit.task.net.MsgData;
import com.amazonaws.mchp.awsprovisionkit.task.net.MsgMulticast;
import com.amazonaws.mchp.awsprovisionkit.task.net.MyConfig;
import com.amazonaws.mchp.awsprovisionkit.task.net.MyHelper;
import com.amazonaws.mchp.awsprovisionkit.task.net.WlanAdapter;
import com.amazonaws.mchp.awsprovisionkit.utils.*;
import com.amazonaws.mchp.awsprovisionkit.R;


@SuppressLint("HandlerLeak")
public class NetworkProvisionStageThreeActivity extends AppCompatActivity {
    static final String LOG_TAG = NetworkProvisionStageThreeActivity.class.getCanonicalName();
    WlanAdapter mWifiAdapter = null;
    private NetworkStateReceiver receiver;
    public WifiConfiguration currentApInfo = null;

    protected static final int PROGRESSDIAG= 1;
    protected static final int PROGRESSDIAG_DISMISS= 2;
    protected static final int TOAST = 3;
    protected static final int QUITTODEVICELIST = 4;

    public ProgressDialog progressDialog;
    final Context context = this;

    private EditText devName;
    private String ap_ssid, ap_password, uuid, ap_sec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiAdapter = new WlanAdapter(this);
        progressDialog = new ProgressDialog(context);

        setContentView(R.layout.activity_network_prov_stage3);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        receiver = new NetworkStateReceiver();
        devName = (EditText) findViewById(R.id.editTextDeviceName);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

        TextView main_title = (TextView) findViewById(R.id.forgot_password_toolbar_title);
        main_title.setText("Network Provision");

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        ap_ssid = extras.getString(ServiceConstant.Ssid);
        ap_password = extras.getString(ServiceConstant.ApPassword);
        ap_sec = extras.getString(ServiceConstant.ApSec);
        uuid = extras.getString(ServiceConstant.CognitoUuid);
        init();

        Log.d(LOG_TAG, "Debug:  uuid= "+uuid);
    }



    public void setDeviceName(View view) {

        Log.d(LOG_TAG, "Debug:  setDeviceName button click");

        NetworkProvisionStageThreeActivity.NetworkProvisionTask taskConnectDev = new NetworkProvisionStageThreeActivity.NetworkProvisionTask();
        taskConnectDev.executeOnExecutor(MyThreadPool.getExecutor(), "connect", devName.getText().toString(), ap_ssid, ap_password, ap_sec, uuid);

    }


    private void init(){

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ServiceConstant.CloudStatus);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    private void exit() {
        finish();
    }

    private void printProgressDiagMsg(String text)
    {

        Message msg = new Message();
        msg.what = PROGRESSDIAG;
        msg.obj = text;
        handler.sendMessage(msg);

    }

    private void clearProgressDiagMsg()
    {

        Message message = new Message();
        message.what = PROGRESSDIAG_DISMISS;
        handler.sendMessage(message);
    }


    private class NetworkProvisionTask extends AsyncTask<String, Integer, String> {

        //private WifiConfiguration currentApInfo = null;




        private String sendDiscoveryCmd(String devName) {
            MsgData msgData = MsgMulticast.single().tryDiscovery1();
            if (msgData == null)
                return MyConfig.ERR_ConnectDevFail;

            if (msgData.hasError()) {
                return msgData.getError();
            } else {
                String mac = msgData.MAC;
                String thingID = msgData.ThingID;
                printProgressDiagMsg("Provisioning...");

                MyHelper.d(">>>> Get device Mac="+ mac);
                MyHelper.d(">>>> Get device ThingID="+ thingID);

                Intent subscribe_intent = new Intent(NetworkProvisionStageThreeActivity.this, AwsService.class);
                subscribe_intent.putExtra(ServiceConstant.DevMacAddr,thingID);
                subscribe_intent.putExtra(ServiceConstant.DevName,devName);
                subscribe_intent.setAction(ServiceConstant.UpdateAcctInfoToDB);
                startService(subscribe_intent);

            }

            return MyConfig.Success_ConnectDev;
        }

        private String networkProvision(String ssid, String password, String sec, String uuid) {
            MsgData msgData = MsgMulticast.single().tryDiscovery2(ssid, password, sec, uuid);
            if (msgData == null)
                return MyConfig.ERR_SendProvDataFail ;

            return MyConfig.Success_SendProvData;
        }

        @Override
        protected String doInBackground(String... params) {
            MyHelper.d(">>>> Start doInBackground ....");

            // send discovery command
            String result1, result2;
            MyHelper.d(">>>>  DiscoveryCmd to device");
            result1 = this.sendDiscoveryCmd(params[1]);
            if (MyConfig.Success_ConnectDev == result1) {
                MyHelper.d(">>>> send provision data to device");
                result2 = this.networkProvision(params[2], params[3], params[4], params[5]);
            }
            else
                return result1;

            if (result2.equals(MyConfig.ERR_SendProvDataFail))
                return result2;

            mWifiAdapter.removeCurrentAP();
            return  "success";	// success finish stage 2
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub

            if (result.equals("success")){
                MyHelper.d(">>>> Finish stage 2: finish provisioning");
                printProgressDiagMsg("Provisioning...");
                //Boolean r1 = mWifiAdapter.tryConnectWlan(currentApInfo);
            }
            else{
                printProgressDiagMsg(result);
                handler.sendEmptyMessageDelayed(QUITTODEVICELIST, 6000);
                mWifiAdapter.removeCurrentAP();
                //Boolean r1 = mWifiAdapter.tryConnectWlan(currentApInfo);
            }
            mWifiAdapter.unRegister();
        }


    }


    Handler handler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

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
                case QUITTODEVICELIST:
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();

                    Intent intent2DevListActivity = new Intent(NetworkProvisionStageThreeActivity.this, DeviceListActivity.class);
                    intent2DevListActivity.putExtra(ServiceConstant.CognitoUuid, uuid);
                    startActivity(intent2DevListActivity);
                    break;
                case TOAST:
                    Toast.makeText(NetworkProvisionStageThreeActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;


            }
        };
    };

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ServiceConstant.CloudStatus)) {

                String connMessage = intent.getStringExtra(ServiceConstant.CloudStatusConn);

                if (connMessage.equals("Connected")) {
                    clearProgressDiagMsg();

                    Intent intent2DevListActivity = new Intent(NetworkProvisionStageThreeActivity.this, DeviceListActivity.class);
                    intent2DevListActivity.putExtra(ServiceConstant.CognitoUuid, uuid);
                    startActivity(intent2DevListActivity);
                } else if (connMessage.equals("Re-Connecting")) {

                }

            }
        }
    }
}
