'use strict';

var jwt_decode = require("jwt-decode");
const main_table = 'SensorBoardAcctTable';

/* ----------------------- IoT Configuration -------------------------------- */

var config = {};

config.IOT_BROKER_ENDPOINT = "xxxxxxxxxxx.iot.us-east-1.amazonaws.com".toLowerCase();

config.IOT_BROKER_REGION = "us-east-1";


// Load AWS SDK libraries
var AWS = require('aws-sdk');

AWS.config.region = config.IOT_BROKER_REGION;

// Initialize client for IoT
var iotData = new AWS.IotData({endpoint : config.IOT_BROKER_ENDPOINT});

// Initialize client for DynamoDB
var docClient = new AWS.DynamoDB.DocumentClient({apiVersion: '2012-08-10'});

/* -------------------- end: IoT Configuration ------------------------------ */


function getCongitoUUID(token)
{
  var accessToken;
  accessToken = jwt_decode(token);
  return accessToken.sub; 
}
/* -------------------- end: Get Cognito UUID from the access token --------------- */

/* ------------ Helpers that buil the link account responses --------------------- */

function buildLinkAccountSpeechletResponse(title, output, repromptText, shouldEndSession)
{
var response =     {
        outputSpeech : {
            type : 'PlainText',
            text : output,
            ssml: "<speak> " + output + " </speak>"
        },
        card :{
            type : 'LinkAccount',
            title : `Sensor Board`,
            content : title,
        },
        reprompt :{
            outputSpeech :
            {
                type : 'PlainText',
                text : repromptText,
            },
        },
        shouldEndSession,
    };
;
    return response;

}
/*----------------Get Cognito UUID from the access token-------------------*/
function getCongitoUUID(token)
{
  var accessToken;
  accessToken = jwt_decode(token);
  return accessToken.sub; 
}
/* -------------------- end: Get Cognito UUID from the access token --------------- */

/* ------------ Helpers that build all of the responses --------------------- */

function buildSpeechletResponse(title, output, repromptText, shouldEndSession, cardType)
{
var response =     {
        outputSpeech : {
            //type : 'PlainText',
            type : 'SSML',
            text : output,
            //ssml: "<speak>" + output + "<break time=\"3s\"/>" + output + "</speak>"
            ssml: "<speak>" + output + "</speak>"
        },
        card :{
            type : cardType,
            title : `Sensor Board`,
            content : title,
        },
        reprompt :{
            outputSpeech :
            {
                type : 'PlainText',
                text : repromptText,
                ssml: "<speak> " + output + " </speak>"
            },
        },
        shouldEndSession,
    };
;
    return response;

}

function buildResponse(sessionAttributes, speechletResponse)
{
var response =     {
        version:
        '1.0',
        sessionAttributes,
        response : speechletResponse,
    };
    return response;


}

/* ---------- end: Helpers that build all of the responses ------------------ */

/* ----------- Functions that control the skill's behavior ------------------ */

function getWelcomeResponse(callback)
{

    // If we wanted to initialize the session to have some attributes we could add those here.
    const sessionAttributes = {Session:"New session"};
    const cardTitle = 'Welcome  to Microchip Sensor board';
    const speechOutput = 'Welcome to Microchip Sensor board Skill. This skill is used to control and get the sensor data from WINC1500 Secure Wi-Fi Board, showed in Microchip Master\'s workshop';
            
    // If the user either does not reply to the welcome message or says something that is not understood, they will be prompted again with this text.
    const repromptText = 'Please tell me if you want the light on or off by saying, turn the light on';
    const shouldEndSession = false;
    const cardType = 'Simple';

    callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, cardType));

}

function getUnknownResponse(callback)
{

    // If we wanted to initialize the session to have some attributes we could add those here.
    const sessionAttributes = {Session:"New session"};
    const cardTitle = '';
    const speechOutput = 'Please try again or say help for a list of possible commands';
            
    // If the user either does not reply to the welcome message or says something that is not understood, they will be prompted again with this text.
    const repromptText = 'Please try again or say help for a list of possible commands';
    const shouldEndSession = false;

    callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));

}

