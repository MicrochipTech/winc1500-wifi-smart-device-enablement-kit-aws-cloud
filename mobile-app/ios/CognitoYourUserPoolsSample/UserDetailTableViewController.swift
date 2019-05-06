//
// Copyright 2014-2018 Amazon.com,
// Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Amazon Software License (the "License").
// You may not use this file except in compliance with the
// License. A copy of the License is located at
//
//     http://aws.amazon.com/asl/
//
// or in the "license" file accompanying this file. This file is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, express or implied. See the License
// for the specific language governing permissions and
// limitations under the License.
//

import Foundation
import AWSCognitoIdentityProvider

import SystemConfiguration.CaptiveNetwork

class UserDetailTableViewController : UITableViewController , AmazonServiceDelegate{
    @IBOutlet weak var TestButton: UIBarButtonItem!
    
    @IBOutlet weak var SignoutButton: UIBarButtonItem!
    
    var response: AWSCognitoIdentityUserGetDetailsResponse?
    var user: AWSCognitoIdentityUser?
    var pool: AWSCognitoIdentityUserPool?
    var AS:AmazonService?
    var QueryStatus: Bool?
    
    var IOT_DeviceList: NSMutableArray = NSMutableArray()
    
    var IOT_State : String?
    var Active_thing : String?
    
    var TableRefresh : Bool = true
    
    var controller:UIAlertController?

