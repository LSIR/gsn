package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.geo.GridTools;
import org.apache.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;

public class GridRenderer extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger.getLogger(GridRenderer.class);

    private static final String CELL_PIXELS = "cellpixels";

    private int map[];
    private double minvalue;
    private double maxvalue;

    int cell_pixels = 20;

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


        initColorMap();

        return true;
    }


    @Override
    public void dispose() {

    }

    @Override
    public void dataAvailable(String inputStreamName, StreamElement streamElement) {
        logger.warn("Data => " + streamElement.toString());
        long timestamp = streamElement.getTimeStamp();
        byte a[] = (byte[]) streamElement.getData("grid");

        Double values[][] = GridTools.deSerialize((byte[]) streamElement.getData("grid"));
        byte b[] = createImageFromArray(values);

        StreamElement se = new StreamElement(new String[]{"grid"},
                new Byte[]{DataTypes.BINARY},
                new Serializable[]{b},
                timestamp);
        dataProduced(se);
    }


    private byte[] createImageFromArray(Double[][] a) {

        // search for minimum and maximum
        minvalue = a[0][0];
        maxvalue = a[0][0];

        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++) {
                if (minvalue > a[i][j]) minvalue = a[i][j];
                if (maxvalue < a[i][j]) maxvalue = a[i][j];
            }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        logger.warn("min: " + minvalue);
        logger.warn("max: " + maxvalue);

        int Y = a.length;
        int X = a[0].length;

        BufferedImage I = new BufferedImage(X * cell_pixels, Y * cell_pixels, BufferedImage.TYPE_INT_RGB);

        int bigPixel[] = new int[cell_pixels * cell_pixels];

        for (int i = 0; i < X; ++i)
            for (int j = 0; j < Y; ++j) {

                int color = map[mapValue(a[j][i])];

                for (int k = 0; k < cell_pixels * cell_pixels; k++)
                    bigPixel[k] = color;

                I.setRGB(i * cell_pixels, j * cell_pixels, cell_pixels, cell_pixels, bigPixel, 0, cell_pixels);

            }

        try {
            ImageIO.write(I, "png", outputStream);
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
            map[i] = r * 256 * 256 + g * 256 + b;
        }
    }

    public int mapValue(double value) {
        if (value > maxvalue)
            return map[255];
        if (value < minvalue)
            return map[0];
        return (int) Math.round((value - minvalue) * 255 / (maxvalue - minvalue));
    }
}
