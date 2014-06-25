package gsn.wrappers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import javax.naming.OperationNotSupportedException;
import javax.net.ServerSocketFactory;
import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.BinaryParser;
import gsn.utils.Helpers;

public class OpensenseConnectorWrapper extends AbstractWrapper {
	
	
	public static final Integer[] TEST_STATIONS = {};
	public static final Integer[] CALIBRATION_STATIONS = {};
	public static final Integer[] OPERATIONAL_STATIONS = {};
	public static final Integer[] STATIC_STATIONS = {};
	public static final Integer[] DOUBLE_STATIONS = {};
	
	private final transient Logger logger = Logger.getLogger( OpensenseConnectorWrapper.class );
	
	private HashMap<Integer,byte[]> messages = new HashMap<Integer,byte[]>();
	
	private ServerSocket socket;
	private boolean running = true;
	private int port;
	private DataField[] df;
	
	@Override
	public DataField[] getOutputFormat() {
		return df;
	}

	@Override
	public boolean initialize() {
		df = new DataField[]{
				new DataField("timestamp","bigint"),
				new DataField("station","smallint"),
				new DataField("type","smallint"),
				new DataField("payload","BINARY")};

		port = getActiveAddressBean().getPredicateValueAsInt("port", 22025);
		
		try {
			ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
            socket = serverSocketFactory.createServerSocket(port);
			socket.setSoTimeout(1000);
		}catch(Exception e){
			logger.error("unable to open socket",e);
			return false;
		}
		return true;
	}

	@Override
	public void dispose() {
		try {
			socket.close();
			running = false;
		}catch(Exception e){}
	}

	@Override
	public String getWrapperName() {
		return "opensense connector";
	}
	
	@Override
	public boolean isTimeStampUnique() {
		return false;
	}
	
	@Override
	public boolean sendToWrapper(String action, String[] paramNames,
			Object[] paramValues) throws OperationNotSupportedException
		{
		try{
			if (action.equals("send")){
				int id=0;
				String command="";
			    for (int i = 0;i<paramNames.length;i++){
			    	if (paramNames[i].equals("id")){
			    		id = Integer.parseInt((String) paramValues[i]);
			    	}else if(paramNames[i].equals("command")){
			    		command = (String) paramValues[i];
			    	}
			    }
			    if (id == 0 || command.equals("")){
			    	return false;
			    }else{
			        messages.put(id,command.getBytes());
			    }
			}else if (action.equals("clear")){
				messages.clear();
			}
		}catch(Exception e){
			logger.error("Unable to parse command",e);
			return false;
		}
		return true;
	}
	
