This page has a demo/example for WINC1500. Please click [here](https://github.com/search?q=topic:winc1500+org:MicrochipTech) for more demos/examples.

# WiFi Smart Device Enablement Kit
This package contains all the files (firmware code, python scripts, mobile app code, lambda function) for the Wi-Fi Smart Device Enablement Kit to connect AWS Cloud.

Below link show more details of the Wi-Fi Smart Device Enablement Kit:
https://www.microchip.com/Developmenttools/ProductDetails/AC164165

You can read [Wi-Fi Smart Device Enablement Kit User Guide](http://ww1.microchip.com/downloads/en/DeviceDoc/ATWINC-15x0-Smart-Device%20Kit-Wi-Fi-Smart-Device-Enablement-Kit-User-Guide-DS50002880A.pdf) for a getting started guide to test the Kit.

If you need to customize the application and connect the board to your private AWS account, you need to read [Wi-Fi Smart Device Enablement Kit Developer Guide](http://ww1.microchip.com/downloads/en/DeviceDoc/ATWINC15x0-Smart-Device-Kit-Wi-Fi-Smart-Device%20Enablement%20Kit%20Developers%20Guide-User-Guide-DS50002885A.pdf) to know the devleopment of the Kit .



# Release notes

**03/05/2020 (mcu-frimware-v1.1.0-mobile-app-v1.0.0:)**

  &nbsp;New Fetures:
  1. Support storing sensor data to ElastiCache Redis
  (For the cloude configuration for ElastiCache Feature, user can read [ElastiCache Setup Guide](https://github.com/MicrochipTech/winc1500-wifi-smart-device-enablement-kit-aws-cloud/blob/master/doc/cloud-setup/Elasticache%20Setup.docx))

  &nbsp;Bug Fixes:
  1. Fix the issue of AWS reonnection failure when AP power off and on
  2. Fix the smart home skill discovery failure issue
  3. Update axios library to ver 0.19.0 for the high severity security warning from Github


**03/14/2019 (mcu-frimware-v1.0.1-mobile-app-v1.0.0:)**

&nbsp;New Features:
1. This is the init version code for the WINC1500 Smart Device Enablement Kit project. 
	The OOB (out-of-box) firmware of the Smart Device Enablement Kit are in this version.

&nbsp;Bug Fixes:
None
