'use strict';
var jwt_decode = require("jwt-decode");
const main_table = 'SensorBoardAcctTable';


var config = {};

config.IOT_BROKER_ENDPOINT = "xxxxxxxxxxxx.iot.us-east-1.amazonaws.com".toLowerCase();

config.IOT_BROKER_REGION = "us-east-1";

// Load AWS SDK libraries
var AWS = require('aws-sdk');

AWS.config.region = config.IOT_BROKER_REGION;



// Initialize client for DynamoDB
var docClient = new AWS.DynamoDB.DocumentClient({apiVersion: '2012-08-10'});

/* -------------------- end: IoT Configuration ------------------------------ */

/*----------------Get Cognito UUID from the access token-------------------*/
function getCognitoUUID(token)
{
  var accessToken;
  //return token;
  //token = 'eyJraWQiOiJrdCs2Q09xWkg2Q0JNOE1ZZ3hjMzFOSk95YnRQM0Z4cTJMQ2hVaFU1N1pBPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiIxOTg0NzEwNC0yNGY0LTQ4MDMtYTdhNi02NTk1OTNiZmQxODYiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6InByb2ZpbGUiLCJhdXRoX3RpbWUiOjE1MzIzODA1MzcsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy1lYXN0LTEuYW1hem9uYXdzLmNvbVwvdXMtZWFzdC0xX2g2OVpDTEtKciIsImV4cCI6MTUzMjM4NDEzNywiaWF0IjoxNTMyMzgwNTM3LCJ2ZXJzaW9uIjoyLCJqdGkiOiI4NzZlZDcyYS1iNGFiLTQwODUtODdiZi00NGYxNzE0N2NmZWYiLCJjbGllbnRfaWQiOiIybDhtazNyNDl0NDllYms4Z2o1bWxhZDgxYSIsInVzZXJuYW1lIjoiTHVjQXJjaGFtYmF1bHQifQ.PO_Q2noF53QakM4Upc8M22d6DK1_Yqb4uuFSk7ENX10NnxQPHMc1sXondpLZo-NZXmqNEIK5dkNeL18jtaMPq-ndfbJZyaliJYwx7-fASF0LIXtjy9EZiFCgJTpdcojnNr9hPcFevHJvCRGZ0wfk1DdSYjJ9Xv5XwUuNBQUW9sRe5sXJoECRSDj7Tf5mnOrBkr96U5ZVM6g08RmogGMoXTOgvjOb3S7BI7jzynvTCb8UJVJA_1RPipDxFUcQavcSDJJu0NWKd8_CNBG3MjDJsCgKxPK7jJPKQev_bPqkdCqtR0HFaWes-EYGeZFPlBu55k-uRHWC0G0bGqCr0JLMSA';
  accessToken = jwt_decode(token);
  return accessToken.sub; 
}

// Initialize client for IoT
var iotData = new AWS.IotData({endpoint : config.IOT_BROKER_ENDPOINT});
var iot = new AWS.Iot({endpoint : config.IOT_BROKER_ENDPOINT});
var done = false;

