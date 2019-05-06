//
//  AmazonService.swift
//  CognitoYourUserPoolsSample
//
//  Created by TestPC on 2019/2/21.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit
import AWSDynamoDB
import AWSCognitoIdentityProvider
import AWSIoT

protocol AmazonServiceDelegate {
    func CommandComplete()
    func SigninSuccess(result : Bool)
    func AWServiceReady()
    func QueryDeviceComplete(devielist : NSArray)
    func UpdateIOTStatus(status : String)
}

protocol DeviceControlDelegate {
    //func UpdateShadowMessage(btn1:NSNumber, btn2:NSNumber, LED:String)
    func UpdateAllData(address:String, thing_id:String)
    func UpdateLED(Led:String)
    func UpdateButtonState(state:NSNumber, btn:NSNumber)
    func UpdateState(state:String)
    func UpdateSensorData(newvalue1:String?, newvalue2:String?, newvalue3:String?, newvalue4:String?)
    func Refresh(LED:String, Btn1:NSNumber, Btn2:NSNumber, Btn3:NSNumber, newvalue1:String?, newvalue2:String?, newvalue3:String?, newvalue4:String?)
}

protocol ProvisionDelegate {
    func ProvisionComplete(status:Bool)
    func IOT_Connection_Status(status:String)
}

class AmazonService: NSObject {
    
    private static var mInstance:AmazonService?
    var Delegate: AmazonServiceDelegate?
    var ControlDelegate: DeviceControlDelegate?
    var ProvisionDelegate: ProvisionDelegate?
    
    //Cognito
    var CognitoUUID: String?
    var IdentityID: String?
    var AWS_Signin_User: String?
    var AWS_Signin_Password: String?
    
    //IOT,Device Control
    var iotDataManager: AWSIoTDataManager!
    var iotManager: AWSIoTManager!
    var iot: AWSIoT!
    
    var InitDeviceShadow : Bool = false
    var InitDeviceShadowMessage : Int = 0
    var GetResponse : Bool = false
    
    var MQTT_State : String?
    var LED_Color : String! = ""
    var Button1 : NSNumber! = 0
    var Button2 : NSNumber! = 0
    var Button3 : NSNumber! = 0
    var DeviceState : String! = ""
    var IOT_DeviceList : NSMutableArray = NSMutableArray()
    var DeviceInfoTable : NSMutableArray = NSMutableArray()
    var AWS_Account : NSMutableArray = NSMutableArray()
    var macAddress : String!
    var active_thing : String!
    
    var LED_R_Value : NSNumber = 0
    var LED_G_Value : NSNumber = 0
    var LED_B_Value : NSNumber = 0
    
    var LED_ON : NSNumber = -1
    
    var Sensor_Temp : NSNumber = 0
    var Sensor_Hum : NSNumber = 0
    var Sensor_UV : NSNumber = 0
    var Sensor_Pressure : NSNumber = 0
    
    var tmp_LED : String = ""
    var btn1 : NSNumber = 1
    var btn2 : NSNumber = 1
    var btn3 : NSNumber = 1
    var state : String = ""
    var tmp_temperature : NSNumber = 0
    var tmp_hum : NSNumber = 0
    var tmp_uv : NSNumber = 0
    var tmp_pressure : NSNumber = 0
    var tmp_LED_ON : NSNumber = -1
    var tmp_state : String = ""
    var tmp_intensity : NSNumber = 0
    
    var didSelectMacAddress : String?
    var didSelectThingID : String?
    var Refresh_Data : Bool = false
    
    var ProvisionState : String?
    
    override init() {
        super.init()
        
        print("[AmazonService] Init.")
    }
    
    deinit {
        print("deinit")
    }
    
    class func sharedInstace() -> AmazonService {
        if(mInstance == nil) {
            mInstance = AmazonService()
            print("[AmazonService]New object")
        }
        return mInstance!
    }
    
    // MARK: - Cognito
    
    func Set_Cognito_UUID(uuid:String, user:String){
        print("[AmazonService] Set_Cognito_UUID = \(uuid)")
        self.CognitoUUID = uuid
        
        if let array = UserDefaults.standard.object(forKey: "AWS_Account_Info"){
            self.AWS_Account = NSMutableArray(array: array as! NSArray)
            
            print("Data exist!")
            print("AWS accounts = \(self.AWS_Account)")
            
            var count: Int = 0
            var dict: [String:String] = [:]
            
            for index in 0..<self.AWS_Account.count {
                let account = self.AWS_Account.object(at: index) as! [String:String]
                let password = account[user]
                if(password == nil){
                    count += 1
                }
                else{
                    print("password is found = \(password!)")
                    break
                }
            }
            
            if(count == AWS_Account.count){
                if((self.AWS_Signin_User != nil) && (user == self.AWS_Signin_User)){
                    dict.updateValue(self.AWS_Signin_Password!, forKey: self.AWS_Signin_User!)
                    self.AWS_Account.add(dict)
                    
                    print("New AWS accounts = \(self.AWS_Account)")
                    UserDefaults.standard.set(self.AWS_Account, forKey: "AWS_Account_Info")
                    
                    self.AWS_Signin_User = nil
                    self.AWS_Signin_Password = nil
                }
            }
        }
        else{
            var dict:[String:String] = [:]
            if((self.AWS_Signin_User != nil) && (user == self.AWS_Signin_User)){
                dict.updateValue(self.AWS_Signin_Password!, forKey: self.AWS_Signin_User!)
                print("dict = \(dict)")
                self.AWS_Account.add(dict)
            
                print("Add.")
                print("AWS accounts = \(self.AWS_Account)")
                UserDefaults.standard.set(self.AWS_Account, forKey: "AWS_Account_Info")
                
                self.AWS_Signin_User = nil
                self.AWS_Signin_Password = nil
            }
        }
        
        self.Delegate?.SigninSuccess(result: true)
    }
    
    func ConnectToIdentityPool(identity:String){
        print("[AmazonService] ConnectToIdentityPool")
        
        self.IdentityID = identity
        
        self.MQTT_State = nil
        self.Delegate?.AWServiceReady()
    }
    
    func AWS_PrepareSignOut(){
        print("[AmazonService]SignOut")
        
        self.AWS_Signin_User = nil
        self.AWS_Signin_Password = nil
        
        if(self.MQTT_State == "connected"){
            if(iotDataManager != nil){
                iotDataManager.disconnect()
                print("IOT Disconnect.")
                iotManager = nil
            }
        }
    }
    
