/**
* \file
*
* \brief Functions used to configure and detect the button press
*
* Copyright (c) 2016 Atmel Corporation. All rights reserved.
*
* \asf_license_start
*
* \page License
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* 3. The name of Atmel may not be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
* 4. This software may only be redistributed and used in connection with an
*    Atmel micro controller product.
*
* THIS SOFTWARE IS PROVIDED BY ATMEL "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT ARE
* EXPRESSLY AND SPECIFICALLY DISCLAIMED. IN NO EVENT SHALL ATMEL BE LIABLE FOR
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
* OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
* STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
* ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*
* \asf_license_stop
*
*/

/*
* Support and FAQ: visit <a href="http://www.atmel.com/design-support/">Atmel
*Support</a>
*/


#include <asf.h>
#include "button.h"



unsigned long pre_tick[NUMBER_OF_BUTTON];
unsigned long press_time[NUMBER_OF_BUTTON];
unsigned long sw_index[NUMBER_OF_BUTTON];
unsigned char prev_state[NUMBER_OF_BUTTON];	// 0: release; 1: press]

void initialise_button(void)
{
	
	/* Set buttons as inputs */
	struct port_config config_port_pin;
	port_get_config_defaults(&config_port_pin);
	config_port_pin.direction = PORT_PIN_DIR_INPUT;
	config_port_pin.input_pull = PORT_PIN_PULL_DOWN;
	port_pin_set_config(SW1_PIN, &config_port_pin);
	port_pin_set_config(SW2_PIN, &config_port_pin);
	port_pin_set_config(SW3_PIN, &config_port_pin);
	
	for (int i=0; i< NUMBER_OF_BUTTON; i++)
		sw_index[i] = 1;

}

int buttonInitCheck()
{
	
	if(SW1_ACTIVE == port_pin_get_input_level(SW1_PIN)){	// Enter WINC1500 FW programming mode
		return 1;
	}
	if(SW2_ACTIVE == port_pin_get_input_level(SW2_PIN)){	// Enter WINC1500 FW programming mode
		return 2;
	}
	if(SW3_ACTIVE == port_pin_get_input_level(SW3_PIN)){	// Enter WINC1500 FW programming mode
		return 3;
	}
	
	return 0;
	
}
void buttonTaskInit()
{
	return;	
}



void button_check(unsigned long tick, int button)
{

	bool pin_lvl;
	
	switch (button)
	{
		case 1:
			pin_lvl = port_pin_get_input_level(SW1_PIN);
			break;
		case 2:
			pin_lvl = port_pin_get_input_level(SW2_PIN);
			break;
		case 3:
			pin_lvl = port_pin_get_input_level(SW3_PIN);
			break;
	}
	
	
	if(SW1_ACTIVE == pin_lvl && pre_tick[button-1] == 0){
		pre_tick[button-1]  = tick;
		
	}
	else if(SW1_ACTIVE == pin_lvl && pre_tick[button-1]  != 0){
		if (tick > pre_tick[button-1] )
			press_time[button-1]  = tick - pre_tick[button-1] ;
		if (press_time[button-1]  >= TIMEOUT_COUNTER_BUTTON_DEBOUNCE && prev_state[button-1]  == 0)
		{
			printf("[%s] In, trigger \r\n", __func__);
			prev_state[button-1]  = 1;
			
			for (int i=0; i<MAX_CB_INDEX; i++)
			{
				if (button_short_press_cb[button -1][i]!=NULL)
				button_short_press_cb[button -1][i]();
			}
		}
		
		
		if (press_time[button-1]  >= (sw_index[button-1] *TIMEOUT_COUNTER_5S) )
		{
			prev_state[button-1]  = 1;
			sw_index[button-1] ++;
			for (int i=0; i<MAX_CB_INDEX; i++)
			{
				if (button_long_press_cb[button-1][i]!=NULL)
				button_long_press_cb[button-1][i]();
			}
		}
	}
	else
	{
		prev_state[button-1]  = 0;
		pre_tick[button-1]  = 0;
		sw_index[button-1]  = 1;
	}
	
}
void buttonTaskExecute(unsigned long tick)
{
	
	button_check(tick, 1);
	button_check(tick, 2);
	button_check(tick, 3);
	
	
#if 0	
	static uint32 pre_tick_sw1 = 0;
	uint32 press_time_sw1 = 0;
	static uint32 sw1_index = 1;
	static uint8 prev_state_sw1 = 0;		// 0: release; 1: press]
	
	
	
	
	bool pin_lvl = port_pin_get_input_level(SW1_PIN);
	
	if(SW1_ACTIVE == pin_lvl && press_time_sw1 == 0){
		pre_tick_sw1 = tick;
		
	}
	else if(SW1_ACTIVE == pin_lvl && pre_tick_sw1 != 0){
		if (tick > pre_tick_sw1)
		press_time_sw1 = tick - pre_tick_sw1;
		
		if (press_time_sw1 >= TIMEOUT_COUNTER_BUTTON_DEBOUNCE && prev_state == 0)
		{
			printf("[%s] In, trigger \r\n", __func__);
			prev_state_sw1 = 1;
		
			for (int i=0; i<MAX_CB_INDEX; i++)
			{
				if (button_short_press_cb[0][i]!=NULL)
				button_short_press_cb[0][i]();
			}
		}
		
		
		if (press_time_sw1 >= (sw1_index*TIMEOUT_COUNTER_5S) )
		{
			prev_state_sw1 = 1;
			sw1_index++;
			for (int i=0; i<MAX_CB_INDEX; i++)
			{
				if (button_long_press_cb[0][i]!=NULL)
					button_long_press_cb[0][i]();
			}
		}
	}
	else
	{
		prev_state_sw1 = 0;
		pre_tick_sw1 = 0;
		sw1_index = 1;
	}

#endif
}

int regButtonShortPressDetectCallback(void* cb, int button)
{
	for (int i=0; i<MAX_CB_INDEX; i++)
	{
		if (button_short_press_cb[button-1][i]==NULL)
		{
			button_short_press_cb[button-1][i] = cb;
			return i;
		}
	}
	
	printf("[%s] No quota...\n", __func__);
	return -1;
}
int unRegButtonShortPressDetectCallback(int sock, int button)
{
	if (button_short_press_cb[button-1][sock]!=NULL)
	{
			button_short_press_cb[button-1][sock] = NULL;
			return 0;
	}
	else
		printf("[%s] Cannot find the related cb..\n", __func__);
	
	return -1;
}

int regButtonLongPressDetectCallback(void* cb, int button)
{
	for (int i=0; i<MAX_CB_INDEX; i++)
	{
		if (button_long_press_cb[button-1][i]==NULL)
		{
			button_long_press_cb[button-1][i] = cb;
			return i;
		}
	}
	
	printf("[%s] No quota...\n", __func__);
	return -1;
}
int unRegButtonLongPressDetectCallback(int sock, int button)
{

	if (button_long_press_cb[button-1][sock]!=NULL)
	{
		button_long_press_cb[button-1][sock] = NULL;
		return 0;
	}
	else
		printf("[%s] Cannot find the related cb..\n", __func__);
		
	return -1;
}
