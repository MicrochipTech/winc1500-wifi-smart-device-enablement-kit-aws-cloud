//
//  NetworkSettingViewController.swift
//  WiFiOTDemo
//
//  Created by TestPC on 2019/4/8.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit

class NetworkSettingViewController: UIViewController {
    
    var AS: AmazonService?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        
        if(AS == nil){
            AS = AmazonService.sharedInstace()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        print("[NetworkSettingViewController] viewWillAppear")
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