	@Override
	public void run(){
	   
		   while (running){
			   try {
			       final Socket server = socket.accept();
			       logger.warn("accepted from "+server.getInetAddress());
                   Thread t = new Thread(new Runnable(){ 
                	   
					   BufferedInputStream input;
					   BufferedOutputStream output;
					   boolean connected = true;
					   StationData sd;
					   int retry = 0;
					   long lastRetry = 0;
					   long ts;
					   int ctr = 0;
					   int err = 0;
					   int pub = 0;
					   int last = 0;
					   PriorityQueue<StreamElement> buffer = new PriorityQueue<StreamElement>(3000,new Comparator<StreamElement>() {
							@Override
							public int compare(StreamElement o1, StreamElement o2) {
								int diff = (int) (((Long)o1.getData("timestamp")) - ((Long)o2.getData("timestamp")));
								return diff;
							}
						});

						@Override
						public void run() {
							try{
								input = new BufferedInputStream(server.getInputStream());
								output = new BufferedOutputStream(server.getOutputStream());
								ts = System.currentTimeMillis();
								while(connected) parse();
								server.close();
							}catch (IOException ioe){
								logger.error("Error while connecting to remote station: " + server.getInetAddress(), ioe);
							}
							//publish data even if connection got interrupted as it may be erased on the logger side
							int c = 0;
							while (!buffer.isEmpty()){
								StreamElement p = buffer.poll();
								p.setTimeStamp(p.getTimeStamp()+(c++));
								postStreamElement(p);
							}
						}
				
						public void parse(){
							try{
								ctr ++;
								if (retry > 0 && System.currentTimeMillis() - lastRetry > 1000) 
								{
									if (messages.containsKey(sd.id)){
										output.write(messages.get(sd.id));
									}
									output.write(("close "+sd.id).getBytes());
									retry--;
									lastRetry = System.currentTimeMillis();
								}
								int type = BinaryParser.readNextChar(input,true);
								switch(type){
								case 36:
									int next = BinaryParser.readNextChar(input,false);
									int id = BinaryParser.readNextShort(input,false);
									logger.debug("received packet number :"+next);
									if (sd == null) sd = create(id);
									else if (sd.id != id)
										throw new IOException("received packet from "+id+", while listening to "+sd.id);
									sd.type = next;
									switch(next){
									case 0:
										sd.readGPS(input);
										break;
									case 1:	
										sd.readODO(input);
										break;
									case 2:	
										sd.readDoor(input);
										break;
									case 3:	
										sd.readBus(input);
										break;
									case 4:	
										sd.readNextStop(input);
										break;
									case 5:	
										sd.readCurrentStop(input);
										break;
									case 6:	
										sd.readAccel(input);
										break;
									case 7:	
										sd.readSignalQ(input);
										break;
									case 8:	
										sd.readDate(input);
										break;
									case 9:	
										sd.readCO(input);
										break;
									case 10:	
										sd.readOzone(input);
										break;
									case 11:	
										sd.readMicsCO(input);
										break;
									case 12:	
										sd.readFPM(input);
										break;
									case 13:	
										sd.readFPH(input);
										break;
									case 29:	
										sd.readSecCO(input);
										break;
									case 30:	
										sd.readSecOzone(input);
										break;
									case 31:	
										sd.readSecMicsCO(input);
										break;
									default:
										throw new IOException("unknown packet: "+next);
									}
									pub ++;
									buffer.add(sd.getStreamElement(ts));
									last = next;
									break;
								case 35:
									byte[] b = new byte[3];
									id = b[0];
									input.read(b);
									logger.debug("char:" + b[0] + ", "+ b[1] + ", "+ b[2]);
									if (sd == null) sd = create(id);
									else if (sd.id != id) logger.warn("received packet from "+id+", while listening to "+sd.id);
									next = BinaryParser.readNextChar(input, true);
									switch(next){
									case 65:
										retry = 0;
										messages.remove(id);
									    break;
									case 82:
										output.write(("telnet_set "+id+"\r\n").getBytes());
										retry = 3;
										lastRetry = 0; // send immediately after
										break;
									default:
										throw new IOException("unknown command:"+next);
									}
								case 43:
									byte[] buf = new byte[2];
									input.read(buf);
									if (new String(buf).equals("++")){
										logger.warn("closed (received "+ctr+" packets, dropped "+err+" packets, published "+pub+" packets)");
										connected = false;
									}
									break;
								default:
									err++;
									//throw new IOException("unknown packet type:"+type);
								}
							}catch(IOException e){
								err ++;
								logger.warn("packet reading error [last:"+last+", ctr:"+ctr+"]",e);
							}
						}
					   
					   });
               t.setName("Opensense-connector");
			   t.start();
			   } catch(SocketTimeoutException ste){
				   continue;
			   } catch(IOException ioe){
				   logger.error("Error while connecting to some remote station.", ioe);
			   }
			    
		   }
		
	}
	
	
	private StationData create(Integer id){
		if (Helpers.contains(TEST_STATIONS, id))
			return new TestStationData(id);
		else if (Helpers.contains(OPERATIONAL_STATIONS, id))
			return new OperationStationData(id);
		else if (Helpers.contains(CALIBRATION_STATIONS, id))
			return new CalibrationStationData(id);
		if (Helpers.contains(STATIC_STATIONS, id))
			return new StaticStationData(id);
		if (Helpers.contains(DOUBLE_STATIONS, id))
			return new DoubleStationData(id);
		else{
			logger.warn("Unknown station ID ("+id+"), assuming operational.");
			return new OperationStationData(id);
		}
	}
	
	private abstract class StationData{
		int ms,ss,mn,hh,dd,mm,yy,id,type;
		byte[] payload;
		
		public StationData(int id){
			this.id = id;
		}
		
		public void readSecMicsCO(BufferedInputStream input) throws IOException {
			//LSSSC
			readTimeFromLong(input);
			payload = new byte[7];
			input.read(payload);
		}

		public void readSecOzone(BufferedInputStream input) throws IOException {
			//LCSSC
			readTimeFromLong(input);
			payload = new byte[6];
			input.read(payload);
		}

		public void readSecCO(BufferedInputStream input) throws IOException {
			//LSSS
			readTimeFromLong(input);
			payload = new byte[6];
			input.read(payload);
		}

		public void readFPH(BufferedInputStream input) throws IOException {
			//CsSCCS
			int _ss = BinaryParser.readNextChar(input, false);
			if ( _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+mn+":"+_ss+")");
			}
			ss = _ss;
			ms = 0;
			payload = new byte[8];
			input.read(payload);			
		}

		public void readFPM(BufferedInputStream input) throws IOException {
			//SLCSs
			readTimeMsFromShort(input);
			payload = new byte[9];
			input.read(payload);
		}

		public void readMicsCO(BufferedInputStream input) throws IOException {
			//SSSSC
			readTimeMsFromShort(input);
			payload = new byte[7];
			input.read(payload);
		}

		public void readOzone(BufferedInputStream input) throws IOException {
			//SCSSC
			readTimeMsFromShort(input);
			payload = new byte[6];
			input.read(payload);
		}

		public void readCO(BufferedInputStream input) throws IOException {
			//SSSS
			readTimeMsFromShort(input);
			payload = new byte[6];
			input.read(payload);
		}

		public void readSignalQ(BufferedInputStream input) throws IOException {
			payload = new byte[3];
			input.read(payload);
		}

