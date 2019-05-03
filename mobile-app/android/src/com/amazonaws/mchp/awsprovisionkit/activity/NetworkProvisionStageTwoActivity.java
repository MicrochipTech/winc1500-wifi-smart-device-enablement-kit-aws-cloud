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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mchp.awsprovisionkit.adapter.apListAdapter;
import com.amazonaws.mchp.awsprovisionkit.base.MyThreadPool;
import com.amazonaws.mchp.awsprovisionkit.model.APDevice;
import android.os.AsyncTask;
import com.amazonaws.mchp.awsprovisionkit.task.net.MyHelper;
import com.amazonaws.mchp.awsprovisionkit.task.net.WlanAdapter;
import com.amazonaws.mchp.awsprovisionkit.task.ui.SlideListView;
import com.amazonaws.mchp.awsprovisionkit.task.ui.VerticalSwipeRefreshLayout;
import com.amazonaws.mchp.awsprovisionkit.R;
import com.amazonaws.mchp.awsprovisionkit.utils.ServiceConstant;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("HandlerLeak")
public class NetworkProvisionStageTwoActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    static final String LOG_TAG = NetworkProvisionStageTwoActivity.class.getCanonicalName();
    WlanAdapter mWifiAdapter = null;
    ArrayList<APDevice> apList = new ArrayList<APDevice>();
    private ScrollView svListGroup;
    private SlideListView slvFoundDevices;
    private View icFoundDevices;
    private ProgressBar pgsBar;
    private android.app.AlertDialog.Builder alertDialogBuilder;
    private android.app.AlertDialog alertDialog;
    private AlertDialog.Builder wrongInputAlert;
    private LayoutInflater li;
    private View promptsView;
    private TextView aDiaglogText;
    private String provision_passphrase, provision_ssid, provision_sec;

    apListAdapter myadapter;

    public WifiConfiguration currentApInfo = null;
    public Integer scanStage = 0;
    private VerticalSwipeRefreshLayout mSwipeLayout;
    private VerticalSwipeRefreshLayout mSwipeLayout1;
    private EditText userInput;
    private String uuid;

    protected static final int SHOWALERTDIAG= 1;
    protected static final int TOAST = 3;
    protected static final int UPDATELIST = 4;
    protected static final int SHOWPROGRESSBAR = 5;
    protected static final int REMOVEPROGRESSBAR = 6;
    protected static final int SHOWDIALOG =7;


    final Context context = this;

    private EditText passwordInput;
    private EditText codeInput;
    private Button setPassword;
    private AlertDialog userDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiAdapter = new WlanAdapter(this);

        setContentView(R.layout.activity_network_prov_stage2);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        uuid = extras.getString(ServiceConstant.CognitoUuid);

        Log.d(LOG_TAG, "Debug:  uuid= "+uuid);

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



        TextView main_title = (TextView) findViewById(R.id.select_network_toolbar_title);
        main_title.setText("Select Network");

        onRefresh();

        svListGroup = (ScrollView) findViewById(R.id.svListGroup);
        icFoundDevices = findViewById(R.id.icFoundDevices);
        slvFoundDevices = (SlideListView) icFoundDevices.findViewById(R.id.slideOnlineListView);
        pgsBar = (ProgressBar) findViewById(R.id.pBar);


        wrongInputAlert= new AlertDialog.Builder(this);
        wrongInputAlert.setTitle("Wrong input");

        wrongInputAlert.setCancelable(true);
        wrongInputAlert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        li = LayoutInflater.from(context);
        promptsView = li.inflate(R.layout.content_new_ssid_password, null);
        aDiaglogText = (TextView)promptsView.findViewById(R.id.aDiagTextView1);
        aDiaglogText.setText("");

        alertDialogBuilder = new android.app.AlertDialog.Builder(
                context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message for network provision
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                MyHelper.d(">>>Password="+userInput.getText());

                                provision_passphrase = userInput.getText().toString();
                                if (provision_passphrase.length() < 8)
                                    showAlertDiagMsg("password length is less than 8 ...");
                                else{
                                    Intent activity_intent = new Intent(NetworkProvisionStageTwoActivity.this, NetworkProvisionStageThreeActivity.class);
                                    activity_intent.putExtra(ServiceConstant.Ssid,provision_ssid);
                                    activity_intent.putExtra(ServiceConstant.ApPassword,provision_passphrase);
                                    activity_intent.putExtra(ServiceConstant.ApSec,provision_sec);
                                    activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
                                    startActivity(activity_intent);
                                }

                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();
        slvFoundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                provision_ssid = apList.get(position).getAPSSIDName();
                provision_sec = apList.get(position).getAPSec();
                Message msg = new Message();
                msg.what = SHOWDIALOG;
                msg.obj = apList.get(position).getAPSSIDName();
                handler.sendMessage(msg);

            }
        });

        Log.d(LOG_TAG, ">>> onCreate");
        scanAP();
    }

    private void UpdateUI() {

        svListGroup.setVisibility(View.VISIBLE);

        Log.d(LOG_TAG, "UpdateUI ");

        if (apList.isEmpty()) {
            slvFoundDevices.setVisibility(View.GONE);
        } else {
            Log.d(LOG_TAG, "UpdateUI 2");
            myadapter = new apListAdapter(this, apList);
            slvFoundDevices.setAdapter(myadapter);
            slvFoundDevices.setVisibility(View.VISIBLE);
        }
    }

    private void showAlertDiagMsg(String text)
    {

        Message msg = new Message();
        msg.what = SHOWALERTDIAG;
        msg.obj = text;
        handler.sendMessage(msg);

    }

    public void scanAP(){

        if (scanStage == 0) {
            scanStage = 1;

            Message msg = new Message();
            msg.what = SHOWPROGRESSBAR;
            handler.sendMessage(msg);

            NetworkProvisionStageTwoActivity.ScanAPTask scanDev = new NetworkProvisionStageTwoActivity.ScanAPTask();
            scanDev.executeOnExecutor(MyThreadPool.getExecutor(), "scanAP");

        }
    }

    public void ScanTrigger(View view) {
        Log.d(LOG_TAG, "Debug: Scan button click");
        scanAP();
    }

    public void onRefresh() {
        Log.d(LOG_TAG, "Debug: onRefresh ");

    }


    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, ">>> onPause");
        //mWifiAdapter.removeCurrentAP();
        mWifiAdapter.unRegister();
        NetworkProvisionStageOneOneActivity.boundMessage.add("remove current AP");
    }

    private void exit() {
        finish();
    }

    private class ScanAPTask extends AsyncTask<String, Integer, String> {


        private String getSecurity(ScanResult result){
            if (result.capabilities.contains("WEP")) {
                return "WEP";
            } else if (result.capabilities.contains("PSK")) {
                return "WPA";
            }
            return "OPEN";
        }

        private boolean ScanAP(){

            List<ScanResult> rdata;
            int cnt = 2;

            do {
                rdata = mWifiAdapter.tryGetWifiList();
                scanStage = 0;

                if (rdata == null) {
                    MyHelper.d(">>>> Scan AP fail ....");
                    return false;
                }
                cnt--;
            } while (rdata.size() == 0 && (cnt>  0));


            apList.clear();
            // add scan result to the list
            for ( ScanResult sr : rdata) {
                if (null == sr.SSID || sr.SSID.isEmpty())
                    continue;

                if (sr.SSID.contains("WiFiSmartDevice_"))
                    continue;

                APDevice ap = new APDevice();
                ap.setAPSSIDName(sr.SSID);
                ap.setAPSec(getSecurity(sr));
                apList.add(ap);

            }

            Message msg1 = new Message();
            msg1.what = REMOVEPROGRESSBAR;
            handler.sendMessage(msg1);

            Message msg2 = new Message();
            msg2.what = UPDATELIST;
            msg2.obj = "";
            handler.sendMessage(msg2);

            return true;

        }

        @Override
        protected String doInBackground(String... params) {
            boolean ret;
            MyHelper.d(">>>> Start doInBackground ....");

			/* Stage 1: Scan the device AP */
            if (params[0].equals("scanAP")) {

                ret = ScanAP();

                if (ret == true)
                    return "scan_success"; 	// finish stage1 to scan the deivce, then show alertDiag for user to enter password
                else
                    return "scan_fail";
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

            if (result.equals("scan_success")) {
                MyHelper.d(">>>> Success to scan");
            }
            else if (result.equals("scan_fail")) {
                MyHelper.d(">>>> Fail to scan");
            }
            //mWifiAdapter.unRegister();

        }

    }


    Handler handler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case SHOWALERTDIAG:
                    wrongInputAlert.setMessage(msg.obj.toString());
                    wrongInputAlert.show();
                    break;

                case TOAST:
                    Toast.makeText(NetworkProvisionStageTwoActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;
                case UPDATELIST:
                    UpdateUI();
                    break;
                case SHOWPROGRESSBAR:
                    pgsBar.setVisibility(View.VISIBLE);
                    break;
                case REMOVEPROGRESSBAR:
                    pgsBar.setVisibility(View.GONE);
                    break;
                case SHOWDIALOG:
                    //String str="Provision to AP "+msg.obj.toString();
                    String str="Connect to " + msg.obj.toString();
                    aDiaglogText.setText(str);

                    alertDialog.show();
                    break;

            }
        };
    };

}
