# Bootloader

## Installation
This folder contains the files to configure the bootloader GUI or bootloader upgrade scripts

To program firmware with bootloader, PC need to install SAM-BA V2.18 (bootloader GUI).
The installation file can be download from below URL:
https://www.microchip.com/developmenttools/ProductDetails/atmel%20sam-ba%20in-system%20programmer


After install SAM-BA V2.18, you need to configure the GUI by below steps to make it work with the WINC1500 Secure Wi-Fi Board:

1. Cope the folder saml21_wsenbrd\ to C:\Program Files (x86)\Atmel\sam-ba_2.18\tcl_lib

2. rename the boards.tcl to boards_old.tcl in C:\Program Files (x86)\Atmel\sam-ba_2.18\tcl_lib

3. copy boards.tcl in this directory to C:\Program Files (x86)\Atmel\sam-ba_2.18\tcl_lib


## User guide

Steps:
1. Connect PC to WINC1500 Secure Wi-Fi Board with USB cable

2. Press and hold SW1 on the Board during power on the board, LD2 turn off if the board success to trigger the bootloader
3. Launch SAM-BA V2.18 on PC, a GUI is display
4. Select the correct port for the connection on the GUI
5. Select "saml21_wsenbrd" for the board   
6. Click "Connect"
7. In Tab "Flash", set the Address to 0x2000
8. In Tab "Flash", select the firmware bin file in "Send File Name"
9. Click "Send File"