		public void readAccel(BufferedInputStream input) throws IOException {
			// Ssss
			readTimeMsFromShort(input);
			payload = new byte[6];
			input.read(payload);
		}

		public void readCurrentStop(BufferedInputStream input) throws IOException {
			// SCCstring
			readTimeSsFromShort(input);
			payload = BinaryParser.readNextString(input,20).getBytes();
		}

		public void readNextStop(BufferedInputStream input) throws IOException {
			// SCCstring
			readTimeSsFromShort(input);
			payload = BinaryParser.readNextString(input,20).getBytes();
		}

		public void readBus(BufferedInputStream input) throws IOException {
			// SCCstring
			readTimeSsFromShort(input);
			int tl_line = BinaryParser.readNextChar(input, false);
			String tl_destination = BinaryParser.readNextString(input,20);
			payload = (tl_line + "," + tl_destination).getBytes();
		}

		public void readDoor(BufferedInputStream input) throws IOException {
			// SC
			readTimeSsFromShort(input);
			payload = new byte[1];
			input.read(payload);
		}

		public void readODO(BufferedInputStream input) throws IOException {
			// LS
			readTimeFromLong(input);
			payload = new byte[2];
			input.read(payload);
		}

		public void readGPS(BufferedInputStream input) throws IOException {
			// LLLSSLCSLs
			long time = BinaryParser.readNextLong(input, false); //not same time format
			int _hh = (int) (time / 10000);
			int _ss = (int) (time % 100);
			int _mn = (int) ((time - _hh*10000 - _ss)/100);
			if (_hh < 0 || _hh > 24 || _mn < 0 || _mn > 59 || _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+_hh+":"+_mn+":"+_ss+")");
			}
			ms = 0;
			ss = _ss;
			mn = _mn;
			hh = _hh;
			payload = new byte[25];
			input.read(payload);
		}

		public void readDate(InputStream input) throws IOException{
			//cccccc
			
			int _ss = BinaryParser.readNextChar(input, false);
			int _mn = BinaryParser.readNextChar(input, false);
			int _hh = BinaryParser.readNextChar(input, false);
			int _dd = BinaryParser.readNextChar(input, false);
			int _mm = BinaryParser.readNextChar(input, false);
			int _yy = BinaryParser.readNextChar(input, false);
			if (_mm <= 0 || _yy < 12 || _dd <= 0 || _mm > 12 || _dd > 31 || _hh < 0 || _hh > 24 || _mn < 0 || _mn > 59 || _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+_yy+"-"+_mm+"-"+_dd+" "+_hh+":"+_mn+":"+_ss+")");
			}
			ms = 0;
			ss = _ss;
			mn = _mn;
			hh = _hh;
			dd = _dd;
			mm = _mm;
			yy = _yy;
			payload = new byte[0];

		}
		
		protected void readTimeFromLong(InputStream input) throws IOException{
			long time = BinaryParser.readNextLong(input, false);
			int _hh = (int) (time/1000000);
			int _mn = (int) ((time - _hh*1000000)/10000);
			int _ss = (int) ((time - _hh*1000000 - _mn*10000)/100);
			int _ms = (int) ((time%100)*10);
			if (_hh < 0 || _hh > 24 || _mn < 0 || _mn > 59 || _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+_hh+":"+_mn+":"+_ss+")");
			}
			ms = 0;
			ss = _ss;
			mn = _mn;
			hh = _hh;
			ms = _ms;
		}
		
		protected void readTimeMsFromShort(InputStream input) throws IOException{
			int time = BinaryParser.readNextShort(input, false);
			int _ss = time/100;
			int _ms = (time%100)*10;
			if ( _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+mn+":"+_ss+")");
			}
			ss = _ss;
			ms = _ms;
		}
		
		protected void readTimeSsFromShort(InputStream input) throws IOException{
			int time = BinaryParser.readNextShort(input, false);
			int _mn = time/100;
			int _ss = time%100;
			if ( _mn < 0 || _mn > 59 || _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+_mn+":"+_ss+")");
			}
			ms = 0;
			ss = _ss;
			mn = _mn;
		}
		
		public StreamElement getStreamElement(long ts){
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(yy + 2000, mm - 1, dd, hh, mn, ss);
			long time = c.getTimeInMillis() + ms;
			
			Serializable[] data = new Serializable[]{time, new Short((short) id), new Short((short) type), payload};
			
			return new StreamElement(OpensenseConnectorWrapper.this.df,data,ts);
		}

	}
	
	private class TestStationData extends StationData{
		
		public TestStationData(int id){
			super(id);
		}

	}

	private class OperationStationData extends StationData{
		
		public OperationStationData(int id){
			super(id);
		}
			
	}
		
	
	private class CalibrationStationData extends StationData{
		
		public CalibrationStationData(int id){
			super(id);
		}
					
	}
	
	private class StaticStationData extends StationData{
		
		public StaticStationData(int id){
			super(id);
		}

	}
	
	private class DoubleStationData extends StationData{
		
		public DoubleStationData(int id){
			super(id);
		}

	}
	
}

