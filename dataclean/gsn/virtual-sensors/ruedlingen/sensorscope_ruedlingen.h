/****************************************************************************
   GSN - EPFL - LSIR
   Record stations data messages (Sensorscope Project)
   Use the following command in order to generate Java classes with MIG:
  
    mig -target=tinynode \
        -I ${TOSROOT}/contrib/shockfish/tos/platform/tinynode \
        java \
        -java-classname=RuedlingenData \
        sensorscope_ruedlingen.h \
        ruedlingenData \
        -o RuedlingenData.java
 
 ***************************************************************************/

enum {
	AM_RUEDLINGENDATA = 128,
};

typedef nx_struct ruedlingenData {
	nx_uint8_t	ntw_sender_id	:	8	;
	nx_uint8_t	ntw_cost_to_bs	:	8	;
	nx_uint8_t	tsp_hop_count	:	8	;
	nx_uint8_t	tsp_packet_sn	:	8	;
	nx_uint8_t	reporter_id	:	8	;
	nx_uint32_t	timestamp	:	32	;		
	nx_uint8_t	rainmeter	:	8	;
	nx_uint16_t	windspeed	:	16	;
	nx_uint16_t	watermark	:	16	;
	nx_uint16_t	airhumidity	:	16	;
	nx_uint16_t	soilmoisture	:	16	;
	nx_uint16_t	winddirection	:	16	;
	nx_uint16_t	solarradiation	:	16	;
	nx_uint16_t	airtemperature	:	16	;
	nx_uint16_t	skintemperature	:	16	;
} ruedlingenData_t;
