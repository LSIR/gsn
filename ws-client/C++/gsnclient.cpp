// gsnclient.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "soapH.h"
#include <Date.h>


using namespace std;


SOAP_NMAC struct Namespace namespaces[] =
{
	{"SOAP-ENV", "http://www.w3.org/2003/05/soap-envelope", "http://www.w3.org/2003/05/soap-envelope", NULL},
	{"SOAP-ENC", "http://www.w3.org/2003/05/soap-encoding", "http://www.w3.org/2003/05/soap-encoding", NULL},
	{"xsi", "http://www.w3.org/2001/XMLSchema-instance", "http://www.w3.org/*/XMLSchema-instance", NULL},
	{"xsd", "http://www.w3.org/2001/XMLSchema", "http://www.w3.org/*/XMLSchema", NULL},
	{"ns3", "http://datarequest.http.gsn/xsd", NULL, NULL},
	{"ns2", "http://standard.webservice.gsn/xsd", NULL, NULL},
	{"ns4", "http://standard.webservice.gsn/GSNWebServiceSoap12Binding", NULL, NULL},
	{"ns1", "http://standard.webservice.gsn", NULL, NULL},
	{"ns5", "http://standard.webservice.gsn/GSNWebServiceSoap11Binding", NULL, NULL},
	{NULL, NULL, NULL, NULL}
};

#define INT_MAX 2147483647
static const double plugin_nodata = -999.0;
static const string LATITUDE = "LATITUDE";
static const string LONGITUDE = "LONGITUDE";
static const string ALTITUDE = "ALTITUDE";

struct soap soap;

char* endpoint = "http://localhost:22001/services/GSNWebService";

// Begining of utility functions
string toUpper(string s) {
	for (int i =0; i<s.size();i++)
		s[i] = toupper(s[i]);
	return s;
}

double parse_double(string s) {
	cout << "[DEBUG] " << "'" << s << "'" << endl;

	if (toUpper(s).compare("NULL")==0)
		return plugin_nodata;
	else
		return atof(s.c_str());
}

// End of utility functions

/*
coordinates of a station
*/
class Coordinates {
public:
	double latitude;
	double longitude;
	double altitude;
	Coordinates() {
		latitude = plugin_nodata;
		longitude = plugin_nodata;
		altitude = plugin_nodata;
	}
	void setByString(const string predicateName, const string predicateValue) {

		if (toUpper(predicateName).compare(LATITUDE) == 0) {
			latitude = parse_double(predicateValue);
		}

		if (toUpper(predicateName).compare(LONGITUDE) == 0) {
			longitude = parse_double(predicateValue);
		}

		if (toUpper(predicateName).compare(ALTITUDE) == 0) {
			altitude = parse_double(predicateValue);
		}
	}
	void list() {
		cout << "lat: "
			<< latitude
			<< ", lon: "
			<< longitude
			<< ", alt: "
			<< altitude
			<< endl;
	}
};

class Sensor {
public:
	string field;
	string type;
};


class StreamElement {
public:
	Date timestamp;
	vector<double> data;
	void list() {
		cout << timestamp.getUnixDate() << ", ";
		for (int i=0;i<data.size();i++) {
			cout << data[i];
			if (i<data.size()-1) 
				cout << ", ";
		}
		cout << endl;
	}
};

class StationData {
public:
	vector<Sensor> format;
	vector<StreamElement> streams;
	void listFormat() {
		cout << "timestamp (long), ";
		for (int i=0;i<format.size();i++) {
			cout << format[i].field
				<< " ("
				<< format[i].type
				<< ")";
			if (i<format.size()-1)
				cout << ", ";
		}
		cout << endl;
	}
	void list() {
		listFormat();
		for (int i=0;i<streams.size();i++) {
			streams[i].list();
		}
	}
};



