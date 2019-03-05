/**
 * \file
 *
 * \brief main file
 *
 * Copyright (c) 2015 Atmel Corporation. All rights reserved.
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
 *    Atmel microcontroller product.
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
 * Support</a>
 */

/*- Includes ---------------------------------------------------------------*/
#include <asf.h>
//#include "platform.h"
//#include "at_ble_api.h"
//#include "timer_hw.h"
#include "conf_serialdrv.h"
#include "conf_board.h"
#include "rtc.h"
#include "bme280\bme280_support.h"
#include "conf_sensor.h"
#include "veml60xx\veml60xx.h"
#include "i2c.h"
#include "adc_measure.h"
#include "driver/include/m2m_wifi.h"
#include "socket/include/socket.h"
#include "main.h"
#include "led.h"
#include "env_sensor.h"
#include "motion_sensor.h"
#include "button.h"
#include "nvm_handle.h"
#include "winc15x0.h"
#include "usb_hid.h"
#include "ecc_provisioning_task.h"
#include "conf_console.h"

/* enum variable */
enum status_code veml60xx_sensor_status = STATUS_ERR_NOT_INITIALIZED;

/* variables */
BME280_RETURN_FUNCTION_TYPE bme280_sensor_status = ERROR;
	
/* function prototypes */
void configure_wdt(void);

void enable_gclk1(void);

extern volatile uint32_t ms_ticks;

/** SysTick counter to avoid busy wait delay. */
//volatile uint32_t ms_ticks = 0;


/** UART module for debug. */
static struct usart_module cdc_uart_module;

/**
 *  Configure console.
 */
static void serial_console_init(void)
{
 	struct usart_config usart_conf;

	usart_get_config_defaults(&usart_conf);
	usart_conf.mux_setting = CONF_STDIO_MUX_SETTING;
	usart_conf.pinmux_pad0 = CONF_STDIO_PINMUX_PAD0;
	usart_conf.pinmux_pad1 = CONF_STDIO_PINMUX_PAD1;
	usart_conf.pinmux_pad2 = CONF_STDIO_PINMUX_PAD2;
	usart_conf.pinmux_pad3 = CONF_STDIO_PINMUX_PAD3;
	usart_conf.baudrate    = CONF_STDIO_BAUDRATE;

	stdio_serial_init(&cdc_uart_module, CONF_STDIO_USART_MODULE, &usart_conf);
	usart_enable(&cdc_uart_module);
}


/* Watchdog configuration */
void configure_wdt(void)
{
	/* Create a new configuration structure for the Watchdog settings and fill
	* with the default module settings. */
	struct wdt_conf config_wdt;
	wdt_get_config_defaults(&config_wdt);
	/* Set the Watchdog configuration settings */
	config_wdt.always_on = false;
	//config_wdt.clock_source = GCLK_GENERATOR_4;
	config_wdt.timeout_period = WDT_PERIOD_2048CLK;
	/* Initialize and enable the Watchdog with the user settings */
	wdt_set_config(&config_wdt);
}



void enable_gclk1(void)
{
	struct system_gclk_gen_config gclk_conf;
	
	system_gclk_init();	
	gclk_conf.high_when_disabled = false;
	gclk_conf.source_clock       = GCLK_SOURCE_OSC16M;
	gclk_conf.division_factor = 1;
	gclk_conf.run_in_standby  = true;
	gclk_conf.output_enable   = false;
	system_gclk_gen_set_config(2, &gclk_conf);
	system_gclk_gen_enable(2);
}





/**
 * \brief SysTick handler used to measure precise delay. 
 */
//void SysTick_Handler(void)
//{
//	ms_ticks++;
//	printf("DBG log1\r\n");
//}


void initialise_gpio(void)
{

	
	/* led port pin initialization */
	struct port_config config_port_pin;
	port_get_config_defaults(&config_port_pin);
	config_port_pin.direction = PORT_PIN_DIR_OUTPUT;
	port_pin_set_config(PIN_PB22, &config_port_pin);
	port_pin_set_config(PIN_PB23, &config_port_pin);

	port_pin_set_config(PIN_PA17, &config_port_pin);
	port_pin_set_config(PIN_PA20, &config_port_pin);
	port_pin_set_config(PIN_PA21, &config_port_pin);
	port_pin_set_output_level(PIN_PB22, 0);
	port_pin_set_output_level(PIN_PB23, 0);
	port_pin_set_output_level(PIN_PA17, 0);
	port_pin_set_output_level(PIN_PA20, 1);
	port_pin_set_output_level(PIN_PA21, 0);
}