getHelpResponse
function getHelpResponse(callback)
{

    // If we wanted to initialize the session to have some attributes we could add those here.
    const sessionAttributes = {};
    
    const speechOutput = 'The options are. Ask Sensor board to turn the light red, green, blue, yellow, on or off.'+
                            ' You can ask what is the temperature or humidity,'+
                            ' the light state or color,' +
                            ' or you can ask what are the buttons state.';
    const cardTitle = speechOutput;        
    // If the user either does not reply to the welcome message or says something that is not understood, they will be prompted again with this text.
    const repromptText = 'Please tell me if you want the light on or off by saying, turn the light on';
    const shouldEndSession = false;
    const cardType = 'Simple';

    callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, cardType));

}


function createFavoriteLEDStatusAttributes(desiredLEDStatus)
{
 var response =     {
        desiredLEDStatus,
    };
    
    return response;


}

/**
 * Sets the LED state
 */
function setGPIO(intent, session, callback, thingId)
{

    console.log("Slots =>", intent.slots);
    
    var cardTitle = intent.name;
    let saml21PORT = intent.slots.port.value;
    let desiredGPIOSlotValue = intent.slots.number.value;
    let set_clear_GPIO = intent.slots.ioState.value;
    let shadowLED_R = 0;
    let shadowLED_G = 0;
    let shadowLED_B = 0;
    let repromptText = '';
    let sessionAttributes = {};    
    
                                    
    const shouldEndSession = false;
    let speechOutput = '';
    
    saml21PORT = saml21PORT.replace('.', '');
    saml21PORT = saml21PORT.toUpperCase();
    desiredGPIOSlotValue = parseInt (desiredGPIOSlotValue);
    if (
        ((saml21PORT === 'A')&&  ((desiredGPIOSlotValue === 17)||(desiredGPIOSlotValue === 20)||(desiredGPIOSlotValue === 21)))
        ||((saml21PORT === 'B')&&  ((desiredGPIOSlotValue === 22)||(desiredGPIOSlotValue === 23)))
        )
    {
        speechOutput = "PORT \"" + saml21PORT + "\"   " + desiredGPIOSlotValue + " will be " + set_clear_GPIO ;
        cardTitle = speechOutput;
    }else
    {
        speechOutput = "PORT \"" + saml21PORT + "\" " + desiredGPIOSlotValue +" is not available.  PORT 'a' 17 20 21, and PORT 'b' 22 and 23 are available. " + '<break time="0.5s"/>'  + "Please provide other command";
        cardTitle = "PORT \"" + saml21PORT + "\" " + desiredGPIOSlotValue +" is not available.  PORT 'a' 17 20 21, and PORT 'b' 22 and 23 are available";
        const cardType = 'Simple';
        
        callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, cardType));
        return;
    }
   
    if (!((saml21PORT === 'A')||(saml21PORT ==='B')))
    {
 
        speechOutput = "Only PORT A and B are available." + '<break time="1s"/>'  + " Please provide other command";
        cardTitle = "Only PORT A and B are available";
        const cardType = 'Simple';
        
        callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, cardType));
        return;
    
    
    }
    
    var payloadObj = {"state" :
        { "desired" :
            { 
                
            }}};
    var PORTbit = "P"+saml21PORT;
    desiredGPIOSlotValue = (set_clear_GPIO == "clear") ? (0-desiredGPIOSlotValue):(desiredGPIOSlotValue);
    

    payloadObj.state.desired = {[PORTbit]: desiredGPIOSlotValue};
        
    speechOutput = speechOutput + '<break time="1s"/>'  + " Please provide other command";
    console.log("Alexa will say =>",speechOutput);


        //Prepare the parameters of the update call
        var paramsUpdate = {
    
            "thingName" : thingId,
            "payload" : JSON.stringify(payloadObj)
    
        };
    
        // Update IoT Device Shadow
        console.log("AWS-IOT => ",paramsUpdate);
        
        iotData.updateThingShadow(paramsUpdate, function(err, data)
        {
    
            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log("UpdateThingShadow=>",data);
                console.log("Calling callback from updateThingShadow returns");
                
                callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
                //context.succeed(buildResponse(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession)));
            
                console.log("buildSpeechletResponse returns =>",buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
                console.log("returning from callback from updateThingShadow returns");
                
            }
    
        });
        
            
        //repromptText = "I'm not sure if you want the light on or off. You can tell me if you " +
                'want the light on or off by saying, turn the light on';

    //callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession));

}


