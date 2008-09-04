/****************************************************************************
   Sensorscope, EPFL, Deployment Messages, marett
   Use the following command in order to generate Java classes with MIG:
   
	mig -target=tinynode \
		-I ${TOSROOT}/contrib/shockfish/tos/platform/tinynode \
		java \
		-java-classname=ThurBoard2Data \
		sensorscope.h \
		thurBoard2Data \
		-o ThurBoard2Data.java
  
 ***************************************************************************/

enum {
  AM_THURBOARD1DATA = 180,
  AM_THURBOARD2DATA = 180
};


typedef nx_struct thurBoard1Data {
	nx_uint16_t	wind_speed				;
	nx_uint16_t	wind_direction		:	12	;
	nx_uint16_t	solar_radiation		:	12	;
	nx_uint16_t	soil_moisture_1		:	12	;
	nx_uint16_t	soil_temperature_1	:	12	;
	nx_uint16_t	soil_conductivity_1	:	12	;
	nx_uint16_t	soil_moisture_2		:	12	;
	nx_uint16_t	soil_temperature_2	:	12	;
	nx_uint16_t	soil_conductivity_2	:	12	;
	nx_uint16_t	soil_moisture_3		:	12	;
	nx_uint16_t	soil_temperature_3	:	12	;
	nx_uint16_t	soil_conductivity_3	:	12	;
} thurBoard1Data_t;

typedef nx_struct thurBoard2Data {
	nx_uint16_t	air_temperature				;
	nx_uint16_t	air_humidity		:	12	;
	nx_uint16_t	surface_temperature	:	12	;	/* Add 3730 to that value */
	nx_uint16_t	soil_moisture_1		:	12	;
	nx_uint16_t	soil_temperature_1	:	12	;
	nx_uint16_t	soil_conductivity_1	:	12	;
	nx_uint16_t	soil_moisture_2		:	12	;
	nx_uint16_t	soil_temperature_2	:	12	;
	nx_uint16_t	soil_conductivity_2	:	12	;
	nx_uint16_t	soil_moisture_3		:	12	;
	nx_uint16_t	soil_temperature_3	:	12	;
	nx_uint16_t	soil_conductivity_3	:	12	;
} thurBoard2Data_t;
