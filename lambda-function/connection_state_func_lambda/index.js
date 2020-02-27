'use strict';

// When endpoint disconnect intentionally or unintentionally, 
// there is a Rule that runs in AWS IoT that will triiger this function
// The Rule is to monitor any evnet on $aws/events/presence/<thingID>

/* ----------------------- Redis Configuration -------------------------------- */
var redis = require('redis');
var bluebird = require('bluebird');
bluebird.promisifyAll(redis.RedisClient.prototype);
bluebird.promisifyAll(redis.Multi.prototype);

const GLOBAL_KEY = 'lambda-test';
const redisOptions = {
    host: "xxxxxxxxxxx.xxxx.xxxxx.xxxxx.cache.amazonaws.com",
    port: 6379
}

redis.debug_mode = true;


/* ----------------------- IoT Configuration -------------------------------- */

var config = {};

config.IOT_BROKER_ENDPOINT = "a3adakhi3icyv9-ats.iot.us-east-1.amazonaws.com".toLowerCase();

config.IOT_BROKER_REGION = "us-east-1";

config.IOT_THING_NAME = "WINC1500_1";

// Load AWS SDK libraries
var AWS = require('aws-sdk');

AWS.config.region = config.IOT_BROKER_REGION;

// Initialize client for IoT
var iotData = new AWS.IotData({endpoint : config.IOT_BROKER_ENDPOINT});
var iot = new AWS.Iot({endpoint : config.IOT_BROKER_ENDPOINT});
var done = false;

function getShadowPreviousTimeStamp(thingId, eventType, timestamp)
{
    var pre_timestamp;
    var state;
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
                console.log("ConnectedTime =>",newdata.state.reported.ConnectedTime);
                console.log("DisConnectedTime =>",newdata.state.reported.DisConnectedTime);

                if (eventType === "connected")
                {
                    pre_timestamp = newdata.state.reported.DisConnectedTime.split(' ');
                    console.log("pre_timestamp = ", pre_timestamp[0]);
                    if (timestamp > pre_timestamp[0])
                        state = 'online';
                    else
                        state = 'offline';
                }
                else if (eventType === "disconnected")
                {
                    pre_timestamp = newdata.state.reported.ConnectedTime.split(' ');
                    console.log("pre_timestamp = ", pre_timestamp[0]);
                    if (timestamp > pre_timestamp[0])
                        state = 'offline';
                    else
                        state = 'online';
                }
                else
                    state = 'offline';

            }
            console.log("state = ",state);
            

        });
}

function setShadow(thingId,eventType, timestamp)
{
    var d = new Date();
    var n = d.toString();
    var key = "Unknown Event";
    
    if (eventType === "connected")
    {
        key = "ConnectedTime";
    }
    if (eventType === "disconnected")
    {
        key = "DisConnectedTime";
    }
    
    var pre_timestamp;
    var state;
    
    //Prepare the parameters of the update call
    var paramsGet = {
        "thingName" : thingId,
    };
    
    
    iotData.getThingShadow(paramsGet, function(err, data)
    {

        if (err)
        {
            console.log(err); // Handle any errors
        } else
        {
            console.log(data);
            var newdata = JSON.parse(data.payload);
            console.log("ConnectedTime =>",newdata.state.reported.ConnectedTime);
            console.log("DisConnectedTime =>",newdata.state.reported.DisConnectedTime);
            
  
            if (eventType === "connected")
            {
                if (newdata.state.reported.DisConnectedTime == null)
                    pre_timestamp = 0;
                else
                    pre_timestamp = newdata.state.reported.DisConnectedTime.split(' ');
                console.log("pre_timestamp = ", pre_timestamp[0]);
                if (timestamp > pre_timestamp[0])
                    state = 'online';
                else
                    state = 'offline';
            }
            else if (eventType === "disconnected")
            {
                if (newdata.state.reported.ConnectedTime == null)
                    pre_timestamp = 0;
                else
                    pre_timestamp = newdata.state.reported.ConnectedTime.split(' ');
                    
                //When you reset the Sensor Board while it was in a conected state,
                //the  Connect event may oocur before the Disconnect event
                //Just ADD 1000 to ensure we do not make a device offline when online
                var myNum = Number(pre_timestamp[0]) +1000;     //Convert to number + 1000
                pre_timestamp[0] = myNum.toString();            //Convert back to string
                
                console.log("pre_timestamp = ", pre_timestamp[0]);
                if (timestamp > pre_timestamp[0])
                    state = 'offline';
                else
                    state = 'online';
            }
            else
                state = 'offline';

        }
        console.log("state = ",state);
        
        // Set both "reported" state to the new eventType
        var payloadObj = {"state" :
        { "reported" :
            { "State" : state,
            [key] : timestamp + ' ' + n
            //[key] : timestamp + ' '    
            }}
        };

        //Prepare the parameters of the update call
        var paramsUpdate = {

            "thingName" : thingId,
            "payload" : JSON.stringify(payloadObj)

        };
        console.log("paramsUpdate",paramsUpdate);
        if (state == "online")
            setTimeout(() => { console.log("delay 1 sec!"); }, 1000);
            
        iotData.updateThingShadow(paramsUpdate, function(err, data)
        {

            if (err)
            {
                console.log(err); // Handle any errors
            } else
            {
                console.log("UpdateThingShadow=>",data);
                console.log("Calling callback from updateThingShadow returns");
                

            }

        });

    });
        
    
}