/**
 * Sets the LED state
 */
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
/**
 * Sets the LED state
 */
function setLEDState(intent, session, callback, thingId)
{

    const cardTitle = "Change LED value";//intent.name;
    const desiredLEDStateSlot = intent.slots.lightState;
    let shadowLED_R = 0;
    let shadowLED_G = 0;
    let shadowLED_B = 0;
    let shadowLight = 0;
    let repromptText = '';
    let sessionAttributes = {};                                    
                                    
    const shouldEndSession = false;
    let speechOutput = '';


    if (desiredLEDStateSlot)
    {

        const desiredLEDState = desiredLEDStateSlot.value;
        sessionAttributes = createFavoriteLEDStatusAttributes(desiredLEDState);
        
        repromptText = ""; //You can ask me if the light is on or off by saying, is the light on or off?";
        

        
        
        if ((desiredLEDState == 'red'))
        {
            shadowLED_R = 1;
            shadowLED_G = 0;
            shadowLED_B = 0;
            shadowLight = 1;
            speechOutput = "The Sensor board light is red. " + '<break time="1s"/>'  + "Please provide other command" ;
            
        }else
        if ((desiredLEDState == 'green'))
        {
            shadowLED_R = 0;
            shadowLED_G = 1;
            shadowLED_B = 0;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is green. " + '<break time="1s"/>'  + "Please provide other command" ;
            
        }else
        if ((desiredLEDState == 'blue'))
        {
            shadowLED_R = 0;
            shadowLED_G = 0;
            shadowLED_B = 1;
            shadowLight = 1;
            speechOutput = "The Sensor board light is blue. " + '<break time="1s"/>'  + "Please provide other command";
            
        }else
        if ((desiredLEDState == 'yellow'))
        {
            shadowLED_R = 1;
            shadowLED_G = 1;
            shadowLED_B = 0;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is yellow. " + '<break time="1s"/>'  + "Please provide other command";
            
        }else
        if ((desiredLEDState == 'cyan'))
        {
            shadowLED_R = 0;
            shadowLED_G = 1;
            shadowLED_B = 1;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is yellow. " + '<break time="1s"/>'  + "Please provide other command";
            
        }else
        if ((desiredLEDState == 'magenta'))
        {
            shadowLED_R = 1;
            shadowLED_G = 0;
            shadowLED_B = 1;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is yellow. " + '<break time="1s"/>'  + "Please provide other command";
            
        }else
        if ((desiredLEDState == 'white'))
        {
            shadowLED_R = 1;
            shadowLED_G = 1;
            shadowLED_B = 1;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is " + desiredLEDState + '<break time="1s"/>'  + " Please provide other command";
            
        }else
        if ((desiredLEDState == 'on')||(desiredLEDState == 'open'))
        {
            shadowLED_R = -1;
            shadowLED_G = -1;
            shadowLED_B = -1;
            shadowLight = 1;
            speechOutput = "The Sensor board  light is on" + '<break time="1s"/>'  + " Please provide other command";
            
        }else
        if ((desiredLEDState == 'off')||(desiredLEDState == 'close'))
        {
            shadowLED_R = -1;
            shadowLED_G = -1;
            shadowLED_B = -1;
            shadowLight = 0;
            speechOutput = "The Sensor board  light has been turned off" + '<break time="1s"/>'  + "Please provide other command";
            
        }else
        {
            speechOutput = "I'm not sure what you want. Please try again.";
            repromptText = "I'm not sure what you want. You can tell me if you " +
                'want the light blue, red, green, white or off';
            callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
            return;
        }
        
        /*
         * Update AWS IoT
         */
        
        var payloadObj;
        
        console.log("Alexa will say =>",speechOutput);
        if ((shadowLED_R == -1) && (shadowLED_G == -1) && (shadowLED_B == -1))
        {
            payloadObj = {"state" :
            { "desired" :
                { 
                  "Light" : shadowLight
                    
                }}};
        }
        else
        {
            payloadObj = {"state" :
            { "desired" :
                { "LED_R" : shadowLED_R,
                  "LED_G" : shadowLED_G,
                  "LED_B" : shadowLED_B,
                  "Light" : shadowLight
                    
                }}};
        }

        //Prepare the parameters of the update call
        var paramsUpdate = {

            "thingName" : thingId,
            "payload" : JSON.stringify(payloadObj)

        };

        // Update IoT Device Shadow
        console.log("AWS-IOT => ",paramsUpdate);
        
        iotData.updateThingShadow(paramsUpdate, function(err, data)
        {

            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log("UpdateThingShadow=>",data);
                console.log("Calling callback from updateThingShadow returns");
                
                callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
                //context.succeed(buildResponse(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession)));
            
                console.log("buildSpeechletResponse returns =>",buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
                console.log("returning from callback from updateThingShadow returns");
                
            }

        });
        
        
    } else
    {

        speechOutput = "I'm not sure if you want the light on or off. Please try again.";
        repromptText = "I'm not sure if you want the light on or off. You can tell me if you " +
                'want the light on or off by saying, turn the light on';

    }

    //callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession));

}

