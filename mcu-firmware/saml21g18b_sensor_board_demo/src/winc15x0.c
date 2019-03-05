/**
* \file
*
* \brief WINC1500 Functions
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
#include "driver/include/m2m_wifi.h"
#include "driver/include/m2m_periph.h"
#include "driver/include/m2m_ssl.h"
#include "driver/include/m2m_types.h"
#include "atcacert/atcacert_client.h"
#include "atcacert/atcacert_host_hw.h"
#include "cert_def_1_signer.h"
#include "cert_def_2_device.h"
#include "tls/atcatls.h"
#include "ecc_provisioning_task.h"
#include "socket/include/socket.h"
#include "socket/include/socket.h"
#include "string.h"
#include "AWS_SDK/aws_iot_src/utils/aws_iot_log.h"
#include "AWS_SDK/aws_iot_src/utils/aws_iot_version.h"
#include "AWS_SDK/aws_iot_src/protocol/mqtt/aws_iot_mqtt_interface.h"
#include "winc15x0.h"
#include "iot_message.h"
#include "cJSON.h"
#include "cloud_wrapper.h"
//#include "button_handle.h"
#include "cmdset.h"
#include "wifi_prov.h"
#include "led.h"
#include "env_sensor.h"
#include "nvm_handle.h"
#include "button.h"
#include "main.h"

uint8_t gMacaddr[M2M_MAC_ADDRES_LEN];
uint8 gDefaultSSID[M2M_MAX_SSID_LEN] = {0};
uint8 gAuthType = M2M_WIFI_SEC_INVALID;
uint8 gDefaultKey[M2M_MAX_PSK_LEN] = {0};
uint8 gUuid[AWS_COGNITO_UUID_LEN] = {0};
int8_t	g_button1_state = 0;
int8_t	g_button2_state = 0;
int8_t	g_button3_state = 0;
uint32 g_pub_count = 0;


/** Wi-Fi status variable. */
bool gbConnectedWifi = false,receivedTime = false;

wifi_FSM_states wifi_states = WIFI_TASK_SWITCHING_TO_STA;

#define PUBLISH_BUTTON	SW0_PIN

int detSw0Sock = -1;	//detect sw0 socket

uint8 gu8WiFiMode = APP_STA;
static SOCKET provServerSocket = -1;
static uint8 gau8RxBuffer[SOCKET_BUFFER_MAX_LENGTH] = {0};
static uint8_t gAPEnabled = 0;

#define SOCK_TIMEOUT                   0x927C0 // 10min	
	
MQTTConnectParams connectParams;
MQTTSubscribeParams subParams;
MQTTMessageParams Msg;
MQTTPublishParams Params;


extern NodeInfo multiSpkDevice;

wifi_nvm_data_t wifi_nvm_data ={0};

extern void setColor (uint8_t LED_COLOR ,int value );
uint g_red_led_value= 0, g_green_led_value=0, g_blue_led_value=1, g_light_state=1, g_publish_init_state = 0;
uint g_light_intensity = 100;

#define WIFI_UPDATE_DEVICE_STATUS_TIME			(2000)

#define MAX_TLS_CERT_LENGTH			1024
#define SIGNER_CERT_MAX_LEN 		(g_cert_def_1_signer.cert_template_size + 8) // Need some space in case the cert changes size by a few bytes
#define SIGNER_PUBLIC_KEY_MAX_LEN 	64
#define DEVICE_CERT_MAX_LEN			(g_cert_def_2_device.cert_template_size + 8) // Need some space in case the cert changes size by a few bytes
#define CERT_SN_MAX_LEN				32
#define TLS_SRV_ECDSA_CHAIN_FILE	"ECDSA.lst"
#define INIT_CERT_BUFFER_LEN        (MAX_TLS_CERT_LENGTH*sizeof(uint32) - TLS_FILE_NAME_MAX*2 - SIGNER_CERT_MAX_LEN - DEVICE_CERT_MAX_LEN)

//========================================================
//========================================================

//! Array of private key slots to rotate through the ECDH calculations
static uint16 g_ecdh_key_slot[] = {2};
//! Index into the ECDH private key slots array
static uint32 g_ecdh_key_slot_index = 0;

static sint8 ecdh_derive_client_shared_secret(tstrECPoint *server_public_key,
uint8 *ecdh_shared_secret,
tstrECPoint *client_public_key)
{
	sint8 status = M2M_ERR_FAIL;
	uint8_t ecdh_mode;
	uint16_t key_id;
	
	if ((g_ecdh_key_slot_index < 0) ||
	(g_ecdh_key_slot_index >= (sizeof(g_ecdh_key_slot) / sizeof(g_ecdh_key_slot[0]))))
	{
		g_ecdh_key_slot_index = 0;
	}
	
	if(_gDevice->mIface->mIfaceCFG->devtype == ATECC608A)
	{
		//do special ecdh functions for the 608, keep ephemeral keys in SRAM
		ecdh_mode = ECDH_MODE_SOURCE_TEMPKEY | ECDH_MODE_COPY_OUTPUT_BUFFER;
		key_id = GENKEY_PRIVATE_TO_TEMPKEY;
	}
	else
	{
		//specializations for the 508, use an EEPROM key slot
		ecdh_mode = ECDH_PREFIX_MODE;
		key_id = g_ecdh_key_slot[g_ecdh_key_slot_index];
		g_ecdh_key_slot_index++;
	}
	
	//generate an ephemeral key
	//TODO - add loop to make sure we get an acceptable private key
	if(atcab_genkey(key_id, client_public_key->X) == ATCA_SUCCESS)
	{
		client_public_key->u16Size = 32;
		//do the ecdh from the private key in tempkey, results put in ecdh_shared_secret
		if(atcab_ecdh_base(ecdh_mode, key_id, server_public_key->X, ecdh_shared_secret, NULL) == ATCA_SUCCESS)
		{
			status = M2M_SUCCESS;
		}
	}

	return status;
}

static sint8 ecdh_derive_key_pair(tstrECPoint *server_public_key)
{
	sint8 status = M2M_ERR_FAIL;
	
	if ((g_ecdh_key_slot_index < 0) ||
	(g_ecdh_key_slot_index >= (sizeof(g_ecdh_key_slot) / sizeof(g_ecdh_key_slot[0]))))
	{
		g_ecdh_key_slot_index = 0;
	}

	if( (status = atcab_genkey(g_ecdh_key_slot[g_ecdh_key_slot_index], server_public_key->X) ) == ATCA_SUCCESS)
	{
		server_public_key->u16Size      = 32;
		server_public_key->u16PrivKeyID = g_ecdh_key_slot[g_ecdh_key_slot_index];

		g_ecdh_key_slot_index++;

		status = M2M_SUCCESS;
	}

	return status;
}

static sint8 ecdsa_process_sign_verify_request(uint32 number_of_signatures)
{
	sint8 status = M2M_ERR_FAIL;
	tstrECPoint	Key;
	uint32 index = 0;
	uint8 signature[80];
	uint8 hash[80] = {0};
	uint16 curve_type = 0;
	
	for(index = 0; index < number_of_signatures; index++)
	{
		status = m2m_ssl_retrieve_cert(&curve_type, hash, signature, &Key);

		if (status != M2M_SUCCESS)
		{
			M2M_ERR("m2m_ssl_retrieve_cert() failed with ret=%d", status);
			return status;
		}

		if(curve_type == EC_SECP256R1)
		{
			bool is_verified = false;
			
			status = atcab_verify_extern(hash, signature, Key.X, &is_verified);
			if(status == ATCA_SUCCESS)
			{
				status = (is_verified == true) ? M2M_SUCCESS : M2M_ERR_FAIL;
				if(is_verified == false)
				{
					M2M_INFO("ECDSA SigVerif FAILED\n");
				}
			}
			else
			{
				status = M2M_ERR_FAIL;
			}
			
			if(status != M2M_SUCCESS)
			{
				m2m_ssl_stop_processing_certs();
				break;
			}
		}
	}

	return status;
}

static sint8 ecdsa_process_sign_gen_request(tstrEcdsaSignReqInfo *sign_request,
uint8 *signature,
uint16 *signature_size)
{
	sint8 status = M2M_ERR_FAIL;
	uint8 hash[32];
	
	status = m2m_ssl_retrieve_hash(hash, sign_request->u16HashSz);
	if (status != M2M_SUCCESS)
	{
		M2M_ERR("m2m_ssl_retrieve_hash() failed with ret=%d", status);
		return status;
	}

	if(sign_request->u16CurveType == EC_SECP256R1)
	{
		*signature_size = 64;
		status = atcab_sign(DEVICE_KEY_SLOT, hash, signature);
	}

	return status;
}

static sint8 ecdh_derive_server_shared_secret(uint16 private_key_id,
tstrECPoint *client_public_key,
uint8 *ecdh_shared_secret)
{
	uint16 key_slot	= private_key_id;
	sint8 status = M2M_ERR_FAIL;
	uint8 atca_status = ATCA_STATUS_UNKNOWN;

	atca_status = atcab_ecdh(key_slot, client_public_key->X, ecdh_shared_secret);
	if(atca_status == ATCA_SUCCESS)
	{
		status = M2M_SUCCESS;
	}
	else
	{
		M2M_INFO("__SLOT = %u, Err = %X\n", key_slot, atca_status);
	}
	
	return status;
}


