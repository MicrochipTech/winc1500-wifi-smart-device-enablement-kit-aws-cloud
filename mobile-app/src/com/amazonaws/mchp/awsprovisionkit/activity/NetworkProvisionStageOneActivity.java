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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mchp.awsprovisionkit.task.net.MyHelper;
import com.amazonaws.mchp.awsprovisionkit.task.net.WlanAdapter;
import com.amazonaws.mchp.awsprovisionkit.utils.*;
import com.amazonaws.mchp.awsprovisionkit.R;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("HandlerLeak")
public class NetworkProvisionStageOneActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    static final String LOG_TAG = NetworkProvisionStageOneActivity.class.getCanonicalName();
    public static List<String> boundMessage;
    WlanAdapter mWifiAdapter = null;
    public WifiConfiguration currentApInfo = null;

    protected static final int PROGRESSDIAG= 1;
    protected static final int PROGRESSDIAG_DISMISS= 2;
    protected static final int TOAST = 3;


    public ProgressDialog progressDialog;
    private String uuid;
    final Context context = this;

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
                    Toast.makeText(NetworkProvisionStageOneActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;


            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boundMessage = new ArrayList<String>();
        mWifiAdapter = new WlanAdapter(this);
        progressDialog = new ProgressDialog(context);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        uuid = extras.getString(ServiceConstant.CognitoUuid);
        Log.d(LOG_TAG, "Debug:  uuid= "+uuid);

        setContentView(R.layout.activity_network_prov_stage1);
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

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    public void GoNextStage(View view) {

        Log.d(LOG_TAG, "Debug:  Scan QR button click");

        Intent activity_intent = new Intent(NetworkProvisionStageOneActivity.this, NetworkProvisionStageOneOneActivity.class);
        activity_intent.putExtra(ServiceConstant.CognitoUuid,uuid);
        startActivity(activity_intent);
    }


    @Override
    protected void onResume() {
        super.onResume();

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




}
