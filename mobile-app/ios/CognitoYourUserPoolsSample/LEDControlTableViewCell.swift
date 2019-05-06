//
//  LEDControlTableViewCell.swift
//  WiFiOTDemo
//
//  Created by TestPC on 2019/3/27.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit

class LEDControlTableViewCell: UITableViewCell {

    @IBOutlet weak var LED_Slider: UISlider!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
