package gsn.http.restapi;

import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.data.format.CsvSerializer;
import gsn.data.format.JsonSerializer;
import gsn.data.SensorData;
import gsn.Main;
import gsn.Mappings;
import gsn.http.ac.User;
import gsn.http.ac.UserUtils;
import gsn.utils.geo.GridTools;
import gsn.xpr.XprConditions;
import static scala.collection.JavaConversions.*;
import scala.collection.Seq;
import scala.collection.mutable.Buffer;
import scala.util.Try;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import gsn.data.DataStore;



@Path("/")
public class RestServices {

	private static final transient Logger logger = LoggerFactory.getLogger(RestServices.class);
	private static final String PARAM_FORMAT = "format";
	private static final String PARAM_ATTACH = "attach";
	private static final String PARAM_USERNAME = "username";
	private static final String PARAM_PASSWORD = "password";
	private static final String PARAM_DATE = "date";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_TO = "to";
	private static final String PARAM_SIZE = "size";
	private static final String PARAM_LATEST_VALS = "latest_values";
	private static final String PARAM_FILTER = "filter";
	private static final String PARAM_FIELDS = "fields";
	private static final String PARAM_TIMEFORMAT = "time_format";

	public static final String FORMAT_JSON = "json";
	public static final String FORMAT_CSV = "csv";
	public static final String FORMAT_GEOJSON = "geojson";
	public static final String FORMAT_NETCDF = "netcdf";

	public static final String FORMAT_UNIX = "unix";
	public static final String FORMAT_ISO8601 = "iso8601";

	public static final String MEDIA_TYPE_CSV = "text/csv";
	public static final String MEDIA_TYPE_NETCDF = "application/netcdf";

	public static final String RESPONSE_HEADER_CONTENT_DISPOSITION_NAME = "Content-Disposition";
	public static final String RESPONSE_HEADER_CONTENT_DISPOSITION_VALUE = "attachment;filename=\"%s\"";

	@QueryParam(PARAM_USERNAME)
	String username;
	@QueryParam(PARAM_PASSWORD)
	String pass;
	@DefaultValue(FORMAT_GEOJSON)
	@QueryParam(PARAM_FORMAT)
	String format;
	@DefaultValue("false")
	@QueryParam(PARAM_LATEST_VALS)
	boolean latestValues;
	@DefaultValue(FORMAT_UNIX)
	@QueryParam(PARAM_TIMEFORMAT)
	boolean timeFormat;

	@DefaultValue("false")
	@QueryParam(PARAM_ATTACH)
	boolean attach;

	private String[] validFormats = new String[] { 
			FORMAT_GEOJSON, FORMAT_JSON,
			FORMAT_CSV, FORMAT_NETCDF };
	private Config config = ConfigFactory.load();
	DateFormat dateFormat = new SimpleDateFormat(config.getString("gsn.dateFormat"));
	Buffer<String> defaultFields = asScalaBuffer(config.getStringList("gsn.metadata.defaultFields"));
	
	
	DataStore ds = new DataStore(gsn.Main.getInstance().getGsnConf());
	
	@GET
	@Path("/sensors")
	public Response sensors() {
		validateFormat();
		return null;
		//if (latestValues)
		//return response(ds.allSensorsLatest(),true).build();
		//else
		//return response(ds.allSensors(),false).build();
	}

	private String toString(StringWriter sw) {
		String res = sw.toString();
		try {
			sw.close();
		} catch (IOException e) {
			throw exception("Error producing csv output: " + e.getMessage());
		}
		return res;
	}

	private ResponseBuilder response(Object body) {
		String datetime = dateFormat.format(Calendar.getInstance().getTime());
		String ext = null;
		ResponseBuilder resp = null;
		if (FORMAT_CSV.equals(format)) {
			resp = Response.ok(body);
			resp.type("text/plain");//MEDIA_TYPE_CSV);
			ext = ".csv";
		} else if (FORMAT_JSON.equals(format) || FORMAT_GEOJSON.equals(format)) {
			resp = Response.ok(body);
			resp.type(MediaType.APPLICATION_JSON);
			ext = ".json";
		} else if (format.equals(FORMAT_NETCDF)) {
			throw exception("NetCDF is not a valid format for retrieving multiple sensors");
		}
		if (attach) {
			String resultname = String.format("multiple_sensors_%s", datetime);
			resp.header(RESPONSE_HEADER_CONTENT_DISPOSITION_NAME, String
					.format(RESPONSE_HEADER_CONTENT_DISPOSITION_VALUE,
							resultname + ext));
		}

		return resp;
	}

	private ResponseBuilder response(SensorData sensor,boolean latest) {
		Object body=null;
		if (FORMAT_CSV.equals(format)) {
			body =CsvSerializer.ser(sensor,defaultFields,latest);
		} else if (FORMAT_JSON.equals(format) || FORMAT_GEOJSON.equals(format)) {
			body = JsonSerializer.ser(sensor,defaultFields,latest);
		} else if (format.equals(FORMAT_NETCDF)) {
			throw exception("NetCDF is not a valid format for retrieving multiple sensors");
		}		
		return response(body);
	}
	