struct tcc_module tcc_instance;
static void configure_tcc(void)
{
	//! [setup_config]
	struct tcc_config config_tcc;
	//! [setup_config]
	//! [setup_config_defaults]
	tcc_get_config_defaults(&config_tcc, TCC0);
	//! [setup_config_defaults]

	//! [setup_change_config]
	config_tcc.counter.period = 0xFFFF;
	config_tcc.compare.wave_generation = TCC_WAVE_GENERATION_SINGLE_SLOPE_PWM;
	config_tcc.compare.match[0] = 0x1000;
	config_tcc.compare.match[1] = 0x1000;
	config_tcc.compare.match[3] = 0x1000;
	//! [setup_change_config]

	//! [setup_change_config_pwm]
	config_tcc.pins.enable_wave_out_pin[0] = true;
	config_tcc.pins.wave_out_pin[0]        = PIN_PA14;
	config_tcc.pins.wave_out_pin_mux[0]    = MUX_PA14F_TCC0_WO4;
	
	config_tcc.pins.enable_wave_out_pin[1] = true;
	config_tcc.pins.wave_out_pin[1]        = PIN_PA15;
	config_tcc.pins.wave_out_pin_mux[1]    = MUX_PA15F_TCC0_WO5;
	
	config_tcc.pins.enable_wave_out_pin[3] = true;
	config_tcc.pins.wave_out_pin[3]        = PIN_PA19;
	config_tcc.pins.wave_out_pin_mux[3]    = MUX_PA19F_TCC0_WO3;
	//! [setup_change_config_pwm]

	//! [setup_set_config]
	tcc_init(&tcc_instance, TCC0, &config_tcc);
	//! [setup_set_config]

	//! [setup_enable]
	tcc_enable(&tcc_instance);
	//! [setup_enable]
}

/* main function */
int main(void)
{
	/* Configure TCC for LED light intensity*/
	configure_tcc();

	/* Initialize RTC */
	rtc_init();
	
	/* initialize LED */
	initialise_led();
	
	/* initialize LED */
	initialise_gpio();
	
	led_ctrl_set_color(LED_COLOR_BLUE, LED_MODE_BLINK_NORMAL);	
		
	/* system clock initialization */
	system_init();

	//i2c configure
	configure_sensor_i2c();
	
	/* delay routine initialization */
	delay_init();

	/* configure adc for battery measurement */
	//configure_adc();
 
#ifdef DEBUG_SUPPORT
	/* Initialize serial console for debugging */
	serial_console_init();
#endif

	
	DBG_LOG("Initializing Wi-Fi Smart Device Enablement Kit");
	DBG_LOG("cpu_freq=%d\n",(int)system_cpu_clock_get_hz());
	DBG_LOG("Firmware version: %s.%s.%s", FIRMWARE_MAJOR_VER, FIRMWARE_MINOR_VER, FIRMWARE_PATCH_VER);
	
	/* Initialize the BSP. */
	nm_bsp_init();
	
	nvm_init();
	
	initialise_button();
	
	if (buttonInitCheck() == 2)
	{
		led_ctrl_set_color(LED_COLOR_GREEN, LED_MODE_BLINK_NORMAL);
		while(1) {
			
		}
	}

	//Initialize bme280
	wearable_bme280_init();
	//Initialize veml60xx
	veml60xx_init();	

#ifdef AWS_JITR
	// Initialize the USB HID interface
	usb_hid_init();

	ecc_provisioning_task();
#endif
	
#ifdef ECC508 
	while (1) {
		zero_touch_provisioning_task();
			led_ctrl_set_color(LED_COLOR_YELLOW, LED_MODE_BLINK_NORMAL);	
		//break;
	}
#endif

	wifiInit();

	//env_sensor_data_init();
	while (1) {
         
		zero_touch_provisioning_task();
		/* Handle pending events from network controller. */
		wifiTaskExecute();
		
		buttonTaskExecute(ms_ticks);
		
		
		if(tick_2second == 1)
		{
			tick_2second = 0;
			if (getWiFiMode()==APP_STA && getWiFiStates() > WIFI_TASK_MQTT_SUBSCRIBE)
				env_sensor_execute();
			
		}
		
	}


}


