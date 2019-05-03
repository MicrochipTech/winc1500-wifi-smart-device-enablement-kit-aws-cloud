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

package com.amazonaws.mchp.awsprovisionkit.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mchp.awsprovisionkit.task.db.DynamoDBManager;
import com.amazonaws.mchp.awsprovisionkit.task.json.AwsShadowJsonMsg;
import com.amazonaws.mchp.awsprovisionkit.utils.AppHelper;
import com.amazonaws.mchp.awsprovisionkit.utils.ConfigFileConstant;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.mchp.awsprovisionkit.utils.ServiceConstant;
import com.amazonaws.mchp.awsprovisionkit.task.json.AwsJsonMsg;
import com.amazonaws.services.iotdata.AWSIotDataClient;


import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import static java.lang.Thread.sleep;

//import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
//import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;

/**
 * Created by Administrator on 2016/12/20.
 */

public class AwsService extends Service {
    static final String LOG_TAG = AwsService.class.getCanonicalName();

/* AWS Related Settings */

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;
    CognitoCachingCredentialsProvider credentialsProviderForShadow;

    AWSIotDataClient iotDataClient;
    private static final String TAG = "AwsService";
    private String idToken;
    private boolean aws_connect = false;

    String customerSpecificEndPointAttr;
    String congnitoPoolId;
    String awsIoTPolicyName;
    String awsIoTRegion;
    String cognitoUserPoolId;
    String cognitoRegion;
    String cognitoUUID;

    /**
           * A connection to the DynamoDD service
           */
     private AmazonDynamoDBClient dbClient;


             /**
       * A reference to the DynamoDB table used to store data
       */

    public static final String DYNAMODB_TABLE = "MainDeviceTable";

    public AwsService() {
        super();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void onCreate()
    {
        super.onCreate();
    }

    public void ReadCloudSettingFromConfigFile() {
        customerSpecificEndPointAttr = AppHelper.getStringfromConfigFile(getApplicationContext(), ConfigFileConstant.CUSTOMER_SPECIFIC_ENDPOINT_ATTR);
        if (customerSpecificEndPointAttr.length() == 0)
            customerSpecificEndPointAttr = ConfigFileConstant.CUSTOMER_SPECIFIC_ENDPOINT_DEFAULT_VAL;

        congnitoPoolId = AppHelper.getStringfromConfigFile(getApplicationContext(),ConfigFileConstant.COGNITO_POOL_ID_ATTR);
        if (congnitoPoolId.length() == 0)
            congnitoPoolId = ConfigFileConstant.COGNITO_POOL_ID_DEFAULT_VAL;

        awsIoTPolicyName = AppHelper.getStringfromConfigFile(getApplicationContext(),ConfigFileConstant.AWS_IOT_POLICY_NAME_ATTR);
        if (awsIoTPolicyName.length() == 0)
            awsIoTPolicyName = ConfigFileConstant.AWS_IOT_POLICY_NAME_DEFAULT_VAL;

        awsIoTRegion = AppHelper.getStringfromConfigFile(getApplicationContext(),ConfigFileConstant.AWS_IOT_REGION_ATTR);
        if (awsIoTRegion.length() == 0)
            awsIoTRegion = ConfigFileConstant.AWS_IOT_REGION_DEFAULT_VAL;

        cognitoUserPoolId = AppHelper.getStringfromConfigFile(getApplicationContext(),ConfigFileConstant.COGNITO_USER_POOL_ID_ATTR);
        if (cognitoUserPoolId.length() == 0)
            cognitoUserPoolId = ConfigFileConstant.COGNITO_USER_POOL_ID_DEFAULT_VAL;

        cognitoRegion = AppHelper.getStringfromConfigFile(getApplicationContext(),ConfigFileConstant.COGNITO_REGION_ATTR);
        if (cognitoRegion.length() == 0)
            cognitoRegion = ConfigFileConstant.COGNITO_REGION_DEFAULT_VAL;
    }
    public void init()
    {
        Log.e(LOG_TAG, "idToken =" + idToken);

        cognitoUUID = CognitoJWTParser.getClaim(idToken, "sub");
        ReadCloudSettingFromConfigFile();
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                congnitoPoolId, // Identity Pool ID
                Regions.fromName(awsIoTRegion) // Region
        );


        //credentialsProviderForShadow = new CognitoCachingCredentialsProvider(
        //        getApplicationContext(), // context
        //        COGNITO_POOL_ID, // Identity Pool ID
        //        MY_REGION // Region
        //);

        iotDataClient = new AWSIotDataClient(credentialsProvider);
        String iotDataEndpoint = customerSpecificEndPointAttr;
        iotDataClient.setEndpoint(iotDataEndpoint);


        Map<String, String> logins = new HashMap<String, String>();
        logins.put("cognito-idp."+cognitoRegion+".amazonaws.com/"+cognitoUserPoolId, idToken);
        credentialsProvider.setLogins(logins);

        Region region = Region.getRegion(Regions.fromName(awsIoTRegion));

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, customerSpecificEndPointAttr);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        dbClient = new AmazonDynamoDBClient(credentialsProvider);
        dbClient.setRegion(Region.getRegion(Regions.fromName(awsIoTRegion)));

