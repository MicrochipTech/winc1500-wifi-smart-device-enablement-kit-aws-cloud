//
//  SocialIdentityManager.swift
//  AWSChat
//
//  Created by Abhishek Mishra on 23/03/2017.
//  Copyright © 2017 ASM Technology Ltd. All rights reserved.
//

import Foundation
import AWSCognitoIdentityProvider

class SocialIdentityManager : NSObject {
    
    fileprivate var loginDictionary:[String : String]
    
    static let sharedInstance: SocialIdentityManager = SocialIdentityManager()
    
    private override init() {
        loginDictionary =  [String : String]()
        super.init()
    }
    
    func registerFacebookToken(_ token:String) {
        self.loginDictionary[AWSIdentityProviderFacebook] = token
    }
    
    func registerGoogleToken(_ token:String) {
        self.loginDictionary[AWSIdentityProviderGoogle] = token
    }
    
    func registerCognitoToken(key:String, token:String) {
        self.loginDictionary[key] = token
    }

}

extension SocialIdentityManager : AWSIdentityProviderManager {
    
    func logins() -> AWSTask<NSDictionary> {
        return AWSTask(result: loginDictionary as NSDictionary)
    }
    
}