function getLEDStatusFromShadow(intent, session, callback, thingId) {
    let cardTitle = "Get LED";
    let desiredLEDStatus ="Test";
    const repromptText = null;
    const sessionAttributes = {};
    let shouldEndSession = false;
    let speechOutput = '';


    
    //Prepare the parameters of the update call
    var paramsUpdate = {
        "thingName" : thingId,
        //"payload" : JSON.stringify(payloadObj)
    };
    console.log("thingName=>",thingId);
    iotData.getThingShadow(paramsUpdate, function(err, data)
        {

            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log(data);
                var newdata = JSON.parse(data.payload);
                console.log("LED RED state is =>",newdata.state.reported.LED_R);
                console.log("LED GREEN state is =>",newdata.state.reported.LED_G);
                console.log("LED BLEU state is =>",newdata.state.reported.LED_B);
                //console.log("LED state is =>",data.payload);
                var redLED = newdata.state.reported.LED_R;
                var greenLED = newdata.state.reported.LED_G;
                var blueLED = newdata.state.reported.LED_B;
                var Light = newdata.state.reported.Light;
                var ledState;
                
                if (intent.slots.lightType.value == "state")
                {
                    if(Light == 0)
                        ledState ="off";
                    else
                        ledState ="on";
                    
                    speechOutput = `The Sensor Board light is ${ledState}.` +  `<break time="2s"/>`  + ` Please provide other command`
                
                }else
                if (intent.slots.lightType.value == "color")
                {
                    if (redLED == 1 && greenLED == 0 && blueLED == 0)
                        ledState = "red";
                    else 
                    if (redLED == 0 && greenLED == 1 && blueLED == 0)
                        ledState = "green";
                    else 
                    if (redLED == 0 && greenLED == 0 && blueLED == 1)
                        ledState = "blue";
                    else 
                    if (redLED == 1 && greenLED == 1 && blueLED == 1)
                        ledState = "white";
                    else 
                    if (redLED == 0 && greenLED == 0 && blueLED == 0)
                        ledState = "black";
                    else 
                    if (redLED == 1 && greenLED == 1 && blueLED == 0)
                        ledState = "yellow";
                    else 
                    if (redLED == 0 && greenLED == 1 && blueLED == 1)
                        ledState = "cyan";
                    else 
                    if (redLED == 1 && greenLED == 0 && blueLED == 1)
                        ledState = "magenta";
                    
                    speechOutput = `The Sensor Board light is ${ledState}.` +  `<break time="2s"/>`  + ` Please provide other command`
                
                }else
                    speechOutput = "Ask me, what is the light color or state";
     
                
                
                //shouldEndSession = true;
                console.log("UpdateThingShadow=>",data);
                console.log("Calling callback from updateThingShadow returns");
                
                //callback(sessionAttributes, buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession));
                callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));
                
                console.log("buildSpeechletResponse returns =>",buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession, "Simple"));
                console.log("returning from callback from updateThingShadow returns");
                
            }

        });


    // Setting repromptText to null signifies that we do not want to reprompt the user.
    // If the user does not respond or says something that is not understood, the session
    // will end.
    //callback(sessionAttributes, buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession));

}
function getButtonStatusFromShadow(intent, session, callback, thingId) {

    let cardTitle = "Get BUTTON";
    let desiredLEDStatus;
    let repromptText = null;
    const sessionAttributes = {};
    let shouldEndSession = false;
    let speechOutput = '';


    
    //Prepare the parameters of the update call
    var paramsUpdate = {
        "thingName" : thingId,
        //"payload" : JSON.stringify(payloadObj)
    };
    
    iotData.getThingShadow(paramsUpdate, function(err, data)
        {

            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log(data);
                var newdata = JSON.parse(data.payload);
                console.log("BUTTON_1 state is =>",newdata.state.reported.BUTTON_1);
                console.log("BUTTON_2 state is =>",newdata.state.reported.BUTTON_2);
                console.log("BUTTON_3 state is =>",newdata.state.reported.BUTTON_3);
                //console.log("LED state is =>",data.payload);
                var buttonstate = newdata.state.reported.setLED;
                var button1 = "up";
                var button2 = "up";
                var button3 = "up";
                if (newdata.state.reported.BUTTON_1 == 0)
                    button1 = "down";
                if (newdata.state.reported.BUTTON_2 == 0)
                    button2 = "down";
                if (newdata.state.reported.BUTTON_3 == 0)
                    button3 = "down";
                speechOutput = "The button states are, button1 is "+ button1 +
                                                    ",button2 is " + button2 +
                                                    ",button3 is " + button3 + ' <break time="2s"/>'  + " Please provide other command?";
                
                repromptText = "Any other command";
                //shouldEndSession = true;
                callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));

            }

        });


    // Setting repromptText to null signifies that we do not want to reprompt the user.
    // If the user does not respond or says something that is not understood, the session
    // will end.
    //callback(sessionAttributes, buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession));

}