        //new DynamoDBManagerTask()
        //        .execute(DynamoDBManagerType.CREATE_TABLE);

    }
    public void mqttConnect()
    {
         {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (clientKeyStore == null) {
                        try {
                            // Create a new private key and certificate. This call
                            // creates both on the server and returns them to the
                            // device.
                            CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                    new CreateKeysAndCertificateRequest();
                            createKeysAndCertificateRequest.setSetAsActive(true);
                            final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                            createKeysAndCertificateResult =
                                    mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                            Log.i(LOG_TAG,
                                    "Cert ID: " +
                                            createKeysAndCertificateResult.getCertificateId() +
                                            " created.");

                            // store in keystore for use in MQTT client
                            // saved as alias "default" so a new certificate isn't
                            // generated each run of this application
                            AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                    createKeysAndCertificateResult.getCertificatePem(),
                                    createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                    keystorePath, keystoreName, keystorePassword);

                            // load keystore from file into memory to pass on
                            // connection
                            clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                    keystorePath, keystoreName, keystorePassword);

                            // Attach a policy to the newly created certificate.
                            // This flow assumes the policy was already created in
                            // AWS IoT and we are now just attaching it to the
                            // certificate.
                            AttachPrincipalPolicyRequest policyAttachRequest =
                                    new AttachPrincipalPolicyRequest();
                            policyAttachRequest.setPolicyName(awsIoTPolicyName);
                            policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                    .getCertificateArn());
                            mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        } catch (Exception e) {
                            Log.e(LOG_TAG,
                                    "Exception occurred when generating new private key and certificate.",
                                    e);
                        }
                    }
                    try {

                        mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        if (status == AWSIotMqttClientStatus.Connecting) {
                            Log.d(LOG_TAG, "Connecting");
                            sendConnectStatus("Connecting");
                            aws_connect = false;
                        } else if (status == AWSIotMqttClientStatus.Connected) {
                            Log.d(LOG_TAG, "Connected");
                            sendConnectStatus("Connected");
                            aws_connect = true;
                            //subscribeTopic(MCHP_IGATEWAY_SUBSCRIBE_TOPIC);
                        } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                            sendConnectStatus("Re-Connecting");
                            aws_connect = false;
                        } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                            sendConnectStatus("Connection Lost");
                            aws_connect = false;
                        } else {
//                                tvAwsStatus.setText("Disconnected");
                            sendConnectStatus("Disconnected");
                            aws_connect = false;
                        }
                        }
                    });
                        } catch (final Exception e) {
                            Log.e(LOG_TAG, "Connection error.", e);
//            tvAwsStatus.setText("Error! " + e.getMessage());
                        }



                    }
            }).start();
        }


    }


    public void updateAcctInfoToDynamodb(String macAddr, String devName) {

        new updateDeviceMappingTableTask().execute(macAddr, cognitoUUID, devName);

    }

    public void publishMessage(String message, String topic) {
        try {
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
        //getShadow();
    }

    public void updateShadow(String message, String thingName) {
        String topic = "$aws/things/"+thingName+"/shadow/update";
        try {
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public void subscribeShadowUpdate(String thingName) {
        final String topic = "$aws/things/"+thingName+"/shadow/update/accepted";
        Log.e(LOG_TAG, "Delta subscribe topic:" + topic);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    String message = new String();

                                    try {
                                        message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);

                                        //tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }

                                    AwsShadowJsonMsg msgContainer = new AwsShadowJsonMsg();
                                    msgContainer.parseJsonMsg(message);
                                    sendAwsShadowJsonMsg(msgContainer, topic);

                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }
            }
        }).start();
    }
    public void subscribeShadowDelta(String thingName) {
        final String topic = "$aws/things/"+thingName+"/shadow/update/delta";
        Log.e(LOG_TAG, "Delta subscribe topic:" + topic);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    String message = new String();

                                    try {
                                        message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);

                                        //tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }

                                    AwsShadowJsonMsg msgContainer = new AwsShadowJsonMsg();
                                    msgContainer.parseJsonMsg(message);
                                    sendAwsShadowJsonMsg(msgContainer, topic);

                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }
            }
        }).start();
    }

    public String tmpTopic ;
    public void subscribeTopic(String topic)
    {
        tmpTopic = topic;
       new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttManager.subscribeToTopic(tmpTopic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    String message = new String();

                                    try {
                                        message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);

                                        //tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                    AwsShadowJsonMsg msgContainer = new AwsShadowJsonMsg();
                                    msgContainer.parseJsonMsg(message);
                                    sendAwsShadowJsonMsg(msgContainer, topic);

                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }
            }
    }).start();
    }

    public void sendConnectStatus(String message)
    {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ServiceConstant.CloudStatus);
        broadcastIntent.putExtra(ServiceConstant.CloudStatusConn, message);
        sendBroadcast(broadcastIntent);

    }

    public void sendAwsShadowJsonMsg(AwsShadowJsonMsg obj, String topic)
    {
        Intent i = new Intent();
        i.putExtra(ServiceConstant.MQTTChannelName, topic);
        i.putExtra(ServiceConstant.JSONShadowMsgObject, obj);
        i.setAction(ServiceConstant.JSONShadowMsgReport);
        sendBroadcast(i);

    }
    public void sendThingIDList(ArrayList<DynamoDBManager.UserPreference> list)
    {
        Intent i = new Intent();
        final ArrayList<String> arr = new ArrayList<>();
        Log.d(LOG_TAG, "sendThingIDList");
        for (DynamoDBManager.UserPreference reply : list) {
            Log.d(LOG_TAG, "thingName =" + reply.getDeviceID() + "deviceName=" + reply.getDeviceName());
            arr.add("thingName="+reply.getDeviceID()+":"+"deviceName="+reply.getDeviceName());
        }

        Intent intent = new Intent();
        intent.putExtra(ServiceConstant.ThingIdList, arr);
        intent.setAction(ServiceConstant.ThingIDListReport);
        sendBroadcast(intent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction().equals(ServiceConstant.CloudStatus)) {
            Log.d(LOG_TAG, "MQTT Connect...");
            idToken = intent.getStringExtra("idToken");
            init();
            Log.d(LOG_TAG, "MQTT Connect idToken=" + idToken);
            mqttConnect();
        }
        else if(intent.getAction().equals(ServiceConstant.JSONMsgPublish)) {
            Log.d(LOG_TAG, "Receive publish message command...");
            Bundle extras = intent.getExtras();
            String connMessage = extras.getString(ServiceConstant.JSONMsgObject);
            String channel = extras.getString(ServiceConstant.MQTTChannelName);
            publishMessage(connMessage, channel);
        }
        else if(intent.getAction().equals(ServiceConstant.MQTTSubscribe)) {
            Log.d(LOG_TAG, "Receive subscribe message command...");
            String channel = intent.getStringExtra(ServiceConstant.MQTTChannelName);
            Log.d(LOG_TAG, "channel =" + channel);
            subscribeTopic(channel);
        }
        else if(intent.getAction().equals(ServiceConstant.MQTTUnSubscribe)) {
            Log.d(LOG_TAG, "Receive unsubscribe message command...");
            String channel = intent.getStringExtra(ServiceConstant.MQTTChannelName);
            Log.d(LOG_TAG, "channel =" + channel);
            if (aws_connect == true)
                mqttManager.unsubscribeTopic(channel);
        }

        else if(intent.getAction().equals(ServiceConstant.JSONMsgShadowGet)) {
            Log.d(LOG_TAG, "Receive shadow get message command...");
            final String thingName = intent.getStringExtra(ServiceConstant.AWSThingName);
            Log.d(LOG_TAG, "thingName =" + thingName);

            String shadow_accept_topic = "$aws/things/"+thingName+"/shadow/get/accepted";
            subscribeTopic(shadow_accept_topic);

            // may not able to subscribe the topic without the delay
            /*
            try {
                sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            */

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 1s = 1000ms
                    String shadow_get_topic = "$aws/things/"+thingName+"/shadow/get";
                    String connMessage = "{}";
                    publishMessage(connMessage, shadow_get_topic);
                }
            }, 100);


        }
        else if(intent.getAction().equals(ServiceConstant.JSONMsgShadowUpdate)) {
            Log.e(LOG_TAG, "Receive shadow update message command...");
            Bundle extras = intent.getExtras();
            String message = extras.getString(ServiceConstant.JSONMsgObject);
            String thingName = extras.getString(ServiceConstant.AWSThingName);
            Log.e(LOG_TAG, "String =" + thingName);
            Log.e(LOG_TAG, "msg =" + message);
            updateShadow(message,thingName);

        }
        else if(intent.getAction().equals(ServiceConstant.JSONMsgSubscribeShadowDelta)) {
            Log.e(LOG_TAG, "Receive subscribe shadow delta message command...");
            String thingName = intent.getStringExtra(ServiceConstant.AWSThingName);
            Log.e(LOG_TAG, "String =" + thingName);

            subscribeShadowDelta(thingName);

        }

        else if(intent.getAction().equals(ServiceConstant.JSONMsgSubscribeShadowUpdate)) {
            Log.e(LOG_TAG, "Receive subscribe shadow delta message command...");
            String thingName = intent.getStringExtra(ServiceConstant.AWSThingName);
            Log.e(LOG_TAG, "String =" + thingName);

            subscribeShadowUpdate(thingName);

        }
        else if(intent.getAction().equals(ServiceConstant.JSONMsgUnSubscribeShadowDelta)) {
            Log.e(LOG_TAG, "Receive unsubscribe message command...");
            String thingName = intent.getStringExtra(ServiceConstant.AWSThingName);
            String topic = "$aws/things/"+thingName+"/shadow/update/delta";
            if (aws_connect == true)
                mqttManager.unsubscribeTopic(topic);
        }
        else if(intent.getAction().equals(ServiceConstant.JSONMsgUnSubscribeShadowUpdate)) {
            Log.e(LOG_TAG, "Receive unsubscribe message command...");
            String thingName = intent.getStringExtra(ServiceConstant.AWSThingName);
            Log.e(LOG_TAG, "String =" + thingName);
            String topic = "$aws/things/"+thingName+"/shadow/update/accepted";
            if (aws_connect == true)
                mqttManager.unsubscribeTopic(topic);
        }
        else if (intent.getAction().equals(ServiceConstant.UpdateAcctInfoToDB))
        {
            String macAddr = intent.getStringExtra(ServiceConstant.DevMacAddr);
            String devName = intent.getStringExtra(ServiceConstant.DevName);
            Log.e(LOG_TAG, "MacAddr =" + macAddr + " Dev Name="+devName);
            updateAcctInfoToDynamodb(macAddr, devName);
        }
        else if (intent.getAction().equals(ServiceConstant.ScanThingID))
        {
            Log.e(LOG_TAG, "ScanThingID received");
            String congitoUUID = intent.getStringExtra(ServiceConstant.CognitoUuid);
            new DynamoDBManagerTask()
                    .execute(DynamoDBManagerType.SEARCH_THING_ID);
        }
        return START_STICKY;
    }

    private enum DynamoDBManagerType {
        GET_TABLE_STATUS, CREATE_TABLE, SEARCH_THING_ID, LIST_USERS, CLEAN_UP, UPDATE_USERS
    }

    private class DynamoDBManagerTaskResult {
        private DynamoDBManagerType taskType;
        private String tableStatus;

        public DynamoDBManagerType getTaskType() {
            return taskType;
        }

        public void setTaskType(DynamoDBManagerType taskType) {
            this.taskType = taskType;
        }

        public String getTableStatus() {
            return tableStatus;
        }

        public void setTableStatus(String tableStatus) {
            this.tableStatus = tableStatus;
        }
    }

    private class DynamoDBManagerTask extends
            AsyncTask<DynamoDBManagerType, Void, DynamoDBManagerTaskResult> {

        protected DynamoDBManagerTaskResult doInBackground(
                DynamoDBManagerType... types) {

            String tableStatus = DynamoDBManager.getTestTableStatus(dbClient);
            //String tableStatus = "ACTIVE";
            Log.d(LOG_TAG, "tableStatus=" + tableStatus);

            DynamoDBManagerTaskResult result = new DynamoDBManagerTaskResult();
            result.setTableStatus(tableStatus);
            result.setTaskType(types[0]);

            if (types[0] == DynamoDBManagerType.CREATE_TABLE) {
                if (tableStatus.length() == 0) {
                    DynamoDBManager.createTable(getApplicationContext(),dbClient);
                }
            } else if (types[0] == DynamoDBManagerType.SEARCH_THING_ID) {

                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    Log.d(LOG_TAG, "scanning uuid=" + cognitoUUID);
                    ArrayList<DynamoDBManager.UserPreference> scanresult = DynamoDBManager.scanUserList(dbClient, cognitoUUID);
                    for (DynamoDBManager.UserPreference reply : scanresult) {
                        Log.d(LOG_TAG, "getDeviceID =" + reply.getDeviceID());
                    }
                    sendThingIDList(scanresult);
                }
            } else if (types[0] == DynamoDBManagerType.LIST_USERS) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    DynamoDBManager.getUserList();
                }
            } else if (types[0] == DynamoDBManagerType.CLEAN_UP) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    DynamoDBManager.cleanUp();
                }
            } else if (types[0] == DynamoDBManagerType.UPDATE_USERS) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    //DynamoDBManager.updateUserPreference(dbClient);
                }
            }

            return result;
        }

        protected void onPostExecute(DynamoDBManagerTaskResult result) {

            if (result.getTaskType() == DynamoDBManagerType.CREATE_TABLE) {

                if (result.getTableStatus().length() != 0) {
                    Toast.makeText(
                            AwsService.this,
                            "The test table already exists.\nTable Status: "
                                    + result.getTableStatus(),
                            Toast.LENGTH_LONG).show();
                }

            } else if (!result.getTableStatus().equalsIgnoreCase("ACTIVE")) {

                Toast.makeText(
                        AwsService.this,
                        "The test table is not ready yet.\nTable Status: "
                                + result.getTableStatus(), Toast.LENGTH_LONG)
                        .show();
            } else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.SEARCH_THING_ID) {
            }
        }
    }

    private class updateDeviceMappingTableTask extends
            AsyncTask<String, Void, DynamoDBManager.UserPreference> {

        protected DynamoDBManager.UserPreference doInBackground(
                String... params) {

            String tableStatus = DynamoDBManager.getTestTableStatus(dbClient);

            Log.d(LOG_TAG, "updateDeviceMappingTableTask In");
            Log.d(LOG_TAG, "tableStatus=" + tableStatus);
            Log.d(LOG_TAG, "macAddr=" + params[0]);
            Log.d(LOG_TAG, "cognitoUUID=" + params[1]);

            DynamoDBManagerTaskResult result = new DynamoDBManagerTaskResult();
            result.setTableStatus(tableStatus);
            Log.d(LOG_TAG, "<<<<<<<<<<<<Update Table");
            DynamoDBManager.updateUserPreference(dbClient, params[0], params[1], params[2]);

            DynamoDBManager.UserPreference ret = new DynamoDBManager.UserPreference() ;

            return ret;
        }

        protected void onPostExecute(DynamoDBManager.UserPreference result) {


        }
    }

}


