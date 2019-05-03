package com.amazonaws.mchp.awsprovisionkit.utils;

public class ConfigFileConstant {
    public static final String ConfigFilePath = "MyFileStorage";
    public static final String ConfigFileName = "config.txt";
    public static final String CUSTOMER_SPECIFIC_ENDPOINT_ATTR = "CUSTOMER_SPECIFIC_ENDPOINT";
    public static final String COGNITO_POOL_ID_ATTR = "COGNITO_POOL_ID";
    public static final String AWS_IOT_POLICY_NAME_ATTR = "AWS_IOT_POLICY_NAME";
    public static final String AWS_IOT_REGION_ATTR = "AWS_IOT_REGION";
    public static final String COGNITO_USER_POOL_ID_ATTR = "COGNITO_USER_POOL_ID";
    public static final String COGNITO_REGION_ATTR = "COGNITO_REGION";
    public static final String COGNITO_CLIENT_ID_ATTR = "COGNITO_CLIENT_ID";
    public static final String COGNITO_CLIENT_SECRET_ATTR = "COGNITO_CLIENT_SECRET";
    public static final String DB_TABLE_NAME_ATTR = "IndexTable";


    //========================================================================
    // User need to set below parameters to your AWS account credentials
    //========================================================================

    public static final String CUSTOMER_SPECIFIC_ENDPOINT_DEFAULT_VAL = "xxxxxxxxxxxxxxxxx.us-east-1.amazonaws.com";
    public static final String COGNITO_POOL_ID_DEFAULT_VAL = "us-east-1:xxxxxx-xxxxx-xxxx-xxxx-xxxxxxxxxxxxx";
    public static final String AWS_IOT_POLICY_NAME_DEFAULT_VAL = "AWS_IOT_POLICY_NAME";
    public static final String AWS_IOT_REGION_DEFAULT_VAL = "us-east-1";
    public static final String COGNITO_USER_POOL_ID_DEFAULT_VAL = "us-east-1_xxxxxxx";
    public static final String COGNITO_REGION_DEFAULT_VAL = "us-east-1";
    public static final String COGNITO_CLIENT_ID_DEFAULT_VAL = "xxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String COGNITO_CLIENT_SECRET_DEFAULT_VAL = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String DB_TABLE_NAME_DEFAULT_VAL = "SensorBoardAcctTable";

}
