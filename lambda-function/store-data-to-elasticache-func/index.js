'use strict';

var redis = require('redis');
var bluebird = require('bluebird');
bluebird.promisifyAll(redis.RedisClient.prototype);
bluebird.promisifyAll(redis.Multi.prototype);

const GLOBAL_KEY = 'lambda-test';
const redisOptions = {
    host: "xxxxx-xxx-xxx-xxxxx.xxx.xxx.xxxx.cache.amazonaws.com",
    port: 6379
}

redis.debug_mode = true;



exports.handler = (event, context, callback) => {
    
    // TODO implement
    console.log("Events", event);
    //console.log("Context", context);
    
    let id = ""
    var testobj = {}
    var json_text = {
        macAddr: "",
        temp: 0,
        hum: 0,
        pressure: 0,
        uv: 0,
        Light: 0,
        LED_INTENSITY: 0,
        LED_R: 0,
        LED_G: 0,
        LED_B: 0,
        BUTTON_1: 0,
        BUTTON_2: 0,
        BUTTON_3: 0,
        State: "online",
        ConnectedTime: "",
        DisConnectedTime: ""
        
    };
    
    var client = redis.createClient(redisOptions);
    
    console.info('Start to connect to Redis Server')
    console.info('Connected to Redis Server')
    console.info('event.pathParameters: ', event.pathParameters);
    console.info('event.httpMethod: ', event.httpMethod);
  
    
    // get the id 
    if (event.hasOwnProperty('state'))
    {
        if (event.state.hasOwnProperty('reported'))
        {
            if (event.state.reported.hasOwnProperty('thingId'))
            {
                id = event.state.reported.thingId;
            }
        }
    }
    //let id = (event.pathParameters || {}).product || false;
    console.info('id: ', id);
    
    client.hgetAsync(GLOBAL_KEY, id).then(res => {
        console.info('step1: Redis responses for get single: ', res);
        if (res != null)
        {
            json_text = JSON.parse(res);
            console.info('temp=', json_text.temp);
        }
        ///callback(null, {body:  "This is a READ operation on product ID " + id, ret: res});
        // callback(null, {body: "This is a READ operation on product ID " + id});
        
        
        if (event.hasOwnProperty('state'))
        {
            console.log("debug log1");
            
            
            
            if (event.state.hasOwnProperty('reported'))
            {
                console.log("debug log2");
                if (event.state.reported.hasOwnProperty('macAddr'))
                {
                    console.log("macAddr", event.state.reported.macAddr);
                    //json_text = json_text + event.state.reported.macAddr;
                    json_text["macAddr"] = event.state.reported.macAddr;
                }
                if (event.state.reported.hasOwnProperty('temp'))
                {
                    console.log("temp", event.state.reported.temp);
                    //json_text = json_text + event.state.reported.temp;
                    json_text["temp"] = event.state.reported.temp;
                }
                if (event.state.reported.hasOwnProperty('hum'))
                {
                    console.log("hum", event.state.reported.hum);
                    //json_text = json_text + event.state.reported.hum;
                    json_text["hum"] = event.state.reported.hum;
                }
                if (event.state.reported.hasOwnProperty('pressure'))
                {
                    console.log("pressure", event.state.reported.pressure);
                    //json_text = json_text + event.state.reported.hum;
                    json_text["pressure"] = event.state.reported.pressure;
                }
                if (event.state.reported.hasOwnProperty('uv'))
                {
                    console.log("uv", event.state.reported.uv);
                    json_text["uv"] = event.state.reported.uv;
                }
                if (event.state.reported.hasOwnProperty('Light'))
                {
                    console.log("Light", event.state.reported.Light);
                    json_text["Light"] = event.state.reported.Light;
                }
                if (event.state.reported.hasOwnProperty('LED_INTENSITY'))
                {
                    console.log("LED_INTENSITY", event.state.reported.LED_INTENSITY);
                    json_text["LED_INTENSITY"] = event.state.reported.LED_INTENSITY;
                }
                if (event.state.reported.hasOwnProperty('LED_R'))
                {
                    console.log("LED_R", event.state.reported.LED_R);
                    json_text["LED_R"] = event.state.reported.LED_R;
                }
                if (event.state.reported.hasOwnProperty('LED_G'))
                {
                    console.log("LED_G", event.state.reported.LED_G);
                    json_text["LED_G"] = event.state.reported.LED_G;
                }
                if (event.state.reported.hasOwnProperty('LED_B'))
                {
                    console.log("LED_B", event.state.reported.LED_B);
                    json_text["LED_B"] = event.state.reported.LED_B;
                }
                if (event.state.reported.hasOwnProperty('BUTTON_1'))
                {
                    console.log("BUTTON_1", event.state.reported.BUTTON_1);
                    json_text["BUTTON_1"] = event.state.reported.BUTTON_1;
                }
                if (event.state.reported.hasOwnProperty('BUTTON_2'))
                {
                    console.log("BUTTON_2", event.state.reported.BUTTON_2);
                    json_text["BUTTON_2"] = event.state.reported.BUTTON_2;
                }
                if (event.state.reported.hasOwnProperty('BUTTON_3'))
                {
                    console.log("BUTTON_3", event.state.reported.BUTTON_3);
                    json_text["BUTTON_3"] = event.state.reported.BUTTON_3;
                }
                
                json_text["State"] = "online";
                
            }
        
        }
        console.log("json_text=", json_text);
        ///console.log("MacAddr", event.state.reported.macAddr);
        ///console.log("Temp", event.state.reported.temp);
        ///console.log("Hum", event.state.reported.hum);
        
        
        
        //let data = event.data;
        let data = JSON.stringify(json_text);
        console.log("data=", data);
    
        
        client.hmsetAsync(GLOBAL_KEY, id, data).then(res => {
            console.info('Redis responses for post: ', res)
            callback(null, {body: "This is a CREATE operation and it's successful", ret: res});
            // callback(null, {body: "This is a CREATE operation"});
        }).catch(err => {
            console.error("Failed to post data: ", err)
            //callback(null, {statusCode: 500, message: "Failed to post data"});
        }).finally(() => {
            //console.info('Disconnect to Redis');
            //client.quit();
        });
        
    }).catch(err => {
        console.error("Failed to get single: ", err)
        //callback(null, {statusCode: 500, message: "Failed to get data"});
    }).finally(() => {
        console.info('Disconnect to Redis');
        client.quit();
    });


    
    
    console.info('Finish all process')
    //callback(null, {body: "Finish"});

}