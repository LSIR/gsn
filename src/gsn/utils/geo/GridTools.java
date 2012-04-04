package gsn.utils.geo;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class GridTools {

    private static transient Logger logger = Logger.getLogger(GridTools.class);

    public static String deSerialize(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            Double deserial[][] = new Double[0][];

            deserial = (Double[][]) in.readObject();
            in.close();

            logger.debug("deserial.length" + deserial.length);
            logger.debug("deserial[0].length" + deserial[0].length);

            for (int i = 0; i < deserial.length; i++) {

                for (int j = 0; j < deserial[0].length; j++) {
                    sb.append(deserial[i][j]).append(" ");
                }
                sb.append("\n");
            }

        } catch (IOException e) {
            logger.warn(e);
        } catch (ClassNotFoundException e) {
            logger.warn(e);
        }

        return sb.toString();
    }
}