    func AWS_Login(user:String, password:String){
        print("[AmazonService] Login")
        
        self.AWS_Signin_User = user
        self.AWS_Signin_Password = password
        
    }
    
    func AWS_GetUserNamePassword(user:String) ->(String){
        print("[AmazonService] GetUserNamePassword")
        
        if let array = UserDefaults.standard.object(forKey: "AWS_Account_Info"){
            self.AWS_Account = NSMutableArray(array: array as! NSArray)
            
            print("AWS accounts = \(self.AWS_Account)")
            
            var count: Int = 0
            var password: String!
            
            for index in 0..<self.AWS_Account.count {
                let account = self.AWS_Account.object(at: index) as! [String:String]
                password = account[user]
                if(password == nil){
                    count += 1
                }
                else{
                    print("password is found = \(password!)")
                    break
                }
            }
            
            return password
        }
        else{
            return ""
        }
    }
    
    // MARK: - IOT
    
    func mqttEventCallback( _ status: AWSIoTMQTTStatus )
    {
        DispatchQueue.main.async {
            //print("[mqttEventCallback]connection status = \(status.rawValue)")
            
            switch(status)
            {
            case .connecting:
                print("[mqttEventCallback] connecting")
            case .connected:
                //print("connected")
                self.MQTT_State = "connected"
                self.Delegate?.UpdateIOTStatus(status: self.MQTT_State!)
                self.ProvisionDelegate?.IOT_Connection_Status(status: self.MQTT_State!)
            case .disconnected:
                //print("disconnected")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
                self.ProvisionDelegate?.IOT_Connection_Status(status: "")
            case .connectionRefused:
                //print("connectionRefused")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
                self.ProvisionDelegate?.IOT_Connection_Status(status: "")
            case .connectionError:
                //print("connectionError")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
                self.ProvisionDelegate?.IOT_Connection_Status(status: "")
            case .protocolError:
                //print("protocolError")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
                self.ProvisionDelegate?.IOT_Connection_Status(status: "")
            default:
                //print("deafult")
                self.MQTT_State = nil
            }
        }
    }
    
    func AWS_IOT_Get_Connection_Status(){
        if(iotManager != nil){
            print("AWS_IOT_Get_Connection_Status")
            let status = iotDataManager.getConnectionStatus()
            print("getConnectionStatus = \(status)")
            
            switch(status)
            {
            case .connecting:
                print("connecting")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
            case .connected:
                print("connected")
                self.MQTT_State = "connected"
                self.Delegate?.UpdateIOTStatus(status: self.MQTT_State!)
            case .disconnected:
                print("disconnected")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
            case .connectionRefused:
                print("connectionRefused")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
            case .connectionError:
                print("connectionError")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
            case .protocolError:
                print("protocolError")
                self.MQTT_State = nil
                self.Delegate?.UpdateIOTStatus(status: "")
            default:
                print("deafult")
                self.MQTT_State = nil
            }
        
            return
        }
        else{
            print("AWS_IOT_Get_Connection_Status")
            print("AWS_IOT_Connect")
            AWS_IOT_Connect()
        }
    }
    
    func AWS_IOT_Connect(){
        if(iotManager != nil){
            print("AWS_IOT_Connect,return")
            return
        }
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        //print("AWS_IOT_Connect")
        print("Getting Credentials,identityId = \(credentialsProvider.identityId)")
        
        //Init IOT
        let iotEndPoint = AWSEndpoint(urlString: IOT_ENDPOINT)
        
        // Configuration for AWSIoT control plane APIs
        let iotConfiguration = AWSServiceConfiguration(region: AWSRegion, credentialsProvider: credentialsProvider)
        
        // Configuration for AWSIoT data plane APIs
        let iotDataConfiguration = AWSServiceConfiguration(region: AWSRegion,endpoint: iotEndPoint,credentialsProvider: credentialsProvider)
        
        AWSServiceManager.default().defaultServiceConfiguration = iotConfiguration
        
        iotManager = AWSIoTManager.default()
        iot = AWSIoT.default()
        
        //AWSIoTMQTTConfiguration
        //keepAliveTimeInterval ???
        
        AWSIoTDataManager.register(with: iotDataConfiguration!, forKey: ASWIoTDataManager)
        iotDataManager = AWSIoTDataManager(forKey: ASWIoTDataManager)
        
        print("AWS_IOT_Connect")
        let uuid = UUID().uuidString
        print("uuid = \(uuid)")
        
        var certificateId : String?
        
        let defaults = UserDefaults.standard
        certificateId = defaults.string( forKey: "certificateId")
        
        if(certificateId == nil){
            let CertificateSigningRequestCommonName = "IoTSampleSwift Application"
            let CertificateSigningRequestCountryName = "Taiwan"
            let CertificateSigningRequestOrganizationName = "Microchip"
            let CertificateSigningRequestOrganizationalUnitName = "WSG"
            
            let csrDictionary = [ "commonName":CertificateSigningRequestCommonName, "countryName":CertificateSigningRequestCountryName, "organizationName":CertificateSigningRequestOrganizationName, "organizationalUnitName":CertificateSigningRequestOrganizationalUnitName ]
            
            self.iotManager.createKeysAndCertificate(fromCsr: csrDictionary, callback: {  (response ) -> Void in
                if (response != nil)
                {
                    defaults.set(response?.certificateId, forKey:"certificateId")
                    defaults.set(response?.certificateArn, forKey:"certificateArn")
                    let certificateId = response?.certificateId
                    print("response: [\(String(describing: response))]")
                    
                    let attachPrincipalPolicyRequest = AWSIoTAttachPrincipalPolicyRequest()
                    attachPrincipalPolicyRequest?.policyName = PolicyName
                    attachPrincipalPolicyRequest?.principal = response?.certificateArn
                    
                    // Attach the policy to the certificate
                    self.iot.attachPrincipalPolicy(attachPrincipalPolicyRequest!).continueWith (block: { (task) -> AnyObject? in
                        if let error = task.error {
                            print("failed: [\(error)]")
                        }
                        print("result: [\(String(describing: task.result))]")
                        
                        // Connect to the AWS IoT platform
                        if (task.error == nil)
                        {
                            DispatchQueue.main.asyncAfter(deadline: .now()+2, execute: {
                                //self.logTextView.text = "Using certificate: \(certificateId!)"
                                print("CreateKeysAndCertificate : PASS")
                                print("certificateId = \(certificateId!)")
                                self.iotDataManager.connect( withClientId: uuid, cleanSession:true, certificateId:certificateId!, statusCallback: self.mqttEventCallback)
                            })
                        }
                        return nil
                    })
                }
                else
                {
                    DispatchQueue.main.async {
                        print("CreateKeysAndCertificate : FAIL")
                    }
                }
            })
        }
        else{
            print("certificateId already exist!")
            self.iotDataManager.connect( withClientId: uuid, cleanSession:true, certificateId:certificateId!, statusCallback: self.mqttEventCallback)
            
            //self.iotDataManager.disconnect()
            
        }
    }
    
