copy %1 delme.bin
set board_name="saml21_xplained_pro[not factory programmed]"
sam-ba.exe \usb\ARM0 %board_name% AppFlash2000.tcl > logfile.log 2>&1
del delme.bin
notepad logfile.log