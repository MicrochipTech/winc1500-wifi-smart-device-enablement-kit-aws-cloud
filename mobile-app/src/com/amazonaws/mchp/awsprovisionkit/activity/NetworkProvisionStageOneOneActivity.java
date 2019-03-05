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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mchp.awsprovisionkit.base.MyThreadPool;
import com.amazonaws.mchp.awsprovisionkit.model.APDevice;
import android.os.AsyncTask;
import com.amazonaws.mchp.awsprovisionkit.service.AwsService;
import com.amazonaws.mchp.awsprovisionkit.task.net.MsgData;
import com.amazonaws.mchp.awsprovisionkit.task.net.MsgMulticast;
import com.amazonaws.mchp.awsprovisionkit.task.net.MyConfig;
import com.amazonaws.mchp.awsprovisionkit.task.net.MyHelper;
import com.amazonaws.mchp.awsprovisionkit.task.net.WlanAdapter;
import com.amazonaws.mchp.awsprovisionkit.utils.*;
import com.amazonaws.mchp.awsprovisionkit.R;

import java.util.ArrayList;
import java.util.List;

import zxing.CaptureActivity;

@SuppressLint("HandlerLeak")
public class NetworkProvisionStageOneOneActivity extends AppCompatActivity {
    static final String LOG_TAG = NetworkProvisionStageOneOneActivity.class.getCanonicalName();
    public static List<String> boundMessage;
    WlanAdapter mWifiAdapter = null;
    public WifiConfiguration currentApInfo = null;

    protected static final int PROGRESSDIAG= 1;
    protected static final int PROGRESSDIAG_DISMISS= 2;
    protected static final int TOAST = 3;


    public ProgressDialog progressDialog;
    private String uuid;
    final Context context = this;

    private EditText passwordInput;
    private EditText codeInput;
    private Button setPassword;
    private AlertDialog userDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boundMessage = new ArrayList<String>();
        mWifiAdapter = new WlanAdapter(this);
        progressDialog = new ProgressDialog(context);

        setContentView(R.layout.activity_network_prov_stage1_1);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

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
        uuid = extras.getString(ServiceConstant.CognitoUuid);
        Log.d(LOG_TAG, "Debug:  uuid= "+uuid);

