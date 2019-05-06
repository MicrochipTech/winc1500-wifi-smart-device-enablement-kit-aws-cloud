//
//  IoTDeviceModel.swift
//  CognitoYourUserPoolsSample
//
//  Created by TestPC on 2019/1/14.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import Foundation
import AWSDynamoDB

class IoTDevice : AWSDynamoDBObjectModel, AWSDynamoDBModeling {
    
    var thingID: String?
    var cognitoUUID: String?
    var deviceName: String?
    var MAC_Address: String?
    var state: String?
    
    override init() {
        super.init()
    }
    
    init(thingid:String , cognitouuid:String, devicename:String){
        super.init()
        thingID = thingid
        cognitoUUID = cognitouuid
        deviceName = devicename
        
    }
    
    override init(dictionary dictionaryValue: [AnyHashable : Any]!, error: ()) throws {
        super.init()
        
        thingID = dictionaryValue["thingID"] as? String
        cognitoUUID = dictionaryValue["cognitoUUID"] as? String
        deviceName = dictionaryValue["deviceName"] as? String
    }
    
    required init!(coder: NSCoder!) {
        fatalError("init(coder:) has not been implemented")
    }
    
    class func dynamoDBTableName() -> String {
        return "SensorBoardAcctTable"
    }
    
    class func hashKeyAttribute() -> String {
        return "thingID"
    }
}