function getSensorStatusFromShadow(intent, session, callback, thingId) {

    let cardTitle = "Get SENSOR";
    let desiredLEDStatus;
    const repromptText = null;
    const sessionAttributes = {};
    let shouldEndSession = false;
    let speechOutput = '';


    
    //Prepare the parameters of the update call
    var paramsUpdate = {
        "thingName" : thingId,
        //"payload" : JSON.stringify(payloadObj)
    };
    
    iotData.getThingShadow(paramsUpdate, function(err, data)
        {

            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log(data);
                var newdata = JSON.parse(data.payload);
                console.log("Temperature value is =>",newdata.state.reported.temp);
                console.log("BUTTON_2 state is =>",newdata.state.reported.hum);
                //console.log("LED state is =>",data.payload);
                
                if (intent.slots.sensorType.value == "temperature")
                {
                    speechOutput = 'The temperature is '+ parseFloat((newdata.state.reported.temp/100)*1.8 + 32).toFixed(1) + " degrees Farenheit. " + ' <break time="2s"/>'  + "Please provide other command" ;
                }else
                if (intent.slots.sensorType.value == "humidity")
                {
                    speechOutput = 'The humidity is '+ newdata.state.reported.hum + " percent. " + ' <break time="2s"/>'  + "Please provide other command";
                }else
                    speechOutput = 'Ask me,  what is the temperature or humidity';
                    
                //shouldEndSession = true;
                callback(sessionAttributes, buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession, "Simple"));

            }

        });


    // Setting repromptText to null signifies that we do not want to reprompt the user.
    // If the user does not respond or says something that is not understood, the session
    // will end.
    //callback(sessionAttributes, buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession));

}

/* --------- end: Functions that control the skill's behavior --------------- */


/* ----------------------------- Events ------------------------------------- */

/**
 * Called when the session starts.
 */
