//
//  DeviceControlTableViewCell.swift
//  WiFiOTDemo
//
//  Created by TestPC on 2019/2/28.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit

class DeviceControlTableViewCell: UITableViewCell {

    @IBOutlet weak var myImage: UIImageView!
    
    @IBOutlet weak var myTitle: UILabel!
    
    @IBOutlet weak var myDetail: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