function set_aws_iot(LEDState, LEDIntensity, response, callback)
{
    console.log("HERE1", JSON.stringify(response));

    if (1)//desiredLEDStatus)
    {


        if (LEDState == "NULL")
        {
            var payloadObj = {"state" :
            { "desired" :
                { "LED_INTENSITY": LEDIntensity
                }}};
        }else if (LEDIntensity == "NULL")
        {
            payloadObj = {"state" :
            { "desired" :
                { 
                  "Light":LEDState
                }}};
        }else
        {
          payloadObj = {"state" :
            { "desired" :
                { "LED_INTENSITY": LEDIntensity,
                  "Light":LEDState
                }}};  
        }
        //console.log("myDevice =>",response.event);
        var myDevice = response.endpoint.endpointId;
        
        
        //Prepare the parameters of the update call
        
        var paramsUpdate = {

            "thingName" : myDevice,
            "payload" : JSON.stringify(payloadObj)

        };

        // Update IoT Device Shadow
        console.log("AWS-IOT => ", paramsUpdate);

        iotData.updateThingShadow(paramsUpdate, function(err, data)
        {

            done = true;
            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log("AWS-IOT returned value=>", data);
                var shadowData = JSON.parse(data.payload);
                
            paramsUpdate = {
                "thingName" : myDevice,
            };
            iotData.getThingShadow(paramsUpdate, function(err, data)
            {
    
                done = true;
                if (err)
                {
                    console.log(err); // Handle any errors
                } else
                {
                    console.log("AWS-IOT returned value=>", data);
                    
                    var shadowData = JSON.parse(data.payload);
                    //console.log("LED state is =>",shadowData.state.desired.setLED);
                    //console.log("LED state is =>",data.payload);
                    
                    var ErrorResponse = {
                    "event": {
                        "header": {
                          "namespace": "Alexa",
                          "name": "ErrorResponse",
                          "messageId": "abc-123-def-456",
                          "correlationToken": "dFMb0z+PgpgdDmluhJ1LddFvSqZ/jCc8ptlAKulUj90jSqg==",
                          "payloadVersion": "3"
                        },
                        "endpoint":{
                            "endpointId":myDevice
                        },
                        "payload": {
                          "type": "ENDPOINT_UNREACHABLE",
                          "message": "Unable to reach endpoint 12345 because it appears to be offline"
                        }
                      }
                    };
                    
                    
                    if (shadowData.state.reported.State == "offline")
                    {
                        console.log("ErrorResponse => ",ErrorResponse);
                        callback(ErrorResponse);
                        return;
                    }
    
                    callback(response);
    
                }
    
            });

                //callback(response);
            }

        });

    } else
    {


    }
    //while (done != true);

}
function get_aws_iot(responseReportState, callback)
{
    console.log("get_aws_iot() => ", JSON.stringify(responseReportState));
    
    if (1)//desiredLEDStatus)
    {
        var myDevice = responseReportState.event.endpoint.endpointId;
        //Prepare the parameters of the update call
        var paramsUpdate = {

            "thingName" : myDevice

        };

        // Update IoT Device Shadow
        console.log("AWS-IOT => ", paramsUpdate);
        


        iotData.getThingShadow(paramsUpdate, function(err, data)
        {

            done = true;
            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log("AWS-IOT returned value=>", data);
                
                var shadowData = JSON.parse(data.payload);
                //console.log("LED state is =>",shadowData.state.desired.setLED);
                //console.log("LED state is =>",data.payload);
                
  var ErrorResponse = {
                "event": {
                    "header": {
                      "namespace": "Alexa",
                      "name": "ErrorResponse",
                      "messageId": "abc-123-def-456",
                      "correlationToken": "dFMb0z+PgpgdDmluhJ1LddFvSqZ/jCc8ptlAKulUj90jSqg==",
                      "payloadVersion": "3"
                    },
                    "endpoint":{
                        "endpointId":myDevice
                    },
                    "payload": {
                      "type": "ENDPOINT_UNREACHABLE",
                      "message": "Unable to reach endpoint 12345 because it appears to be offline"
                    }
                  }
                };
                
                
                if (shadowData.state.reported.State == "disconnected")
                {
                    console.log("ErrorResponse => ",ErrorResponse);
                    callback(ErrorResponse);
                    return;
                }

                var lightIntensity = shadowData.state.reported.LED_INTENSITY;

                var ledstate = shadowData.state.reported.Light;
                if (ledstate == 0)
                {
                    //Value for PowerController "ON" or "OFF"
                    responseReportState.context.properties[0].value = "OFF";
                    //Value for PowerLevelController,  value between 0-100 %
                    responseReportState.context.properties[1].value = lightIntensity;
                    console.log("Light is OFF",JSON.stringify(responseReportState.context.properties));
                }else
                {    
                    //Value for PowerController "ON" or "OFF"
                    responseReportState.context.properties[0].value = "ON";
                    //Value for PowerLevelController,  value between 0-100 %
                    responseReportState.context.properties[1].value = lightIntensity;
                    console.log("Light is ON",JSON.stringify(responseReportState.context.properties));
                }
                console.log("The responseReportState will be ", JSON.stringify(responseReportState));
                callback(responseReportState);

            }

        });

    } else
    {


    }
    //while (done != true);

}

