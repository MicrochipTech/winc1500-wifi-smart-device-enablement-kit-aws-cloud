//
//  ProvisionViewController.swift
//  CognitoYourUserPoolsSample
//
//  Created by TestPC on 2019/2/21.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit
import SwiftSocket

class ProvisionViewController: UIViewController ,UITextFieldDelegate, ProvisionDelegate{
    
    var AS: AmazonService?
    
    var CognitoUUID: String?
    var Provision_thingID: String?
    var Provision_DevName: String?
    var Provision_SSID: String?
    var Provision_PWD: String?
    
    var IOT_Connection: String!
    
    let host = "192.168.1.1"
    let port = 8899
    var client: TCPClient?
    var Connection : Bool!
    var ProvisionStage : UInt8!
    var ProvisionSequence : UInt8!
    var ProvisionComplete : Bool!

    @IBOutlet weak var provision_ssid: UITextField!
    
    @IBOutlet weak var provision_pwd: UITextField!
    
    @IBOutlet weak var provision_device_name: UITextField!
    
    @IBOutlet weak var TestButton: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        
        print("[ProvisionViewController]viewDidLoad")
        
        if(AS == nil){
            AS = AmazonService.sharedInstace()
            AS?.ProvisionDelegate = self
            AS?.Delegate = nil
        }
        
        provision_ssid.delegate = self
        provision_pwd.delegate = self
        provision_device_name.delegate = self
        
        if(self.client == nil){
            print("Initial TCP client")
            self.client = TCPClient(address: self.host, port: Int32(self.port))
            self.Connection = false
            self.ProvisionStage = 0x00
            self.ProvisionSequence = 0x00
        }
        
        Provision_DevName = ""
        Provision_SSID = ""
        Provision_PWD = ""
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        print("[ProvisionViewController] viewWillAppear")
        
        TestButton.isHidden = true
        
        if let uuid = AS?.CognitoUUID{
            print("Cognito_uuid = \(uuid)")
        }
        
        self.title = "Provisioning"
        
