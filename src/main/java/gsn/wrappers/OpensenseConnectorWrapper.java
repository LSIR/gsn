package gsn.wrappers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.OperationNotSupportedException;
import javax.net.ServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.BinaryParser;
import gsn.utils.Helpers;

public class OpensenseConnectorWrapper extends AbstractWrapper {
	
	
	public static final Integer[] MOBILE_STATIONS = {41,43,45,47,48,49,50,51,54,55,192};
	public static final Integer[] STATIC_STATIONS = {101,102};
	
	private final ReentrantLock timerLock = new ReentrantLock(true);
	private static final Object tokenLock = new Object();
	private static Long timer = System.currentTimeMillis();

	private Long timeWaiting = 0L;
	private Long timeReceiving = 0L;
	private Long timeProcessing = 0L;
	private Long connectionCount = 0L;
	
	private final transient Logger logger = LoggerFactory.getLogger( OpensenseConnectorWrapper.class );
	
	private HashMap<Integer,byte[]> messages = new HashMap<Integer,byte[]>();
	
	private ServerSocket socket;
	private boolean running = true;
	private int port;
	private DataField[] df;
	private int token = 0;
	
	@Override
	public Hashtable<String,Object> getStatistics() {
		Hashtable<String, Object> stat = super.getStatistics();
		stat.put("vs."+getActiveAddressBean().getVirtualSensorName().replaceAll("\\.", "_")+".input."+ getActiveAddressBean().getInputStreamName().replaceAll("\\.", "_") +".waitingTime.counter", timeWaiting);
		stat.put("vs."+getActiveAddressBean().getVirtualSensorName().replaceAll("\\.", "_")+".input."+ getActiveAddressBean().getInputStreamName().replaceAll("\\.", "_") +".receivingTime.counter", timeReceiving);
		stat.put("vs."+getActiveAddressBean().getVirtualSensorName().replaceAll("\\.", "_")+".input."+ getActiveAddressBean().getInputStreamName().replaceAll("\\.", "_") +".processingTime.counter", timeProcessing);
		stat.put("vs."+getActiveAddressBean().getVirtualSensorName().replaceAll("\\.", "_")+".input."+ getActiveAddressBean().getInputStreamName().replaceAll("\\.", "_") +".connection.counter", connectionCount);
		return stat;
	}
	
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
			running = false;
			Thread.sleep(1000);
			socket.close();
			boolean done = false;
			while (! done){
			synchronized (tokenLock) {
				  done = token <= 0;	
				}
			Thread.sleep(100);
			}
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
				boolean force = false;
			    for (int i = 0;i<paramNames.length;i++){
			    	if (paramNames[i].equals("id")){
			    		id = Integer.parseInt((String) paramValues[i]);
			    	}else if(paramNames[i].equals("command")){
			    		command = (String) paramValues[i];
			    	}else if(paramNames[i].equals("force")){
			    		force = ((String) paramValues[i]).equalsIgnoreCase("ok");
			    	}
			    }
			    if (id == 0 || command.equals("")){
			    	return false;
			    }else{
			    	if (force){
			    		id += 9000;
			    	}
			        messages.put(id,command.getBytes());
			        logger.info("added message ("+id+"): "+command);
			    }
			}else if (action.equals("clear")){
				messages.clear();
				logger.info("cleared messages");
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
			       server.setSoTimeout(60000);
			       connectionCount = connectionCount == Long.MAX_VALUE ? 0 : connectionCount + 1;
			       logger.debug("accepted from "+server.getInetAddress());
                   Thread t = new Thread(new Runnable(){ 
                	   
                	   Long startTime = System.currentTimeMillis();
                	   
					   BufferedInputStream input;
					   BufferedOutputStream output;
					   BinaryParser parser;
					   boolean connected = true;
					   StationData sd;
					   boolean resync = false;
					   int retry = 0;
					   long lastRetry = 0;
					   int ctr = 0;
					   int err = 0;
					   int pub = 0;
					   int last = 0;
					   String bye = "";
					   PriorityQueue<StreamElement> buffer = new PriorityQueue<StreamElement>(3000,new Comparator<StreamElement>() {
							@Override
							public int compare(StreamElement o1, StreamElement o2) {
								int diff = (int) (((Long)o1.getData("timestamp")) - ((Long)o2.getData("timestamp")));
								return diff;
							}
						});

						@Override
						public void run() {
							synchronized (tokenLock) {
							  token++;	
							}
							try{
								input = new BufferedInputStream(server.getInputStream());
								output = new BufferedOutputStream(server.getOutputStream());
								FileOutputStream fos = new FileOutputStream("logs/"+System.currentTimeMillis()+".dat");
								parser = new BinaryParser(input,fos);
								while(connected && ! server.isClosed()) parse();
								fos.close();
								server.close();
								timeReceiving += (System.currentTimeMillis()-startTime)/1000;
								startTime = System.currentTimeMillis();
							}catch (IOException ioe){
								logger.error("Error while connecting to remote station: " + server.getInetAddress(), ioe);
							}
							if (sd == null){
								logger.info("node unknown, ip "+server.getInetAddress()+", parse loop "+ctr+", dropped bytes "+err+", published packets "+pub+", bye:"+bye);
							}else{
								logger.info("node "+sd.id+", ip "+server.getInetAddress()+", parse loop "+ctr+", dropped bytes "+err+", published packets "+pub+", bye:"+bye);
							}
							
							//publish data even if connection got interrupted as it may be erased on the logger side
							
							timerLock.lock();
							try{
								timeWaiting += (System.currentTimeMillis()-startTime)/1000;
								startTime = System.currentTimeMillis();
								timer = Math.max(timer,System.currentTimeMillis());
								while (!buffer.isEmpty()){
									StreamElement p = buffer.poll();
									p.setTimeStamp(++timer); //timestamp of those SE are a bit artificial but should guarantee ordering
									postStreamElement(p);
								}
								try {
									Thread.sleep(3000); //guarantee that there is at least 2s between the generation of the groups of stream elements (< 2000 SE)
								} catch (InterruptedException e) {} 
							}finally {
							       timerLock.unlock();
						    }
							synchronized (tokenLock) {
								token--;
							}
							timeProcessing += (System.currentTimeMillis()-startTime)/1000;
						}
				
						public void parse(){
							try{
								ctr ++;
								if (sd != null && messages.containsKey(sd.id+9000)){ //forced messages
									logger.info("writing (forced) '"+new String(messages.get(sd.id+9000))+"' to "+sd.id);
									output.write(messages.get(sd.id+9000));
									output.write("\r\n".getBytes());
									output.flush();
									messages.remove(sd.id+9000);
								}
								if (retry > 0 && System.currentTimeMillis() - lastRetry > 1000) 
								{
									output.write(("close "+sd.id+"\r\n").getBytes());
									output.flush();
									retry--;
									lastRetry = System.currentTimeMillis();
								}
								parser.resetChecksum();
								int type = parser.readNextChar(true);
								switch(type){
								case 36:
									int next = parser.readNextChar(false);
									int id = parser.readNextShort(false);
									if (sd == null) sd = create(id);
									else if (sd.id != id){
										throw new IOException("received packet from "+id+", while listening to "+sd.id);
									}
									sd.type = next;
									sd.resetTime();
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
									buffer.add(sd.getStreamElement());
									last = next;
									resync = false;
									break;
								case 35:
									byte[] b = parser.readBytes(3);
									id = Integer.parseInt(new String(b));
									if (sd == null) sd = create(id);
									else if (sd.id != id) logger.warn("received packet from "+id+", while listening to "+sd.id);
									next = parser.readNextChar(true);
									switch(next){
									case 65:
										logger.debug("received ACK from "+sd.id);
										bye += "-ACK";
										retry = 0;
										messages.remove(id);
									    break;
									case 82:
										logger.debug("received R from "+sd.id);
										bye += "-R";
										if (messages.containsKey(sd.id)){
											logger.info("writing '"+new String(messages.get(sd.id))+"' to "+sd.id);
											output.write(messages.get(sd.id));
											output.write("\r\n".getBytes());
											output.flush();
										}
										retry = 3;
										lastRetry = 0; // send immediately after
										break;
									default:
										throw new IOException("unknown command:"+next);
									}
									resync = false;
									break;
								case 43:
									byte[] buf = parser.readBytes(2);
									if (new String(buf).equals("++")){
										logger.debug("Good Bye received");
										bye += "++";
										connected = false;
										resync = false;
									}
									break;
								default:
									throw new IOException("unknown packet type:"+type);
								}
							}catch(EOFException e){
								logger.debug("packet reading error [last:"+last+", ctr:"+ctr+"] " + e.getMessage());
								bye += "-EOF";
								connected = false;
								resync = false;
							}catch(SocketTimeoutException e){
								logger.debug("packet reading error [last:"+last+", ctr:"+ctr+"] " + e.getMessage());
								bye += "-timeout";
								connected = false;
								resync = false;
							}catch(SocketException e){
								logger.debug("packet reading error [last:"+last+", ctr:"+ctr+"] " + e.getMessage());
								bye += "-socket";
								connected = false;
								resync = false;
							}
							catch(IOException e){
								err ++;
								if (! resync){
								    logger.debug("packet reading error [last:"+last+", ctr:"+ctr+"] " + e.getMessage());
								}
								resync = true;
							}catch(NumberFormatException e){
								err ++;
								if (! resync){
								    logger.debug("packet reading error [last:"+last+", ctr:"+ctr+"] " + e.getMessage());
								}
								resync = true;
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
			logger.info("Unknown station ID ("+id+"), assuming mobile.");
			return new MobileStationData(id);
		}
	}
	
	private abstract class StationData{
		int t_ms, t_ss, t_mn, t_hh, t_dd, t_mm,t_yy;
		int ms,ss,mn,hh,dd,mm,yy,id,type;
		byte[] payload;
		boolean time_set = false;
		
		public StationData(int id){
			this.id = id;
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			yy = c.get(Calendar.YEAR);
			mm = c.get(Calendar.MONTH+1);
			dd = c.get(Calendar.DAY_OF_MONTH);
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
			int _ss = p.readNextChar(false);
			if (_ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+mn+":"+_ss+")");
			}
			t_ss = _ss;
			payload = p.readBytes(8);			
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
			payload = p.readBytes(1);
			int time = p.readNextShort(false);
			int _hh = (int) (time / 100);
			int _mn = (int) (time % 100);
			if (_hh < 0 || _hh > 24 || _mn < 0 || _mn > 59 ){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+_hh+":"+_mn+":00)");
			}
			//ms = 0; ??
			//ss = 0; ??
			//t_mn = _mn;
			//t_hh = _hh;
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
			t_ms = 0;
			t_ss = _ss;
			t_mn = _mn;
			t_hh = _hh;
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
			t_ms = 0;
			t_ss = _ss;
			t_mn = _mn;
			t_hh = _hh;
			t_dd = _dd;
			t_mm = _mm;
			t_yy = _yy;
			payload = ("20"+_yy+"-"+_mm+"-"+_dd+" "+_hh+":"+_mn+":"+_ss).getBytes();

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
			t_ms = 0;
			t_ss = _ss;
			t_mn = _mn;
			t_hh = _hh;
			t_ms = _ms;
		}
		
		protected void readTimeMsFromShort(BinaryParser p) throws IOException{
			int time = p.readNextShort(false);
			int _ss = time/100;
			int _ms = (time%100)*10;
			if ( _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+mn+":"+_ss+")");
			}
			t_ss = _ss;
			t_ms = _ms;
		}
		
		protected void readTimeSsFromShort(BinaryParser p) throws IOException{
			int time = p.readNextShort(false);
			int _mn = time/100;
			int _ss = time%100;
			if ( _mn < 0 || _mn > 59 || _ss < 0 || _ss > 59){
				throw new IOException("invalid datetime received for station "+ id +". (20"+yy+"-"+mm+"-"+dd+" "+hh+":"+_mn+":"+_ss+")");
			}
			t_ms = 0;
			t_ss = _ss;
			t_mn = _mn;
		}
		
		public void resetTime(){
			t_yy = yy;
			t_mm = mm;
			t_dd = dd;
			t_hh = hh;
			t_mn = mn;
			t_ss = ss;
			t_ms = ms;
		}
		
		public StreamElement getStreamElement() throws IOException {
			Calendar c = Calendar.getInstance();
			c.clear();
			if (type == 1 || type == 8){
				time_set = true;
				yy = t_yy;
				mm = t_mm;
				dd = t_dd;
				hh = t_hh;
				mn = t_mn;
				ss = t_ss;
				ms = t_ms;
			}
			if (!time_set) {
				throw new IOException("Time is not set yet, waiting for a OSDMY or OSODO packet.");
			}
			c.set(t_yy + 2000, t_mm - 1, t_dd, t_hh, t_mn, t_ss);
			c.setTimeZone(TimeZone.getTimeZone("UTC"));
			long time = c.getTimeInMillis() + t_ms;
			
			if (time > System.currentTimeMillis() + 600000) {
				throw new IOException("Packet time ("+c.toString()+") is in the future, dropping packet.");
			}
			
			Serializable[] data = new Serializable[]{time, new Short((short) id), new Short((short) type), payload};
			
			return new StreamElement(OpensenseConnectorWrapper.this.df,data,0);
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
			t_ms = 0;
			t_ss = _ss;
			t_mn = _mn;
			t_hh = _hh;
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
			payload = p.readBytes(8);			
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

