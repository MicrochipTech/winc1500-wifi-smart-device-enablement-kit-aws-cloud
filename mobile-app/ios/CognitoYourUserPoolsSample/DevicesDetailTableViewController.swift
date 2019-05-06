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
import UIKit

class DevicesDetailTableViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, DeviceControlDelegate {
    
    var AS:AmazonService?
    var mac_address:String!
    var thing_id:String!
    var LED_str:String!
    var Temperature_str:String!
    var Humidity_str:String!
    var UV_str:String!
    var Pressure_str:String!
    var ColorList: [String] = ["Red", "Green", "Blue", "Yellow", "White", "Cyan", "Magenta", "LED OFF"]
    var Intensity_Value: Int = 50
    var Count: Int = 0
    
    @IBOutlet weak var myTable: UITableView!
    
    @IBOutlet weak var Button1: UISwitch!
    
    @IBOutlet weak var Button2: UISwitch!
    
    @IBOutlet weak var Button3: UISwitch!
    
    @IBOutlet weak var MAC_label: UILabel!
    
    @IBOutlet weak var Status_Label: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view.
        
        print("[DevicesDetailTableViewController] viewDidLoad")
        
        mac_address = "f8f005eeaa89"
        LED_str = "Blue"
        Temperature_str = "26.6"
        Humidity_str = "45"
        UV_str = "70"
        Pressure_str = "70"
        
        myTable.delegate = self
        myTable.dataSource = self
        
        if(AS == nil){
            AS = AmazonService.sharedInstace()
            AS?.ControlDelegate = self
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        print("[DevicesDetailTableViewController] viewWillAppear")
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        print("[DevicesDetailTableViewController] viewWillDisappear")
        
        if(self.isMovingFromParentViewController) {
            print("Detecting 'BACK' button event!!")
            AS?.AWS_IOT_UnSubscribe()
        }
    }
    //@objc func Refresh_DeviceList(sender:AnyObject) {
    
    @objc func LED_Intensity_Value_Changed(sender:AnyObject){
        //print("LED_Intensity_Value_Changed")
        
        let slider = sender as! UISlider
        
        print("LED_Intensity_Value_Changed = \(slider.value)")
        
        Count += 1
        
        Intensity_Value = Int(slider.value)
        
        if(Count >= 6 ){
            Count = 0
            AS?.AWS_LED_Intensity_Changed(value: Intensity_Value)
        }
    }
    
    //MARK: - DeviceControlDelegate
    
    func Refresh(LED:String, Btn1:NSNumber, Btn2:NSNumber, Btn3:NSNumber, newvalue1:String?, newvalue2:String?, newvalue3:String?, newvalue4:String?)
    {
        LED_str = LED
        
        if(Btn1 == 1){
            Button1.setOn(true, animated: true)
        }
        else{
            Button1.setOn(false, animated: true)
        }
        
        if(Btn2 == 1){
            Button2.setOn(true, animated: true)
        }
        else{
            Button2.setOn(false, animated: true)
        }
        
        if(Btn3 == 1){
            Button3.setOn(true, animated: true)
        }
        else{
            Button3.setOn(false, animated: true)
        }
        
        if(newvalue1 != nil){
            Temperature_str = newvalue1
        }
        
        if(newvalue2 != nil){
            Humidity_str = newvalue2
        }
        
        if(newvalue3 != nil){
            UV_str = newvalue3
        }
        
        if(newvalue4 != nil){
            Pressure_str = newvalue4
        }
        
        myTable.reloadData()
    }
    
    func UpdateAllData(address: String, thing_id:String) {
        print("UpdateAllData")
        
        self.mac_address = address
        //MAC Address: f8f005eeaa89
        
        self.thing_id = thing_id
        
        self.MAC_label.text = "MAC Address: " + self.mac_address
        
        var mac:String?
        var state:String?
        var Btn1:NSNumber
        var Btn2:NSNumber
        var Btn3:NSNumber
        var LED:String
        var Sensor1:String
        var Sensor2:String
        var Sensor3:String
        var Sensor4:String
        var intensity:NSNumber
        
        (mac, state, Btn1, Btn2, Btn3, LED, Sensor1, Sensor2, Sensor3, Sensor4, intensity) = (AS?.AWS_Get_AllData(thing_id: self.thing_id))!
        
        if((mac != nil) && (state != nil)){
            self.Status_Label.text = "Status: " + state!
            
            if(Btn1 == 1){
                Button1.setOn(true, animated: true)
            }
            else{
                Button1.setOn(false, animated: true)
            }
            
            if(Btn2 == 1){
                Button2.setOn(true, animated: true)
            }
            else{
                Button2.setOn(false, animated: true)
            }
            
            if(Btn3 == 1){
                Button3.setOn(true, animated: true)
            }
            else{
                Button3.setOn(false, animated: true)
            }
            
            self.LED_str = LED
            self.Temperature_str = Sensor1
            self.Humidity_str = Sensor2
            self.UV_str = Sensor3
            self.Pressure_str = Sensor4
            self.Intensity_Value = intensity.intValue
            
            myTable.reloadData()
            
            print("Update sensor data")
        }
    }
    
