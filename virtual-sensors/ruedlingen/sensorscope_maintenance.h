/****************************************************************************
   GSN - EPFL - LSIR
   Maintenance stations data messages (Sensorscope Project)
   Use the following command in order to generate Java classes with MIG:
  
    mig -target=tinynode \
        -I ${TOSROOT}/contrib/shockfish/tos/platform/tinynode \
        java \
        -java-classname=SensorscopeMaintenance \
        sensorscope_maintenance.h \
        sensorscopeMaintenance \
        -o SensorscopeMaintenance.java
 
 ***************************************************************************/

enum {
	AM_SENSORSCOPEMAINTENANCE = 138,
};

typedef nx_struct sensorscopeMaintenance {
	nx_uint8_t	ntw_sender_id		:	8	;
	nx_uint8_t	ntw_cost_to_bs		:	8	;
	nx_uint8_t	tsp_hop_count		:	8	;
	nx_uint8_t	tsp_packet_sn		:	8	;
	nx_uint32_t	timestamp_offset	:	32	;		
} sensorscopeMaintenance_t;