function scanThingId(cognitoUuid)
{

    var scanParams = {
        TableName: main_table,
        //ProjectionExpression: "#yr, title, info.rating",
        FilterExpression: "#cognitoUUID = :cognitoUUID",
        ExpressionAttributeNames: {
            "#cognitoUUID": "cognitoUUID",
        },
        ExpressionAttributeValues: {
             ":cognitoUUID": cognitoUuid,
        }
    };

    
    docClient.scan(scanParams, onScan);
    function onScan(err, data) {
        if (err) {
            console.error("Unable to scan the table. Error JSON:", JSON.stringify(err, null, 2));
        } 
        else
        {
            console.log("Scan succeeded.");
              if (data.Items.length == 0)
              {
                  console.error("Cannot find devices for this congito account");
                  
              }
              else
              {
                  console.error("Able to find" + data.Items.length + "device");
                  return data;
              }

        }
    }
    
    return null;
}
function handleDiscovery(request, context)
{
    //var myGlobalEndPointArray = [];
    var myEndPointID = {
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        };
        
    var myDevices = {
        "endpoints" :
        [
        {
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            },
            {
                "interface" : "Alexa.PowerLevelController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        },{
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            },
            {
                "interface" : "Alexa.PowerLevelController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        },{
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            },
            {
                "interface" : "Alexa.PowerLevelController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        },{
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            },
            {
                "interface" : "Alexa.PowerLevelController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        },{
            "endpointId" : "test1",
            "manufacturerName" : "Microchip Technologies Inc.",
            "friendlyName" : "Sensor Board",
            "description" : "Sensor Board RGB light",
            "displayCategories" : ["LIGHT"],
            "cookie" : {
                "key1" : "arbitrary key/value pairs for skill to reference this endpoint.",
                "key2" : "There can be multiple entries",
                "key3" : "but they should only be used for reference purposes.",
                "key4" : "This is not a suitable place to maintain current endpoint state."
            },
            "capabilities" :
            [
            {
                "type" : "AlexaInterface",
                "interface" : "Alexa",
                "version" : "3"
            },
            {
                "interface" : "Alexa.PowerController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            },
            {
                "interface" : "Alexa.PowerLevelController",
                "version" : "3",
                "type" : "AlexaInterface",
                "properties" :
                {
                    "supported" : [
                    {
                        "name" : "powerState"
                    }
                    ],
                    "retrievable" : true
                }
            }
            ]
        }
        
        ]
    };
    console.log("Discovery request =>:",request);
    console.log("Discovery request.directive.payload =>:",request.directive.payload);
    console.log("Discovery request.directive.payload.scope.token =>:",request.directive.payload.scope.token);
    const congitoUUID = getCognitoUUID(request.directive.payload.scope.token);
    console.log("congitoUUID =>",congitoUUID);
    var scanParams = {
            TableName: main_table,
            //ProjectionExpression: "#yr, title, info.rating",
            FilterExpression: "#cognitoUUID = :cognitoUUID",
            ExpressionAttributeNames: {
                "#cognitoUUID": "cognitoUUID",
        },
            ExpressionAttributeValues: {
                ":cognitoUUID": congitoUUID,
            }
        };
    
    console.log("scanParams =>",scanParams);
        
    docClient.scan(scanParams, function(err, data) {
        if (err) {
            console.error("Unable to scan the table. Error JSON:", JSON.stringify(err, null, 2));
        } 
        else
        {
            console.log("Scan succeeded.");
            console.log("data => ",data);
              if (data.Items.length == 0)
              {
                  console.error("Cannot find devices for this congito account");
                 
              }
              else
              {
                    console.log("Able to find " + data.Items.length + " device");
                    console.log("data.Items " + JSON.stringify(data.Items));

                    for ( var i =0; i < data.Items.length; i++)
                    {
                        //myEndPointID.endpointId = data.Items[i].thingID;
                        //console.log("myEndPointID",JSON.stringify(myEndPointID));
                        myDevices.endpoints[i].endpointId = data.Items[i].thingID;
                        myDevices.endpoints[i].friendlyName = data.Items[i].deviceName;
                        
                        //myDevices.endpoints.push(myEndPointID);
                    }
                    for (; i < 5;i++)
                    {
                        delete myDevices.endpoints[i];
                    }
                    console.log("myDevices",JSON.stringify(myDevices));
                    
/*                  

This code should work but when updating the  myDevices.endpoints[i].endpointId = data.Items[i].thingID
it updates all existing myDevices.endpoints[i].endpointId ([0], [1], [2])
So if we have 3 devices, the last value is loaded in all [0], [1], [2]...

                    //data.Items[0].thingID = 55; //Testing if this only updates Items[0]???? 
                    //console.log("data.Items " + JSON.stringify(data.Items));

                    for (  i =0; i < data.Items.length; i++)
                    {
                        myEndPointID.endpointId = data.Items[i].thingID;
                        console.log("myEndPointID",JSON.stringify(myEndPointID));
                        myDevices.endpoints[i].endpointId = data.Items[i].thingID;
                        myDevices.endpoints.push(myEndPointID);
                        console.log("HERE",i);
                    
                        console.log("myDevices",JSON.stringify(myDevices));
                    }
                

*/                    
                    // Dispatch to your skill's intent handlers
                    var header = request.directive.header;
                    header.name = "Discover.Response";
                    log("DEBUG", "Discovery Response: ", JSON.stringify({header : header, payload : myDevices}));
                    context.succeed({event :
                    { header : header, payload : myDevices}});

              }

        }
    });
 
}

function log(message, message1, message2)
{
    console.log(message + message1 + message2);
}

function handlePowerControl(request, context, callback)
{
    // get device ID passed in during discovery
    var requestMethod = request.directive.header.name;
    // get user token pass in request
    var requestToken = request.directive.endpoint.scope.token;
    var powerResult;
    var ledState;
    var ledColor = "NULL";
    console.log("Request=>", JSON.stringify(request));
    console.log("Context=>", JSON.stringify(context));
    console.log(requestMethod);
    if (requestMethod === "TurnOn")
    {

        // Make the call to your device cloud for control 
        // powerResult = stubControlFunctionToYourCloud(endpointId, token, request);
        ledState = 1;
        
        powerResult = "ON";
    } else if (requestMethod === "TurnOff")
    {
        // Make the call to your device cloud for control and check for success 
        // powerResult = stubControlFunctionToYourCloud(endpointId, token, request);
        ledState = 0;
        
        powerResult = "OFF";
    }
    var contextResult = {
        "properties" : [
        {
            "namespace" : "Alexa.PowerController",
            "name" : "powerState",
            "value" : powerResult,
            "timeOfSample" : "2017-12-17T14:46:50.52Z", //retrieve from result.
            "uncertaintyInMilliseconds" : 50
        }
        ]
    };
    var responseHeader = request.directive.header;
    var endPoint = request.directive.endpoint.endpointId;
    responseHeader.namespace = "Alexa";
    responseHeader.name = "Response";
    responseHeader.messageId = responseHeader.messageId + "-R";
    var response = {
        context : contextResult,
        event :
        {
            header : responseHeader
        },
        "endpoint" :
        {
            "scope" :
            {
                "type" : "BearerToken",
                "token" : "access-token-from-Amazon"
            },
            "endpointId" : endPoint
        },
        payload :
        {}

    };
    console.log("HERE", callback);
    set_aws_iot(ledState, ledColor, response, callback);
    //response.endpoint.scope.token = requestToken;
    log("DEBUG", "Alexa.PowerController ", JSON.stringify(response));

}
function handlePowerLevelControl(request, context, callback)
{
    // get device ID passed in during discovery
    var levelValue = request.directive.payload.powerLevel;
    // get user token pass in request
    var requestToken = request.directive.endpoint.scope.token;
    var powerResult;
    var ledState,ledColor;
    console.log("Request=>", JSON.stringify(request));
    console.log("Context=>", JSON.stringify(context));
    console.log("Power Level : ",levelValue);
    
    // Make the call to your device cloud for control 
    // powerResult = stubControlFunctionToYourCloud(endpointId, token, request);
    ledState = 1;
    ledColor = levelValue;
    powerResult = levelValue;
    
    var contextResult = {
        "properties" : [
        {
            "namespace" : "Alexa.PowerLevelController",
            "name" : "powerLevel",
            "value" : powerResult,
            "timeOfSample" : "2017-12-17T14:46:50.52Z", //retrieve from result.
            "uncertaintyInMilliseconds" : 50
        }
        ]
    };
    var responseHeader = request.directive.header;
    var endPoint = request.directive.endpoint.endpointId;
    responseHeader.namespace = "Alexa";
    responseHeader.name = "Response";
    responseHeader.messageId = responseHeader.messageId + "-R";
    var response = {
        context : contextResult,
        event :
        {
            header : responseHeader
        },
        "endpoint" :
        {
            "scope" :
            {
                "type" : "BearerToken",
                "token" : "access-token-from-Amazon"
            },
            "endpointId" : endPoint
        },
        payload :
        {}

    };
    console.log("HERE", callback);
    set_aws_iot(ledState, ledColor, response, callback);
    //response.endpoint.scope.token = requestToken;
    log("DEBUG", "Alexa.PowerController ", JSON.stringify(response));

}
var responseReportState = {  
   "context":{  
      "properties":[  
         {  
            "namespace":"Alexa.PowerController",
            "name":"powerState",
            "value":"ON",
            "timeOfSample":"2017-02-03T16:20:50.52Z",
            "uncertaintyInMilliseconds":6000
         },
         {  
            "namespace":"Alexa.PowerLevelController",
            "name":"powerLevel",
            "value":50,
            "timeOfSample":"2017-02-03T16:20:50.52Z",
            "uncertaintyInMilliseconds":6000
         }
      ]
   },
   "event":{  
      "header":{  
         "messageId":"abc-123-def-456",
         "correlationToken":"abcdef-123456",
         "namespace":"Alexa",
         "name":"StateReport",
         "payloadVersion":"3"
      },
      "endpoint":{  
         "scope":{  
            "type":"BearerToken",
            "token":"access-token-from-skill"
         },
         "endpointId":"luc_id2",
         "cookie":{  

         }
      },
      "payload":{  

      }
   }
}



exports.handler = (request, context, callback) =>
{
    //exports.handler = function (request, context) {

    try{

        if (request.directive.header.namespace === 'Alexa.Discovery' && request.directive.header.name === 'Discover')
        {
            log("DEGUG:", "Discover request", JSON.stringify(request));
            log("DEGUG:", "Discover context", JSON.stringify(context));
            handleDiscovery(request, context, "");
        } else if (request.directive.header.namespace === 'Alexa.PowerController')
        {
            if (request.directive.header.name === 'TurnOn' || request.directive.header.name === 'TurnOff')
            {
                log("DEBUG:", "Request PowerCtrl =>", JSON.stringify(request));
                log("DEBUG:", "Context PowerCtrl =>", JSON.stringify(context));
                handlePowerControl(request, context, (response) =>{
                    console.log("DONE LUC =>", JSON.stringify(response));
                    context.succeed(response);
                });
            }
        } else if (request.directive.header.namespace === 'Alexa.PowerLevelController')
        {
            log("DEBUG:", "Request PowerLevel =>", JSON.stringify(request));
            log("DEBUG:", "Context PowerLevel =>", JSON.stringify(context));
            handlePowerLevelControl(request, context, (response) =>{
                console.log("DONE LUC =>", JSON.stringify(response));
                context.succeed(response);
            });
            
        }
        else
        {
            
            log("DEGUG:", "Else request", JSON.stringify(request));
            log("DEGUG:", "Else context", JSON.stringify(context));
//            console.log("My Request is =>",request);
//            console.log("My Context is =>",context);
            console.log("My CallBack is =>",callback);
            if (request.directive.header.name === "ReportState")
            {
                responseReportState.event.header.correlationToken = request.directive.header.correlationToken;
                responseReportState.event.endpoint.endpointId = request.directive.endpoint.endpointId;
                get_aws_iot(responseReportState,response =>{
                    
                    console.log("DONE LUC =>", JSON.stringify(response));
                    
                    context.succeed(response);
                });
                //context.succeed(responseReportState);
                
            }
            
        }
        //callback();

    }

    catch(err)
    {
        callback(err);
    }
};