    func UpdateLED(Led:String) {
        LED_str = Led
        
        myTable.reloadData()
    }
    
    func UpdateButtonState(state:NSNumber, btn:NSNumber) {
        if(btn == 1){
            if(state == 1){
                Button1.setOn(true, animated: true)
            }
            else{
                Button1.setOn(false, animated: true)
            }
        }
        
        if(btn == 2){
            if(state == 1){
                Button2.setOn(true, animated: true)
            }
            else{
                Button2.setOn(false, animated: true)
            }
        }
        
        if(btn == 3){
            if(state == 1){
                Button3.setOn(true, animated: true)
            }
            else{
                Button3.setOn(false, animated: true)
            }
        }
        
        myTable.reloadData()
    }
    
    func UpdateState(state:String) {
        //Status: Online
        
        self.Status_Label.text = "Status: " + state
    }
    
    func UpdateSensorData(newvalue1:String?, newvalue2:String?, newvalue3:String?, newvalue4:String?){
        if(newvalue1 != nil){
            Temperature_str = newvalue1
        }
        
        if(newvalue2 != nil){
            Humidity_str = newvalue2
        }
        
        if(newvalue3 != nil){
            UV_str = newvalue3
        }
        
        if(newvalue4 != nil){
            Pressure_str = newvalue4
        }
        
        myTable.reloadData()
    }
    
    /*
     // MARK: - Navigation
     
     // In a storyboard-based application, you will often want to do a little preparation before navigation
     override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
     // Get the new view controller using segue.destination.
     // Pass the selected object to the new view controller.
     }
     */
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 5
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "LED control") as! LEDControlTableViewCell
        if(LED_str != "OFF"){
            print("LED intensity control = enable")
            cell.LED_Slider.isEnabled = true
            cell.LED_Slider.value = Float(Intensity_Value)
            print("LED intensity = \(cell.LED_Slider.value)")
            cell.LED_Slider.addTarget(self, action: #selector(LED_Intensity_Value_Changed(sender:)), for: .valueChanged)
        }
        else{
            print("LED intensity control = disable")
            cell.LED_Slider.isEnabled = false
        }
        return cell
    }
    
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "Sensor_data", for: indexPath) as! DeviceControlTableViewCell
        
        if indexPath.row < 5 {
            switch indexPath.row {
            case 0:
                cell.myImage.image = UIImage.init(named: "light_icon.png")
                cell.myTitle.text = "LED Control"
                cell.myDetail.text = LED_str
                
                break
            case 1:
                cell.myImage.image = UIImage.init(named: "temperature_icon.png")
                cell.myTitle.text = "Temperature"
                cell.myDetail.text = Temperature_str + " ^C"
                break
            case 2:
                cell.myImage.image = UIImage.init(named: "humidity_icon.png")
                cell.myTitle.text = "Humidity"
                cell.myDetail.text = Humidity_str + " %"
                break
            case 3:
                cell.myImage.image = UIImage.init(named: "uv_icon.png")
                cell.myTitle.text = "UV"
                cell.myDetail.text = UV_str + " lx"
                break
            case 4:
                cell.myImage.image = UIImage.init(named: "pressure_icon.png")
                cell.myTitle.text = "Pressure"
                cell.myDetail.text = Pressure_str + " mbar"
                break
            default:
                break
            }
        }
        
        return cell
        
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let cell = tableView.cellForRow(at: indexPath) as! DeviceControlTableViewCell
        
        //print("didSelectRowAt = \(indexPath.row)")
        //print("Device Name = \(cell?.textLabel!.text)")
        //print("thingID = \(cell?.detailTextLabel!.text!)")
        
        if(indexPath.row == 0){
            let alertController = UIAlertController(
                title: "Change LED color",
                message: "",
                preferredStyle: .alert)
            
            let cancelAction = UIAlertAction(
                title: "Cancel",
                style: .cancel,
                handler: nil)
            
            alertController.addAction(cancelAction)
            
            for index in 0..<ColorList.count {
                let action = UIAlertAction(title: ColorList[index] , style: .default, handler:{
                    (action: UIAlertAction!) -> Void in
                    print("Change color = \(self.ColorList[index])")
                    self.AS?.AWS_IOT_Control(color: self.ColorList[index])
                }
                )
                
                alertController.addAction(action)
            }
            
            self.present(alertController, animated: true, completion: nil)
        }
    }
    
}