    override func viewDidLoad() {
        super.viewDidLoad()
        self.tableView.delegate = self
        
        print("[UserDetailTableViewController]viewDidLoad")
        
        #if !Debug
        print("Debug mode : Enable")
        #endif
        
        let versionButton = UIBarButtonItem(title: "Version", style: UIBarButtonItemStyle.plain, target: self, action: #selector(UserDetailTableViewController.VersionbuttonTapped(_:)))
        
        self.navigationItem.rightBarButtonItem = versionButton
        
        if #available(iOS 10.0, *) {
            print("Init refreshControl")
            self.tableView.refreshControl?.attributedTitle = NSAttributedString(string: "Device Discovery")
            self.tableView.refreshControl?.addTarget(self, action: #selector(Refresh_DeviceList(sender:)), for: UIControl.Event.valueChanged)
        } else {
            // Fallback on earlier versions
        }
        
        if(AS == nil){
            AS = AmazonService.sharedInstace()
        }
        
        if(AS != nil){
            AS?.Delegate = self
        }
        
        //Cognito
        self.pool = AWSCognitoIdentityUserPool(forKey: AWSCognitoUserPoolsSignInProviderKey)
        if (self.user == nil) {
            print("Set user")
            self.user = self.pool?.currentUser()
        }
        
        self.title = "Connecting"
        
        GetTime()
        refresh()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.navigationController?.setToolbarHidden(true, animated: true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.setToolbarHidden(false, animated: true)
    
        print("[UserDetailTableViewController] viewWillAppear")
        
        if(AS != nil){
            AS?.Delegate = self
        }
        
        TableRefresh = true
        
        if let device_array = UserDefaults.standard.object(forKey: "Sensorboard_Info"){
            let device_info = device_array as! NSArray
            print("device_info = \(device_info)")
            print("DeviceInfoTable.count = \(device_info.count)")
        }
        
        if(IOT_State == nil){
            print("IOT State = nil")
            if(IOT_DeviceList.count != 0){
            }
            else{
                print("QueryStatus = \(QueryStatus)")
                
                if((self.title == "Connecting") && (TableRefresh == true)){
                    if(QueryStatus == nil){
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                            if((self.title == "Connecting") && (self.QueryStatus == nil) && (self.TableRefresh == true)){
                                if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                                    print("delay 1 , Return")
                                    return
                                }
                                
                                print("delay 1 , Reconnect")
                                self.TableRefresh = false
                                
                                let wifi_ssid = self.fetchSSIDInfo()
                                if(wifi_ssid != nil){
                                    print("Connected WIFI SSID = \(wifi_ssid)")
                                }
                                else{
                                    print("Connected WIFI SSID = nil")
                                    
                                    self.user?.signOut()
                                    self.refresh()
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if((AS?.ProvisionState != nil) && (AS?.ProvisionState == "ProvisionComplete")){
            AS?.ProvisionState = nil
            
            if(IOT_State == nil){
                IOT_State = "connected"
            }
            
            print("Provision complete, refresh deviceList")
            
            if(IOT_DeviceList.count != 0){
                IOT_DeviceList.removeAllObjects()
                
                self.tableView.reloadData()
            }
            
            AS?.AWS_GetDeviceList(action: true)
            
        }
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        print("prepare for segue")
        
        //let controller = segue.destination as! DevicesDetailTableViewController
        //controller.ScanOption = tag as! Int
    }
    
    func GetTime(){
        // get the current date and time
        let currentDateTime = Date()
        
        // initialize the date formatter and set the style
        let formatter = DateFormatter()
        formatter.timeStyle = .medium
        formatter.dateStyle = .none
        
        // get the date time String from the date object
        let CurrentTime = formatter.string(from: currentDateTime)
        print("CurrentTime = \(CurrentTime)")
        
        //return CurrentTime
    }
    
    @objc func Refresh_DeviceList(sender:AnyObject) {
        // Code to refresh table view
        
        if(IOT_State != nil){
            print("[Refresh_DeviceList] IOT_State = \(IOT_State!)")
        }
        else{
            print("[Refresh_DeviceList] IOT_State = nil")
            
            let wifi_ssid = self.fetchSSIDInfo()
            if(wifi_ssid == nil){
                AS?.AWS_PrepareSignOut()
                print("signout, user = \(self.user?.username)")
                self.user?.signOut()
                self.title = "Connecting"
                IOT_DeviceList.removeAllObjects()
                self.tableView.reloadData()
                TableRefresh = true
                IOT_State = nil
                QueryStatus = nil
                self.refresh()
                return
            }
            else{
                print("wifi ssid = \(wifi_ssid)")
            }
            
            if #available(iOS 10.0, *) {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    if((self.tableView.refreshControl?.isRefreshing)!){
                        self.tableView.refreshControl?.endRefreshing()
                        self.AWS_AlertMessage(title: "AWS Error", Message: "Can't connect to AWS IOT Cloud,Reconnect")
                    }
                }
            }
            else{
                self.AWS_AlertMessage(title: "AWS Error", Message: "Can't connect to AWS IOT Cloud,Reconnect")
            }
            if(TableRefresh){
                TableRefresh = false
                AS?.AWS_IOT_Get_Connection_Status()
            }
            return
        }
        
        if(self.title == "Device List"){
            
            print("Refresh DeviceList")
            if(TableRefresh){
                TableRefresh = false
                
                if(IOT_DeviceList.count != 0){
                    IOT_DeviceList.removeAllObjects()
                    print("reloadData22")
                    self.tableView.reloadData()
                }
            
                AS?.AWS_GetDeviceList(action: true)
            }
        }
        else if(self.title == "Connecting"){
            print("Connecting!")
            if #available(iOS 10.0, *) {
                if((self.tableView.refreshControl?.isRefreshing)!){
                    self.tableView.refreshControl?.endRefreshing()
                }
            } else {
                // Fallback on earlier versions
            }
            
            if(TableRefresh){
                TableRefresh = false
            
                if(IOT_DeviceList.count == 0){
                    self.AWS_AlertWaiting(str: "Connecting...")
                }
            }
        }
        else{
            if #available(iOS 10.0, *) {
                if((self.tableView.refreshControl?.isRefreshing)!){
                    self.tableView.refreshControl?.endRefreshing()
                }
            } else {
                // Fallback on earlier versions
            }
        }
    }
    
    func AWS_AlertMessage(title:String , Message:String) {
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
    
    func AWS_AlertWaiting(str:String) {
        var title:String = ""
        
        //print("string length = \(str.count)")
        
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
        
        self.controller = UIAlertController(title: nil, message: title, preferredStyle: .alert)
        
        let activityIndicator : UIActivityIndicatorView = UIActivityIndicatorView.init(activityIndicatorStyle: UIActivityIndicatorViewStyle.whiteLarge)
        
        activityIndicator.color = UIColor.black
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        
        self.controller!.view.addSubview(activityIndicator)
        
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
        
        self.present(self.controller!, animated: true, completion: nil)
    }
    
    func VersionbuttonTapped(_ sender:UIBarButtonItem!) {
        var ver_str1:String = ""
        var ver_str2:String = ""
        
        if let app_version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
            print("app_version = \(app_version)")
            ver_str1 = app_version
        }
        
        if let build_number = Bundle.main.infoDictionary?["CFBundleVersion"] as? String {
            print("build_number = \(build_number)")
            ver_str2 = build_number
        }
        
        AWS_AlertMessage(title:ver_str1 + "." + ver_str2 , Message: "")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.dismiss(animated: true, completion: nil)
        }
    }
    
    func refresh() {
        print("refresh")
        self.user?.getDetails().continueOnSuccessWith { (task) -> AnyObject? in
            DispatchQueue.main.async(execute: {
                print("[UserDetailTableViewController] SigninSuccess")
                self.response = task.result
                self.title = self.user?.username
                
                let userAttribute = self.response?.userAttributes![0]
                
                if let uuid = userAttribute?.value{
                    print("CognitoUUID = \(uuid)")
                    self.QueryStatus = true
                    self.AS?.Set_Cognito_UUID(uuid: uuid, user: (self.user?.username!)!)
                }
            })
            
            return nil
        }
    }
    
    func GetCredentials(){
        print("GetCredentials")
        
        //self.IdentityID = nil
        var user_name:String!
        var user_password:String!
        
        user_name = self.user?.username
        
        user_password = AS?.AWS_GetUserNamePassword(user: user_name)
        
        print("user = \(user_name), password = \(user_password)")
        
        let task = self.user?.getSession(user_name, password: user_password, validationData: nil)
        
        task!.continueWith(block: { (task: AWSTask<AWSCognitoIdentityUserSession>) -> Any? in
            
            if let error = task.error {
                print("[Test_GetIdentityID]Error 1")
                return nil
            }
            
            let userSession = task.result!
            let idToken = userSession.idToken!
            
            let indentityPoolController = CognitoIdentityPoolController.sharedInstance
            indentityPoolController.getFederatedIdentityForAmazon(idToken: idToken.tokenString,
                                                                  //username: signin_user,
                                                                  username: user_name,
                emailAddress: user_email,
                userPoolRegion: CognitoRegion,
                userPoolID: CognitoIdentityUserPoolId,
                completion: { (error: Error?) in
                    
                    if let error = error {
                        print("[Test_GetIdentityID]Error 2")
                        return
                    }
                    
                    self.GetTime()
                    print("identityId = \(indentityPoolController.currentIdentityID!)")
                    
                    DispatchQueue.main.async {
                        if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                            print("Dismiss the Alert view")
                            self.dismiss(animated: true, completion: nil)
                        }
                    }
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        self.AS?.ConnectToIdentityPool(identity: indentityPoolController.currentIdentityID!)
                    }
                    
                    return
            })
            
            return nil
            
        })
    }
    
    func fetchSSIDInfo() ->  String? {
        if let interfaces = CNCopySupportedInterfaces() {
            for i in 0..<CFArrayGetCount(interfaces){
                let interfaceName: UnsafeRawPointer = CFArrayGetValueAtIndex(interfaces, i)
                let rec = unsafeBitCast(interfaceName, to: AnyObject.self)
                let unsafeInterfaceData = CNCopyCurrentNetworkInfo("\(rec)" as CFString)
                
                if let unsafeInterfaceData = unsafeInterfaceData as? Dictionary<AnyHashable, Any> {
                    return unsafeInterfaceData["SSID"] as? String
                }
            }
        }
        return nil
    }
    
    // MARK: - AmazonService delegate
    
    func CommandComplete() {
        print("[AmazonService] Command Complete")
    }
    
    func SigninSuccess(result:Bool){
        print("[AmazonService] SigninSuccess = \(result)")
        GetTime()
        
        QueryStatus = result
        
        TableRefresh = true
        
        if(self.QueryStatus != nil){
            self.title = "Connected"
            self.AWS_AlertWaiting(str: "Connecting...")
            GetCredentials()
        }
    }
    
    func AWServiceReady(){
        if(self.navigationController?.visibleViewController?.isKind(of: UITableViewController.self))! {
            print("Check ViewController")
        }
        else{
            return
        }
        
        print("[AmazonService]Ready, Get IOT devices")
        GetTime()
        
        //DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            if((self.title == "Device List") || (self.title == "Connected")){
                if #available(iOS 10.0, *) {
                    if((self.tableView.refreshControl?.isRefreshing)!){
                        self.tableView.refreshControl?.endRefreshing()
                    }
                } else {
                    // Fallback on earlier versions
                }
                
                self.AWS_AlertWaiting(str: "Discovering devices")
                self.AS?.AWS_GetDeviceList(action: false)
            }
        }
    }
    
    func QueryDeviceComplete(devielist : NSArray){
        print("[AmazonService] Get DeviceList")
        GetTime()
        
        self.IOT_DeviceList = NSMutableArray(array: devielist)
        
        self.title = "Device List"
        
        TableRefresh = true
        
        if(self.IOT_DeviceList.count != 0){
            
            if(IOT_State == nil){
                self.AS?.AWS_IOT_Connect()
            }
            else{
                AS?.AWS_IOT_Device_Shadow(action: true)
                
                let result = AS?.AWS_IOT_Check_MacAddress()
                if(result == false){
                    AS?.AWS_IOT_Device_Shadow(action: false)
                }
                else{
                    if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                        self.dismiss(animated: true, completion: nil)
                    }
                    
                    GetTime()
                    self.tableView.reloadData()
                }
            }
            
            if #available(iOS 10.0, *) {
                if((self.tableView.refreshControl?.isRefreshing)!){
                    self.tableView.refreshControl?.endRefreshing()
                }
            } else {
                // Fallback on earlier versions
            }
        }
        else{
            if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                self.dismiss(animated: true, completion: nil)
            }
            
            print("No device!!")
            if #available(iOS 10.0, *) {
                if((self.tableView.refreshControl?.isRefreshing)!){
                    self.tableView.refreshControl?.endRefreshing()
                }
            } else {
                // Fallback on earlier versions
            }
        }
    }
    
    func UpdateIOTStatus(status : String){
        print("[AmazonService] UpdateIOTStatus = \(status)")
        
        if(TableRefresh == false){
            TableRefresh = true
        }
        
        if(status == "connected"){
            IOT_State = status
            
            if(IOT_DeviceList.count != 0){
                AS?.AWS_IOT_Device_Shadow(action: true)
                
                let result = AS?.AWS_IOT_Check_MacAddress()
                if(result == false){
                    AS?.AWS_IOT_Device_Shadow(action: false)
                }
                else{
                    if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                        self.dismiss(animated: true, completion: nil)
                    }
                    GetTime()
                    self.tableView.reloadData()
                }
            }
            else{
                print("[Connecting --> Connected] Refresh device")
                self.title = "Device List"
                
                if(IOT_DeviceList.count == 0){
                    self.AWS_AlertWaiting(str: "Discovering devices")
                    self.AS?.AWS_GetDeviceList(action: false)
                }
            }
        }
        else if(status == "ShadowGet"){
            AS?.ASW_IOT_Shadow_Update(thingID: Active_thing!)
        }
        else if(status == "UpdateDevList"){
            if(self.navigationController?.visibleViewController?.isKind(of: UIAlertController.self))! {
                print("Dismiss the Alert view5")
                self.dismiss(animated: true, completion: nil)
            }
        }
        else{
            if((self.title == "Connected") || (self.title == "Device List")){
                self.title = "Connecting"
            }
            
            IOT_State = nil
            
            if(IOT_DeviceList.count != 0){
                IOT_DeviceList.removeAllObjects()
                print("reloadData33")
                self.tableView.reloadData()
            }
        }
    }
    
    // MARK: - Table view data source
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return IOT_DeviceList.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "attribute", for: indexPath) as! MainTableViewCell
        
        let dev = IOT_DeviceList.object(at: indexPath.row) as! IoTDevice
        
        cell.myImage.image = UIImage.init(named: "rsz_winc1500_secure_wifi_board.png")
        
        print("Device:\(indexPath.row)")
        if let devnmae = dev.deviceName{
            cell.myTitle.text = devnmae
            print("\(devnmae)")
        }
        else{
            cell.textLabel!.text = "unknown"
        }
        
        if let thingid = dev.thingID{
            print("thingid = \(thingid)")
            cell.myUUID.text = thingid
        }
        
        if let mac = dev.MAC_Address{
            print("MAC = \(mac)")
            cell.myDetail.text = mac
        }
        else{
            cell.myDetail.text = ""
        }
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let cell = tableView.cellForRow(at: indexPath) as! MainTableViewCell
        
        if(cell.myDetail.text != nil){
            Active_thing = cell.myDetail.text
            
            print("MAC address = \(cell.myDetail.text)")
            
            if(IOT_State == "connected"){
                AS?.AWS_Select_Device(mac:cell.myDetail.text!, uuid:cell.myUUID.text!)
                AS?.AWS_IOT_Shadow_Init(thingID: cell.myUUID.text!, action: false)
            }
            else{
                print("Can't connect to AWS IOT cloud")
            }
        }
    }
    
    // MARK: - IBActions
    
    @IBAction func signOut(_ sender: AnyObject) {
        AS?.AWS_PrepareSignOut()
        print("signout, user = \(self.user?.username)")
        self.user?.signOut()
        self.title = "Connecting"
        IOT_DeviceList.removeAllObjects()
        self.tableView.reloadData()
        TableRefresh = true
        IOT_State = nil
        QueryStatus = nil
        
        self.refresh()
    }

}