/*
Returns list of stations deployed on the GSN instance
*/
vector<string> listStations() {
	vector<string> _listStations;

	_ns1__listVirtualSensorNames listVirtualSensorNames;
	_ns1__listVirtualSensorNamesResponse listVirtualSensorNamesResponse;	

	if (soap_call___ns4__listVirtualSensorNames(&soap,endpoint,NULL,&listVirtualSensorNames,&listVirtualSensorNamesResponse) == SOAP_OK) {
		_listStations = listVirtualSensorNamesResponse.virtualSensorName;		
	}
	else
		soap_print_fault(&soap, stderr);

	return _listStations;
}
/*
Returns list of sensors for the given station
*/
vector<Sensor> getStationSensors(string station) {

	vector<Sensor> a_SensorStructure;

	_ns1__getVirtualSensorsDetails request;
	_ns1__getVirtualSensorsDetailsResponse response;

	ns2__GSNWebService_USCOREFieldSelector fieldSelector;
	fieldSelector.vsname = station;

	request.fieldSelector.push_back(&fieldSelector);

	request.detailsType.push_back(ns2__GSNWebService_USCOREDetailsType__OUTPUTSTRUCTURE);

	if (soap_call___ns4__getVirtualSensorsDetails(&soap, endpoint, NULL, &request, &response) == SOAP_OK) {

		ns2__GSNWebService_USCOREVirtualSensorDetails virtualSensorDetails = *response.virtualSensorDetails.at(0);
		int fields_size = virtualSensorDetails.outputStructure->fields.size();
		cout << fields_size;
		for (int i=0;i<fields_size; i++) {
			Sensor aSensor;
			aSensor.field = *virtualSensorDetails.outputStructure->fields.at(i)->name;
			aSensor.type = *virtualSensorDetails.outputStructure->fields.at(i)->type;

			a_SensorStructure.push_back(aSensor);
		}
	}
	else
		soap_print_fault(&soap, stderr);

	return a_SensorStructure;
}

/*
Returns coordinates of a station
*/
Coordinates getStationCoordinates(string station) {

	Coordinates coords;

	_ns1__getVirtualSensorsDetails request;
	_ns1__getVirtualSensorsDetailsResponse response;

	ns2__GSNWebService_USCOREFieldSelector fieldSelector;
	fieldSelector.vsname = station;

	request.fieldSelector.push_back(&fieldSelector);

	request.detailsType.push_back(ns2__GSNWebService_USCOREDetailsType__ADDRESSING);

	if (soap_call___ns4__getVirtualSensorsDetails(&soap, endpoint, NULL, &request, &response) == SOAP_OK) {
		ns2__GSNWebService_USCOREVirtualSensorDetails virtualSensorDetails = *response.virtualSensorDetails.at(0);
		int predicates_size = virtualSensorDetails.addressing->predicates.size();

		for (int i=0;i<predicates_size;i++) {
			coords.setByString(virtualSensorDetails.addressing->predicates.at(i)->name, virtualSensorDetails.addressing->predicates.at(i)->__item);
		}
	}
	else
		soap_print_fault(&soap, stderr);
	return coords;
}