static void ecc_process_request(tstrEccReqInfo *ecc_request)
{
	tstrEccReqInfo ecc_response;
	uint8 signature[80];
	uint16 response_data_size = 0;
	uint8 *response_data_buffer = NULL;
	
	ecc_response.u16Status = 1;
	
	switch (ecc_request->u16REQ)
	{
		case ECC_REQ_CLIENT_ECDH:
		ecc_response.u16Status = ecdh_derive_client_shared_secret(&(ecc_request->strEcdhREQ.strPubKey),
		ecc_response.strEcdhREQ.au8Key,
		&ecc_response.strEcdhREQ.strPubKey);
		break;

		case ECC_REQ_GEN_KEY:
		ecc_response.u16Status = ecdh_derive_key_pair(&ecc_response.strEcdhREQ.strPubKey);
		break;

		case ECC_REQ_SERVER_ECDH:
		ecc_response.u16Status = ecdh_derive_server_shared_secret(ecc_request->strEcdhREQ.strPubKey.u16PrivKeyID,
		&(ecc_request->strEcdhREQ.strPubKey),
		ecc_response.strEcdhREQ.au8Key);
		break;
		
		case ECC_REQ_SIGN_VERIFY:
		ecc_response.u16Status = ecdsa_process_sign_verify_request(ecc_request->strEcdsaVerifyREQ.u32nSig);
		break;
		
		case ECC_REQ_SIGN_GEN:
		ecc_response.u16Status = ecdsa_process_sign_gen_request(&(ecc_request->strEcdsaSignREQ), signature,
		&response_data_size);
		response_data_buffer = signature;
		break;
		
		default:
		// Do nothing
		break;
	}
	
	ecc_response.u16REQ      = ecc_request->u16REQ;
	ecc_response.u32UserData = ecc_request->u32UserData;
	ecc_response.u32SeqNo    = ecc_request->u32SeqNo;

	m2m_ssl_ecc_process_done();
	m2m_ssl_handshake_rsp(&ecc_response, response_data_buffer, response_data_size);
}

static size_t winc_certs_get_total_files_size(const tstrTlsSrvSecHdr* header)
{
	uint8 *pBuffer = (uint8*) header;
	uint16 count = 0;

	while ((*pBuffer) == 0xFF)
	{
		
		if(count == INIT_CERT_BUFFER_LEN)
		break;
		count++;
		pBuffer++;
	}

	if(count == INIT_CERT_BUFFER_LEN)
	return sizeof(*header); // Buffer is empty, no files
	
	return header->u32NextWriteAddr;
}

static const char* bin2hex(const void* data, size_t data_size)
{
	static char buf[256];
	static char hex[] = "0123456789abcdef";
	const uint8_t* data8 = data;
	
	if (data_size*2 > sizeof(buf)-1)
	return "[buf too small]";
	
	for (size_t i = 0; i < data_size; i++)
	{
		buf[i*2 + 0] = hex[(*data8) >> 4];
		buf[i*2 + 1] = hex[(*data8) & 0xF];
		data8++;
	}
	buf[data_size*2] = 0;
	
	return buf;
}

static sint8 winc_certs_append_file_buf(uint32* buffer32, uint32 buffer_size,
const char* file_name, uint32 file_size,
const uint8* file_data)
{
	tstrTlsSrvSecHdr* header = (tstrTlsSrvSecHdr*)buffer32;
	tstrTlsSrvSecFileEntry* file_entry = NULL;
	uint16 str_size = m2m_strlen((uint8*)file_name) + 1;
	uint16 count = 0;
	uint8 *pBuffer = (uint8*)buffer32;

	while ((*pBuffer) == 0xFF)
	{
		
		if(count == INIT_CERT_BUFFER_LEN)
		break;
		count++;
		pBuffer++;
	}

	if(count == INIT_CERT_BUFFER_LEN)
	{
		// The WINC will need to add the reference start pattern to the header
		header->u32nEntries = 0; // No certs
		// The WINC will need to add the offset of the flash were the certificates are stored to this address
		header->u32NextWriteAddr = sizeof(*header); // Next cert will be written after the header
	}
	
	if (header->u32nEntries >= sizeof(header->astrEntries)/sizeof(header->astrEntries[0]))
	return M2M_ERR_FAIL; // Already at max number of files
	
	if ((header->u32NextWriteAddr + file_size) > buffer_size)
	return M2M_ERR_FAIL; // Not enough space in buffer for new file
	
	file_entry = &header->astrEntries[header->u32nEntries];
	header->u32nEntries++;
	
	if (str_size > sizeof(file_entry->acFileName))
	return M2M_ERR_FAIL; // File name too long
	m2m_memcpy((uint8*)file_entry->acFileName, (uint8*)file_name, str_size);
	
	file_entry->u32FileSize = file_size;
	file_entry->u32FileAddr = header->u32NextWriteAddr;
	header->u32NextWriteAddr += file_size;
	
	// Use memmove to accommodate optimizations where the file data is temporarily stored
	// in buffer32
	memmove(((uint8*)buffer32) + (file_entry->u32FileAddr), (uint8*)file_data, file_size);
	
	return M2M_SUCCESS;
}

