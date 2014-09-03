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
	
	
	public static final Integer[] MOBILE_STATIONS = {};
	public static final Integer[] STATIC_STATIONS = {101};

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
					   BinaryParser parser;
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
								parser = new BinaryParser(input);
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
								p.setTimeStamp(p.getTimeStamp()+(c++)); //timestamp of those SE are a bit artificial but should guarantee ordering
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
								parser.resetChecksum();
								int type = parser.readNextChar(true);
								switch(type){
								case 36:
									int next = parser.readNextChar(false);
									int id = parser.readNextShort(false);
									logger.debug("received packet number :"+next);
									if (sd == null) sd = create(id);
									else if (sd.id != id)
										throw new IOException("received packet from "+id+", while listening to "+sd.id);
									sd.type = next;
									switch(next){
									case 0:
										sd.readGPS(parser);
										break;
									case 1:	
										sd.readODO(parser);
										break;
									case 2:	
										sd.readDoor(parser);
										break;
									case 3:	
										sd.readBus(parser);
										break;
									case 4:	
										sd.readNextStop(parser);
										break;
									case 5:	
										sd.readCurrentStop(parser);
										break;
									case 6:	
										sd.readAccel(parser);
										break;
									case 7:	
										sd.readSignalQ(parser);
										break;
									case 8:	
										sd.readDate(parser);
										break;
									case 9:	
										sd.readCO(parser);
										break;
									case 10:	
										sd.readOzone(parser);
										break;
									case 14:	
										sd.readAPM(parser);
										break;
									case 12:	
										sd.readFPM(parser);
										break;
									case 13:	
										sd.readFPH(parser);
										break;
									case 29:	
										sd.readSecCO(parser);
										break;
									case 30:	
										sd.readSecOzone(parser);
										break;
									default:
										throw new IOException("unknown packet: "+next);
									}
									pub ++;
									if (!parser.checkSum()){
										throw new IOException("Checksum error reading packet ("+next+") for station "+id);
									}
									buffer.add(sd.getStreamElement(ts));
									last = next;
									break;
								case 35:
									byte[] b = parser.readBytes(3);
									id = Integer.parseInt(new String(b));
									logger.debug("char:" + b[0] + ", "+ b[1] + ", "+ b[2]);
									if (sd == null) sd = create(id);
									else if (sd.id != id) logger.warn("received packet from "+id+", while listening to "+sd.id);
									next = parser.readNextChar(true);
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
									break;
								case 43:
									byte[] buf = parser.readBytes(2);
									if (new String(buf).equals("++")){
										logger.warn("closed (received "+ctr+" packets, dropped "+err+" packets, published "+pub+" packets)");
										connected = false;
									}
									break;
								default:
									err++;
									throw new IOException("unknown packet type:"+type);
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
		if (Helpers.contains(MOBILE_STATIONS, id))
			return new MobileStationData(id);
		if (Helpers.contains(STATIC_STATIONS, id))
			return new StaticStationData(id);
		else{
			logger.warn("Unknown station ID ("+id+"), assuming mobile.");
			return new MobileStationData(id);
		}
	}
	
	private abstract class StationData{
		int ms,ss,mn,hh,dd,mm,yy,id,type;
		byte[] payload;
		
		public StationData(int id){
			this.id = id;
		}

		public void readSecOzone(BinaryParser p) throws IOException {
			//LCSSC
			readTimeMsFromShort(p);
			payload = p.readBytes(5);

		}

		public void readSecCO(BinaryParser p) throws IOException {
			//LSSS
			readTimeMsFromShort(p);
			payload = p.readBytes(6);
		}

		public void readFPH(BinaryParser p) throws IOException {
			//CsSCCS
			readTimeMsFromShort(p);
			payload = p.readBytes(9);			
		}

		public void readFPM(BinaryParser p) throws IOException {
			//SLCSs
			readTimeMsFromShort(p);
			payload = p.readBytes(9);
		}

		public void readAPM(BinaryParser p) throws IOException {
			//SSSSC
			readTimeMsFromShort(p);
			payload = p.readBytes(4);
		}

		public void readOzone(BinaryParser p) throws IOException {
			//SSSC
			readTimeMsFromShort(p);
			payload = p.readBytes(5);
		}

		public void readCO(BinaryParser p) throws IOException {
			//SSSS
			readTimeMsFromShort(p);
			payload = p.readBytes(6);
		}

		public void readSignalQ(BinaryParser p) throws IOException {
			payload = p.readBytes(3);
		}

		public void readAccel(BinaryParser p) throws IOException {
			// Ssss
			readTimeMsFromShort(p);
			payload = p.readBytes(6);
		}

		public void readCurrentStop(BinaryParser p) throws IOException {
			// SCCstring
			readTimeSsFromShort(p);
			payload = p.readNextString(20).getBytes();
		}

		public void readNextStop(BinaryParser p) throws IOException {
			// SCCstring
			readTimeSsFromShort(p);
			payload = p.readNextString(20).getBytes();
		}

		public void readBus(BinaryParser p) throws IOException {
			// SCCstring
			readTimeSsFromShort(p);
			int tl_line = p.readNextChar(false);
			String tl_destination = p.readNextString(20);
			payload = (tl_line + "," + tl_destination).getBytes();
		}

		public void readDoor(BinaryParser p) throws IOException {
			// SC
			readTimeSsFromShort(p);
			payload = p.readBytes(1);
		}

		public void readODO(BinaryParser p) throws IOException {
			// LS
			readTimeFromLong(p);
			payload = p.readBytes(2);
		}

		public void readGPS(BinaryParser p) throws IOException {
			// LLLSLCSL
			long time = p.readNextLong(false); //not same time format
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
			payload = p.readBytes(21);
		}

		public void readDate(BinaryParser p) throws IOException{
			//cccccc
			
			int _ss = p.readNextChar(false);
			int _mn = p.readNextChar(false);
			int _hh = p.readNextChar(false);
			int _dd = p.readNextChar(false);
			int _mm = p.readNextChar(false);
			int _yy = p.readNextChar(false);
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
		
		protected void readTimeFromLong(BinaryParser p) throws IOException{
			long time = p.readNextLong(false);
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
		
		protected void readTimeMsFromShort(BinaryParser p) throws IOException{
			int time = p.readNextShort(false);
			int _ss = time/100;
			int _ms = (time%100)*10;
			if ( _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+mn+":"+_ss+")");
			}
			ss = _ss;
			ms = _ms;
		}
		
		protected void readTimeSsFromShort(BinaryParser p) throws IOException{
			int time = p.readNextShort(false);
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
	
	private class MobileStationData extends StationData{
		
		public MobileStationData(int id){
			super(id);
		}

	}

	private class StaticStationData extends StationData{
		
		public StaticStationData(int id){
			super(id);
		}
		
		public void readGPS(BinaryParser p) throws IOException {
			// LLLSCS
			long time = p.readNextLong(false); //not same time format
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
			payload = p.readBytes(13);
		}
		
		public void readCO(BinaryParser p) throws IOException {
			//SSSS
			readTimeFromLong(p);
			payload = p.readBytes(6);
		}
		
		public void readOzone(BinaryParser p) throws IOException {
			//SSSC
			readTimeFromLong(p);
			payload = p.readBytes(5);
		}
		
		public void readFPM(BinaryParser p) throws IOException {
			//SLCSs
			readTimeFromLong(p);
			payload = p.readBytes(9);
		}
		
		public void readFPH(BinaryParser p) throws IOException {
			//CsSCCS
			readTimeFromLong(p);
			payload = p.readBytes(9);			
		}
		
		public void readAPM(BinaryParser p) throws IOException {
			//L
			readTimeFromLong(p);
			payload = p.readBytes(4);
		}
		
		public void readSecCO(BinaryParser p) throws IOException {
			//LSSS
			readTimeFromLong(p);
			payload = p.readBytes(6);
		}
		
		public void readSecOzone(BinaryParser p) throws IOException {
			//LCSSC
			readTimeFromLong(p);
			payload = p.readBytes(5);

		}

	}
	
}