/*
Reads station data fiven station name and time intervals
*/
StationData getStationData(string station, Date from, Date to, int nb=INT_MAX) {

	StationData stationData;

	LONG64 _from = from.getUnixDate()*1000; // GSN timestamps are expressed in millisecods
	LONG64 _to = to.getUnixDate()*1000; 

	_ns1__getMultiData request;
	_ns1__getMultiDataResponse response;

	ns2__GSNWebService_USCOREFieldSelector fieldSelector;
	fieldSelector.vsname = station;

	request.from = &_from;
	request.to = &_to;
	request.nb = &nb;
	request.fieldSelector.push_back(&fieldSelector);

	if (soap_call___ns4__getMultiData(&soap, endpoint, NULL, &request, &response) == SOAP_OK) {

		if (response.queryResult.at(0)->format == NULL) // no data to return
			return stationData;

		for (int k=0; k<response.queryResult.at(0)->format->field.size();k++) {
			Sensor aSensor;
			aSensor.field = *response.queryResult.at(0)->format->field.at(k)->name;
			aSensor.type = *response.queryResult.at(0)->format->field.at(k)->type;
			stationData.format.push_back(aSensor);
		}

		string executed_query = *response.queryResult.at(0)->executedQuery;
		bool multipage = response.queryResult.at(0)->hasNext;

		int n_tuples = response.queryResult.at(0)->streamElements.size();

		for (int i=0; i < response.queryResult.at(0)->streamElements.size();i++) {

			StreamElement aStreamElement; 

			aStreamElement.timestamp = atof (response.queryResult.at(0)->streamElements.at(i)->timed->c_str()) / 1000; // implicit ocnversion from double to long



			for (int j=0;j<response.queryResult.at(0)->streamElements.at(i)->field.size();j++) {

				aStreamElement.data.push_back(parse_double(response.queryResult.at(0)->streamElements.at(i)->field.at(j)->__item));

			}

			stationData.streams.push_back(aStreamElement);

		}

		cout << "\n[debug] Page 0 : "
			<< n_tuples 
			<< " elements read.\n";


		if (multipage) {

			string sid = response.queryResult.at(0)->sid;

			_ns1__getNextData requestNext;
			_ns1__getNextDataResponse responseNext;
			requestNext.sid = sid;

			bool still_pages = TRUE;
			int result = SOAP_OK;
			int page_number = 0;

			while(still_pages && result==SOAP_OK) { // loop through pages

				result = soap_call___ns4__getNextData(&soap, endpoint, NULL, &requestNext, &responseNext);

				if (result == SOAP_OK) {
					still_pages = responseNext.queryResult.at(0)->hasNext;
					sid = responseNext.queryResult.at(0)->sid;

					int n_tuples_in_newPage = responseNext.queryResult.at(0)->streamElements.size();

					for (int i=0; i < n_tuples_in_newPage;i++) {

						StreamElement aStreamElement;

						aStreamElement.timestamp = atof (responseNext.queryResult.at(0)->streamElements.at(i)->timed->c_str()) / 1000;


						for (int j=0;j<responseNext.queryResult.at(0)->streamElements.at(i)->field.size();j++) {

							aStreamElement.data.push_back(parse_double(responseNext.queryResult.at(0)->streamElements.at(i)->field.at(j)->__item));

						}


						stationData.streams.push_back(aStreamElement);

					}
					page_number++;

					cout << "[debug] Page "
						<< page_number
						<<" : " 
						<< n_tuples_in_newPage 
						<< " elements read.\n" 
						<< "[debug] "
						<< still_pages
						<< " more pages to read.\n";


					n_tuples += n_tuples_in_newPage;
				}
				else
					soap_print_fault(&soap, stderr);
			}
		}
	}
	else
		soap_print_fault(&soap, stderr);

	return stationData;

}

void listSensors(vector<string> listOfSensors) {
	for (int i = 0; i < listOfSensors.size(); i++) {
		cout << i 
			<< " : "
			<< listOfSensors[i] 
		<< endl;
	}
}

void list_SensorStructure(vector<Sensor> s) {
	for (int i = 0; i< s.size(); i++ ) {
		cout << i 
			<< " : "
			<< s[i].field
			<< " ("
			<< s[i].type
			<< ")"
			<< endl;
	}
}


int _tmain(int argc, _TCHAR* argv[])
{

	// initializing soap
	soap_init(&soap);

	// getting list of stations
	vector<string> stations = listStations();
	listSensors(stations);

	// getting station sensors
	vector<Sensor> stationSensors = getStationSensors("wan5");
	list_SensorStructure(stationSensors);

    // getting station coordinates
	Coordinates coords = getStationCoordinates("wan5");
	coords.list();

	Date from = Date(0);
	Date to   = Date(1296133200);

	// getting station data for specified interval
	StationData stationData = getStationData("wan2" , from, to, 100);
	stationData.list();

	return 0;
}

