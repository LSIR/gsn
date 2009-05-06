package gsn.beans;

import org.junit.*;
import static org.junit.Assert.*;
import gsn.utils.GSNRuntimeException;

import java.sql.Types;

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

    @Test(expected = GSNRuntimeException.class)
    public void testConvertGSNTypeNameToTypeIDBadInput() {
        String type = "   TiMe ";
        DataTypes.convertGSNTypeNameToTypeID(type);
    }


}