        init();
    }



    public void ConnectByScanQR(View view) {

        Log.d(LOG_TAG, "Debug:  Scan QR button click");

        Intent activity_intent = new Intent(NetworkProvisionStageOneOneActivity.this, CaptureActivity.class);
        activity_intent.putExtra(ServiceConstant.CallerCmd,"networkProvisionActivity");
        startActivity(activity_intent);
    }

    public void ConnectBySearching(View view) {
        Log.d(LOG_TAG, "Debug: Search device button click");

        NetworkProvisionStageOneOneActivity.ConnectTargetBoardTask taskConnectDev = new NetworkProvisionStageOneOneActivity.ConnectTargetBoardTask();
        taskConnectDev.executeOnExecutor(MyThreadPool.getExecutor(), "searchDevice");

        //Intent activity_intent = new Intent(NetworkProvisionStageOneActivity.this, NetworkProvisionStageTwoActivity.class);
        //activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
        //startActivity(activity_intent);
    }
    private void init(){

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "onResume");
        if (boundMessage.size() != 0) {

            if (boundMessage.get(0) == "QR Mac") {
                if (boundMessage.get(1) == "invalid")
                    Toast.makeText(NetworkProvisionStageOneOneActivity.this, "Wrong devices", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(NetworkProvisionStageOneOneActivity.this, "add dev: "+boundMessage.get(1), Toast.LENGTH_LONG).show();
                    //ConnectBoard(boundMessage.get(1));

                    NetworkProvisionStageOneOneActivity.ConnectTargetBoardTask taskConnectDev = new NetworkProvisionStageOneOneActivity.ConnectTargetBoardTask();
                    taskConnectDev.executeOnExecutor(MyThreadPool.getExecutor(), "scanQR", boundMessage.get(1));
                }
            }
            else if (boundMessage.get(0) == "remove current AP") {
                Log.d(LOG_TAG, "remove current AP");
                mWifiAdapter.removeCurrentAP();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MyHelper.d(">>>> on Pause ....");
        boundMessage.clear();



    }

    @Override
    public void onBackPressed(){
        MyHelper.d(">>>> onBackPressed ....");
        super.onBackPressed();

        if (currentApInfo != null) {
            progressDialog.setTitle("Loading");
            progressDialog.setMessage("Loading");
            progressDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progressDialog.show();

            mWifiAdapter.tryConnectWlan(currentApInfo);

            progressDialog.dismiss();
        }

        finish();
    }

    private void exit() {
        onBackPressed();
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

    private class ConnectTargetBoardTask extends AsyncTask<String, Integer, String> {

        public Boolean isStoped = false;



        private boolean ConnectBoardByScanQR(String mac_addr){

            ArrayList<ScanResult> data = new ArrayList<ScanResult>();
            String targetSsid = "WiFiSmartDevice_" + mac_addr;

            Boolean r1 = mWifiAdapter.connectToNewWifi(targetSsid);
            if (r1)
                MyHelper.d(">>>> Success connected to wifi, SSID= " + targetSsid);
            else {
                MyHelper.d(">>>> Fail to connected to wifi, SSID= " + targetSsid);
                return false;
            }
            return true;
        }


        private ScanResult searchTargetDevice(List<ScanResult> list, String ssid)
        {

            for ( ScanResult sr : list) {
                MyHelper.d(">>>> searchTargetDevice, ssid=" + sr.SSID);
                if (null == sr.SSID || sr.SSID.isEmpty())
                    continue;
                if (sr.SSID.contains(ssid)) {
                    return sr;
                }
            }
            return null;
        }

        private boolean SearchAndConnectDevice(){

            List<ScanResult> rdata;
            int cnt = 2;

            do {
                rdata = mWifiAdapter.tryGetWifiList();

                if (rdata == null) {
                    return false;
                }
                cnt--;
            } while (rdata.size() == 0 && (cnt>  0));

            String targetSsid = "WiFiSmartDevice_";

            ScanResult sr = searchTargetDevice(rdata, targetSsid);
            if (sr == null)
                return false;

            Boolean r1 = mWifiAdapter.tryConnectWlan(sr, "12345678");
            if (r1)
                MyHelper.d(">>>> Success connected to wifi, SSID= " + sr.SSID);
            else
                return false;

            return true;
        }
        @Override
        protected String doInBackground(String... params) {
            boolean ret;
            MyHelper.d(">>>> Start doInBackground ....");

            currentApInfo = mWifiAdapter.getCurrentAPInfo();
            mWifiAdapter.doRegister();


            if (params[0].equals("scanQR")) {

                ret = ConnectBoardByScanQR(params[1]);

                if (ret == true)
                    return "scanQR_success";
                else
                    return "scanQR_fail";
            }
            else if (params[0].equals("searchDevice"))
            {
                ret = SearchAndConnectDevice();
                if (ret == true)
                    return "searchDevice_success";
                else
                    return "searchDevice_fail";
            }
            return "";
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub

            if (result.equals("scanQR_success")) {

                Intent activity_intent = new Intent(NetworkProvisionStageOneOneActivity.this, NetworkProvisionStageTwoActivity.class);
                activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
                startActivity(activity_intent);
            }
            else if (result.equals("searchDevice_success")) {

                Intent activity_intent = new Intent(NetworkProvisionStageOneOneActivity.this, NetworkProvisionStageTwoActivity.class);
                activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
                startActivity(activity_intent);
            }
            else if (result.equals("scanQR_fail")) {
                printProgressDiagMsg(result);
                mWifiAdapter.tryConnectWlan(currentApInfo);
                handler.sendEmptyMessageDelayed(PROGRESSDIAG_DISMISS, 6000);
            }
            else if (result.equals("searchDevice_fail"))
            {
                MyHelper.d(">>>> fail to scan the device");
                printProgressDiagMsg("Search device fail.. try again..");
                mWifiAdapter.tryConnectWlan(currentApInfo);
                handler.sendEmptyMessageDelayed(PROGRESSDIAG_DISMISS, 6000);
            }
            else{
                printProgressDiagMsg(result);
                Boolean r1 = mWifiAdapter.tryConnectWlan(currentApInfo);
                handler.sendEmptyMessageDelayed(PROGRESSDIAG_DISMISS, 6000);
            }

            mWifiAdapter.unRegister();
        }

        public void tryStop() {
            this.isStoped = true;
            try {
                if (this.getStatus() != Status.FINISHED)
                    this.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                case TOAST:
                    Toast.makeText(NetworkProvisionStageOneOneActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;


            }
        };
    };

}