    func AWS_Select_Device(mac:String, uuid:String){
        
        print("AWS_Select_Device")
        print("MAC = \(mac)")
        print("UUID = \(uuid)")
        
        didSelectMacAddress = mac
        didSelectThingID = uuid
        
        Refresh_Data = true
    }
    
    func AWS_Get_Device() -> (String? , Int){
        print("AWS_Get_Device")
        
        if(self.IOT_DeviceList.count == 0){
            return (nil , 0)
        }
        
        for index in 0..<self.IOT_DeviceList.count {
            let dev = self.IOT_DeviceList.object(at: index) as! IoTDevice
            if(dev.MAC_Address == nil){
                return (dev.thingID!, index)
            }
        }
        
        return (nil , 0)
    }
    
    func AWS_IOT_Check_MacAddress() -> (Bool){
        
        var count:Int = 0
        for index in 0..<self.IOT_DeviceList.count {
            let dev = self.IOT_DeviceList.object(at: index) as! IoTDevice
            if(dev.MAC_Address == nil){
                break
            }
            else{
                count += 1
            }
        }
        
        if((count != 0) && (count == self.IOT_DeviceList.count)){
            print("AWS_IOT_Check_MacAddress = true")
            return true
        }
        else{
            print("AWS_IOT_Check_MacAddress = false")
            return false
        }
    }
    
    func AWS_IOT_Device_Shadow(action:Bool){
        if(action == true){
            if let device_array = UserDefaults.standard.object(forKey: "Sensorboard_Info"){
                print("[Refresh Device List]Update Mac address")
                print("device_array = \(device_array)")
                
                var Update : Bool = false
                
                let device_info = device_array as! NSArray
                self.DeviceInfoTable = NSMutableArray(array: device_info)
                
                for index in 0..<self.IOT_DeviceList.count {
                    var dict:[String:String] = [:]
                    let dev = self.IOT_DeviceList.object(at: index) as! IoTDevice
                    let thing_id = dev.thingID!
                    
                    var count:Int = 0
                    for k in 0..<device_info.count{
                        let dict = device_info.object(at: k) as! [String:String]
                        //print("Compared dict = \(dict)")
                        let found = dict[thing_id]
                        
                        if(found == nil){
                            count += 1
                        }
                        else{
                            print("Value is found = \(found!)")
                            if(dev.MAC_Address == nil){
                                dev.MAC_Address = found
                                print("Update MAC Address = \(found) , IOT_DeviceList.index = \(index)")
                            }
                            break
                        }
                    }
                    if(count == device_info.count){
                        print("UpdateTable : New thing_ID = \(thing_id)")
                        if(dev.MAC_Address != nil){
                            dict.updateValue(dev.MAC_Address!, forKey: dev.thingID!)
                        
                            self.DeviceInfoTable.add(dict)
                            Update = true
                        }
                    }
                }
                
                if(Update){
                    UserDefaults.standard.set(self.DeviceInfoTable, forKey: "Sensorboard_Info")
                    print("Save sensorboard thingID and MAC address")
                    print("New DeivceInfoTable = \(self.DeviceInfoTable)")
                
                    print("IOT_DeviceList = \(IOT_DeviceList)")
                }
            }
            return
        }
        
        if(self.MQTT_State == "connected"){
            print("DevList.count = \(self.IOT_DeviceList.count)")
            if(self.IOT_DeviceList.count != 0){
                
                InitDeviceShadowMessage = 0
                GetResponse = true
                InitDeviceShadow = true
                
                var device:String? = nil
                (device, self.InitDeviceShadowMessage) = self.AWS_Get_Device()
                
                if(device != nil){
                    print("thingID = \(device!) , index = \(self.InitDeviceShadowMessage)")
                    
                    let initDevice_queue = DispatchQueue(label: "com.microchip.aws_init")
                    
                    initDevice_queue.async {
                        
                        self.AWS_IOT_Shadow_Init(thingID: device! , action: false)
                        
                        while(self.InitDeviceShadow == true){
                            sleep(1)
                            print("Wait...")
                        }
                        
                        print("Complete: initDevice_queue")
                        
                        if let device_array = UserDefaults.standard.object(forKey: "Sensorboard_Info"){
                            print("device_array = \(device_array)")
                            
                            var Update : Bool = false
                            
                            let device_info = device_array as! NSArray
                            self.DeviceInfoTable = NSMutableArray(array: device_info)
                            
                            for index in 0..<self.IOT_DeviceList.count {
                                var dict:[String:String] = [:]
                                let dev = self.IOT_DeviceList.object(at: index) as! IoTDevice
                                let thing_id = dev.thingID!
                                
                                var count:Int = 0
                                for k in 0..<device_info.count{
                                    let dict = device_info.object(at: k) as! [String:String]
                                    //print("Compared dict = \(dict)")
                                    let found = dict[thing_id]
                                    
                                    if(found == nil){
                                        count += 1
                                    }
                                    else{
                                        print("Value is found = \(found!)")
                                        break
                                    }
                                }
                                if(count == device_info.count){
                                    print("UpdateTable : New thing_ID = \(thing_id)")
                                    dict.updateValue(dev.MAC_Address!, forKey: dev.thingID!)
                                    Update = true
                                    
                                    self.DeviceInfoTable.add(dict)
                                }
                            }
                            
                            if(Update){
                                UserDefaults.standard.set(self.DeviceInfoTable, forKey: "Sensorboard_Info")
                                print("Save sensorboard thingID and MAC address")
                                print("New DeivceInfoTable = \(self.DeviceInfoTable)")
                            }
                        }
                        else{
                            for index in 0..<self.IOT_DeviceList.count {
                                var dict:[String:String] = [:]
                                let dev = self.IOT_DeviceList.object(at: index) as! IoTDevice
                                dict.updateValue(dev.MAC_Address!, forKey: dev.thingID!)
                                print("dict = \(dict)")
                                self.DeviceInfoTable.insert(dict, at: index)
                            }
                            print("DeivceInfoTable = \(self.DeviceInfoTable)")
                        
                            UserDefaults.standard.set(self.DeviceInfoTable, forKey: "Sensorboard_Info")
                            print("Save sensorboard thingID and MAC address")
                        }
                        
                        DispatchQueue.main.async {
                            self.Delegate?.UpdateIOTStatus(status: "UpdateDevList")
                            self.Delegate?.QueryDeviceComplete(devielist: self.IOT_DeviceList)
                        }
                    }
                }
                else{
                    
                }
            }
        }
    }
    
