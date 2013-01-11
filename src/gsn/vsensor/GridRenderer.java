package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.geo.GridTools;
import org.apache.log4j.Logger;
import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.TreeMap;

public class GridRenderer extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(GridRenderer.class);

    private static final String CELL_PIXELS = "cellpixels";
    private static final String MAP_OVERLAY = "mapoverlay";
    private static final String MAX_V = "max_value";
    private static final String MIN_V = "min_value";

    private int map[];
    private double min_v;
    private double max_v;
    private double minvalue;
    private double maxvalue;
    private double cellsize;
    private double yllcorner;
    private double xllcorner;
    private int ncols;
    private int nrows;
    
    private Hashtable<Double,BufferedImage> cache = new Hashtable<Double,BufferedImage>();


    int cell_pixels = 20;
    boolean map_overlay = false;
    
    
    public static void main(String args[]){
    	
    	GridRenderer gr = new GridRenderer();
    	gr.initColorMap();
    	Double[][] ar = new Double[][]{new Double[]{2.0,3.0,4.1,3.5,3.2,3.1},new Double[]{2.0,3.0,4.1,3.5,3.2,3.1},
    			new Double[]{2.0,3.0,4.1,3.5,3.2,3.1},new Double[]{2.0,3.0,4.1,3.5,3.2,3.1},
    	        new Double[]{2.0,3.0,4.1,3.5,3.2,3.1},new Double[]{2.0,3.0,4.1,3.5,3.2,3.1}};
    	gr.ncols = 6;
    	gr.nrows = 6;
    	gr.xllcorner = 8.3;
    	gr.yllcorner = 47.3;
    	gr.cellsize = 200;
    	gr.cell_pixels = 50;
    	try {
			FileOutputStream fos = new FileOutputStream("test2.png");
			fos.write(gr.createImageFromArray(ar));
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	
    }

    @Override
    public boolean initialize() {

        VSensorConfig vsensor = getVirtualSensorConfiguration();
        TreeMap<String, String> params = vsensor.getMainClassInitialParams();

        String cell_pixels_str = params.get(CELL_PIXELS);

        if (cell_pixels_str != null) {
            cell_pixels_str = cell_pixels_str.trim().toLowerCase();
            try {
                cell_pixels = Integer.parseInt(cell_pixels_str.trim());
            } catch (NumberFormatException e) {
                logger.warn("Parameter \"" + CELL_PIXELS + "\" has incorrect value in Virtual Sensor file. Assuming default value.");
            }
        }
        
        String min_str = params.get(MIN_V);

        if (min_str != null) {
        	min_str = min_str.trim().toLowerCase();
            try {
                min_v = Double.parseDouble(min_str.trim());
            } catch (NumberFormatException e) {
                logger.warn("Parameter \"" + MIN_V + "\" has incorrect value in Virtual Sensor file.");
            }
        }
        
        String max_str = params.get(MAX_V);

        if (max_str != null) {
        	max_str = max_str.trim().toLowerCase();
            try {
                max_v = Double.parseDouble(max_str.trim());
            } catch (NumberFormatException e) {
                logger.warn("Parameter \"" + MAX_V + "\" has incorrect value in Virtual Sensor file.");
            }
        }
        
        String map_overlay_str = params.get(MAP_OVERLAY);

        if (map_overlay_str != null) {
        	map_overlay_str = map_overlay_str.trim().toLowerCase();
            if(map_overlay_str.equals("no") || map_overlay_str.equals("false")){
                map_overlay = false;
            }else if(map_overlay_str.equals("yes") || map_overlay_str.equals("true")){
            	map_overlay = true;
            } else {
                logger.warn("Parameter \"" + MAP_OVERLAY + "\" has incorrect value in Virtual Sensor file. Assuming default value.");
            }
        }


        initColorMap();

        return true;
    }


    @Override
    public void dispose() {

    }

    @Override
    public void dataAvailable(String inputStreamName, StreamElement streamElement) {
    	
    	 ncols = (Integer) streamElement.getData("ncols");
         nrows = (Integer) streamElement.getData("nrows");
         xllcorner = (Double) streamElement.getData("xllcorner");
         yllcorner = (Double) streamElement.getData("yllcorner");
         cellsize = (Double) streamElement.getData("cellsize"); // must be in meters
         
        //logger.warn("Data => " + streamElement.toString());
        long timestamp = streamElement.getTimeStamp();
        Double values[][] = GridTools.deSerialize((byte[]) streamElement.getData("grid"));
        byte b[] = createImageFromArray(values);
        
       
        

        StreamElement se = new StreamElement(new String[]{"grid"},
                new Byte[]{DataTypes.BINARY},
                new Serializable[]{b},
                timestamp);
        dataProduced(se);
    }


    /**
     * earth radius taken from 
     * http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm
     * grayscale conversion from
     * http://en.wikipedia.org/wiki/Grayscale
     * @param a
     * @return
     */
    
    private byte[] createImageFromArray(Double[][] a) {
    	
    	BufferedImage back;
    	
    	
    	if ((min_v == 0 && max_v == 0) || min_v >= max_v){
    	// search for minimum and maximum
        minvalue = a[0][0];
        maxvalue = a[0][0];

        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++) {
                if (minvalue > a[i][j]) minvalue = a[i][j];
                if (maxvalue < a[i][j]) maxvalue = a[i][j];
            }
    	}else{
    		minvalue = min_v;
    		maxvalue = max_v;
    	}
    	
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	
    	if (map_overlay){
	    	double centerY = yllcorner + (360/(6356752.0*2*Math.PI) * cellsize * nrows)/2;
	    	double centerX = xllcorner + (360/(6378137*2*Math.PI*Math.cos(Math.toRadians(centerY)))*cellsize * ncols)/2;
	    	
	    	int zoom = (int) Math.ceil(Math.log(cell_pixels*6378137*2*Math.PI*Math.cos(Math.toRadians(centerY))/cellsize)/Math.log(2)-8);
	    	
	    	//adjusting to match the zoom level's pixel/meter
	    	int width = (int) (cellsize * ncols / (6378137*2*Math.PI*Math.cos(Math.toRadians(centerY))/Math.pow(2, zoom+8)));
	    	int height = (int) (cellsize * nrows / (6378137*2*Math.PI*Math.cos(Math.toRadians(centerY))/Math.pow(2, zoom+8)));
	    	
	    	BufferedImage osmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    	back = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    	
	    	try {
	    		
	    		if (cache.containsKey(centerX+centerY*180+zoom*180*360+width*180*360*20+height*180*360*20*1000)){
	    			osmap = cache.get(centerX+centerY*180+zoom*180*360+width*180*360*20+height*180*360*20*1000);
	    		}else{
	    			osmap = ImageIO.read(new URL("http://staticmap.openstreetmap.de/staticmap.php?center="+centerY+","+centerX+"&zoom="+zoom+"&size="+width+"x"+height));
	    		    cache.put(centerX+centerY*180+zoom*180*360+width*180*360*20+height*180*360*20*1000, osmap);
	    		}
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	    	
	    	for (int x=0;x<width;x++)
	        	for (int y=0;y<height;y++){
	        		
	        		int i = (int) (x/(width * 1.0 / ncols));
	        		int j = (int) (y/(height * 1.0 / ncols));
	        		
	        		int val = osmap.getRGB(x, y);
	        		int r = (val & 0x00ff0000) >> 16;
	        		int g = (val & 0x0000ff00) >> 8;
	        		int b = (val & 0x000000ff);
	        		int bw = (int)(0.2126*r+0.7152*g+0.0722*b);
	        		int color = mapValue(a[j][i]);
	        		int r2 = (color & 0x00ff0000) >> 16;
	        		int g2 = (color & 0x0000ff00) >> 8;
	        		int b2 = (color & 0x000000ff);
	        		int rgb = (bw+r2)/2 * 256 * 256 + (bw+g2)/2 * 256 + (bw+b2)/2;
	        		back.setRGB(x, y, rgb);
	        	}
    	} else {
	        int Y = a.length;
	        int X = a[0].length;
	
	        back = new BufferedImage(X * cell_pixels, Y * cell_pixels, BufferedImage.TYPE_INT_RGB);
	
	        int bigPixel[] = new int[cell_pixels * cell_pixels];
	
	        for (int i = 0; i < X; ++i)
	            for (int j = 0; j < Y; ++j) {
	
	                int color = mapValue(a[j][i]);
	
	                for (int k = 0; k < cell_pixels * cell_pixels; k++)
	                    bigPixel[k] = color;
	
	                back.setRGB(i * cell_pixels, j * cell_pixels, cell_pixels, cell_pixels, bigPixel, 0, cell_pixels);
	            }
    	}
    	int bigPixel[] = new int[15];
    	for (int x=0;x<back.getHeight();x++){
    		int color = map[255-(int)(x*255.0/back.getHeight())];
            for (int k = 0; k < 15; k++)
                bigPixel[k] = color;
    		back.setRGB(0, x, 15, 1, bigPixel, 0,15);
    	}
    	
    	Graphics2D gp = back.createGraphics();
    	gp.setColor(Color.black);
    	gp.drawString(""+maxvalue, 3, 12);
    	gp.drawString(""+minvalue, 3, back.getHeight()-3);
    	
        try {
            ImageIO.write(back, "png", outputStream);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        return outputStream.toByteArray();
    }


    public void initColorMap() {
        map = new int[256];
        int r, g, b;
        for (int i = 0; i <= 255; i++) {
            if (i < 128)
                r = 255;
            else
                r = 255 - i * 2;
            if (i < 128)
                g = i * 2;//i;
            else
                g = 255;
            b = 0;//255;
            map[255-i] = r * 256 * 256 + g * 256 + b;
        }
    }

    public int mapValue(double value) {
        if (value > maxvalue)
            return  0; //black
        if (value < minvalue)
            return 255 * 256 * 256 + 255 * 256 + 255; //white
        return map[(int) Math.round((value - minvalue) * 255 / (maxvalue - minvalue))];
    }
}
