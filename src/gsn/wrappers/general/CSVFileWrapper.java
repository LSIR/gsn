package gsn.wrappers.general;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;


/**
 * @author Andreas Wombacher
 */
public class CSVFileWrapper extends AbstractWrapper {


	private static final String PARAM_FILE = "file";

	private static final String PARAM_SKIP_LINES = "skip_lines";

	private static final String PARAM_SEPERATOR = "seperator";

	private static final String PARAM_QUOTE = "quote";

	private static int               threadCounter      = 0;

	private final transient Logger   logger             = Logger.getLogger( CSVFileWrapper.class );


	private AddressBean              addressBean;


	private static DataField [] structure;

	private String filename;

	private int columns;

	private int skip_lines = 0;
	
	private char seperator = '\t';

	private char quote= '#';

	/**
	 */
	public boolean initialize (  ) {
		logger.warn("cvsfile wrapper initialize started...");
		this.addressBean =getActiveAddressBean( );
		ArrayList<? extends KeyValue> predicates = this.addressBean.getPredicates();
		int column=0;
		Vector v = new Vector();
		for(int i=0;i<predicates.size();i++){
			String key = (String) predicates.get(i).getKey();
			String value = (String) predicates.get(i).getValue();
			logger.debug(v.size()+": type value: "+key);
			if (key.equals(PARAM_FILE))
				filename = value;
			else if (key.equals(PARAM_SKIP_LINES))
				skip_lines  = Integer.valueOf(value).intValue();
			else if (key.equals(PARAM_SEPERATOR))
				seperator = value.charAt(0);
			else if (key.equals(PARAM_FILE))
				quote = value.charAt(0);
			else if (key.equals("time"))
				v.add(new DataField(value, "bigint"));
			else v.add(new DataField(value, key));	 
		}
		this.columns= v.size();
		this.structure = new DataField[columns];
		for(int i=0;i<columns;i++)
			structure[i]=(DataField)v.get(i);
		setName( "CVSFileWrapper-Thread:" + ( ++threadCounter ) );
		logger.warn("cvsfile wrapper initialize completed ...");
		return true;
	}

	public void run ( ) {
		logger.warn("cvsfile wrapper run started...");
		// Parse the data
		try {//i/o may go wrong
			CSVReader reader = new CSVReader(new FileReader(filename),seperator, quote ,skip_lines);
			String [] nextLine;
			SimpleDateFormat dateTimeForm = new SimpleDateFormat( "HH:mm:ss-dd.MM.yyyy" );
			Date date = null;
			long time;
			Serializable[] serialized = new Serializable[columns];
			while ((nextLine = reader.readNext()) != null) {
				int k=0;
				time = 0;
				logger.debug("length: "+nextLine.length );
				for (int j=0; j<Math.min(columns,nextLine.length); j++){
					logger.debug("Type ID of "+nextLine[j]+"  : "+structure[j].getDataTypeID());
					if(structure[j].getDataTypeID() == DataTypes.BIGINT){
						date = dateTimeForm.parse(nextLine[j]);
						if (date==null){
							logger.error("invalide date format! "+nextLine[j]);
							serialized[k++] = null;
						} else {
							time = date.getTime();
							serialized[k++] = time;
						}
					}
					if(structure[j].getDataTypeID() == DataTypes.DOUBLE){
						Double d = Double.valueOf(nextLine[j]);
						if (d==null) { 
							logger.error("invalide double format! "+nextLine[j]);
							serialized[k++] = null;
						} else serialized[k++] = d.doubleValue();
						logger.debug("double: "+nextLine[j]+" - "+Double.valueOf(nextLine[j])+" - "+serialized[k-1]);
					}
				}// end of j loop
//				logger.debug("-----");
				String str ="";
				for (int j=0;j<serialized.length;j++)
					str = str+","+serialized[j];
				logger.debug("serialized: "+str);
				logger.debug("time: "+time);
				logger.debug("system time: "+System.currentTimeMillis());
				
				StreamElement stream = new StreamElement(structure, serialized, time );
				this.postStreamElement(stream);
			}// end of while loop

		}catch(IOException e){
			logger.error("the file "+filename+" is not accessible!", e);
		} catch(ParseException e){
			logger.error("there has been a parse excpetion! ", e);
		}
		logger.warn("processing of file "+filename+" completed.....");
	}


	public String getWrapperName() {
		return "CSV File Wrapper";
	}

	public void finalize (  ) {
		logger.warn("cvsfile wrapper initialize completed ...");

		threadCounter--;  
	}

	@Override
	public DataField[] getOutputFormat() {
		logger.debug("getOutputFormat called addressBean"+structure.toString());
		return structure;
	}
}