static sint8 ecc_transfer_certificates(uint8_t subject_key_id[20])
{
	sint8 status = M2M_SUCCESS;
	int atca_status = ATCACERT_E_SUCCESS;
	uint32_t signer_ca_public_key_size = 0;
	uint8_t *signer_cert = NULL;
	size_t signer_cert_size;
	uint8_t signer_public_key[SIGNER_PUBLIC_KEY_MAX_LEN];
	uint8_t *device_cert = NULL;
	size_t device_cert_size;
	uint8_t cert_sn[CERT_SN_MAX_LEN];
	size_t cert_sn_size;
	uint8_t *file_list = NULL;
	char *device_cert_filename = NULL;
	char *signer_cert_filename = NULL;
	uint32 sector_buffer[MAX_TLS_CERT_LENGTH];
	
	do
	{
		// Clear cert chain buffer
		memset(sector_buffer, 0xFF, sizeof(sector_buffer));

		// Use the end of the sector buffer to temporarily hold the data to save RAM
		file_list   = ((uint8_t*)sector_buffer) + (sizeof(sector_buffer) - TLS_FILE_NAME_MAX*2);
		signer_cert = file_list - SIGNER_CERT_MAX_LEN;
		device_cert = signer_cert - DEVICE_CERT_MAX_LEN;

		// Init the file list
		memset(file_list, 0, TLS_FILE_NAME_MAX*2);
		device_cert_filename = (char*)&file_list[0];
		signer_cert_filename = (char*)&file_list[TLS_FILE_NAME_MAX];


		// Get the Signer's CA public key from the ATECCx08A
		signer_ca_public_key_size = SIGNER_PUBLIC_KEY_MAX_LEN;
		atca_status = provisioning_get_signer_ca_public_key(&signer_ca_public_key_size,
		g_signer_1_ca_public_key);
		if (atca_status != ATCA_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		// Uncompress the signer certificate from the ATECCx08A device
		signer_cert_size = SIGNER_CERT_MAX_LEN;
		atca_status = atcacert_read_cert(&g_cert_def_1_signer, g_signer_1_ca_public_key,
		signer_cert, &signer_cert_size);
		if (atca_status != ATCACERT_E_SUCCESS)
		{
			// Break the do/while loop
			break;
		}

		
		// Get the signer's public key from its certificate
		atca_status = atcacert_get_subj_public_key(&g_cert_def_1_signer, signer_cert,
		signer_cert_size, signer_public_key);
		if (atca_status != ATCACERT_E_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		// Uncompress the device certificate from the ATECCx08A device.
		device_cert_size = DEVICE_CERT_MAX_LEN;
		atca_status = atcacert_read_cert(&g_cert_def_2_device, signer_public_key,
		device_cert, &device_cert_size);
		if (atca_status != ATCACERT_E_SUCCESS)
		{
			// Break the do/while loop
			break;
		}

		if (subject_key_id)
		{
			atca_status = atcacert_get_subj_key_id(&g_cert_def_2_device, device_cert,
			device_cert_size, subject_key_id);
			if (atca_status != ATCACERT_E_SUCCESS)
			{
				// Break the do/while loop
				break;
			}
		}
		
		// Get the device certificate SN for the filename
		cert_sn_size = sizeof(cert_sn);
		atca_status = atcacert_get_cert_sn(&g_cert_def_2_device, device_cert,
		device_cert_size, cert_sn, &cert_sn_size);
		if (atca_status != ATCACERT_E_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		// Build the device certificate filename
		strcpy(device_cert_filename, "CERT_");
		strcat(device_cert_filename, bin2hex(cert_sn, cert_sn_size));
		
		// Add the DER device certificate the TLS certs buffer
		status = winc_certs_append_file_buf(sector_buffer, sizeof(sector_buffer),
		device_cert_filename, device_cert_size,
		device_cert);
		if (status != M2M_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		device_cert = NULL; // Make sure we don't use this now that it has moved
		
		// Get the signer certificate SN for the filename
		cert_sn_size = sizeof(cert_sn);
		atca_status = atcacert_get_cert_sn(&g_cert_def_1_signer, signer_cert,
		signer_cert_size, cert_sn, &cert_sn_size);
		if (atca_status != ATCACERT_E_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		
		// Build the signer certificate filename
		strcpy(signer_cert_filename, "CERT_");
		strcat(signer_cert_filename, bin2hex(cert_sn, cert_sn_size));
		
		// Add the DER signer certificate the TLS certs buffer
		status = winc_certs_append_file_buf(sector_buffer, sizeof(sector_buffer),
		signer_cert_filename, signer_cert_size, signer_cert);
		if (status != M2M_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
		
		// Add the cert chain list file to the TLS certs buffer
		status = winc_certs_append_file_buf(sector_buffer, sizeof(sector_buffer),
		TLS_SRV_ECDSA_CHAIN_FILE,
		TLS_FILE_NAME_MAX*2, file_list);
		if (status != M2M_SUCCESS)
		{
			// Break the do/while loop
			break;
		}

		file_list = NULL;
		signer_cert_filename = NULL;
		device_cert_filename = NULL;
		
		// Update the TLS cert chain on the WINC.
		status = m2m_ssl_send_certs_to_winc((uint8 *)sector_buffer,
		(uint32)winc_certs_get_total_files_size((tstrTlsSrvSecHdr*)sector_buffer));
		if (status != M2M_SUCCESS)
		{
			// Break the do/while loop
			break;
		}
	} while (false);

	if (atca_status)
	{
		M2M_ERR("eccSendCertsToWINC() failed with ret=%d", atca_status);
		status =  M2M_ERR_FAIL;
	}

	return status;
}

static void aws_wifi_ssl_callback(uint8 u8MsgType, void *pvMsg)
{
	tstrEccReqInfo *ecc_request = NULL;
	
	switch (u8MsgType)
	{
		case M2M_SSL_REQ_ECC:
		ecc_request = (tstrEccReqInfo*)pvMsg;
		ecc_process_request(ecc_request);
		break;
		
		case M2M_SSL_RESP_SET_CS_LIST:
		default:
		// Do nothing
		break;
	}
}


static void EnvSensorCallbackHandler(environment_data_t sensor_data, unsigned char flag)
{
	cJSON* item;
	NodeInfo node_info[15];
	int8_t cnt = 0;
	
	if (flag & TEMP_UPDATE_BIT)
	{
		strcpy(node_info[cnt].dataType,TEMP_DATATYPE_NAME);
		node_info[cnt].value = sensor_data.temperature;
		cnt++;
	}
	if (flag & HUM_UPDATE_BIT)
	{
		strcpy(node_info[cnt].dataType,HUM_DATATYPE_NAME);
		node_info[cnt].value = sensor_data.humidity;
		cnt++;
	}
	if (flag & UV_UPDATE_BIT)
	{
		strcpy(node_info[cnt].dataType,UV_DATATYPE_NAME);
		node_info[cnt].value = sensor_data.uv;
		cnt++;
	}
	if (flag & PRESSURE_UPDATE_BIT)
	{
		strcpy(node_info[cnt].dataType,PRESSURE_DATATYPE_NAME);
		node_info[cnt].value = sensor_data.pressure;
		cnt++;
	}
#ifndef USE_SHADOW	
	item = iot_message_reportInfo(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);

	cloud_mqtt_publish(gPublish_Channel,item);
	cloud_mqtt_publish(gSearchResp_Channel,item);
	cJSON_Delete(item);
#endif

#ifdef USE_SHADOW
	strcpy(node_info[cnt].dataType,"COUNT");
	node_info[cnt].value = g_pub_count++;
	cnt++;



	item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
	cloud_mqtt_publish(gPublish_Channel_shadow,item);
	cJSON_Delete(item);
#endif

}


static void MQTTSubscribeCBCallbackHandler(MQTTCallbackParams params)
{
	printf("%s In\n", __func__);
	printf("%.*s\t%.*s",
	params.TopicNameLen, params.pTopicName, params.MessageParams.PayloadLen, params.MessageParams.pPayload);
	printf("\n\r");
	

	cJSON* item=NULL;
	char data_type[30];
	int data_value;

	
	Iot_Msg_Command cmd = iot_message_parser_cmd_type(params.MessageParams.pPayload);
	if (cmd == MSG_CMD_UPDATE)
	{
		int info_cnt = iot_message_get_info_count(params.MessageParams.pPayload);
		
		for (int i=0; i<info_cnt; i++)
		{
			iot_message_parser_info_data(params.MessageParams.pPayload, i, data_type, &data_value);
			printf("info --- dataType: %s, val: %d\n", data_type, data_value);
			
			switch (data_value)
			{
				case 0:
				led_ctrl_set_color(LED_COLOR_BLUE, LED_MODE_NONE);
				break;
				case 1:
				led_ctrl_set_color(LED_COLOR_GREEN, LED_MODE_NONE);
				break;
				case 2:
				led_ctrl_set_color(LED_COLOR_RED, LED_MODE_NONE);
				break;
				case 3:
				led_ctrl_set_color(LED_COLOR_YELLOW, LED_MODE_NONE);
				break;
				case 4:
				led_ctrl_set_color(LED_COLOR_WHTIE, LED_MODE_NONE);
				break;
				case 5:
				led_ctrl_set_color(LED_COLOR_BLACK, LED_MODE_NONE);
				break;
				
			}
			
			return;
		}
	}
	else if (cmd == MSG_CMD_SEARCH)
	{
		
		//item = iot_message_searchResp(DEVICE_TYPE,gAwsMqttClientId);
		environment_data_t env_data;
		get_env_sensor_data_for_display(&env_data);
		DBG_LOG("DBG: temperature = %d, humidity = %d, uv = %d, pressure = %d\r\n", env_data.temperature, env_data.humidity, env_data.uv, env_data.pressure);
		
		item = iot_message_searchResp_with_temp_uv(DEVICE_TYPE,gAwsMqttClientId, env_data.temperature, env_data.uv, DEVICE_NAME);
		cloud_mqtt_publish(gSearchResp_Channel,item);
	}
	else if (cmd == MSG_CMD_GET)
	{
		environment_data_t env_data;
		get_env_sensor_data_for_display(&env_data);
		
		DBG_LOG("DBG: temperature = %d, humidity = %d, uv = %d, pressure = %d\r\n", env_data.temperature, env_data.humidity, env_data.uv, env_data.pressure);
		
		NodeInfo info[5];
		
		strcpy(info[0].dataType,TEMP_DATATYPE_NAME);
		info[0].value = (int) env_data.temperature;
		
		strcpy(info[1].dataType,HUM_DATATYPE_NAME);
		info[1].value = (int) env_data.humidity;
		
		strcpy(info[2].dataType,UV_DATATYPE_NAME);
		info[2].value = (int) env_data.uv;
		
		strcpy(info[3].dataType,PRESSURE_DATATYPE_NAME);
		info[3].value = (int) env_data.pressure;
		
		strcpy(info[4].dataType,LED1_DATATYPE_NAME);
		Led_Color color = led_ctrl_get_color();
		if (color == LED_COLOR_YELLOW)	//align with the mobile APP option number, yellow is option number 2, blue is 0 and green is 1
		color = 2;
		info[4].value = color;
		
		
		item = iot_message_reportAllInfo(DEVICE_TYPE, gAwsMqttClientId, 5, info, DEVICE_NAME, gUuid);
		cloud_mqtt_publish(gPublish_Channel,item);
		
	}

	
	if (item!=NULL)
	cJSON_Delete(item);
	
	return 0;
}

static void MQTTSubscribeCBCallbackHandler_shadow(MQTTCallbackParams params)
{
	printf("%s In\n", __func__);
	printf("%.*s\t%.*s",
	params.TopicNameLen, params.pTopicName, params.MessageParams.PayloadLen, params.MessageParams.pPayload);
	printf("\n\r");
	

	//	cJSON* item=NULL;
	//	char data_type[30];
	//	int data_value;

	cJSON *json;
	cJSON *json_state;
	cJSON *json_key;
	Iot_Msg_Command cmd;
	
	cJSON* item;
	NodeInfo node_info[7];
	int8_t cnt = 0;
	
	
	json=cJSON_Parse(params.MessageParams.pPayload);
	
	if (!json) {
		printf("Error when decode json: [%s]\n",cJSON_GetErrorPtr());
		return MSG_CMD_UNKNOWN;
	}
	json_state = cJSON_GetObjectItem(json,"state");
	
	json_key = cJSON_GetObjectItem(json_state,"LED_INTENSITY");
	if (json_key)
	{
		g_light_intensity = json_key->valueint;
		
		if (g_red_led_value)
			setColor(LED_COLOR_RED,g_light_intensity);
		else
			setColor(LED_COLOR_RED,0);
		
		if (g_green_led_value)
			setColor(LED_COLOR_GREEN,g_light_intensity);
		else
			setColor(LED_COLOR_GREEN,0);
		
		if (g_blue_led_value)
			setColor(LED_COLOR_BLUE,g_light_intensity);
		else
			setColor(LED_COLOR_BLUE,0);
			
		strcpy(node_info[cnt].dataType,"LED_INTENSITY");
		node_info[cnt].value = json_key->valueint;
		cnt++;
		
	}
	
	
	json_key = cJSON_GetObjectItem(json_state,"LED_R");
	if (json_key)
	{
		strcpy(node_info[cnt].dataType,"LED_R");
		node_info[cnt].value = json_key->valueint;
		g_red_led_value = json_key->valueint;
		
		if (g_red_led_value)
			setColor(LED_COLOR_RED,g_light_intensity);
		else
			setColor(LED_COLOR_RED,0);
			
		cnt++;
	}
	
	json_key = cJSON_GetObjectItem(json_state,"LED_G");
	if (json_key)
	{
		strcpy(node_info[cnt].dataType,"LED_G");
		node_info[cnt].value = json_key->valueint;
		g_green_led_value = json_key->valueint;
		
		if (g_green_led_value)
			setColor(LED_COLOR_GREEN,g_light_intensity);
		else
			setColor(LED_COLOR_GREEN,0);
		
		cnt++;
	}
	json_key = cJSON_GetObjectItem(json_state,"LED_B");
	if (json_key)
	{
		strcpy(node_info[cnt].dataType,"LED_B");
		node_info[cnt].value = json_key->valueint;
		g_blue_led_value = json_key->valueint;
		
		if (g_blue_led_value)
			setColor(LED_COLOR_BLUE,g_light_intensity);
		else
			setColor(LED_COLOR_BLUE,0);
		
		cnt++;
	}
	
	json_key = cJSON_GetObjectItem(json_state,"Light");
	if (json_key)
	{
		strcpy(node_info[cnt].dataType,"Light");
		node_info[cnt].value = json_key->valueint;
		g_light_state = json_key->valueint;
		
		if (g_light_state)
		{
			
			if (g_blue_led_value)
			setColor(LED_COLOR_BLUE,g_light_intensity);
			else
			setColor(LED_COLOR_BLUE,0);
			
			if (g_green_led_value)
			setColor(LED_COLOR_GREEN,g_light_intensity);
			else
			setColor(LED_COLOR_GREEN,0);
			
			if (g_red_led_value)
			setColor(LED_COLOR_RED,g_light_intensity);
			else
			setColor(LED_COLOR_RED,0);
		}
		else
		{
			setColor(LED_COLOR_BLUE,0);
			setColor(LED_COLOR_GREEN,0);
			setColor(LED_COLOR_RED,0);
		}
		
		cnt++;
	}
#if 0
	json_key = cJSON_GetObjectItem(json_state,"Light");
	if(json_key)
	{
		if (json_key->valueint == 0)
		{
			printf("OFF\n");
			g_light_state = 0;
			setColor(LED_COLOR_RED,0);
			setColor(LED_COLOR_GREEN,0);
			setColor(LED_COLOR_BLUE,0);
		}else
		{
			printf("ON\n");
			g_light_state = 1;
			setColor(LED_COLOR_RED,g_red_led_value);
			setColor(LED_COLOR_GREEN,g_green_led_value);
			setColor(LED_COLOR_BLUE,g_blue_led_value);

		}
		strcpy(node_info[cnt].dataType,"Light");
		node_info[cnt].value = g_light_state;
		cnt++;
	}
#endif
	json_key = cJSON_GetObjectItem(json_state,"PA");
	if (json_key)
	{
printf("DBG command = PORTA\n");
printf("DBG command = %d\n", json_key->valueint);

		switch (json_key->valueint)
		{
			case 17:
				port_pin_set_output_level(PIN_PA17, 1);
				break;
			case 20:
				port_pin_set_output_level(PIN_PA20, 1);
				break;
			case 21:
				port_pin_set_output_level(PIN_PA21, 1);
				break;
			case -17:
				port_pin_set_output_level(PIN_PA17, 0);
			break;
			case -20:
				port_pin_set_output_level(PIN_PA20, 0);
			break;
			case -21:
				port_pin_set_output_level(PIN_PA21, 0);
			break;
		}
		
		strcpy(node_info[cnt].dataType,"PA");
		node_info[cnt].value = json_key->valueint;
		cnt++;
		
		
	
	}
	json_key = cJSON_GetObjectItem(json_state,"PB");
	if (json_key)
	{
printf("DBG command = PORTB\n");
printf("DBG command = %d\n", json_key->valueint);

		switch (json_key->valueint)
		{
			case 22:
				port_pin_set_output_level(PIN_PB22, 1);
			break;
			case 23:
				port_pin_set_output_level(PIN_PB23, 1);
			break;
			case -22:
				port_pin_set_output_level(PIN_PB22, 0);
			break;
			case -23:
				port_pin_set_output_level(PIN_PB23, 0);
			break;
		}
		strcpy(node_info[cnt].dataType,"PB");
		node_info[cnt].value = json_key->valueint;
		cnt++;
		
				
	
	}
	
	if (cnt){
		item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
		cloud_mqtt_publish(gPublish_Channel_shadow,item);
		cJSON_Delete(item);
	}
	if (json)
	cJSON_Delete(json);
	
	return;
	
}


static void MQTTSubscribeCBCallbackHandler_shadow_get(MQTTCallbackParams params)
{
	printf("%s In\n", __func__);
	printf("%.*s\t%.*s",
	params.TopicNameLen, params.pTopicName, params.MessageParams.PayloadLen, params.MessageParams.pPayload);
	printf("\n\r");
	

	//	cJSON* item=NULL;
	//	char data_type[30];
	//	int data_value;

	cJSON *json;
	cJSON *json_state;
	cJSON *json_key;
	cJSON *json_desired;
	
	Iot_Msg_Command cmd;
	
	cJSON* item;
	NodeInfo node_info[4];
	int8_t cnt = 0;
	
	
	json=cJSON_Parse(params.MessageParams.pPayload);
	if (!json) {
		printf("Error when decode json: [%s]\n",cJSON_GetErrorPtr());
		return MSG_CMD_UNKNOWN;
	}
	json_state = cJSON_GetObjectItem(json,"state");
	json_desired = cJSON_GetObjectItem(json_state,"desired");
	if (json_desired)
	{
		
		json_key = cJSON_GetObjectItem(json_desired,"LED_INTENSITY");
		if (json_key)
		{
			g_light_intensity = json_key->valueint;
			
			if (g_red_led_value)
			setColor(LED_COLOR_RED,g_light_intensity);
			else
			setColor(LED_COLOR_RED,0);
			
			if (g_green_led_value)
			setColor(LED_COLOR_GREEN,g_light_intensity);
			else
			setColor(LED_COLOR_GREEN,0);
			
			if (g_blue_led_value)
			setColor(LED_COLOR_BLUE,g_light_intensity);
			else
			setColor(LED_COLOR_BLUE,0);
			
			strcpy(node_info[cnt].dataType,"LED_INTENSITY");
			node_info[cnt].value = json_key->valueint;
			cnt++;
		}
		
		
		json_key = cJSON_GetObjectItem(json_desired,"LED_R");
		if (json_key)
		{
			strcpy(node_info[cnt].dataType,"LED_R");
			node_info[cnt].value = json_key->valueint;
			g_red_led_value = json_key->valueint;
			
			if (g_red_led_value)
			{
				setColor(LED_COLOR_RED,g_light_intensity);
			}
			else
			setColor(LED_COLOR_RED,0);
			
			cnt++;
		}
		
		json_key = cJSON_GetObjectItem(json_desired,"LED_G");
		if (json_key)
		{
			strcpy(node_info[cnt].dataType,"LED_G");
			node_info[cnt].value = json_key->valueint;
			g_green_led_value = json_key->valueint;
			
			if (g_green_led_value)
			setColor(LED_COLOR_GREEN,g_light_intensity);
			else
			setColor(LED_COLOR_GREEN,0);
			
			cnt++;
		}
		json_key = cJSON_GetObjectItem(json_desired,"LED_B");
		if (json_key)
		{
			strcpy(node_info[cnt].dataType,"LED_B");
			node_info[cnt].value = json_key->valueint;
			g_blue_led_value = json_key->valueint;
			
			if (g_blue_led_value)
			setColor(LED_COLOR_BLUE,g_light_intensity);
			else
			setColor(LED_COLOR_BLUE,0);
			
			cnt++;
		}
		
		json_key = cJSON_GetObjectItem(json_desired,"Light");
		if (json_key)
		{
			strcpy(node_info[cnt].dataType,"Light");
			node_info[cnt].value = json_key->valueint;
			g_light_state = json_key->valueint;
			
			if (g_light_state)
			{
				
			
				if (g_blue_led_value)
					setColor(LED_COLOR_BLUE,g_light_intensity);
				else
					setColor(LED_COLOR_BLUE,0);
					
				if (g_green_led_value)
					setColor(LED_COLOR_GREEN,g_light_intensity);
				else
					setColor(LED_COLOR_GREEN,0);
					
				if (g_red_led_value)
					setColor(LED_COLOR_RED,g_light_intensity);
				else
					setColor(LED_COLOR_RED,0);
			}
			else
			{
				setColor(LED_COLOR_BLUE,0);
				setColor(LED_COLOR_GREEN,0);
				setColor(LED_COLOR_RED,0);
			}
			
			cnt++;
		}
		
		if (cnt){
			item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
			cloud_mqtt_publish(gPublish_Channel_shadow,item);
			cJSON_Delete(item);
		}
		
	}
	
	if (json)
	{
		cJSON_Delete(json);
	}
	
	
	if (g_publish_init_state)
	{
		
		g_publish_init_state = 0;
		NodeInfo node[8];
		int i= 0;
		
		strcpy(node[i].dataType,"BUTTON_1");
		node[i].value = g_button1_state;	
		i++;
		
		strcpy(node[i].dataType,"BUTTON_2");
		node[i].value = g_button2_state;
		i++;
		
		strcpy(node[i].dataType,"BUTTON_3");
		node[i].value = g_button3_state;
		i++;
		
		strcpy(node[i].dataType,"LED_R");
		node[i].value = g_red_led_value;
		i++;
		
		strcpy(node[i].dataType,"LED_G");
		node[i].value = g_green_led_value;
		i++;
		
		strcpy(node[i].dataType,"LED_B");
		node[i].value = g_blue_led_value;
		i++;
		
		strcpy(node[i].dataType,"LED_INTENSITY");
		node[i].value = g_light_intensity;
		i++;
		
		strcpy(node[i].dataType,"Light");
		node[i].value = g_light_state;
		i++;
		
		item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, i, &node);
		printf("publish the init value to the channel\r\n");
		cloud_mqtt_publish(gPublish_Channel_shadow,item);
		cJSON_Delete(item);
	}
	
	
	return;
	
}


static void set_dev_param_to_mac(uint8 *param, uint8 *addr, uint8_t offset)
{
	/* Name must be in the format AtmelSmartPlug000000 */
	uint16 len;

	len = m2m_strlen(param);
	if (len >= offset) {
		param[len - 1] = HEX2ASCII((addr[5] >> 0) & 0x0f);
		param[len - 2] = HEX2ASCII((addr[5] >> 4) & 0x0f);
		param[len - 3] = HEX2ASCII((addr[4] >> 0) & 0x0f);
		param[len - 4] = HEX2ASCII((addr[4] >> 4) & 0x0f);
		param[len - 5] = HEX2ASCII((addr[3] >> 0) & 0x0f);
		param[len - 6] = HEX2ASCII((addr[3] >> 4) & 0x0f);
	}
}

static void close_app_socket(void)
{
	int8_t ret = M2M_SUCCESS;

	if (provServerSocket != -1) {
		ret = close(provServerSocket);
		printf("[AP] TCP server socket %d closed %d!\r\n", provServerSocket, ret);
		provServerSocket = -1;
	}
	return;
}

static void start_AP_app(void)
{
	struct sockaddr_in	addr;
	int ret = -1;
	
	/* TCP Server. */
	if(provServerSocket == -1) {
		if((provServerSocket = socket(AF_INET, SOCK_STREAM, 0)) >= 0) {
			// Initialize socket address structure.
			addr.sin_family      = AF_INET;
			addr.sin_port        = _htons(AP_TCP_SERVER_PORT);
			addr.sin_addr.s_addr = 0;

			if((ret = bind(provServerSocket, (struct sockaddr*)&addr, sizeof(addr))) == 0) {
				M2M_INFO("[AP] TCP socket bind success!\r\n");
			}
			else {
				M2M_INFO("[AP] Bind Failed. Error code = %d\r\n", ret);
				close(provServerSocket);
				M2M_INFO("[AP] TCP server socket %d closed!\r\n", provServerSocket);
				provServerSocket = -1;
			}
		}
		else {
			M2M_INFO("[AP] TCP Server Socket Creation Failed\r\n");
			return;
		}
	}
	else {
		accept(provServerSocket, NULL, 0);
	}
}


static void wifiSwitchtoSTA(void)
{
	int8_t ret;
	
	close_app_socket();
	if (gAPEnabled) {
		ret = m2m_wifi_disable_ap();
		if (M2M_SUCCESS != ret) {
			printf("main: m2m_wifi_disable_ap call error!\r\n");
			return;
		}
		else {
			printf("main: m2m_wifi_disable_ap call success!\r\n");
		}
		gAPEnabled = 0;
		nm_bsp_sleep(500);
	}
	if (wifi_states == WIFI_TASK_AP_CONNECTED) {
		// If there is station connected, wait M2M_WIFI_DISCONNECTED event
		printf("main: WIFI_TASK_SWITCHING_TO_STA\r\n");
		wifi_states = WIFI_TASK_SWITCHING_TO_STA;
	}
	else {
		printf("main: m2m_wifi_connect \r\n");
		ret = m2m_wifi_connect((char *)gDefaultSSID, strlen((char *)gDefaultSSID), \
		gAuthType, (char *)gDefaultKey, M2M_WIFI_CH_ALL);
		
		if (M2M_SUCCESS != ret) {
			printf("main: m2m_wifi_connect call error!\r\n");
			while (1) {
			}
		}
		gu8WiFiMode = APP_STA;
		wifi_states = WIFI_TASK_IDLE;
		set_prov_state(STA_INIT);
	}
}

static void parse_cmd_frame(SOCKET sock, uint8 *provbuffer)
{
	uint16_t cmd_length = _get_u16(provbuffer + CMD_LEN_OFFSET) + SOF_LEN + CMDLNTH_LEN;
	if (cmd_length > MIN_CMD_LEN && cmd_length < SOCKET_BUFFER_MAX_LENGTH){
		//CRC32 checksum
		uint32_t chksum = 0;
		//To: Add the checksum
		//crc32_calculate(provbuffer, cmd_length, &chksum);
		//uint32_t rx_chksum = _get_u32(provbuffer + cmd_length);
		uint32_t rx_chksum = 0;
		if (chksum == rx_chksum) {
			//M2M_INFO("CRC pass\r\n");
			uint8_t cmd_id =  *(provbuffer + CMD_ID_OFFSET);
			cmd_resp_t cmd_response;
			memset(&cmd_response, 0, sizeof(cmd_response));
			// Parse IoT command
			parse_iot_cmd(sock, cmd_id, provbuffer + CMD_PARAM_OFFSET, &cmd_response);
			// Fill seq number
			cmd_response.data.base[CMD_SEQ_OFFSET] = *(provbuffer + CMD_SEQ_OFFSET);
			
			//To Do: Add checksum
			//crc32_calculate(cmd_response.data.base, cmd_response.length, &msg_check_sum);
			//memcpy(&cmd_response.data.base[cmd_response.length], &msg_check_sum, CHECKSUM_LEN);
			{
				DBG_LOG("send data, len = %d\r\n",cmd_response.length);
				//send(sock, (void *)cmd_response.data.base, \
				//cmd_response.length + CHECKSUM_LEN, 0);
				//if (cmd_response.length == 19)
				{
					DBG_LOG("send !!\r\n");
					send(sock, (void *)cmd_response.data.base, \
					cmd_response.length, 0);
				}
			}
			if (get_prov_state() == PROV_DONE) {
				nm_bsp_sleep(500);
				// Store WiFi information in NVM before connecting.
				wifi_nvm_data.ssidlen = strlen((char *)gDefaultSSID);
				printf("SSID len=%d\r\n", wifi_nvm_data.ssidlen);
				memcpy(wifi_nvm_data.ssid, gDefaultSSID, wifi_nvm_data.ssidlen);
				printf("SSID =%s\r\n", wifi_nvm_data.ssid);
				wifi_nvm_data.keylen = strlen((char *)gDefaultKey);
				memcpy(wifi_nvm_data.key, gDefaultKey, wifi_nvm_data.keylen);
				wifi_nvm_data.authtype = gAuthType;
				wifi_nvm_data.valid = 1;
				memcpy(wifi_nvm_data.uuid,gUuid,strlen(gUuid));
				
				//To Do:
				// Save wifi_nvm_data to flash
				nvm_store_config_data(wifi_nvm_data);
				///nv_flash_write(CONFIG_PAGE_ADDRESS,CONFIG_PAGE,(uint32_t*)(&wifi_nvm_data),sizeof(wifi_nvm_data));
				printf("Write config page\r\n");
				nvm_get_config_data(&wifi_nvm_data);
				printf("DBG SSID=%s\r\n", wifi_nvm_data.ssid);
				//To Do: 
				//Switch to STA mode after NVM store.
				wifiSwitchtoSTA();
			}
		}
		else {
			//send(get_session(session_num)->sock, (void *)"Checksum error!", strlen("Checksum error!"), 0);
		}
	}
	else {
		//send(get_session(session_num)->sock, (void *)"Msg length error!", strlen("Msg length error!"), 0);
	}
}



static void tcpsendresponse(SOCKET sock, tstrSocketRecvMsg *pstrRx)
{
	uint8 *provbuffer = pstrRx->pu8Buffer;
	
	if (provbuffer[0] == SOF_INDICATER) {
		// Plain text frame
		{
			parse_cmd_frame(sock, provbuffer);
		}
	}
	
}

/**
 * \brief Callback to get the Data from socket.
 *
 * \param[in] sock socket handler.
 * \param[in] u8Msg socket event type. Possible values are:
 *  - SOCKET_MSG_BIND
 *  - SOCKET_MSG_LISTEN
 *  - SOCKET_MSG_ACCEPT
 *  - SOCKET_MSG_CONNECT
 *  - SOCKET_MSG_RECV
 *  - SOCKET_MSG_SEND
 *  - SOCKET_MSG_SENDTO
 *  - SOCKET_MSG_RECVFROM
 * \param[in] pvMsg is a pointer to message structure. Existing types are:
 *  - tstrSocketBindMsg
 *  - tstrSocketListenMsg
 *  - tstrSocketAcceptMsg
 *  - tstrSocketConnectMsg
 *  - tstrSocketRecvMsg
 */

static void m2m_wifi_socket_handler(SOCKET sock, uint8 u8Msg, void *pvMsg)
{
	int ret =-1;
	M2M_INFO(" %s In\n", __func__);("socket %d, event %d!\r\n", sock, u8Msg);
	switch (u8Msg) {
		case SOCKET_MSG_BIND:
		{
			/* TCP socket */
			if (provServerSocket == sock) {
				tstrSocketBindMsg *pstrBind = (tstrSocketBindMsg*)pvMsg;
				if(pstrBind != NULL && pstrBind->status == 0) {
					ret = listen(sock, 0);
					if(ret < 0) {
						M2M_INFO("Listen failure! Error = %d\r\n", ret);
					}
					else {
						M2M_INFO("TCP socket %d listen!\r\n", sock);
					}
				}
				else {
					if (pstrBind->status != 0) M2M_INFO("TCP bind error %d!\r\n", pstrBind->status);
				}
			}
		}
		break;
  
		case SOCKET_MSG_LISTEN:
		{
			if (provServerSocket == sock) {
				tstrSocketListenMsg	*pstrListen = (tstrSocketListenMsg*)pvMsg;
				if(pstrListen != NULL && pstrListen->status == 0) {
					ret = accept(sock, NULL, 0);
					M2M_INFO("TCP socket %d accept!\r\n", sock);
				}
				else {
					if (pstrListen->status != 0) M2M_INFO("TCP listen error %d!\r\n", pstrListen->status);
				}
			}
		}
		break;
  
		case SOCKET_MSG_ACCEPT:
		{
			if (provServerSocket == sock) {
				tstrSocketAcceptMsg *pstrAccept = (tstrSocketAcceptMsg*)pvMsg;
				if(pstrAccept) {
					if (pstrAccept->sock >= 0) {
						#if 0
						uint8_t i = 0; 
						for (i = 0; i < 7; i++) {
							session_t* tcp_session = get_session(i);
							if (tcp_session->sock == -1) {
								tcp_session->sock = pstrAccept->sock;
								M2M_INFO("TCP data socket [%d]%d accepted!\r\n", i, tcp_session->sock);
								break;
							}
						}
						if (i >= 7) {
							M2M_INFO("TCP socket full!\r\n");
							return;
						}		
						#endif				
						recv(pstrAccept->sock, gau8RxBuffer, sizeof(gau8RxBuffer), 0);
					}
					else {
						M2M_INFO("accept sock error\r\n");
					}
				}
				else {
					M2M_INFO("accept error\r\n");
				}
			}
		}
		break;
  
		case SOCKET_MSG_SEND:
		{
			int16_t s16Sent = *((int16_t*)pvMsg);
			if(s16Sent != 0) {
				//WINC_PRINT("MSG_SEND %d bytes!\r\n", s16Sent);
			}
		}
		break;
  
		case SOCKET_MSG_RECV:
		{
			tstrSocketRecvMsg *pstrRx = (tstrSocketRecvMsg*)pvMsg;
			if (pstrRx) {
				if(pstrRx->pu8Buffer != NULL && pstrRx->s16BufferSize) {
					//tcpsendresponse(sock, pstrRx);
					printf("message = %s\r\n",pstrRx->pu8Buffer);
					tcpsendresponse(sock, pstrRx);
					recv(sock, gau8RxBuffer, sizeof(gau8RxBuffer), SOCK_TIMEOUT);
				}
				else {
					M2M_INFO("TCP Socket %d error: %d!\r\n", sock, pstrRx->s16BufferSize);
				}
			}
			else {
				M2M_INFO("Empty stream!\r\n");
				recv(sock, gau8RxBuffer, sizeof(gau8RxBuffer), 0);
			}
		}
		break;
		
		case SOCKET_MSG_SENDTO:
		{
			int16_t s16Sent = *((int16_t*)pvMsg);
			if(s16Sent != 0) {
				//WINC_PRINT("MSG_SENDTO %d bytes!\r\n", s16Sent);
			}
		}
		break;
		
		case SOCKET_MSG_RECVFROM:
		
		break;
		
		default:
			//WINC_PRINT("Other socket handler\r\n");
			if (u8Msg > SOCKET_MSG_RECVFROM) {
				M2M_INFO("Unknown state %d\r\n", u8Msg);
			}
			else {
				M2M_INFO("Not handled state %d\r\n", u8Msg);
			}
		break;
	}
}

/**
 * \brief Callback to get the Wi-Fi status update.
 *
 * \param[in] u8MsgType type of Wi-Fi notification. Possible types are:
 *  - [M2M_WIFI_RESP_CON_STATE_CHANGED](@ref M2M_WIFI_RESP_CON_STATE_CHANGED)
 *  - [M2M_WIFI_REQ_DHCP_CONF](@ref M2M_WIFI_REQ_DHCP_CONF)
 * \param[in] pvMsg A pointer to a buffer containing the notification parameters
 * (if any). It should be casted to the correct data type corresponding to the
 * notification type.
 */
static void wifi_cb(uint8_t u8MsgType, void *pvMsg)
{
	switch (u8MsgType) {
	case M2M_WIFI_RESP_CON_STATE_CHANGED:
	{
		tstrM2mWifiStateChanged *pstrWifiState = (tstrM2mWifiStateChanged *)pvMsg;
		if (pstrWifiState->u8CurrState == M2M_WIFI_CONNECTED) {
			printf("wifi_cb: M2M_WIFI_RESP_CON_STATE_CHANGED: CONNECTED\r\n");
			m2m_wifi_request_dhcp_client();
		} else if (pstrWifiState->u8CurrState == M2M_WIFI_DISCONNECTED) {
			printf("wifi_cb: M2M_WIFI_RESP_CON_STATE_CHANGED: DISCONNECTED\r\n");
			if (gu8WiFiMode == APP_AP)
			{
				if (wifi_states == WIFI_TASK_SWITCHING_TO_STA) {
					wifi_states = WIFI_TASK_STA_DISCONNECTED;
					printf("Switching to STA mode!\r\n");
					led_ctrl_set_color(LED_COLOR_BLUE, LED_MODE_BLINK_NORMAL);
					wifiSwitchtoSTA();	// Switch to STA mode if required
				}
				else
				{
					led_ctrl_set_mode(LED_MODE_BLINK_NORMAL);
					close_app_socket();
				}
			}
			else
			{
				led_ctrl_set_color(LED_COLOR_BLUE, LED_MODE_BLINK_NORMAL);
				gbConnectedWifi = false;
				m2m_wifi_disconnect();
				m2m_wifi_connect((char *)gDefaultSSID, strlen((char *)gDefaultSSID), \
				gAuthType, (char *)gDefaultKey, M2M_WIFI_CH_ALL);
			}
		}

		break;
	}

	case M2M_WIFI_REQ_DHCP_CONF:
	{
		uint8_t *pu8IPAddress = (uint8_t *)pvMsg;
		/* Turn LED0 on to declare that IP address received. */
		printf("wifi_cb: M2M_WIFI_REQ_DHCP_CONF: IP is %u.%u.%u.%u\r\n",
				pu8IPAddress[0], pu8IPAddress[1], pu8IPAddress[2], pu8IPAddress[3]);
		gbConnectedWifi = true;

		led_ctrl_set_mode(LED_MODE_BLINK_FAST);
		/* Obtain the IP Address by network name */
		///gethostbyname((uint8_t *)HostAddress);
		if (gu8WiFiMode == APP_AP)
		{
			start_AP_app();
			wifi_states = WIFI_TASK_AP_CONNECTED;
		}
		else
			m2m_wifi_get_sytem_time();
		break;
	}
	
	case M2M_WIFI_RESP_GET_SYS_TIME:
	{
		printf("Received time\r\n");
		receivedTime = true;
		wifi_states = WIFI_TASK_CONNECT_CLOUD;
	}

	default:
	{
		break;
	}
	}
}



static void wifiSwitchtoAP(void)
{
	int8_t ret;
	static uint8_t mac_addr[M2M_MAC_ADDRES_LEN];
	
	tstrM2MAPConfig strM2MAPConfig;
	/* Initialize AP mode parameters structure with SSID, channel and security type. */
	memset(&strM2MAPConfig, 0x00, sizeof(tstrM2MAPConfig));
	strcpy((char *)&strM2MAPConfig.au8SSID, AP_WLAN_SSID);
	
	m2m_wifi_get_mac_address(mac_addr);
	/* Generate SSID according mac addr */
	set_dev_param_to_mac(strM2MAPConfig.au8SSID, mac_addr, MAIN_WLAN_SSID_OFFSET);

	strM2MAPConfig.u8ListenChannel = MAIN_WLAN_CHANNEL;
	strM2MAPConfig.u8SecType = AP_WLAN_AUTH;
	
	strM2MAPConfig.au8DHCPServerIP[0] = 192;
	strM2MAPConfig.au8DHCPServerIP[1] = 168;
	strM2MAPConfig.au8DHCPServerIP[2] = 1; 
	strM2MAPConfig.au8DHCPServerIP[3] = 1;
	
#if USE_WEP
	strcpy((char *)&strM2MAPConfig.au8WepKey, MAIN_WLAN_WEP_KEY);
	strM2MAPConfig.u8KeySz = strlen(MAIN_WLAN_WEP_KEY);
	strM2MAPConfig.u8KeyIndx = MAIN_WLAN_WEP_KEY_INDEX;

	/* Generate WEP key according mac addr */
	set_dev_param_to_mac(strM2MAPConfig.au8WepKey, mac_addr, MAIN_WLAN_WEP_KEY_OFFSET);

#endif

	/* Bring up AP mode with parameters structure. */
	DBG_LOG("[%s] bring up AP!\r\n", __func__);
	ret = m2m_wifi_enable_ap(&strM2MAPConfig);
	if (M2M_SUCCESS != ret) {
		printf("main: m2m_wifi_enable_ap call error!\r\n");
		while (1) {
		}
	}
	
	/* Initialize Socket module */
	socketInit();
	registerSocketCallback(m2m_wifi_socket_handler, NULL);
	
	gAPEnabled = 1;
	wifi_states = WIFI_TASK_IDLE;
	set_prov_state(PROV_WAITING);
}

void readWiFiSettingFromMemory(void)
{
    char ssid[SLOT8_SSID_SIZE];
    uint32_t ssid_length = sizeof(ssid);
    char password[SLOT8_WIFI_PASSWORD_SIZE];
    uint32_t password_length = sizeof(password);;
    ATCA_STATUS atca_status = ATCA_STATUS_UNKNOWN;
    
    atca_status = provisioning_get_ssid(&ssid_length, ssid);
    DBG_LOG("provisioning_get_ssid, ssid = %s\n", ssid);
    atca_status = provisioning_get_wifi_password(&password_length, password);
    DBG_LOG("provisioning_get_password, password = %s\n", password);
    
    if (strlen(ssid) > 0)
    {
        memset(gDefaultSSID, 0, sizeof(gDefaultSSID));
        memset(gDefaultKey, 0, sizeof(gDefaultKey));
        if (strlen(password) > 0)
        {
            memcpy(gDefaultSSID, ssid, strlen(ssid));
            memcpy(gDefaultKey, password, strlen(password));
            gAuthType = M2M_WIFI_SEC_WPA_PSK;
        }
        else
        {
            memcpy(gDefaultSSID, ssid, strlen(ssid));
            gAuthType = M2M_WIFI_SEC_OPEN;
        }
        nvm_get_config_data(&wifi_nvm_data);
        memcpy(gUuid, wifi_nvm_data.uuid, strlen((const char*) wifi_nvm_data.uuid));
    }
    else
    {
        printf("Read config page\r\n");
        nvm_get_config_data(&wifi_nvm_data);
        //nv_flash_read(CONFIG_PAGE_ADDRESS,CONFIG_PAGE,(uint32_t*)(&wifi_nvm_data),sizeof(wifi_nvm_data));
        
        if (wifi_nvm_data.ssid[0] != 0xFF && wifi_nvm_data.ssid[0] != 0)
        {
            memset(gDefaultSSID, 0, sizeof(gDefaultSSID));
            memset(gDefaultKey, 0, sizeof(gDefaultKey));
            
            printf("ssid=%s, ssidlen=%d\r\n", wifi_nvm_data.ssid, wifi_nvm_data.ssidlen);
            memcpy(gDefaultSSID, wifi_nvm_data.ssid, wifi_nvm_data.ssidlen);
            printf("key=%s, keylen=%d\r\n", wifi_nvm_data.key, wifi_nvm_data.keylen);
            memcpy(gDefaultKey, wifi_nvm_data.key, wifi_nvm_data.keylen);
            printf("gAuthType=%d\r\n", wifi_nvm_data.authtype);
            gAuthType = wifi_nvm_data.authtype;
            
            printf("gUuid=%s, len=%d\r\n", wifi_nvm_data.uuid, strlen((const char*) wifi_nvm_data.uuid));
            memcpy(gUuid, wifi_nvm_data.uuid, strlen((const char*) wifi_nvm_data.uuid));
        }
    }
    return;
}
void setWiFiStates(wifi_FSM_states state)
{
	wifi_states = state;
	return;
}

wifi_FSM_states getWiFiStates()
{
	return wifi_states;
}

wifi_mode getWiFiMode()
{
	return gu8WiFiMode;
}

void getThingID(char* id)
{
	memcpy(id, g_thing_name, sizeof(g_thing_name));
}


void buttonSW1Handle()
{
	cJSON* item;
	NodeInfo node_info[2];
	int8_t cnt = 0;
	
	strcpy(node_info[cnt].dataType,"COUNT");
	node_info[cnt].value = g_pub_count++;
	cnt++;
	
	g_button1_state = !g_button1_state;

	//  Add BUTTON state to AWS shadow thing
	strcpy(node_info[cnt].dataType,"BUTTON_1");
	node_info[cnt].value = g_button1_state;
	cnt++;

	
	item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
	cloud_mqtt_publish(gPublish_Channel_shadow,item);
	cJSON_Delete(item);
	//unRegButtonPressDetectCallback(detSw0Sock);
	
}

void buttonSW2Handle()
{
	cJSON* item;
	NodeInfo node_info[2];
	int8_t cnt = 0;
	
	strcpy(node_info[cnt].dataType,"COUNT");
	node_info[cnt].value = g_pub_count++;
	cnt++;
	
	g_button2_state = !g_button2_state;

	//  Add BUTTON state to AWS shadow thing
	strcpy(node_info[cnt].dataType,"BUTTON_2");
	node_info[cnt].value = g_button2_state;
	cnt++;

	
	item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
	cloud_mqtt_publish(gPublish_Channel_shadow,item);
	cJSON_Delete(item);
	//unRegButtonPressDetectCallback(detSw0Sock);
	
}

void buttonSW3Handle()
{
	cJSON* item;
	NodeInfo node_info[2];
	int8_t cnt = 0;
	
	strcpy(node_info[cnt].dataType,"COUNT");
	node_info[cnt].value = g_pub_count++;
	cnt++;
	
	g_button3_state = !g_button3_state;

	//  Add BUTTON state to AWS shadow thing
	strcpy(node_info[cnt].dataType,"BUTTON_3");
	node_info[cnt].value = g_button3_state;
	cnt++;

	
	item = iot_message_reportInfo_shadow(DEVICE_TYPE, gAwsMqttClientId, cnt, &node_info);
	cloud_mqtt_publish(gPublish_Channel_shadow,item);
	cJSON_Delete(item);
	//unRegButtonPressDetectCallback(detSw0Sock);
	
}
void buttonSW3LongPressHandle()
{
	DBG_LOG("[%s] In\n", __func__);
	
	switch (wifi_states)
	{
		case WIFI_TASK_MQTT_RUNNING:
		case WIFI_TASK_MQTT_SUBSCRIBE:
			cloud_disconnect();
			///m2m_wifi_disconnect();
			wifi_states = WIFI_TASK_SWITCH_TO_AP;
			break;
			
		case WIFI_TASK_CONNECT_CLOUD:
		case WIFI_TASK_SWITCHING_TO_STA:
		case WIFI_TASK_IDLE:
			///m2m_wifi_disconnect();
			wifi_states = WIFI_TASK_SWITCH_TO_AP;
			break;
		default: 
			break;
		
	}
	
	//unRegButtonPressDetectCallback(detSw0Sock);
	
}
void detectWiFiMode()
{
	//detSw0Sock = regButtonPressDetectCallback(buttonSW0Handle);
}

void configureSW()
{
	int buttonSock, button5SecSock;
	buttonSock = regButtonShortPressDetectCallback(buttonSW3Handle,3);
	button5SecSock = regButtonLongPressDetectCallback(buttonSW3LongPressHandle, 3);
	
	buttonSock = regButtonShortPressDetectCallback(buttonSW2Handle,2);
	
	buttonSock = regButtonShortPressDetectCallback(buttonSW1Handle,1);
}


int wifiInit(void)
{
	DBG_LOG(" %s In\n", __func__);
	
	tstrWifiInitParam param;
	int8_t ret;

	configureSW();
	
	/* Initialize Wi-Fi parameters structure. */
	memset((uint8_t *)&param, 0, sizeof(tstrWifiInitParam));

	/* Initialize Wi-Fi driver with data and status callbacks. */
	param.pfAppWifiCb = wifi_cb;
	
	ret = m2m_wifi_init(&param);
	if (M2M_SUCCESS != ret) {
		printf("main: m2m_wifi_init call error!(%d)\r\n", ret);
		while (1) {
		}
	}

	/* Get MAC Address. */
	m2m_wifi_get_mac_address(gMacaddr);
	DBG_LOG("MAC Address: %02X:%02X:%02X:%02X:%02X:%02X\r\n",
	gMacaddr[0], gMacaddr[1], gMacaddr[2],
	gMacaddr[3], gMacaddr[4], gMacaddr[5]);
		
	sprintf(gAwsMqttClientId, "%02x%02x%02x%02x%02x%02x", gMacaddr[0],gMacaddr[1],gMacaddr[2],gMacaddr[3],gMacaddr[4],gMacaddr[5] );
	gAwsMqttClientId[12] = 0;
	DBG_LOG("gAwsMqttClientId Address: %s\r\n",gAwsMqttClientId);
	//strcpy(gAwsMqttClientId,"f8f005e45e4c");
	
	cloud_create_topic(gSubscribe_Channel, DEVICE_TYPE, gAwsMqttClientId, SUBSCRIBE_TOPIC);
	cloud_create_topic(gPublish_Channel, DEVICE_TYPE, gAwsMqttClientId, PUBLISH_TOPIC);


	
	//DBG_LOG("gSubscribe_Channel: %s\r\n", gSubscribe_Channel);
	//DBG_LOG("gPublish_Channel: %s\r\n", gPublish_Channel);
	
	if (gDefaultSSID[0]==0xFF || gDefaultSSID[0]==0x0 )	// Read nothing from flash, assign default value
	{
		printf("use default SSID\r\n");
		memcpy(gDefaultSSID, MAIN_WLAN_SSID, strlen(MAIN_WLAN_SSID));
		memcpy(gDefaultKey, MAIN_WLAN_PSK, strlen(MAIN_WLAN_PSK));
		gAuthType = MAIN_WLAN_AUTH;
		gDefaultSSID[strlen(MAIN_WLAN_SSID)]=0;
		gDefaultKey[strlen(MAIN_WLAN_PSK)]=0;
		DBG_LOG("gDefaultSSID=%s, pw=%s, auth=%d, ssidlen=%d, pslen=%d\n", gDefaultSSID,gDefaultKey,gAuthType, strlen(MAIN_WLAN_SSID), strlen(MAIN_WLAN_PSK));
	}
	
	register_env_sensor_udpate_callback_handler(EnvSensorCallbackHandler);
	
	#if AWS_JITR
	sint8 wifi_status = M2M_SUCCESS;
	
	// Initialize the WINC3400 SSL module
	wifi_status = m2m_ssl_init(aws_wifi_ssl_callback);
	if (wifi_status != M2M_SUCCESS)
	{
		printf("main: m2m_ssl_init call error!(%d)\r\n", wifi_status);
		while (1) {
		}
	}

	// Set the active WINC1500 TLS cipher suites
	wifi_status = m2m_ssl_set_active_ciphersuites(SSL_ECC_ONLY_CIPHERS);
	if (wifi_status != M2M_SUCCESS)
	{
		printf("main: m2m_ssl_set_active_ciphersuites call error!(%d)\r\n", wifi_status);
		while (1) {
		}
	}
	
	
	uint8_t subject_key_id[20];
	ret= ecc_transfer_certificates(subject_key_id);
	printf("ecc_transfer_certificates, ret = %d\r\n", ret);
	
	if (ret == M2M_SUCCESS)
	{
		// Convert the binary subject key ID to a hex string to use as the MQTT client ID
		for (int i=0; i<20; i++)
		{
			g_mqtt_client_id[i*2+0] = "0123456789abcdef"[subject_key_id[i] >> 4];
			g_mqtt_client_id[i*2+1] = "0123456789abcdef"[subject_key_id[i] & 0x0F];
		}
		g_mqtt_client_id[20*2] = 0; // Add terminating null

		// Make the thing name the same as the MQTT client ID
		memcpy(g_thing_name, g_mqtt_client_id, min(sizeof(g_thing_name), sizeof(g_mqtt_client_id)));
		g_thing_name[sizeof(g_thing_name)-1] = 0; // Ensure a terminating null
		
		printf("ecc_transfer_certificates, g_thing_name = %s\r\n", g_thing_name);
		
	}
	#endif

#ifdef USE_SHADOW	

/* This is using the deice ID g_thing_name */
	cloud_create_topic_shadow(gSubscribe_Channel_shadow, DEVICE_TYPE, g_thing_name, SUBSCRIBE_TOPIC_SHADOW);
	cloud_create_topic_shadow(gSubscribe_Channel_shadow_get, DEVICE_TYPE, g_thing_name, SUBSCRIBE_TOPIC_SHADOW_GET);
	cloud_create_topic_shadow(gPublish_Channel_shadow, DEVICE_TYPE, g_thing_name, PUBLISH_TOPIC_SHADOW);
	cloud_create_topic_shadow(gPublish_Channel_shadow_get, DEVICE_TYPE, g_thing_name, PUBLISH_TOPIC_SHADOW_GET);
#endif		

	
	
}



int wifiTaskExecute()
{	
	Cloud_RC ret = CLOUD_RC_NONE_ERROR;
    ATCA_STATUS atca_status = ATCA_STATUS_UNKNOWN;
    static char  hostname[SLOT8_HOSTNAME_SIZE];
    uint32_t hostname_length = sizeof(hostname);
    
	m2m_wifi_handle_events(NULL);
	switch (wifi_states)
	{
			
		case WIFI_TASK_SWITCH_TO_AP:
			
			led_ctrl_set_color(LED_COLOR_RED, LED_MODE_BLINK_NORMAL);
			m2m_wifi_disconnect();
			wifiSwitchtoAP();
			gu8WiFiMode = APP_AP;
			wifi_states = WIFI_TASK_SWITCHING_TO_AP;
			break;
			
		case WIFI_TASK_IDLE:	
		case WIFI_TASK_SWITCHING_TO_AP:
		case WIFI_TASK_AP_CONNECTED:
			// no special task
			break;
			
		case WIFI_TASK_SWITCHING_TO_STA:
			/* Connect to router. */
            readWiFiSettingFromMemory();
			gu8WiFiMode = APP_STA;
			m2m_wifi_disconnect();
			m2m_wifi_connect((char *)gDefaultSSID, strlen(gDefaultSSID), gAuthType, (char *)gDefaultKey, M2M_WIFI_CH_ALL);
			//led_ctrl_set_color(LED_COLOR_GREEN, LED_MODE_BLINK_FAST);//Luc
			wifi_states = WIFI_TASK_IDLE;
			break;
			
		case WIFI_TASK_CONNECT_CLOUD:
            atca_status = provisioning_get_hostname(&hostname_length, hostname);
            
            printf("hostname=%s\r\n", hostname);
			ret = cloud_connect(hostname, g_mqtt_client_id);

			if (ret == CLOUD_RC_SUCCESS)
			{
				led_ctrl_set_mode(LED_MODE_TURN_ON);
				wifi_states = WIFI_TASK_MQTT_SUBSCRIBE;
			}
			else
				printf("Cloud connect fail...\r\n");
			
			break;
		
		case WIFI_TASK_MQTT_SUBSCRIBE:
			
#ifdef USE_SHADOW
			ret = cloud_mqtt_subscribe(gSubscribe_Channel_shadow_get, MQTTSubscribeCBCallbackHandler_shadow_get);
			ret = cloud_mqtt_subscribe(gSubscribe_Channel_shadow, MQTTSubscribeCBCallbackHandler_shadow);
			
			
			cJSON *jsonObj;
			jsonObj=cJSON_CreateObject();
			cloud_mqtt_publish(gPublish_Channel_shadow_get,jsonObj);
			cJSON_Delete(jsonObj);
			g_publish_init_state = 1;
#else
			ret = cloud_mqtt_subscribe(gSubscribe_Channel, MQTTSubscribeCBCallbackHandler);
#endif

#ifndef USE_SHADOW		
			if (1)//ret == CLOUD_RC_SUCCESS)
			{
				//strcpy(gUuid, "20aaa2de-297e-413f-9ace-a1bebfccf08b");
				cloud_create_search_topic(gSearch_Channel, gUuid, SUBSCRIBE_SEARCH_TOPIC);
				cloud_create_search_topic(gSearchResp_Channel, gUuid, PUBLISH_SEARCH_RESP_TOPIC);
				
				DBG_LOG("subscribe search channel: %s\n", gSearch_Channel);
				ret = cloud_mqtt_subscribe(gSearch_Channel, MQTTSubscribeCBCallbackHandler);
				if (ret == CLOUD_RC_SUCCESS)
				{
					wifi_states = WIFI_TASK_MQTT_RUNNING;
				}
				else
					printf("Publish MQTT channel fail...\r\n");
			}
			else
				printf("Publish MQTT channel fail...\r\n");
#endif
			
			wifi_states = WIFI_TASK_MQTT_RUNNING;
			
			break;
			
		case WIFI_TASK_MQTT_RUNNING:
			
			//Max time the yield function will wait for read messages
			ret = cloud_mqtt_yield(100);
			if(CLOUD_RC_NETWORK_ATTEMPTING_RECONNECT == ret){
				printf("-->sleep reconnect\r\n");
				led_ctrl_set_mode(LED_MODE_BLINK_FAST);
				delay_ms(1);
				break;
			}	
			led_ctrl_set_mode(LED_MODE_TURN_ON);

			break;
			
		default:
				break;
		
	}
	
}
