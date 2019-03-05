/**
* \file
*
* \brief LED Functions
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

/**
* \mainpage
* \section preface Preface
* This is the reference manual for the Multi-Role/Multi-Connect Application
*/
/*- Includes ---------------------------------------------------------------*/
#include <asf.h>
//#include "conf_board.h"
#include "led.h"
#include "rtc.h"

unsigned char gu8Blue;
unsigned char gu8Red;
unsigned char gu8Green;
unsigned char gu8OnOffState;
unsigned char gu8BlueIntensity;
unsigned char gu8RedIntensity;
unsigned char gu8GreenIntensity;
Led_Mode gu8LedMode;
Led_Color gu8Color;

#define CONF_PWM_MODULE      TCC0
#define CONF_PWM_CHANNELR    0 // W04
#define CONF_PWM_CHANNELG    1 // W05
#define CONF_PWM_CHANNELB    3 // W03
extern tcc_instance;
void setColor (uint8_t LED_COLOR ,int value )
{
	switch (LED_COLOR)
	{
		case LED_COLOR_RED:
		printf("LED_RED, val=%d\r\n", value);
		gu8RedIntensity = value;
		if (value > 0)
			gu8Red = 0;
		else
			gu8Red = 1;
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR),0xFFFF - ((gu8RedIntensity *0xffff)/100 & 0xffff));
		break;
		case LED_COLOR_GREEN:
		printf("LED_GREEN, val=%d\r\n", value);
		gu8GreenIntensity = value;
		if (value > 0)
			gu8Green = 0;
		else
			gu8Green = 1;
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG),0xFFFF - ((gu8GreenIntensity * 0xffff)/100 & 0xffff));
		break;
		case LED_COLOR_BLUE:
		printf("LED_BLUE, val=%d\r\n", value);
		gu8BlueIntensity = value;
		if (value > 0)
			gu8Blue = 0;
		else
			gu8Blue = 1;
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB),0xFFFF - ((gu8BlueIntensity * 0xffff)/100 & 0xffff));
		break;
		
	}
	
}

void toggleLED()
{
	if (gu8OnOffState == 1)
	{
		
		gu8OnOffState = 0;
		
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR),0xFFFF);
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG),0xFFFF);
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB),0xFFFF);

	}
	else
	{
		gu8OnOffState = 1;
		tcc_set_compare_value (&tcc_instance,TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR,gu8Red ? 0xFFFF :0 );
		tcc_set_compare_value (&tcc_instance,TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG,gu8Green ? 0xFFFF : 0);
		tcc_set_compare_value (&tcc_instance,TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB,gu8Blue ? 0xFFFF : 0);

	}
}

static inline void turnOnLED(void)
{
	gu8OnOffState = 1;

	if (gu8Red == 0)
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR),0xFFFF - ((gu8RedIntensity *0xffff)/100 & 0xffff));
	else
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR),0xFFFF - ((0 *0xffff)/100 & 0xffff));
		
	if (gu8Green == 0)
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG),0xFFFF - ((gu8GreenIntensity *0xffff)/100 & 0xffff));
	else
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG),0xFFFF - ((0 *0xffff)/100 & 0xffff));
	
	if (gu8Blue == 0)
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB),0xFFFF - ((gu8BlueIntensity *0xffff)/100 & 0xffff));	
	else
		tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB),0xFFFF - ((0 *0xffff)/100 & 0xffff));	

}

static inline void turnOffLED(void)
{
	gu8OnOffState = 0;
	tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELR),0xFFFF);
	tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELG),0xFFFF);
	tcc_set_compare_value (&tcc_instance,(enum tcc_match_capture_channel)(TCC_MATCH_CAPTURE_CHANNEL_0 + CONF_PWM_CHANNELB),0xFFFF);
}

void initialise_led(void)
{
	gu8Blue = 0;
	gu8Green = 1;
	gu8Red = 1;
	gu8Color = LED_COLOR_BLUE;
	gu8BlueIntensity = 100;
	gu8RedIntensity = 0;
	gu8GreenIntensity = 0;
	
}


Led_Color led_ctrl_get_color()
{
	return gu8Color;
}


void led_ctrl_set_color(Led_Color color, Led_Mode mode)
{
	
	switch(color)
	{
		case LED_COLOR_BLUE:
			gu8Blue = 0;
			gu8Green = 1;
			gu8Red = 1;
			gu8Color = LED_COLOR_BLUE;
		break;
		case LED_COLOR_GREEN:
			gu8Blue = 1;
			gu8Green = 0;
			gu8Red = 1;
			gu8Color = LED_COLOR_GREEN;
		break;
		case LED_COLOR_RED:
			gu8Blue = 1;
			gu8Green = 1;
			gu8Red = 0;
			gu8Color = LED_COLOR_RED;
		break;
		case LED_COLOR_YELLOW:
			gu8Blue = 1;
			gu8Green = 0;
			gu8Red = 0;
			gu8Color = LED_COLOR_YELLOW;
		break;
		case LED_COLOR_Magneta:
			gu8Blue = 0;
			gu8Green = 1;
			gu8Red = 0;
			gu8Color = LED_COLOR_Magneta;
		break;
		case LED_COLOR_Cyan:
			gu8Blue = 0;
			gu8Green = 0;
			gu8Red = 1;
			gu8Color = LED_COLOR_Cyan;
		break;
		case LED_COLOR_WHTIE:
			gu8Blue = 0;
			gu8Green = 0;
			gu8Red = 0;
			gu8Color = LED_COLOR_WHTIE;
		break;
		case LED_COLOR_BLACK:
			gu8Blue = 1;
			gu8Green = 1;
			gu8Red = 1;
			gu8Color = LED_COLOR_BLACK;
		break;
		
		default:
		break;
		
	}
	
	if (mode == LED_MODE_NONE)
	{
		if (gu8OnOffState == 1)
		turnOnLED();	// color change take effect
		return;
	}
	
	gu8LedMode = mode;
	
}

void led_ctrl_set_mode(Led_Mode mode)
{
	gu8LedMode = mode;
	switch (mode)
	{
		case LED_MODE_TURN_OFF:
			turnOffLED();
			break;
		case LED_MODE_TURN_ON:
			turnOnLED();
			break;
		
		default:
			break;
	}

}

void led_ctrl_execute()
{
	switch (gu8LedMode)
	{
		case LED_MODE_TURN_OFF:
			turnOffLED();
			break;
		case LED_MODE_TURN_ON:
			turnOnLED();
			break;
		
		case LED_MODE_BLINK_NORMAL:
			if (tick_500ms)
			{
				tick_500ms = 0;
				if (gu8OnOffState == 1)
					turnOffLED();
				else
					turnOnLED();
			}
			break;
		case LED_MODE_BLINK_FAST:
			if (tick_100ms)
			{
				tick_100ms = 0;
				if (gu8OnOffState == 1)
					turnOffLED();
				else
					turnOnLED();
			}
			break;
		
		default:
			break;
	}
}