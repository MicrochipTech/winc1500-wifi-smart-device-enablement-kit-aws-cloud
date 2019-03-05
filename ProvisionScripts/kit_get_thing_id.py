import argparse
import hid
import time
from mchp_aws_zt_kit import MchpAwsZTKitDevice
from sim_hid_device import SimMchpAwsZTHidDevice
from aws_kit_common import *

def main():
    # Create argument parser to document script use
    

    print('\nOpening AWS Zero-touch Kit Device')
    device = MchpAwsZTKitDevice(hid.device())
    #device = MchpAwsZTKitDevice(SimMchpAwsZTHidDevice())
    device.open()

    print('\nInitializing Kit')
    resp = device.init()
    print('    ATECC508A SN: %s' % resp['deviceSn'])
    
    time.sleep(3)
    
    print('\nGet Thing ID')
    resp = device.get_thing_id()
    print('    Thing ID: %s' % resp['thingId'])
    
    print('\nDone')
    return 1

try:
    main()
except AWSZTKitError as e:
    print(e)
