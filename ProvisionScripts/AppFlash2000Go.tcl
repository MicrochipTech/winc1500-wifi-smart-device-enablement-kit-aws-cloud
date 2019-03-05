# ----------------------------------------------------------------------------
#         ATMEL Microcontroller Software Support 
# ----------------------------------------------------------------------------
# Copyright (c) 2011, Atmel Corporation
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# - Redistributions of source code must retain the above copyright notice,
# this list of conditions and the disclaimer below.
#
# Atmel's name may not be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# DISCLAIMER: THIS SOFTWARE IS PROVIDED BY ATMEL "AS IS" AND ANY EXPRESS OR
# IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT ARE
# DISCLAIMED. IN NO EVENT SHALL ATMEL BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
# OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
# EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# ----------------------------------------------------------------------------

################################################################################
#  Main script: Load a file into Flash
################################################################################
set exampleFile	"delme.bin"
set err_code 0

## Turn on a RED LED
## turn off R,G,B drive high
TCL_Write_Int $target(handle) 0x0008C000 0x40002818
## enable RGB as output
TCL_Write_Int $target(handle) 0x0008C000 0x40002808
## turn off G,B drive high
TCL_Write_Int $target(handle) 0x00088000 0x40002818
## drive red low
TCL_Write_Int $target(handle) 0x4000 0x40002814

## Flashing binaries
send_file {Flash} "$exampleFile" 0x00002000 0

## Turn on a YELLOW LED
## turn on R,G drive low
TCL_Write_Int $target(handle) 0x0000C000 0x40002814

set n [expr { int(10000000 * rand()) }]
set ReceivefileName "temp$n.bin"
set fileSize [file size $exampleFile]
receive_file {Flash} $ReceivefileName 0x00002000 $fileSize 0
set returnCompare [TCL_Compare $exampleFile $ReceivefileName]
file delete $ReceivefileName

if {$returnCompare == 1} {
        puts "-I- Sent file do not match!\n"
		## turn on RED LED
		TCL_Write_Int $target(handle) 0x00088000 0x40002818
		TCL_Write_Int $target(handle) 0x00004000 0x40002814
} else {
        puts "-I- Sent file match exactly!\n"
		## turn on Green LED
		TCL_Write_Int $target(handle) 0x00084000 0x40002818
		TCL_Write_Int $target(handle) 0x00008000 0x40002814
}

puts "-I------------------------------------------------------"
puts "-I- Starting Application ..... "
puts "-I------------------------------------------------------"

## could turn off LED pins with TCL_Write_Int $target(handle) 0x0008C000 0x40002804

TCL_Go $target(handle) 0x00002000 err_code
