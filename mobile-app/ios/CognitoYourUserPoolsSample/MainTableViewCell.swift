//
//  MainTableViewCell.swift
//  WiFiOTDemo
//
//  Created by TestPC on 2019/3/3.
//  Copyright © 2019 Dubal, Rohan. All rights reserved.
//

import UIKit

class MainTableViewCell: UITableViewCell {

    @IBOutlet weak var myImage: UIImageView!
    @IBOutlet weak var myTitle: UILabel!
    @IBOutlet weak var myDetail: UILabel!
    @IBOutlet weak var myUUID: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
