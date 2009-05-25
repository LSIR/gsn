package gsn.beans;

import gsn.utils.GSNRuntimeException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;


public class TestDataType {
    @Test
    public void testConvertGSNTypeNameToTypeID() {
        String type = "   sTring  ";
        assertEquals(DataTypes.STRING, DataTypes.convertGSNTypeNameToTypeID(type));
        type = "   binarY  ";
        assertEquals(DataTypes.BINARY, DataTypes.convertGSNTypeNameToTypeID(type));
        type = "   binarY : image/JPeG  ";
        assertEquals(DataTypes.BINARY, DataTypes.convertGSNTypeNameToTypeID(type));
        type = "   NumERiC  ";
        assertEquals(DataTypes.NUMERIC, DataTypes.convertGSNTypeNameToTypeID(type));
        type = "   TiMeStAmP  ";
        assertEquals(DataTypes.TIME, DataTypes.convertGSNTypeNameToTypeID(type));
    }

    @Test
    public void testConvertSQLTypeToGSNType() {

//        byte type = Types.VARCHAR;
//        assertEquals(DataTypes.STRING,DataTypes.convertSQLTypeToGSNType(type));
        // TODO: Hibernate Mapping method to be implemented.

    }

    @Test(expectedExceptions = GSNRuntimeException.class)
    public void testConvertGSNTypeNameToTypeIDBadInput() {
        String type = "   TiMe ";
        DataTypes.convertGSNTypeNameToTypeID(type);
    }


}
