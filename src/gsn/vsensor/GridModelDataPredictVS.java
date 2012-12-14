package gsn.vsensor;


import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.utils.models.ModelLoader;

import org.apache.log4j.Logger;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.TreeMap;


public class GridModelDataPredictVS extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensor.class);

    private static final String PARAM_MODEL = "model";
    private static final String PARAM_CLASS_INDEX = "class_index";
    private static final String PARAM_GRID_SIZE = "grid_size";
    private static final String PARAM_CELL_SIZE = "cell_size";


    private String model = "";
    private Instances dataset;
    private int classIndex = 0;
    private ModelLoader ms;
    private int gridSize = 0;
    private double cellSize = 0;
    private FastVector att = new FastVector();



    public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        String model_str = params.get(PARAM_MODEL);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL + "\" not provided in Virtual Sensor file");
            return false;
        } else {
            model = model_str.trim();
        }
        
        String classIndex_str = params.get(PARAM_CLASS_INDEX);

        if (classIndex_str == null) {
            logger.warn("Parameter \"" + PARAM_CLASS_INDEX + "\" not provided in Virtual Sensor file");
            return false;
        } else try {
            classIndex = Integer.parseInt(classIndex_str.trim());
        } catch (NumberFormatException e) {
            logger.warn("Parameter \"" + PARAM_CLASS_INDEX + "\" incorrect in Virtual Sensor file");
            return false;
        }

        if (classIndex < 0) {
            logger.warn("Class index should always be positive.");
            return false;
        }
        
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

        ms = new ModelLoader(model);

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
		
		//mapping the input stream to an instance and setting its dataset
		String[] dfn = data.getFieldNames().clone();
		Byte[] dft = data.getFieldTypes().clone();
		Serializable[] da = data.getData().clone();
		data = new StreamElement(dfn, dft, da, data.getTimeStamp());
		Instance i = instanceFromStream(data);
		if (att.size() == 0){
			att = attFromStream(data);
		}
		dataset = new Instances("input",att,0);
		dataset.setClassIndex(classIndex);
		if(i != null){
			dataset.add(i);
			i = dataset.firstInstance();
			
			boolean success = true;
			
			//extracting latitude/longitude
			Double center_lat = i.value(1);
			Double center_long = i.value(2);
			
			//filling the grid with predictions/extrapolations
			Double[][] rawData = new Double[gridSize][gridSize];
			for (int j=0;j<gridSize;j++){
				for(int k=0;k<gridSize;k++){
					i.setValue(1, center_lat - (cellSize*gridSize/2) + cellSize * j);
					i.setValue(2, center_long - (cellSize*gridSize/2) + cellSize * k);
					rawData[j][k] = ms.predict(i);
					success = success && (rawData[j][k] != null);
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
	            stream[2] = new Double(center_lat - (cellSize*gridSize/2));
	            stream[3] = new Double(center_long - (cellSize*gridSize/2));
	            stream[4] = new Double(cellSize);
	            stream[5] = new Double(0);
	            stream[6] = bos.toByteArray();

	        } catch (IOException e) {
	            logger.warn(e.getMessage(), e);
	            success = false;
	        }
	        
	        if(success){
	        	StreamElement se = new StreamElement(getOutputFormat(), stream, data.getTimeStamp());
	        	dataProduced(se);
	        }else{
				logger.warn("Prediction error. Something get wrong with the prediction.");
			}

		}else{
			logger.warn("Predicting instance has wrong attibutes, please check the model and the inputs.");
		}
    }


	/*public boolean dataFromWeb ( String command, String[] paramNames, Serializable[] paramValues ) {
		try{
        	if(command.equalsIgnoreCase("model")){
        		for (int j=0;j<paramNames.length;j++){
        			if(paramNames[j].equalsIgnoreCase("model")){
        				ms = new ModelLoader(paramValues[j].toString());
        			}
        		}
	        }
			return true;
		}catch(Exception e){
			return false;
		}
	}*/
	
	 private FastVector attFromStream(StreamElement data) {
			FastVector fv = new FastVector();
			for (int i=0;i<data.getFieldNames().length;i++){
				Attribute a = new Attribute(data.getFieldNames()[i]);
				fv.addElement(a);
			}
			return fv;
		}

	private Instance instanceFromStream(StreamElement data) {
		try{
		Instance i = new Instance(data.getFieldNames().length);
		for(int j=0;j<data.getFieldNames().length;j++){
			i.setValue(j, ((Double)data.getData()[j]));
		}
		//scaling specific to opensense data!! should be put in the parameters?
		i.setValue(0, i.value(0)/1400.0);
		i.setValue(2, i.value(2)/50);
		i.setValue(3, i.value(3)/100.0);
		i.setValue(4, i.value(4)/100.0 - 4);	
		return i;
		}catch(Exception e){
			return null;
		}
	}

	public void dispose() {

    }


}