function onSessionStarted(sessionStartedRequest, session)
{
    console.log(`onSessionStarted requestId = ${sessionStartedRequest.requestId}, sessionId = ${session.sessionId}
    `);
}

/**
 * Called when the user launches the skill without specifying what they want.
 */
function onLaunch(launchRequest, session, callback)
{

    console.log(`onLaunch requestId = ${launchRequest.requestId}, sessionId = ${session.sessionId}
    `);

    // Dispatch to your skill's launch.
    console.log("Calling getWelcomeResponse");

    getWelcomeResponse(callback);

}

function handleSessionEndRequest(callback) {

    const cardTitle = 'Session Ended';
    const speechOutput = 'Thank you for using Microchip Sensor board skill. Have a nice day!';
    // Setting this to true ends the session and exits the skill.
    const shouldEndSession = true;

    callback({}, buildSpeechletResponse(cardTitle, speechOutput, null, shouldEndSession, "Simple"));

}

function deviceTooMuchResponse(intentName, callback)
{
    const sessionAttributes = {};
    let speechOutput = "More than one device are registered to your account, we cannot report the status";
    let repromptText = "";
    let shouldEndSession = true;
      
      callback(sessionAttributes, buildSpeechletResponse(intentName, speechOutput, repromptText, shouldEndSession, "Simple"));
    
}

function handleAcctNotLinkCase(intentRequest, session, callback)
{
    const intent = intentRequest.intent;
    const intentName = intentRequest.intent.name;
    
    if (intentName === 'LEDStateChangeIntent' || intentName === 'GPIOControl' || intentName === 'WhatsLEDStatusIntent' || intentName === 'WhatsButtonStatusIntent' || intentName === 'WhatsSensorStatusIntent')
    {
        const sessionAttributes = {};
        let speechOutput = "Account is not linked. The link account card was delivered to home section, please complete the account linking by following the steps showed in the link account card";
        let repromptText = "";
        let shouldEndSession = true;
                      
        callback(sessionAttributes, buildSpeechletResponse("LinkAccount", speechOutput, repromptText, shouldEndSession, "LinkAccount"));
    }
    else if ((intentName === 'AMAZON.HelpIntent')) {
        getHelpResponse(callback);
                        
    }
    else if (intentName === 'AMAZON.StopIntent' || intentName === 'AMAZON.CancelIntent') {
        handleSessionEndRequest(callback);
        
    } 
    else if(intentName === 'closeSession')
    {
        handleSessionEndRequest(callback);
    }
    else 
    {
        //throw new Error('Invalid intent');
        getUnknownResponse(callback);
    }
}
/**
 * Called when the user specifies an intent for this skill.
 */