    func AWS_IOT_Shadow_Init(thingID:String, action:Bool){
        print("AWS_IOT_Shadow_Init")
        
        active_thing = thingID
        
        //"$aws/things/01e92178ee15dd37cceb6002e91fc2f39b3cf546/shadow/get/accepted"
        var Subtopic:String = "$aws/things/"
        Subtopic += thingID
        Subtopic += "/shadow/get/accepted"
        
        //self.SubscribeTopic(topic: "$aws/things/01e92178ee15dd37cceb6002e91fc2f39b3cf546/shadow/get/accepted")
        self.SubscribeTopic(id:thingID, topic:Subtopic)
        
        var Pubtopic:String = "$aws/things/"
        Pubtopic += thingID
        Pubtopic += "/shadow/get"
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            
            //self.PublishTopic(message: "{}", topic: "$aws/things/01e92178ee15dd37cceb6002e91fc2f39b3cf546/shadow/get")
            
            self.PublishTopic(message: "{}", topic: Pubtopic)
            
            if(action){
                self.Delegate?.UpdateIOTStatus(status: "ShadowGet")
            }
            else{
                self.ASW_IOT_Shadow_Update(thingID: thingID)
            }
        }
    }
    
    func ASW_IOT_Shadow_Update(thingID:String){
        print("ASW_IOT_Shadow_Update")
        
        var Subtopic:String = "$aws/things/"
        Subtopic += thingID
        Subtopic += "/shadow/update/delta"
        
        self.SubscribeTopic(id:thingID, topic:Subtopic)
        
        var Subtopic1:String = "$aws/things/"
        Subtopic1 += thingID
        Subtopic1 += "/shadow/update/accepted"
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            //self.SubscribeTopic(topic: "$aws/things/01e92178ee15dd37cceb6002e91fc2f39b3cf546/shadow/update/accepted")
            self.SubscribeTopic(id:thingID, topic:Subtopic1)
        }
    }
    
    func AWS_Get_AllData(thing_id:String) -> (String?, String?, NSNumber, NSNumber, NSNumber, String, String, String, String, String, NSNumber){
        print("AWS_Get_AllData")
        
        var dat1:String?
        var dat2:String?
        var dat3:String?
        var dat4:String?
        
        print("temp = \(tmp_temperature)")
        print("hum = \(tmp_hum)")
        print("uv = \(tmp_uv)")
        print("pressure = \(tmp_pressure)")

        let temp = tmp_temperature.intValue
        dat1 = (NSNumber(value: Int(temp/100))).stringValue
    
        dat2 = tmp_hum.stringValue
        
        let uv = tmp_uv.intValue
        dat3 = (NSNumber(value:Int(uv/100000))).stringValue + "." + (NSNumber(value:Int(uv%100000/10000))).stringValue
        
        dat4 = tmp_pressure.stringValue
        
        return(didSelectMacAddress, tmp_state, btn1, btn2, btn3, tmp_LED, dat1!, dat2!, dat3!, dat4!, tmp_intensity)
    }
    
    func AWS_Get_Data(){
        print("AWS_Get_Data")
        
        if(Sensor_Temp == 0 || Sensor_Hum == 0 || Sensor_UV == 0 || Sensor_Pressure == 0 || LED_Color == ""){
            return
        }
        
        var dat1:String!
        var dat3:String!
        let temp = Sensor_Temp.intValue
        
        dat1 = (NSNumber(value: Int(temp/100))).stringValue
        
        let uv = Sensor_UV.intValue
        
        dat3 = (NSNumber(value:Int(uv/100000))).stringValue + "." + (NSNumber(value:Int(uv%100000/10000))).stringValue
        
        self.ControlDelegate?.Refresh(LED: LED_Color, Btn1: Button1, Btn2: Button2, Btn3: Button3, newvalue1: dat1, newvalue2: Sensor_Hum.stringValue, newvalue3: dat3, newvalue4: Sensor_Pressure.stringValue)
    }
    
    func PublishTopic(message:String, topic:String){
        print("PublishTopic,topic = \(topic),message = \(message)")
        
        let iotDataManager = AWSIoTDataManager(forKey: ASWIoTDataManager)
        
        iotDataManager.publishString(message, onTopic: topic, qoS: .messageDeliveryAttemptedAtMostOnce)
    }
    
    func SubscribeTopic(id:String, topic:String){
        print("SubscribeTopic , topic = \(topic)")
        
        iotDataManager = AWSIoTDataManager(forKey: ASWIoTDataManager)
        
        iotDataManager.subscribe(toTopic: topic, qoS: .messageDeliveryAttemptedAtMostOnce, messageCallback: {
            (payload) ->Void in
            let stringValue = NSString(data: payload, encoding: String.Encoding.utf8.rawValue)!
            
            //print("received: \(stringValue)")
            DispatchQueue.main.async {
                print("received: \(stringValue)")
                
                self.JSON_Parsing(jsonData: payload)
                
                if(self.InitDeviceShadow){
                    
                    var device:String? = nil
                    (device,self.InitDeviceShadowMessage) = self.AWS_Get_Device()
                    
                    if(device == nil){
                        print("End.")
                        self.InitDeviceShadow = false
                        self.InitDeviceShadowMessage = 0
                        self.macAddress = ""
                        self.state = ""
                    }
                }
            }
        } )
    }
    
    func AWS_IOT_UnSubscribe(thingid:String){
        
        print("AWS_IOT_UnSubscribe:thingid")
        
        var topic:String = "$aws/things/"
        topic += thingid
        topic += "/shadow/get/accepted"
        
        UnsubscribeTopic(Fromtopic: topic)
        
        var topic1:String = "$aws/things/"
        topic1 += thingid
        topic1 += "/shadow/update/delta"
        
        UnsubscribeTopic(Fromtopic: topic1)
        
        var topic2:String = "$aws/things/"
        topic2 += thingid
        topic2 += "/shadow/update/accepted"
        
        UnsubscribeTopic(Fromtopic: topic2)
    }
    
    func AWS_IOT_UnSubscribe(){
        
        if(didSelectThingID == nil){
            return
        }
        print("AWS_IOT_UnSubscribe")
        
        var topic:String = "$aws/things/"
        topic += didSelectThingID!
        topic += "/shadow/get/accepted"
        
        UnsubscribeTopic(Fromtopic: topic)
        
        var topic1:String = "$aws/things/"
        topic1 += didSelectThingID!
        topic1 += "/shadow/update/delta"
        
        UnsubscribeTopic(Fromtopic: topic1)
        
        var topic2:String = "$aws/things/"
        topic2 += didSelectThingID!
        topic2 += "/shadow/update/accepted"
        
        UnsubscribeTopic(Fromtopic: topic2)
        
        didSelectMacAddress = ""
        didSelectThingID = ""
    }
    
    func UnsubscribeTopic(Fromtopic:String){
        print("UnsubscribeTopic = \(Fromtopic)")
        
        iotDataManager = AWSIoTDataManager(forKey: ASWIoTDataManager)
        
        iotDataManager.unsubscribeTopic(Fromtopic)
    }
    
    func JSON_Parsing(jsonData:Data){
        do {
            let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options:JSONSerialization.ReadingOptions(rawValue: 0))
            guard let dictionary = jsonObject as? Dictionary<String, Any> else {
                print("Not a Dictionary")
                // put in function
                return
            }
            //print("JSON Dictionary! \(dictionary)")
            
            if let state_data = dictionary["state"]{
                if let dic = state_data as? Dictionary<String, Any> {
                    for (key, value) in dic  {
                        // access all key / value pairs in dictionary
                        //print("key = \(key) , value = \(value)")
                    }
                    
                    var check_mac : String? = nil
                    
                    if let reported_data = dic["reported"] as? Dictionary<String, Any> {
                        print("[Reported]")
                        
                        for (key, value) in reported_data  {
                            print("key = \(key) , value = \(value)")
                            
                            if(key == "macAddr"){
                                check_mac = value as! String
                                
                                self.macAddress = value as! String
                                print("macAddr = \(self.macAddress!)")
                            }
                            
                            if(key == "State"){
                                print("State = \(value as! String)")
                                state = value as! String
                                tmp_state = state
                            }
                            
                            if(key == "temp"){
                                tmp_temperature = value as! NSNumber
                                print("temp = \(tmp_temperature)")
                            }
                            
                            if(key == "hum"){
                                tmp_hum = value as! NSNumber
                                print("hum = \(tmp_hum)")
                            }
                            
                            if(key == "uv"){
                                tmp_uv = value as! NSNumber
                                print("uv = \(tmp_uv)")
                            }
                            
                            if(key == "pressure"){
                                tmp_pressure = value as! NSNumber
                                print("pressure = \(tmp_pressure)")
                            }
                            
                            if(key == "LED_R"){
                                LED_R_Value = (value as? NSNumber)!
                            }
                            else if(key == "LED_G"){
                                LED_G_Value = (value as? NSNumber)!
                            }
                            if(key == "LED_B"){
                                LED_B_Value = (value as? NSNumber)!
                            }
                            
                            if(key == "Light"){
                                tmp_LED_ON = (value as? NSNumber)!
                                print("Light = \(tmp_LED_ON),\(LED_ON)")
                            }
                            
                            if(key == "LED_INTENSITY"){
                                print("Update LED intensity = \((value as? NSNumber)!)")
                                
                                tmp_intensity = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_1"){
                                btn1 = (value as? NSNumber)!
                                //Button1 = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_2"){
                                btn2 = (value as? NSNumber)!
                                //Button2 = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_3"){
                                btn3 = (value as? NSNumber)!
                                //Button3 = (value as? NSNumber)!
                            }
                        }
                        
                        if(self.InitDeviceShadow){
                            
                            print("InitDeviceShadow = \(self.InitDeviceShadowMessage)")
                            
                            let dev = self.IOT_DeviceList.object(at: self.InitDeviceShadowMessage) as! IoTDevice
                            dev.MAC_Address = self.macAddress
                            if(self.state != ""){
                                dev.state = self.state
                                self.state = ""
                            }
                            print("dev MAC & State = \(dev)")
                            self.AWS_IOT_UnSubscribe(thingid: dev.thingID!)
                            
                            var device:String? = nil
                            (device,self.InitDeviceShadowMessage) = self.AWS_Get_Device()
                            
                            if(device == nil){
                                
                            }
                            else{
                                print("Next thingID = \(device!) , index = \(self.InitDeviceShadowMessage)")
                                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                                    self.AWS_IOT_Shadow_Init(thingID: device! , action: false)
                                }
                            }
                        }
                        
                        if(didSelectMacAddress != nil){
                            if((self.macAddress == didSelectMacAddress!) && (didSelectMacAddress != "")){
                                if(state != DeviceState){
                                    DeviceState = state
                                    print("Device state = \(DeviceState)")
                            
                                    self.ControlDelegate?.UpdateState(state: DeviceState)
                                }
                            }
                            else{
                                print("MAC address != didSelectMacAddress")
                                return
                            }
                        }
                        
                        print("\(LED_R_Value),\(LED_G_Value),\(LED_B_Value)")
                        
                        if(LED_R_Value == 0 && LED_G_Value == 0 && LED_B_Value == 1){
                            print("Blue LED!")
                            tmp_LED = "Blue"
                            
                        }
                        else if(LED_R_Value == 0 && LED_G_Value == 1 && LED_B_Value == 0){
                            print("Green LED!")
                            tmp_LED = "Green"
                            
                        }
                        else if(LED_R_Value == 1 && LED_G_Value == 0 && LED_B_Value == 0){
                            print("Red LED!")
                            tmp_LED = "Red"
                            
                        }
                        else if(LED_R_Value == 1 && LED_G_Value == 1 && LED_B_Value == 1){
                            print("White LED!")
                            tmp_LED = "White"
                            
                        }
                        else if(LED_R_Value == 1 && LED_G_Value == 1 && LED_B_Value == 0){
                            print("Yellow LED!")
                            tmp_LED = "Yellow"
                            
                        }
                        else if(LED_R_Value == 0 && LED_G_Value == 1 && LED_B_Value == 1){
                            print("Cyan LED!")
                            tmp_LED = "Cyan"
                            
                        }
                        else if(LED_R_Value == 1 && LED_G_Value == 0 && LED_B_Value == 1){
                            print("Magenta LED!")
                            tmp_LED = "Magenta"
                            
                        }
                        else{
                            
                        }
                        
                        if(tmp_LED_ON == 0){
                            print("Update LED On/Off")
                            
                            self.ControlDelegate?.UpdateLED(Led: "OFF")
                        }
                        
                        if(tmp_LED != LED_Color){
                            LED_Color = tmp_LED
                            self.ControlDelegate?.UpdateLED(Led: tmp_LED)
                        }
                        
                        if((Button1 != btn1) || (Button2 != btn2) || (Button3 != btn3)){
                            if(Button1 != btn1){
                                Button1 = btn1
                                self.ControlDelegate?.UpdateButtonState(state: Button1, btn: 1)
                            }
                            else if(Button2 != btn2){
                                Button2 = btn2
                                self.ControlDelegate?.UpdateButtonState(state: Button2, btn: 2)
                            }
                            else if(Button3 != btn3){
                                Button3 = btn3
                                self.ControlDelegate?.UpdateButtonState(state: Button3, btn: 3)
                            }
                        }
                        
                        if((tmp_pressure != Sensor_Pressure) || (tmp_hum != Sensor_Hum) || (tmp_uv != Sensor_UV) || (tmp_temperature != Sensor_Temp)){
                            var dat1:String?
                            var dat2:String?
                            var dat3:String?
                            var dat4:String?
                            
                            if(tmp_temperature != Sensor_Temp){
                                Sensor_Temp = tmp_temperature
                                let temp = Sensor_Temp.intValue
                                
                                dat1 = (NSNumber(value: Int(temp/100))).stringValue
                            }
                            
                            if(tmp_hum != Sensor_Hum){
                                Sensor_Hum = tmp_hum
                                dat2 = Sensor_Hum.stringValue
                            }
                            
                            if(tmp_uv != Sensor_UV){
                                Sensor_UV = tmp_uv
                                
                                let uv = Sensor_UV.intValue
                                
                                dat3 = (NSNumber(value:Int(uv/100000))).stringValue + "." + (NSNumber(value:Int(uv%100000/10000))).stringValue
                            }
                            
                            if(tmp_pressure != Sensor_Pressure){
                                Sensor_Pressure = tmp_pressure
                                dat4 = Sensor_Pressure.stringValue
                            }
                            
                            self.ControlDelegate?.UpdateSensorData(newvalue1: dat1, newvalue2: dat2, newvalue3: dat3, newvalue4: dat4)
                        }
                        
                        if(Refresh_Data){
                            Refresh_Data = false
                            self.ControlDelegate?.UpdateAllData(address: self.didSelectMacAddress!, thing_id: self.didSelectThingID!)
                        }
                    }
                }
            }
        }
        catch let error as NSError {
            print("Found an error - \(error)")
            
        }
    }
    
    func JSON_Get_State(jsonData:Data) -> (String?, String?, Btn1:NSNumber, Btn2:NSNumber, Btn3:NSNumber, LED:String, temp:String, hum:String, uv:String, pressure:String){
        do {
            let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options:JSONSerialization.ReadingOptions(rawValue: 0))
            guard let dictionary = jsonObject as? Dictionary<String, Any> else {
                print("Not a Dictionary")
                return (nil, nil, 0, 0, 0, "", "", "", "", "")
            }
            
            if let state_data = dictionary["state"]{
                if let dic = state_data as? Dictionary<String, Any> {
                    for (key, value) in dic  {
                        // access all key / value pairs in dictionary
                        //print("key = \(key) , value = \(value)")
                    }
                    
                    var dev_mac:String?
                    var dev_state:String?
                    var _btn1:NSNumber!
                    var _btn2:NSNumber!
                    var _btn3:NSNumber!
                    var _LED_R_Value:NSNumber!
                    var _LED_G_Value:NSNumber!
                    var _LED_B_Value:NSNumber!
                    var _LED_ON:NSNumber!
                    var _temperature:NSNumber!
                    var _hum:NSNumber!
                    var _uv:NSNumber!
                    var _pressure:NSNumber!
                    var _LED:String!
                    var dat1:String!
                    var dat2:String!
                    var dat3:String!
                    var dat4:String!
                    
                    if let reported_data = dic["reported"] as? Dictionary<String, Any> {
                        print("JSON_Get_State")
                        print("[Reported]")
                        
                        for (key, value) in reported_data  {
                            print("key = \(key) , value = \(value)")
                            
                            if(key == "macAddr"){
                                dev_mac = value as! String
                                print("macAddr = \(dev_mac)")
                            }
                            
                            if(key == "State"){
                                print("State = \(value as! String)")
                                dev_state = value as! String
                            }
                            
                            if(key == "temp"){
                                _temperature = value as! NSNumber
                                print("temp = \(_temperature)")
                                let temp = _temperature.intValue
                                dat1 = (NSNumber(value: Int(temp/100))).stringValue
                            }
                            
                            if(key == "hum"){
                                _hum = value as! NSNumber
                                print("hum = \(_hum)")
                                dat2 = _hum.stringValue
                            }
                            
                            if(key == "uv"){
                                _uv = value as! NSNumber
                                let uv = _uv.intValue
                                dat3 = (NSNumber(value:Int(uv/100000))).stringValue + "." + (NSNumber(value:Int(uv%100000/10000))).stringValue
                                print("uv = \(_uv)")
                            }
                            
                            if(key == "pressure"){
                                _pressure = value as! NSNumber
                                dat4 = _pressure.stringValue
                                print("pressure = \(_pressure)")
                            }
                            
                            if(key == "LED_R"){
                                _LED_R_Value = (value as? NSNumber)!
                            }
                            else if(key == "LED_G"){
                                _LED_G_Value = (value as? NSNumber)!
                            }
                            if(key == "LED_B"){
                                _LED_B_Value = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_1"){
                                _btn1 = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_2"){
                                _btn2 = (value as? NSNumber)!
                            }
                            
                            if(key == "BUTTON_3"){
                                _btn3 = (value as? NSNumber)!
                            }
                            
                            if(key == "Light"){
                                _LED_ON = (value as? NSNumber)!
                            }
                        }
                        
                        if(_LED_R_Value == 0 && _LED_G_Value == 0 && _LED_B_Value == 1){
                            _LED = "Blue"
                        }
                        else if(_LED_R_Value == 0 && _LED_G_Value == 1 && _LED_B_Value == 0){
                            _LED = "Green"
                        }
                        else if(_LED_R_Value == 1 && _LED_G_Value == 0 && _LED_B_Value == 0){
                            _LED = "Red"
                        }
                        else if(_LED_R_Value == 1 && _LED_G_Value == 1 && _LED_B_Value == 1){
                            _LED = "White"
                        }
                        else if(_LED_R_Value == 1 && _LED_G_Value == 1 && _LED_B_Value == 0){
                            _LED = "Yellow"
                        }
                        else if(_LED_R_Value == 0 && _LED_G_Value == 1 && _LED_B_Value == 1){
                            _LED = "Cyan"
                        }
                        else if(_LED_R_Value == 1 && _LED_G_Value == 0 && _LED_B_Value == 1){
                            _LED = "Magenta"
                        }
                        
                        if(_LED_ON == 0){
                            _LED = "OFF"
                        }
                        
                        return(dev_mac, dev_state, _btn1, _btn2, _btn3, _LED, dat1, dat2, dat3, dat4)
                    }
                }
            }
        }
        catch let error as NSError {
            print("Found an error - \(error)")
            return (nil, nil, 0, 0, 0, "", "", "", "", "")
        }
        
        return (nil, nil, 0, 0, 0, "", "", "", "", "")
    }
    
    func Test_JSON_Parsing(){
        let defaults = UserDefaults.standard
        let jsonData = defaults.data(forKey: "JSON_Test_Data")
        
        if(jsonData == nil){
            return
        }
        
        print("Test_JSON_Parsing")
        
        let stringValue = NSString(data: jsonData!, encoding: String.Encoding.utf8.rawValue)!
        //print("str = \(stringValue)")
        
        do {
            let jsonObject = try JSONSerialization.jsonObject(with: jsonData!, options:JSONSerialization.ReadingOptions(rawValue: 0))
            guard let dictionary = jsonObject as? Dictionary<String, Any> else {
                print("Not a Dictionary")
                // put in function
                return
            }
            
            if let state_data = dictionary["state"]{
                
                if let dic = state_data as? Dictionary<String, Any> {
                    for (key, value) in dic  {
                        // access all key / value pairs in dictionary
                        //print("key = \(key) , value = \(value)")
                    }
                    
                    if let reported_data = dic["reported"] as? Dictionary<String, Any> {
                        print("[Reported]")
                        var LED_R_Value : NSNumber = 0
                        var LED_G_Value : NSNumber = 0
                        var LED_B_Value : NSNumber = 0
                        
                        for (key, value) in reported_data  {
                            print("key = \(key) , value = \(value)")
                            
                            if(key == "LED_R"){
                                LED_R_Value = (value as? NSNumber)!
                            }
                            else if(key == "LED_G"){
                                LED_G_Value = (value as? NSNumber)!
                            }
                            if(key == "LED_B"){
                                LED_B_Value = (value as? NSNumber)!
                            }
                        }
                        
                        print("\(LED_R_Value),\(LED_G_Value),\(LED_B_Value)")
                        
                        if(LED_R_Value == 0 && LED_G_Value == 0 && LED_B_Value == 1){
                            print("Blue LED!")
                        }
                        else if(LED_R_Value == 0 && LED_G_Value == 1 && LED_B_Value == 0){
                            print("Green LED!")
                        }
                        else if(LED_R_Value == 1 && LED_G_Value == 0 && LED_B_Value == 0){
                            print("Red LED!")
                        }
                        else{
                            print("Unknown color")
                        }
                    }
                }
            }
        }
        catch let error as NSError {
            print("Found an error - \(error)")
        }
        
    }
    
    func AWS_IOT_LED_Control_Topic(message:String){
        if(didSelectThingID == nil){
            return
        }
        
        if(didSelectThingID! == ""){
            return
        }
        
        var Pubtopic:String = "$aws/things/"
        Pubtopic += didSelectThingID!
        Pubtopic += "/shadow/update"
        
        self.PublishTopic(message: message, topic: Pubtopic)
    }
    
    func AWS_LED_Intensity_Changed(value:Int){
        if(self.MQTT_State != "connected"){
            return
        }
        
        print("AWS_LED_Intensity_Changed")
        var message : String = ""
        var subitem = NSMutableDictionary()
        var mydic = NSMutableDictionary()
        var maindic = NSMutableDictionary()
        
        subitem.setObject(NSNumber(value:value), forKey: "LED_INTENSITY" as NSCopying)
        mydic.setObject(subitem, forKey: "desired" as NSCopying)
        maindic.setObject(mydic, forKey: "state" as NSCopying)
        
        let jsondata = try? JSONSerialization.data(withJSONObject: maindic, options:.prettyPrinted)
        
        if(jsondata != nil){
            message = String(data: jsondata!, encoding: .utf8)!
            AWS_IOT_LED_Control_Topic(message: message)
        }
    }
    
    func AWS_IOT_Control(color:String){
        
        if(self.MQTT_State != "connected"){
            return
        }
        
        var message : String = ""
        var subitem = NSMutableDictionary()
        var mydic = NSMutableDictionary()
        var maindic = NSMutableDictionary()
        
        if(color == "Red"){
            subitem.setObject(NSNumber(value:1), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "Blue"){
            subitem.setObject(NSNumber(value:0), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "Green"){
            subitem.setObject(NSNumber(value:0), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "Yellow"){
            subitem.setObject(NSNumber(value:1), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "Cyan"){
            subitem.setObject(NSNumber(value:0), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "Magenta"){
            subitem.setObject(NSNumber(value:1), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:0), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "White"){
            subitem.setObject(NSNumber(value:1), forKey: "LED_R" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_G" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "LED_B" as NSCopying)
            subitem.setObject(NSNumber(value:1), forKey: "Light" as NSCopying)
        }
        else if(color == "LED OFF"){
            print("LED OFF!!")
            subitem.setObject(NSNumber(value:0), forKey: "Light" as NSCopying)
        }
        else{
            return
        }
        
        mydic.setObject(subitem, forKey: "desired" as NSCopying)
        maindic.setObject(mydic, forKey: "state" as NSCopying)
        
        let jsondata = try? JSONSerialization.data(withJSONObject: maindic, options:.prettyPrinted)
        
        if(jsondata != nil){
            message = String(data: jsondata!, encoding: .utf8)!
            AWS_IOT_LED_Control_Topic(message: message)
        }
        
        if(tmp_state == "offline"){
            print("Update LED state to IOT Cloud")
            
            if(color == "LED OFF"){
                self.ControlDelegate?.UpdateLED(Led: "OFF")
                LED_Color = "OFF"
            }
            else{
                self.ControlDelegate?.UpdateLED(Led: color)
                LED_Color = color
            }
        }
    }

    // MARK: - DynamoDB
    func AWS_GetDeviceList(action:Bool){
        print("AWS_GetDeviceList")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        IOT_DeviceList.removeAllObjects()
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let scanExpression = AWSDynamoDBScanExpression()
        //scanExpression.limit = 10
        scanExpression.filterExpression = "cognitoUUID = :val"
        scanExpression.expressionAttributeValues = [":val": CognitoUUID]
        
        let task = dynamoDBObjectMapper.scan(IoTDevice.self, expression: scanExpression)
        
        task.continueWith { (task: AWSTask<AWSDynamoDBPaginatedOutput>) -> Any? in
            if let error = task.error as? NSError {
                print("[Test_retrieveData1 : cognitoUUID]Error 1")
                return nil
            }
            else if let paginatedOutput = task.result {
                print("Complete... \(paginatedOutput.items.count)")
                self.IOT_DeviceList.removeAllObjects()
                print("DeviceList : removeAllObjects")
                
                for device in paginatedOutput.items{
                    //print("IotDevice = \(device)")
                    let dev = device as? IoTDevice
                    self.IOT_DeviceList.add(IoTDevice(thingid: (dev?.thingID! ?? nil)!, cognitouuid: (dev?.cognitoUUID! ?? nil)!, devicename: (dev?.deviceName! ?? nil)!))
                }
            }
            
            print("Complete,action = \(action)")
    
            if(action){
                self.AWS_IOT_Device_Shadow(action: true)
            }
            
            DispatchQueue.main.async {
                self.Delegate?.QueryDeviceComplete(devielist: self.IOT_DeviceList)
            }
            
            return nil
        }
    }
    
    func AWS_UpdateItem(thingID:String , cognitoUUID:String, Name:String) {
        print("Test_UpdateItem")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let iot : IoTDevice = IoTDevice()
        iot.thingID = thingID
        iot.cognitoUUID = cognitoUUID
        iot.deviceName = Name
        
        dynamoDBObjectMapper.save(iot, completionHandler: {(error:Error?) -> Void in
            if let error = error{
                print("DynamoDB save error,\(error)")
                self.ProvisionDelegate?.ProvisionComplete(status: false)
                self.ProvisionState = nil
                return
            }
            print("deviceName was updated.")
            self.ProvisionState = "ProvisionComplete"
            DispatchQueue.main.async {
                self.ProvisionDelegate?.ProvisionComplete(status: true)
            }
        })
    }
    
    func Test_UpdateDeviceName(thingID:String , cognitoUUID:String, Name:String){
        print("Test_UpdateDeviceName")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let scanExpression = AWSDynamoDBScanExpression()
        
        scanExpression.filterExpression = "cognitoUUID = :val"
        scanExpression.expressionAttributeValues = [":val": cognitoUUID]
        
        let task = dynamoDBObjectMapper.scan(IoTDevice.self, expression: scanExpression)
        
        task.continueWith { (task: AWSTask<AWSDynamoDBPaginatedOutput>) -> Any? in
            if let error = task.error as? NSError {
                print("[Test_retrieveData1 : cognitoUUID]Error 1")
                return nil
            }
            else if let paginatedOutput = task.result {
                print("Complete... \(paginatedOutput.items.count)")
                for device in paginatedOutput.items{
                    print("IotDevice = \(device)")
                    
                    if let dev = device as? IoTDevice{
                        if(dev.thingID == thingID){
                            print("Found!,\(dev.deviceName)")
                            break
                        }
                    }
                }
            }
            return nil
        }
    }
    
    func Test_retrieveData1(cognitoUUID: String){
        print("Test_retrieveData1 : cognitoUUID = \(cognitoUUID)")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let scanExpression = AWSDynamoDBScanExpression()
        //scanExpression.limit = 10
        scanExpression.filterExpression = "cognitoUUID = :val"
        scanExpression.expressionAttributeValues = [":val": cognitoUUID]
        
        let task = dynamoDBObjectMapper.scan(IoTDevice.self, expression: scanExpression)
        
        task.continueWith { (task: AWSTask<AWSDynamoDBPaginatedOutput>) -> Any? in
            if let error = task.error as? NSError {
                print("[Test_retrieveData1 : cognitoUUID]Error 1")
                return nil
            }
            else if let paginatedOutput = task.result {
                print("Complete... \(paginatedOutput.items.count)")
                for device in paginatedOutput.items{
                    print("IotDevice = \(device)")
                }
            }
            print("scan fail!")
            return nil
        }
    }
    
    func Test_retrieveData(thingID: String){
        print("Test_retrieveData : thingID")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let task = dynamoDBObjectMapper.load(IoTDevice.self, hashKey: thingID, rangeKey:nil)
        
        task.continueWith { (task: AWSTask<AnyObject>) -> Any? in
            if let error = task.error as? NSError {
                print("[Test_retrieveData : thingID]Error 1")
                return nil
            }
            
            if let result = task.result as? IoTDevice {
                print("Complete..")
                print("thingID = \(result.thingID!)")
                print("cognitoUUID = \(result.cognitoUUID!)")
                print("deviceName = \(result.deviceName!)")
                
            } else {
                
            }
            
            return nil
        }
    }
    
    func Test_AWSDynameDB(){
        
        print("Test_AWSDynameDB")
        
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType: .USEast1, identityPoolId:PoolId)
        
        print("Getting Credentials,\(credentialsProvider.identityId)")
        
        let configuration = AWSServiceConfiguration(region: .USEast1, credentialsProvider: credentialsProvider)
        AWSServiceManager.default().defaultServiceConfiguration = configuration
        
        let dynamoDBObjectMapper = AWSDynamoDBObjectMapper.default()
        
        let scanExpression = AWSDynamoDBScanExpression()
        scanExpression.limit = 20
        
        let task = dynamoDBObjectMapper.scan(IoTDevice.self, expression: scanExpression)
        
        task.continueWith { (task:AWSTask<AWSDynamoDBPaginatedOutput>) -> Any? in
            
            if let error = task.error as? NSError {
                print("[Test_AWSDynameDB]Error 1")
                return nil
            }
            
            guard let paginatedOutput = task.result else {
                let error = NSError(domain: "com.asmtechnology.awschat",
                                    code: 200,
                                    userInfo: ["__type":"Unknown Error", "message":"DynamoDB error."])
                print("[Test_AWSDynameDB]Error 2")
                return nil
            }
            
            
            if paginatedOutput.items.count == 0 {
                print("[Test_AWSDynameDB]Error 3")
                return nil
            }
            
            for index in 0...(paginatedOutput.items.count - 1) {
                
                let device = paginatedOutput.items[index] as? IoTDevice
                
                if(device != nil){
                    print("index = \(index) , thingID = \(device?.thingID!) , cognitoUUID = \(device?.cognitoUUID!)")
                }
            }
            
            return nil
        }
    }
}
