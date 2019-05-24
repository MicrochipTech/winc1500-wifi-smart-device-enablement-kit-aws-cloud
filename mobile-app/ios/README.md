# The iOS Smartphone APP Sample

This directory contains all the source files for the iOS smartphone APP that used to sign-up/ sign-in account, register the Wi-Fi Smart Device Enablement Kit to the account, perfrom network provisioning and control or monitor the Wi-Fi Smart Device Enablement Kit. These features are implemented with the AWS Mobile SDK.

## Requirements

* Xcode 9.2 and later
* iOS 8 and later

## Using the Sample

1. The AWS Mobile SDK for iOS is available through [CocoaPods](http://cocoapods.org). If you have not installed CocoaPods, install CocoaPods:

		sudo gem install cocoapods
		pod setup

2. To install the AWS Mobile SDK for iOS run the following command in the directory containing this sample:

		pod install

3. Create an Amazon Cognito User Pool. Follow the 4 steps under **Creating your Cognito Identity user pool** in this [blog post](http://mobile.awsblog.com/post/TxGNH1AUKDRZDH/Announcing-Your-User-Pools-in-Amazon-Cognito).

4. Open `CognitoYourUserPoolsSample.xcworkspace`.

5. Open **Constants.swift**. Set **CognitoIdentityUserPoolId**, **CognitoIdentityUserPoolAppClientId** and **CognitoIdentityUserPoolAppClientSecret**, **CognitoRegion**, **PoolId**, **AWSRegion**, **IOT_ENDPOINT**, **PolicyName** to the values obtained when you created your user pool.
```swift
		
		let CognitoIdentityUserPoolId = "YOUR_USER_POOL_ID"
		let CognitoIdentityUserPoolAppClientId = "YOUR_APP_CLIENT_ID"
		let CognitoIdentityUserPoolAppClientSecret = "YOUR_APP_CLIENT_SECRET"
		let CognitoRegion = "YOUR_COGNITO_REGION"  
		let PoolId = "YOUR_IDENTITY_POOL_ID"  
		let AWSRegion = "YOUR_AWS_IOT_REGION"  
		let IOT_ENDPOINT = "YOUR_AWS_IOT_ENDPOINT" 
		let PolicyName = "YOUR_AWS_IOT_POLICY_NAME" 
```

6. Build and run the sample app.

## Notes
The sample showcases how to display a UI that requires an authenticated user.  
If valid tokens don't exist, it implements the AWSCognitoIdentityInteractiveAuthenticationDelegate to display the sign-in UI and prompt the user to login.  
If you quit the app while signed in and restart it, it will remain signed in.  It also implements AWSCognitoIdentityRememberDevice to show how to remember devices
and AWSCognitoIdentityNewPasswordRequired to demonstrate how to prompt your end user to change their password during sign-in.
