/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mchp.awsprovisionkit.task.db;

import android.content.Context;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBManager {

    private static final String TAG = "DynamoDBManager";
    public static AmazonClientManager clientManager = null;

    /*
     * Creates a table with the following attributes: Table name: testTableName
     * Hash key: userNo type N Read Capacity Units: 10 Write Capacity Units: 5
     */
    public static void createTable(Context context, AmazonDynamoDBClient dbclient) {

        Log.d(TAG, "Create table called");

        clientManager = new AmazonClientManager(context, dbclient);
        AmazonDynamoDBClient ddb = clientManager.ddb();

        KeySchemaElement kse = new KeySchemaElement().withAttributeName(
                "userNo").withKeyType(KeyType.HASH);
        AttributeDefinition ad = new AttributeDefinition().withAttributeName(
                "userNo").withAttributeType(ScalarAttributeType.N);
        ProvisionedThroughput pt = new ProvisionedThroughput()
                .withReadCapacityUnits(10l).withWriteCapacityUnits(5l);

        CreateTableRequest request = new CreateTableRequest()
                .withTableName("SensorBoardAcctTable")
                .withKeySchema(kse).withAttributeDefinitions(ad)
                .withProvisionedThroughput(pt);

        try {
            Log.d(TAG, "Sending Create table request");
            ddb.createTable(request);
            Log.d(TAG, "Create request response successfully recieved");
        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error sending create table request", ex);
            clientManager.wipeCredentialsOnAuthError(ex);
        }
    }

    /*
     * Retrieves the table description and returns the table status as a string.
     */
    public static String getTestTableStatus(AmazonDynamoDBClient ddb) {

        try {
            //AmazonDynamoDBClient ddb = clientManager
            //        .ddb();

            DescribeTableRequest request = new DescribeTableRequest()
                    .withTableName("SensorBoardAcctTable");
            DescribeTableResult result = ddb.describeTable(request);

            String status = result.getTable().getTableStatus();
            return status == null ? "" : status;

        } catch (ResourceNotFoundException e) {
        } catch (AmazonServiceException ex) {
            clientManager.wipeCredentialsOnAuthError(ex);
        }

        return "";
    }

    /*
     * Inserts ten users with userNo from 1 to 10 and random names.
     */
    public static void insertUsers(AmazonDynamoDBClient ddb) {
        //AmazonDynamoDBClient ddb = clientManager.ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        try {
            for (int i = 1; i <= 10; i++) {
                UserPreference userPreference = new UserPreference();
                userPreference.setDeviceID("1");
                userPreference.setCognitoUUID("Test1");

                mapper.save(userPreference);
            }
        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error inserting users");
            clientManager.wipeCredentialsOnAuthError(ex);
        }
    }

    /*
     * Scans the table and returns the list of users.
     */
    public static ArrayList<UserPreference> getUserList() {

        AmazonDynamoDBClient ddb = clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        try {
            PaginatedScanList<UserPreference> result = mapper.scan(
                    UserPreference.class, scanExpression);



            ArrayList<UserPreference> resultList = new ArrayList<UserPreference>();
            for (UserPreference up : result) {
                resultList.add(up);
            }
            return resultList;

        } catch (AmazonServiceException ex) {
            clientManager.wipeCredentialsOnAuthError(ex);
        }

        return null;
    }

    public static ArrayList<UserPreference> scanUserList(AmazonDynamoDBClient ddb, String uuid) {


        DynamoDBMapper mapper = new DynamoDBMapper(ddb);


        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(uuid));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("cognitoUUID = :val1").withExpressionAttributeValues(eav);

        PaginatedScanList<UserPreference> scanResult = mapper.scan(UserPreference.class, scanExpression);
        ArrayList<UserPreference> resultList = new ArrayList<UserPreference>();
        for (UserPreference up : scanResult) {
            resultList.add(up);
        }

        return resultList;

    }

    /*
     * Retrieves all of the attribute/value pairs for the specified user.
     */
    public static UserPreference getUserPreference(int userNo) {

        AmazonDynamoDBClient ddb = clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        try {
            UserPreference userPreference = mapper.load(UserPreference.class,
                    userNo);

            return userPreference;

        } catch (AmazonServiceException ex) {
            clientManager
                    .wipeCredentialsOnAuthError(ex);
        }

        return null;
    }

    /*
     * Updates one attribute/value pair for the specified user.
     */
    public static void updateUserPreference(AmazonDynamoDBClient ddb, String devId, String uuid, String devName) {

        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        UserPreference userPreference = new UserPreference();
        userPreference.setDeviceID(devId);
        userPreference.setCognitoUUID(uuid);
        userPreference.setDeviceName(devName);

        mapper.save(userPreference);
        /*
        try {
            mapper.save(test);


        } catch (AmazonServiceException ex) {
            clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
        */
    }

    /*
     * Deletes the specified user and all of its attribute/value pairs.
     */
    public static void deleteUser(UserPreference deleteUserPreference) {

        AmazonDynamoDBClient ddb = clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        try {
            mapper.delete(deleteUserPreference);

        } catch (AmazonServiceException ex) {
            clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
    }

    /*
     * Deletes the test table and all of its users and their attribute/value
     * pairs.
     */
    public static void cleanUp() {

        AmazonDynamoDBClient ddb = clientManager
                .ddb();

        DeleteTableRequest request = new DeleteTableRequest()
                .withTableName("SensorBoardAcctTable");
        try {
            ddb.deleteTable(request);

        } catch (AmazonServiceException ex) {
            clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
    }

    @DynamoDBTable(tableName = "SensorBoardAcctTable")
    public static class UserPreference {
        private String deviceID;
        private String cognitoUUID;
        private String deviceName;

        @DynamoDBHashKey(attributeName = "thingID")
        public String getDeviceID() {
            return deviceID;
        }

        public void setDeviceID(String deviceID) {
            this.deviceID = deviceID;
        }

        @DynamoDBAttribute(attributeName = "cognitoUUID")
        public String getCognitoUUID() {
            return cognitoUUID;
        }

        public void setCognitoUUID(String cognitoUUID) {
            this.cognitoUUID = cognitoUUID;
        }

        @DynamoDBAttribute(attributeName = "deviceName")
        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String name) {
            this.deviceName = name;
        }


    }
}