function updateOnlineState(thingId,eventType, timestamp)
{
    var d = new Date();
    var n = d.toString();
    var key = "Unknown Event";
    
    
    
    var pre_timestamp;
    var state;

    
    var client = redis.createClient(redisOptions);
    let redis_val = {};

    client.hgetAsync(GLOBAL_KEY, thingId).then(res => {
        console.info('step1: Redis responses for get single: ', res);
        
        if (res != null)
        {
            redis_val = JSON.parse(res);
            
            console.log("ConnectedTime =>",redis_val.ConnectedTime);
            console.log("DisConnectedTime =>",redis_val.DisConnectedTime);
            
  
            if (eventType === "connected")
            {
                if (redis_val.DisConnectedTime == null)
                    pre_timestamp = 0;
                else
                    pre_timestamp = redis_val.DisConnectedTime.split(' ');
                console.log("pre_timestamp = ", pre_timestamp[0]);
                if (timestamp > pre_timestamp[0])
                    state = 'online';
                else
                    state = 'offline';
            }
            else if (eventType === "disconnected")
            {
                if (redis_val.ConnectedTime == null)
                    pre_timestamp = 0;
                else
                    pre_timestamp = redis_val.ConnectedTime.split(' ');
                    
                //When you reset the Sensor Board while it was in a conected state,
                //the  Connect event may oocur before the Disconnect event
                //Just ADD 1000 to ensure we do not make a device offline when online
                var myNum = Number(pre_timestamp[0]) +1000;     //Convert to number + 1000
                pre_timestamp[0] = myNum.toString();            //Convert back to string
                
                console.log("pre_timestamp = ", pre_timestamp[0]);
                if (timestamp > pre_timestamp[0])
                    state = 'offline';
                else
                    state = 'online';
            }
            else
                state = 'offline';

            console.log("state = ",state);
            redis_val.State = state;
            if (eventType === "connected")
            {
                redis_val.ConnectedTime = timestamp + ' ' + n
            }
            if (eventType === "disconnected")
            {
                redis_val.DisConnectedTime = timestamp + ' ' + n
            }
            
            
            //let data = event.data;
            let data = JSON.stringify(redis_val);
            console.log("data=", data);
        
            
            client.hmsetAsync(GLOBAL_KEY, thingId, data).then(res => {
                console.info('Redis responses for post: ', res)
                // callback(null, {body: "This is a CREATE operation"});
            }).catch(err => {
                console.error("Failed to post data: ", err)
                //callback(null, {statusCode: 500, message: "Failed to post data"});
            }).finally(() => {
                //console.info('Disconnect to Redis');
                //client.quit();
            });

        }    
        
        else
        {
            
        }
        
    }).catch(err => {
        console.error("Failed to get single: ", err)
    }).finally(() => {
        console.info('Disconnect to Redis');
        client.quit();
    });
    
    
    setShadow(thingId,eventType, timestamp)


}


/* -------------------- end: IoT Configuration ------------------------------ */


/* -------------------------- Main handler ---------------------------------- */

// Route the incoming request based on type (LaunchRequest, IntentRequest, etc.) The JSON body of the request is provided in the event parameter.
exports.handler = (event) =>
{

    try{
        
        console.log("\rStarting handler =>\r");
        //return;
        console.log("Events", event);
        console.log("ClientID", event.clientId);
        updateOnlineState(event.clientId,event.eventType,event.timestamp);
        
        
        /**
         * Uncomment this if statement and populate with your skill's application ID to
         * prevent someone else from configuring a skill that sends requests to this function.
         */
        /*
        if (event.session.application.applicationId !== 'amzn1.echo-sdk-ams.app.[unique-value-here]') {
             callback('Invalid Application ID');l
        }
         */
    }
    catch(err)
    {
        console.log(err);
    }

};

/* ----------------------- end: Main handler -------------------------------- */