function onIntent(intentRequest, session, callback)
{

    console.log(`onIntent requestId = ${intentRequest.requestId}, sessionId = ${session.sessionId}
    `);

    const intent = intentRequest.intent;
    const intentName = intentRequest.intent.name;
    
    console.log("inIntent =>",intentName);
    
    if (session.user.accessToken == null)
    {
        handleAcctNotLinkCase(intentRequest, session, callback);
        
         return;
    }
    
    const congitoUUID = getCongitoUUID(session.user.accessToken);
    console.log("congitoUUID =>",congitoUUID);
    //var result = scanThingId(congitoUUID);
    
    if (intentName === 'LEDStateChangeIntent' || intentName === 'GPIOControl' || intentName === 'WhatsLEDStatusIntent' || intentName === 'WhatsButtonStatusIntent' || intentName === 'WhatsSensorStatusIntent')
    {
        
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
    
        
        docClient.scan(scanParams, onScan);
        function onScan(err, data) {
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
                      const sessionAttributes = {};
                      let speechOutput = "Your device cannot be found";
                      let repromptText = "";
                      let shouldEndSession = false;
                      
                      callback(sessionAttributes, buildSpeechletResponse(intent.name, speechOutput, repromptText, shouldEndSession, "Simple"));
                     
                  }
                  else
                  {
                        console.log("Able to find " + data.Items.length + " device");
    
                        console.log("thing ID =>",data.Items[0].thingID);
                        // Dispatch to your skill's intent handlers
                        // Dispatch to your skill's intent handlers
                        
                        if (intentName === 'LEDStateChangeIntent') {
                            if (data.Items.length > 1)
                                deviceTooMuchResponse(intent.name, callback);
                            else
                                setLEDState(intent, session, callback, data.Items[0].thingID);
                            
                        }
                        else if (intentName === 'GPIOControl') {
                            if (data.Items.length > 1)
                                deviceTooMuchResponse(intent.name, callback);
                            else
                                setGPIO(intent, session, callback, data.Items[0].thingID);
        
                        }
                        else if (intentName === 'WhatsLEDStatusIntent') {
                            if (data.Items.length > 1)
                                deviceTooMuchResponse(intent.name, callback);
                            else
                                getLEDStatusFromShadow(intent, session, callback, data.Items[0].thingID);
                            
                        }
                        else if (intentName === 'WhatsButtonStatusIntent') {
                            if (data.Items.length > 1)
                                deviceTooMuchResponse(intent.name, callback);
                            else
                                getButtonStatusFromShadow(intent, session, callback, data.Items[0].thingID);
                            
                        }
                        else if (intentName === 'WhatsSensorStatusIntent') {
                            if (data.Items.length > 1)
                                deviceTooMuchResponse(intent.name, callback);
                            else
                                getSensorStatusFromShadow(intent, session, callback, data.Items[0].thingID);
                            
                        }
                        
                      
                  }
    
            }
        }
    }
    else if ((intentName === 'AMAZON.HelpIntent')) {
        getHelpResponse(callback);
                        
    }
    else if (intentName === 'AMAZON.StopIntent' || intentName === 'AMAZON.CancelIntent') {
        handleSessionEndRequest(callback);
        
    } 
    else if(intentName === 'closeSession')
    {
        handleSessionEndRequest(callback);
    }
    else 
    {
        //throw new Error('Invalid intent');
        getUnknownResponse(callback);
    }
    
    
    
}

/**
 * Called when the user ends the session.
 * Is not called when the skill returns shouldEndSession=true.
 */
function onSessionEnded(sessionEndedRequest, session)
{

    console.log(`onSessionEnded requestId = ${sessionEndedRequest.requestId}, sessionId = ${session.sessionId}
    `);
    // Add cleanup logic here

}

/* --------------------------- end: Events ---------------------------------- */


/* -------------------------- Main handler ---------------------------------- */

// Route the incoming request based on type (LaunchRequest, IntentRequest, etc.) The JSON body of the request is provided in the event parameter.
exports.handler = (event, context, callback) =>
{

    try{
        
        console.log("\rStarting handler =>\r");
        //return;
        console.log("Events", event);
        console.log("Context", context);
        console.log("callback", callback);
        
        
        /**
         * Uncomment this if statement and populate with your skill's application ID to
         * prevent someone else from configuring a skill that sends requests to this function.
         */
        /*
        if (event.session.application.applicationId !== 'amzn1.echo-sdk-ams.app.[unique-value-here]') {
             callback('Invalid Application ID');l
        }
         */

        if (event.request.type == 'LaunchRequest')
        {
            onLaunch(event.request,
                    event.session,
                    (sessionAttributes, speechletResponse) =>{
                        console.log("Returning from onLaunch");
                        //callback(null, buildResponse(sessionAttributes, speechletResponse));
                        context.succeed(buildResponse(sessionAttributes, speechletResponse));
            });
        } else if (event.request.type == 'IntentRequest')
        {
            onIntent(event.request,
                    event.session,
                    (sessionAttributes, speechletResponse) =>{
                        console.log("Returning from onIntent");
                        console.log("buildResponse returns =>",buildResponse(sessionAttributes, speechletResponse));
                        
                        //callback(null, buildResponse(sessionAttributes, speechletResponse));
                        context.succeed(buildResponse(sessionAttributes, speechletResponse));
            
                        console.log("Returning from callback");
                        
            });
        } else if (event.request.type == 'SessionEndedRequest')
        {
            onSessionEnded(event.request, event.session);
            callback();
        }


    }

    catch(err)
    {
        callback(err);
    }

};

/* ----------------------- end: Main handler -------------------------------- */