        if(self.client != nil){
            self.Connection = false
            self.ProvisionStage = 0x00
                
            switch self.client!.connect(timeout: 10) {
                case .success:
                    self.Connection = true
                    ProvisionStage = 0x01
                    print("Connect success")
                case .failure(_):
                    print("Connect fail")
                    let alertController = UIAlertController(
                        title: "Network error",
                        message: "Connection fail(Server ip = 192.168.1.1 ,port = 8899)",
                        preferredStyle: .alert)
                    
                    let okAction = UIAlertAction(title: "OK", style: .default) { (UIAlertAction) in
                        print("Back...")
                        
                        self.navigationController?.popViewController(animated: true)
                    }
                    
                    alertController.addAction(okAction)
                    
                    self.present(alertController, animated: true, completion: nil)
            }
        }
    }
    
    @IBAction func Provisioning(_ sender: Any) {
        
        if let uuid = AS?.CognitoUUID{
            print("uuid = \(uuid)")
            CognitoUUID = uuid
        }
        else{
            print("CognitoUUID = nil")
            self.navigationController?.popViewController(animated: true)
            return
        }
        
        print("ssid = \(provision_ssid.text)")
        print("pwd = \(provision_pwd.text)")
        print("name = \(provision_device_name.text)")
        
        Provision_DevName = provision_device_name.text
        Provision_SSID = provision_ssid.text
        Provision_PWD = provision_pwd.text
        
        if((Provision_SSID == nil) || (Provision_SSID == "")){
            return
        }
        
        if((Provision_PWD == nil) || (Provision_PWD == "")){
            return
        }
        
        if((Provision_DevName == nil) || (Provision_DevName == "")){
            return
        }
        
        AlertWaiting(str: "Provisioning")

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            self.Provisioning()
        }
    }
    
    func AlertWaiting(str:String) {
        var title:String = ""
        
        //print("str length = \(str.count)")
        
        if(str == "") {
            title = "Please wait...\n\n\n"
        }
        else {
            if(str.count > 15){
                title = str + "\n\n\n"
            }
            else{
                title = str + " ,Please wait...\n\n\n"
            }
        }
        
         let alertController = UIAlertController(title: nil, message: title, preferredStyle: .alert)
         
         let activityIndicator : UIActivityIndicatorView = UIActivityIndicatorView.init(activityIndicatorStyle: UIActivityIndicatorViewStyle.whiteLarge)
         
         activityIndicator.color = UIColor.black
         activityIndicator.translatesAutoresizingMaskIntoConstraints = false
         
         alertController.view.addSubview(activityIndicator)
         
         let centerHorizontally = NSLayoutConstraint(item: activityIndicator,
         attribute: .centerX,
         relatedBy: .equal,
         toItem: activityIndicator.superview,
         attribute: .centerX,
         multiplier: 1.0,
         constant: 0.0)
         
         let centerVertically = NSLayoutConstraint(item: activityIndicator,
         attribute: .centerY,
         relatedBy: .equal,
         toItem: activityIndicator.superview,
         attribute: .centerY,
         multiplier: 1.0,
         constant: 0.0)
         
         NSLayoutConstraint.activate([centerHorizontally, centerVertically])
         
         activityIndicator.startAnimating()
         
         self.present(alertController, animated: true, completion: nil)
    }
    
    func AlertMessage(title:String , Message:String) {
        let alertController = UIAlertController(
            title: title,
            message: Message,
            preferredStyle: .alert)
        
        let okAction = UIAlertAction(
            title: "OK",
            style: .default,
            handler: nil)
        
        alertController.addAction(okAction)
        
        self.present(alertController, animated: true, completion: nil)
    }
    
    func ProvisionComplete(status: Bool) {
        if(status){
            self.client?.close()
            
            let alertController = UIAlertController(
                title: "Provisioning",
                message: "Successful",
                preferredStyle: .alert)
            
            let okAction = UIAlertAction(title: "OK", style: .default){ (_) in
                self.navigationController?.popToRootViewController(animated: true)
            }
            
            alertController.addAction(okAction)
            
            self.present(alertController, animated: true, completion: nil)
        }
        else{
            AlertMessage(title: "Provisioning", Message: "Can't update the device name")
        }
    }
    
    func IOT_Connection_Status(status: String) {
        print("IOT_Connection_Status = \(status)")
        
        IOT_Connection = status
        
        if((IOT_Connection == "connected") && (ProvisionComplete == true)){
            
            if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                print("Dismiss the Alert view")
                self.dismiss(animated: true, completion: nil)
            }
            
            ProvisionComplete = false
            
            self.ProvisionStage = 0x00
            self.ProvisionSequence = 0x00
            
            self.provision_ssid.text = ""
            self.provision_pwd.text = ""
            self.provision_device_name.text = ""
            
            //Connect to original wifi-network
            print("Connect to original wifi-network")
            self.AS?.AWS_UpdateItem(thingID: self.Provision_thingID!, cognitoUUID: self.CognitoUUID!, Name: self.Provision_DevName!)
        }
    }
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
    
    // MARK: - Network Provisioning
    
    func Provisioning(){
        if(ProvisionStage == 0x00){
            ConnectToServer()
        }
        else if(ProvisionStage == 0x01){
            ProvisionComplete = false
            
            if(Send_DiscoveryRequest())
            {
                sleep(2)
                ProvisionStage = 0x02
                Provisioning()
            }
            else{
                print("Provision fail!\(ProvisionStage!)")
                
                sleep(3)
                
                if(Send_DiscoveryRequest())
                {
                    sleep(2)
                    ProvisionStage = 0x02
                    Provisioning()
                }
            }
        }
        else if(ProvisionStage == 0x02){
            var ssid : String!
            var pwd : String!
            
            ssid = Provision_SSID
            pwd = Provision_PWD
            
            if(Send_Provision_data(ssid: ssid, password: pwd, cognitoUUID: CognitoUUID!)){
                sleep(2)
                ProvisionStage = 0x03
                Provisioning()
            }
            else{
                print("Provision fail!\(ProvisionStage!)")
            }
        }
        else if(ProvisionStage == 0x03){
            if(send_condone_message()){
                print("Provision PASS")
                ProvisionComplete = true
            }
            else{
                print("Provision fail!\(ProvisionStage!)")
                
                sleep(3)
                print("Try again!")
                if(send_condone_message()){
                    print("Provision PASS")
                    ProvisionComplete = true
                }
            }
        }
    }
    
    func Provision_Complete(){
        
        self.ProvisionStage = 0x00
        self.ProvisionSequence = 0x00
        
        let alertController = UIAlertController(
            title: "Connect to original network",
            message: "",
            preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK", style: .default) { (UIAlertAction) in
            print("Update data...")
            
            self.provision_ssid.text = ""
            self.provision_pwd.text = ""
            self.provision_device_name.text = ""
            
            //Connect to original wifi-network
            print("Connect to original wifi-network")
            self.AS?.AWS_UpdateItem(thingID: self.Provision_thingID!, cognitoUUID: self.CognitoUUID!, Name: self.Provision_DevName!)
        }
        
        alertController.addAction(okAction)
        
        self.present(alertController, animated: true, completion: nil)
    }
    
    func ConnectToServer(){
        guard let client = client else { return }
        
        if(self.Connection == false){
            switch client.connect(timeout: 10) {
            case .success:
                print("Connect success")
                self.Connection = true
                sleep(3)
                ProvisionStage = 0x01
                Provisioning()
            case .failure(_):
                print("Connect fail")
            }
        }
    }
    
    func ParseData(data:[Byte]) -> Bool{
        //print("ParseData = \(data)")
        print("Parsing...")
        
        if(data[0] == 0x5a){//SOF
            if((data[1] == 0x38) && (data[4] == 0x81)){
                
                if(ProvisionStage == 0x01){
                    //Get device MAC & thingID
                    
                    print("Receive Sequence = \(data[3])")
                    print("Send Sequence = \(ProvisionSequence)")
                    
                    var MAC = [UInt8]()
                    var thingID = [UInt8]()
                    for i in 0..<6 {
                        MAC.append(data[9+i])
                    }
                    let MAC_data = Data(bytes: MAC)
                    for i in 0..<40 {
                        thingID.append(data[15+i])
                    }
                    let thingID_data = Data(bytes: thingID)
                    Provision_thingID = String(decoding: thingID_data, as: UTF8.self)
                    
                    print("Get device MAC = \(MAC_data as NSData)")
                    print("Get thingID = \(Provision_thingID)")
                    return true
                }
                else if(ProvisionStage == 0x02){
                    print("Error")
                    print("Receive Sequence = \(data[3])")
                    print("Send Sequence = \(ProvisionSequence)")
                    
                    return true
                    //return false
                }
            }
            else if((data[1] == 0x0d) && (data[4] == 0x81)){
                
                print("Receive Sequence = \(data[3])")
                print("Send Sequence = \(ProvisionSequence)")
                
                if((data[9] == 0x2b) && (data[10] == 0x6f) && (data[11] == 0x6b)){//"+ok"
                    print("+ok")
                    return true
                }
                else{
                    return false
                }
            }
        }
        
        return false
    }
    
    func Send_DiscoveryRequest() -> Bool{
        let str = "Atmel_WiFi_Discovery"
        let buf : [UInt8] = Array(str.utf8)
        
        var array = [UInt8]()
        array.append(0x5A)              //SOF
        array.append(0x1A)              // data length low byte
        array.append(0x00)              // data length High byte
        ProvisionSequence += UInt8(0x01)
        array.append(ProvisionSequence)
        array.append(0x01)              //Discovery command
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        for i in 0..<str.count {
            array.append(buf[i])
        }
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        
        let dd1 = Data(bytes: array)
        
        guard let client = client else { return false}
        
        if(self.Connection)
        {
            switch client.send(data: dd1){
            case .success:
                print("Send_DiscoveryRequest = \(dd1 as NSData)")
                
                guard let data = client.read(1024) else { return false}
                print("receive data = \(data)")
                
                if(data.count != 0){
                    print("data size = \(data.count)")
                    if(data.count < 13){
                        print("Data error = \(data)")
                        return false
                    }
                    else{
                        return ParseData(data: data)
                    }
                }
            case .failure(_):
                print("Send data fail")
                return false
            }
        }
        
        return false
    }
    
    func Send_Provision_data(ssid:String, password:String, cognitoUUID:String) -> Bool{
        let str_pwd = password
        let str_uuid = cognitoUUID
        let str_ssid = ssid
        let ssid_buf: [UInt8] = Array(str_ssid.utf8)
        let pwd_buf: [UInt8] = Array(str_pwd.utf8)
        let uuid_buf : [UInt8] = Array(str_uuid.utf8)
        
        let str_CONFIG = "CONFIG="
        let str_devName = "kitchen"
        
        let config : [UInt8] = Array(str_CONFIG.utf8)
        let devName : [UInt8] = Array(str_devName.utf8)
        
        let len = 9 + str_CONFIG.count + 1 + str_ssid.count + 1 + str_pwd.count + 2 + str_uuid.count + 1 + str_devName.count + 4
        
        var array = [UInt8]()
        array.append(0x5A)              //SOF
        array.append(UInt8(len-7))      // data length low byte
        array.append(0x00)              // data length High byte
        ProvisionSequence += UInt8(0x01)
        array.append(ProvisionSequence)
        array.append(0x01)              //Discovery command
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        for i in 0..<str_CONFIG.count {
            array.append(config[i])
        }
        array.append(UInt8(str_ssid.count))     //ssid length
        for i in 0..<str_ssid.count {
            array.append(ssid_buf[i])
        }
        array.append(UInt8(str_pwd.count))      //password length
        for i in 0..<str_pwd.count {
            array.append(pwd_buf[i])
        }
        array.append(0x02)                     //Security type
        array.append(UInt8(str_uuid.count))     //CognitoUUID length
        for i in 0..<str_uuid.count {
            array.append(uuid_buf[i])
        }
        array.append(UInt8(str_devName.count))  //devName length
        for i in 0..<str_devName.count {
            array.append(devName[i])
        }
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        
        let dd1 = Data(bytes: array)
        
        guard let client = client else { return false}
        
        if(self.Connection)
        {
            switch client.send(data: dd1){
            case .success:
                print("Send_Provision_data = \(dd1 as NSData)")
                sleep(2)
                
                guard let data = client.read(1024) else { return false}
                print("receive data = \(data)")
                
                if(data.count != 0){
                    print("data size = \(data.count)")
                    if(data.count < 13){
                        print("Data error = \(data)")
                        return false
                    }
                    else{
                        return ParseData(data: data)
                    }
                }
            case .failure(_):
                print("Send data fail")
                return false
            }
        }
        
        return false
    }
    
    func send_condone_message() -> Bool{
    
        let str_condone = "CONDONE"
        let condone : [UInt8] = Array(str_condone.utf8)
        
        var array = [UInt8]()
        array.append(0x5A)      //SOF
        array.append(0x0d)      // data length low byte
        array.append(0x00)      // data length High byte
        ProvisionSequence += UInt8(0x01)
        array.append(ProvisionSequence)
        array.append(0x01)      //Discovery command
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        for i in 0..<str_condone.count {
            array.append(condone[i])
        }
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        array.append(0x00)
        
        let dd1 = Data(bytes: array)
        
        guard let client = client else { return false}
        
        if(self.Connection)
        {
            switch client.send(data: dd1){
            case .success:
                print("send_condone_message = \(dd1 as NSData)")
                
                guard let data = client.read(1024) else { return false}
                print("receive data = \(data)")
                
                if(data.count != 0){
                    print("data size = \(data.count)")
                    if(data.count < 13){
                        print("Data error = \(data)")
                        return false
                    }
                    else{
                        return ParseData(data: data)
                    }
                }
            case .failure(_):
                print("Send data fail")
                return false
            }
        }
        
        return false
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        
        textField.resignFirstResponder()
        
        return true
    }
}
