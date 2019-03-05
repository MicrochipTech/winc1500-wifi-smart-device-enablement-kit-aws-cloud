for /f "tokens=4" %%A in ('mode^|findstr "COM[0-9]*:"') do set comport_set=%%A
copy %1 delme.bin
SET comport_set=%comport_set:~0,-1%
set board_name="saml21_wsenbrd[not factory programmed]"
sam-ba.exe %comport_set% %board_name% AppFlash2000Go.tcl 2>&1 
echo %*  
del delme.bin
notepad logfile.log