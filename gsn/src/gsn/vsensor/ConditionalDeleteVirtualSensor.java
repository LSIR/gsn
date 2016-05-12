package gsn.vsensor;

import java.util.ArrayList;
import java.util.TreeMap;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConditionalDeleteVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(ConditionalDeleteVirtualSensor.class);
	
	private PreparedStatement preparedDeleteStatement;
	private ArrayList<String> fieldList;
    String conditional = "";
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		fieldList = new ArrayList<String>();

		int index = 1;
		while (true) {
			String field = params.get("field" + index);
			if (field == null) {
				if (index == 1)
					logger.error("no field1 parameter available");
				break;
			}
			boolean fieldExists = false;
		    for ( DataField fields : vsensor.getOutputStructure( ) ) {
		    	if (field.equalsIgnoreCase(fields.getName())) {
		    		fieldExists = true;
		    		break;
		    	}
			}
		    if (!fieldExists) {
				logger.error("field"+index+" "+field+" does not exist in the virtual sensors output structure");
				return false;
		    }
			String operation = params.get("operation" + index);
			if (operation == null) {
				logger.error("no operation"+index+" parameter for field"+index+" available");
				return false;
			}
			if (index > 1) {
				String join = params.get("join" + index);
				if (join == null) {
					logger.error("no join"+index+" parameter for field"+index+" and operation"+index+" available");
					return false;
				}
				conditional += " " + join + " " + field + " " + operation + " ?";
			}
			else
				conditional = "DELETE FROM " + vsensor.getName() + " WHERE " + field + " " + operation + " ?";
			
			fieldList.add(field);
			index++;
		}
		try {
			preparedDeleteStatement = Main.getStorage(vsensor).getConnection().prepareStatement(conditional);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		
		if (logger.isDebugEnabled())
			logger.debug("prepared delete statement: " + conditional);
		
		return ret;
	}
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		long time = System.currentTimeMillis();
		
		boolean retry = true;
		int cnt = 0;
		
		while (retry && cnt++ < 3) {
			try {
				for (int i=0; i<fieldList.size(); i++) {
					if (data.getData(fieldList.get(i)) == null) {
						logger.warn("field " + fieldList.get(i) + " does not exist in stream element or is NULL -> delete query can not be applied");
						super.dataAvailable(inputStreamName, data);
						return;
					}
					else {
						switch (data.getType(fieldList.get(i))) {
							case DataTypes.BIGINT:
								preparedDeleteStatement.setLong(i+1, (Long)data.getData(fieldList.get(i)));
								break;
							case DataTypes.INTEGER:
							case DataTypes.SMALLINT:
							case DataTypes.TINYINT:
								preparedDeleteStatement.setInt(i+1, (Integer)data.getData(fieldList.get(i)));
								break;
							case DataTypes.DOUBLE:
								preparedDeleteStatement.setDouble(i+1, (Double)data.getData(fieldList.get(i)));
								break;
							case DataTypes.VARCHAR:
								preparedDeleteStatement.setString(i+1, (String)data.getData(fieldList.get(i)));
								break;
							case DataTypes.CHAR:
								preparedDeleteStatement.setByte(i+1, (Byte)data.getData(fieldList.get(i)));
								break;
							default:
								logger.error("unknown data type");
						}
					}
				}
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
			
			try {
				preparedDeleteStatement.execute();
				retry = false;
			} catch (SQLException e) {
				try {
					preparedDeleteStatement = Main.getStorage(getVirtualSensorConfiguration()).getConnection().prepareStatement(conditional);
				} catch (SQLException e1) {
					logger.error(e.getMessage());
				}
			}
		}

		if (logger.isDebugEnabled())
			logger.debug("delete execution time: " + (System.currentTimeMillis()-time) + "ms");
		
		super.dataAvailable(inputStreamName, data);
	}
}
