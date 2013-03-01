package gsn.vsensor;

import gsn.Mappings;
import gsn.VirtualSensor;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.models.AbstractModel;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.TreeMap;


public class GridModelVS extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(GridModelVS.class);

    private static final String PARAM_MODEL_VS = "model_VS";
    private static final String PARAM_MODEL = "model_index";
    private static final String PARAM_FIELD = "field";
    private static final String PARAM_GRID_SIZE = "grid_size";
    private static final String PARAM_CELL_SIZE = "cell_size";
    private static final String PARAM_X = "x_bottomLeft";
    private static final String PARAM_Y = "y_bottomLeft";


    private AbstractModel modelVS = null;
    private String field = "";
    private double x_BL = 0;
    private double y_BL = 0;
    private int gridSize = 0;
    private double cellSize = 0;
    
    private double XCellSize = 0;
    private double YCellSize = 0;



    public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        //get the model from the modelling virtual sensor
        String model_str = params.get(PARAM_MODEL_VS);
        String model_i_str = params.get(PARAM_MODEL);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL_VS + "\" not provided in Virtual Sensor file");
            return false;
        }
        if (model_i_str == null) {
            model_i_str = "0";
        }

    	try {
		    VirtualSensor vs = Mappings.getVSensorInstanceByVSName(model_str);
		    if (vs == null){
		    	logger.error("can't find VS: "+ model_str);
				return false;
		    }
		    AbstractVirtualSensor avs = vs.borrowVS();
			if (avs instanceof ModellingVirtualSensor){
				int modelIndex = Integer.parseInt(model_i_str.trim());
				modelVS = ((ModellingVirtualSensor)avs).getModel(modelIndex);
				if (modelVS==null){
					logger.error("Virtual Sensor " + model_str + " returned no model["+modelIndex+"].");
				}
			}else{
				logger.error("Virtual Sensor " + model_str + " is not a modelling Virtual Sensor.");	
				return false;
			}
			vs.returnVS(avs);
		} catch (VirtualSensorInitializationFailedException e) {
			logger.error("Error loading the model["+model_i_str+"] from " + model_str);
			return false;
		}

        //get the field to query to build the grid (fields are converted to double)
        field = params.get(PARAM_FIELD);

        if (field == null) {
            logger.warn("Parameter \"" + PARAM_FIELD + "\" not provided in Virtual Sensor file");
            return false;
        }
        
        //get the bottom-left corner longitude coordinate in degree
        String cx_str = params.get(PARAM_X);

        if (cx_str == null) {
            logger.warn("Parameter \"" + PARAM_X + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            x_BL = Double.parseDouble(cx_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_X + "\" incorrect in Virtual Sensor file");
            return false;
        }
        
      //get the bottom-left corner latitude coordinate in degree
        String cy_str = params.get(PARAM_Y);

        if (cy_str == null) {
            logger.warn("Parameter \"" + PARAM_Y + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            y_BL = Double.parseDouble(cy_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_Y + "\" incorrect in Virtual Sensor file");
            return false;
        }
        
      //get the number of cells of a side of the square grid
        String gridSize_str = params.get(PARAM_GRID_SIZE);

        if (gridSize_str == null) {
            logger.warn("Parameter \"" + PARAM_GRID_SIZE + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
        	gridSize = Integer.parseInt(gridSize_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_GRID_SIZE + "\" incorrect in Virtual Sensor file");
            return false;
        }

        if (gridSize < 0) {
            logger.warn("Grid size should always be positive.");
            return false;
        }
        
        // get the size of a square cell in meter
        String cellSize_str = params.get(PARAM_CELL_SIZE);

        if (cellSize_str == null) {
            logger.warn("Parameter \"" + PARAM_CELL_SIZE + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
        	cellSize = Double.parseDouble(cellSize_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_CELL_SIZE + "\" incorrect in Virtual Sensor file");
            return false;
        }

        if (cellSize < 0) {
            logger.warn("Cell size should always be positive.");
            return false;
        }
        
        //compute the width and height of a cell in degree (if the grid is too big, their maybe some deformation)
        YCellSize = cellSize *360.0 / (6356753*2*Math.PI);
        XCellSize = cellSize *360.0 / (6378137*Math.cos(Math.toRadians(y_BL))*2*Math.PI);

        return true;
    }
    
    public DataField[] getOutputFormat() {
        return new DataField[]{
                new DataField("ncols", "int", "number of columns"),
                new DataField("nrows", "int", "number of rows"),
                new DataField("xllcorner", "double", "xll corner"),
                new DataField("yllcorner", "double", "yll corner"),
                new DataField("cellsize", "double", "cell size"),
                new DataField("nodata_value", "double", "no data value"),
                new DataField("grid", "binary", "raw  data")};
    }


	public void dataAvailable(String inputStreamName, StreamElement data) { 
		

			DataField []fields = new DataField[]{new DataField("latitude","double"),new DataField("longitude","double")};
			
			//filling the grid with predictions/extrapolations
			//if the model is slow to query this loop may take some time
			Double[][] rawData = new Double[gridSize][gridSize];
			for (int j=0;j<gridSize;j++){
				for(int k=0;k<gridSize;k++){
					double[] pos = new double[]{y_BL+YCellSize * j, x_BL+XCellSize * k};
					StreamElement se = new StreamElement(fields, new Serializable[]{pos[0],pos[1]});
					StreamElement r = modelVS.query(se)[0];
					Serializable s = r.getData(field);
					if (s instanceof Double){
					    rawData[gridSize-j-1][k] = (Double) r.getData(field);
					}else if(s instanceof Integer){
						rawData[gridSize-j-1][k] = ((Integer) r.getData(field)).doubleValue();
					}else if(s instanceof Boolean){
						rawData[gridSize-j-1][k] = ((Boolean) r.getData(field)) ? 1.0 : 0.0;
					}else {
						rawData[gridSize-j-1][k] = 0.0;
					}
				}
			}

			//preparing the output
			
			Serializable[] stream = new Serializable[7];
	        try {

	            ByteArrayOutputStream bos = new ByteArrayOutputStream();
	            ObjectOutputStream oos = new ObjectOutputStream(bos);
	            oos.writeObject(rawData);
	            oos.flush();
	            oos.close();
	            bos.close();

	            stream[0] = new Integer(gridSize);
	            stream[1] = new Integer(gridSize);
	            stream[2] = new Double(x_BL);
	            stream[3] = new Double(y_BL);
	            stream[4] = new Double(cellSize);
	            stream[5] = new Double(0);
	            stream[6] = bos.toByteArray();
	            
	        	StreamElement se = new StreamElement(getOutputFormat(), stream, data.getTimeStamp());
	        	dataProduced(se);

	        } catch (IOException e) {
	            logger.warn(e.getMessage(), e);
	        }
    }

	
	public void dispose() {

    }


}