	private ResponseBuilder response(Seq<SensorData> sensors,boolean latest) {
		Object body=null;
		if (FORMAT_CSV.equals(format)) {
			body =CsvSerializer.ser(sensors,defaultFields,latest);
		} else if (FORMAT_JSON.equals(format) || FORMAT_GEOJSON.equals(format)) {
			body = (JsonSerializer.ser(sensors,defaultFields,latest));
		} else if (format.equals(FORMAT_NETCDF)) {
			throw exception("NetCDF is not a valid format for retrieving multiple sensors");
		}		
		return response(body);
	}

	private void validateFormat() {
		boolean exists = false;
		for (String f : validFormats) {
			if (format.equals(f))
				exists = true;
		}
		if (!exists)
			throw exception("Invalid format: " + format);
	}

	protected WebApplicationException exception(String text) {
		String json="{\"error\": "+Status.BAD_REQUEST.name()+", \"message\":"+text+"}";
		return new WebApplicationException(Response.status(Status.BAD_REQUEST)
				.entity(json).type(MediaType.APPLICATION_JSON).build());
	}

	private void authenticate() {
		if (Main.getContainerConfig().isAcEnabled()) {
			logger.debug("Access control is enabled.");
			User user = null;
			if ((username != null) && (pass != null)) {
				logger.debug("Login for user: " + username);
				user = UserUtils.allowUserToLogin(username, pass);
				if (user == null)
					throw exception("Invalid username or password.");
			} else
				throw exception("Missing mandatory username and password parameters.");
		}
	}

	private void authorize(String sensorid) {
		if (!Main.getContainerConfig().isAcEnabled()) return;
		if (!UserUtils.userHasAccessToVirtualSensor(username, pass, sensorid))
			throw exception("User " + username
					+ " not authorized for virtual sensor " + sensorid);
	}

	@GET
	@Path("/sensors/{sensorid}")
	public Response sensor(@PathParam("sensorid") String vsname,
			@QueryParam(PARAM_FROM) String from,
			@QueryParam(PARAM_TO) String to,
			@DefaultValue("-1") @QueryParam(PARAM_SIZE) int size,
			@QueryParam(PARAM_FIELDS) String fields,
			@QueryParam(PARAM_FILTER) String filter) {
		logger.debug("REST service requesting vs: "+vsname);
		authenticate();
		authorize(vsname);
		validateFormat();

		//DataStoreBis ds = new DataStoreBis();
		VSensorConfig sensorConfig = Mappings.getConfig(vsname);
		logger.debug("Retrieving metadata for vs: "+sensorConfig.getName());
		
		ArrayList<String> allfields = new ArrayList<String>();
		for (DataField df : sensorConfig.getOutputStructure()) {
			allfields.add(df.getName().toLowerCase());
		}
		
        ArrayList<String> filters=new ArrayList<String>();
        if (filter!=null)
          filters.addAll(Arrays.asList(filter.split(",")));
		long fromTime = -1;
		long toTime = -1;
		try {
			if (from!=null)
				fromTime = dateFormat.parse(from).getTime();
			if (to!=null)
				toTime = dateFormat.parse(to).getTime();
		} catch (ParseException e) {
			throw exception("Invalid from-to date parameters: " + from + " - " + to);
		}
		if (fromTime>0)
			filters.add("timed>="+fromTime);
		if (toTime>0)
			filters.add("timed<="+toTime);

		String[] fieldNames = null;
		if (fields != null) {
			fieldNames = fields.toLowerCase().split(",");
			for (String f : fieldNames) {
				if (!allfields.contains(f)) {
					throw exception("Invalid field name in selection: " + f);
				}
			}
		} else {
			fieldNames = allfields.toArray(new String[] {});
		}

		String[] conditionList = null;
		if (filter != null) {
			Try<String[]> conditions = XprConditions.serializeConditions(filters.toArray(new String[]{}));
			if (conditions.isFailure()) {
				logger.error(conditions.failed().toString(), conditions.failed().get());
				throw exception("Invalid filter condition " + filter);
			} else {
				conditionList = conditions.get();
			}
		}
		scala.Option<Integer>optSize=scala.Option.apply(null);
		if (size>0)
			optSize=scala.Option.apply((Integer)size);
		//SensorData s=ds.sensorData(vsname, fieldNames, conditionList, optSize).get();
		//System.out.println(s);
		return null;
		//return response(s,true).build();
	}

	@GET
	@Path("/grid/{sensorid}")
	public Response grid(@PathParam("sensorid") String vsname,
			@QueryParam(PARAM_DATE) String date) {

		long timestamp = 0;
		try {
			timestamp = dateFormat.parse(date).getTime();
		} catch (ParseException e) {
			throw exception("Invalid date parameter: " + date);
		}
		String resp = GridTools.executeQueryForGridAsJSON(vsname, timestamp);
		return Response.ok(resp).type(MediaType.APPLICATION_JSON).build();
	}

	
	
}


