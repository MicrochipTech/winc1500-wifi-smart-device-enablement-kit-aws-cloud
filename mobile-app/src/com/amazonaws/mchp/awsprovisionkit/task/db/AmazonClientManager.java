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
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mchp.awsprovisionkit.utils.AppHelper;
import com.amazonaws.mchp.awsprovisionkit.utils.ConfigFileConstant;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * This class is used to get clients to the various AWS services. Before
 * accessing a client the credentials should be checked to ensure validity.
 */
public class AmazonClientManager {

    private static final String LOG_TAG = "AmazonClientManager";

    private AmazonDynamoDBClient ddb = null;
    private Context context;

    String customerSpecificEndPointAttr;
    String congnitoPoolId;
    String awsIoTPolicyName;
    String awsIoTRegion;
    String cognitoUserPoolId;
    String cognitoRegion;

    public AmazonClientManager(Context context, AmazonDynamoDBClient dbclient) {
        this.context = context;
        ddb = dbclient;
    }

    public AmazonDynamoDBClient ddb() {
        //validateCredentials();
        return ddb;
    }

    public void ReadCloudSettingFromConfigFile() {
        customerSpecificEndPointAttr = AppHelper.getStringfromConfigFile(context, ConfigFileConstant.CUSTOMER_SPECIFIC_ENDPOINT_ATTR);
        if (customerSpecificEndPointAttr.length() == 0)
            customerSpecificEndPointAttr = ConfigFileConstant.CUSTOMER_SPECIFIC_ENDPOINT_DEFAULT_VAL;

        congnitoPoolId = AppHelper.getStringfromConfigFile(context,ConfigFileConstant.COGNITO_POOL_ID_ATTR);
        if (congnitoPoolId.length() == 0)
            congnitoPoolId = ConfigFileConstant.COGNITO_POOL_ID_DEFAULT_VAL;

        awsIoTPolicyName = AppHelper.getStringfromConfigFile(context,ConfigFileConstant.AWS_IOT_POLICY_NAME_ATTR);
        if (awsIoTPolicyName.length() == 0)
            awsIoTPolicyName = ConfigFileConstant.AWS_IOT_POLICY_NAME_DEFAULT_VAL;

        awsIoTRegion = AppHelper.getStringfromConfigFile(context,ConfigFileConstant.AWS_IOT_REGION_ATTR);
        if (awsIoTRegion.length() == 0)
            awsIoTRegion = ConfigFileConstant.AWS_IOT_REGION_DEFAULT_VAL;

        cognitoUserPoolId = AppHelper.getStringfromConfigFile(context,ConfigFileConstant.COGNITO_USER_POOL_ID_ATTR);
        if (cognitoUserPoolId.length() == 0)
            cognitoUserPoolId = ConfigFileConstant.COGNITO_USER_POOL_ID_DEFAULT_VAL;

        cognitoRegion = AppHelper.getStringfromConfigFile(context,ConfigFileConstant.COGNITO_REGION_ATTR);
        if (cognitoRegion.length() == 0)
            cognitoRegion = ConfigFileConstant.COGNITO_REGION_DEFAULT_VAL;
    }

    public boolean hasCredentials() {
        String congnitoPoolId = AppHelper.getStringfromConfigFile(context, ConfigFileConstant.COGNITO_POOL_ID_ATTR);
        if (congnitoPoolId.length() == 0)
            congnitoPoolId = ConfigFileConstant.COGNITO_POOL_ID_DEFAULT_VAL;

        String tableName = AppHelper.getStringfromConfigFile(context, ConfigFileConstant.DB_TABLE_NAME_ATTR);
        if (tableName.length() == 0)
            tableName = ConfigFileConstant.DB_TABLE_NAME_DEFAULT_VAL;

        return (!(congnitoPoolId.equalsIgnoreCase("CHANGE_ME")
                || tableName.equalsIgnoreCase("CHANGE_ME")));
    }

    public void validateCredentials() {

        if (ddb == null) {
            initClients();
        }
    }

    private void initClients() {
        CognitoCachingCredentialsProvider credentials = new CognitoCachingCredentialsProvider(
                context,
                congnitoPoolId,
                Regions.fromName(awsIoTRegion));

        ddb = new AmazonDynamoDBClient(credentials);
        ddb.setRegion(Region.getRegion(Regions.fromName(awsIoTRegion)));
    }

    public boolean wipeCredentialsOnAuthError(AmazonServiceException ex) {
        Log.e(LOG_TAG, "Error, wipeCredentialsOnAuthError called" + ex);
        if (
        // STS
        // http://docs.amazonwebservices.com/STS/latest/APIReference/CommonErrors.html
        ex.getErrorCode().equals("IncompleteSignature")
                || ex.getErrorCode().equals("InternalFailure")
                || ex.getErrorCode().equals("InvalidClientTokenId")
                || ex.getErrorCode().equals("OptInRequired")
                || ex.getErrorCode().equals("RequestExpired")
                || ex.getErrorCode().equals("ServiceUnavailable")

                // DynamoDB
                // http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/ErrorHandling.html#APIErrorTypes
                || ex.getErrorCode().equals("AccessDeniedException")
                || ex.getErrorCode().equals("IncompleteSignatureException")
                || ex.getErrorCode().equals(
                        "MissingAuthenticationTokenException")
                || ex.getErrorCode().equals("ValidationException")
                || ex.getErrorCode().equals("InternalFailure")
                || ex.getErrorCode().equals("InternalServerError")) {

            return true;
        }

        return false;
    }
}
