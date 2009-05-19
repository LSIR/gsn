package gsn.beans;

import gsn.utils.GSNRuntimeException;
import org.apache.log4j.Logger;

import java.sql.Types;
import java.util.regex.Pattern;

public class DataTypes {

    private final static transient Logger logger = Logger.getLogger(DataTypes.class);

    // NEXT FIELD
    public final static String STRING_PATTERN = "STRING";

    public final static byte STRING = Types.VARCHAR;

    public final static String STRING_NAME = "string";

    public final static String STRING_SQL = "varchar";

    // NEXT FIELD
    public final static String INTEGER_PATTERN_STRING = "NUMERIC";

    public final static byte NUMERIC = Types.NUMERIC;

    public final static String NUMERIC_NAME = "numeric";

    public final static String NUMERIC_SQL = "double";

    // NEXT FIELD
    public final static String BINARY_PATTERN_STRING = "BINARY\\s*(:.*)?\\s*";

    public final static byte BINARY = Types.BINARY;

    public final static String BINARY_NAME = "binary";

    public final static String BINARY_SQL = "blob";

    // NEXT FIELD
    /**
     * Type Time is not supported at the moment. If you want to present time, please use
     * longint. For more information consult the GSN mailing list on the same subject.
     */
    public final static String TIME_PATTERN_STRING = "TIMESTAMP";

    public final static byte TIME = Types.BIGINT;

    public final static String TIME_NAME = "timestamp";

    public final static String TIME_SQL = "bigint";


    // FINISH
    public final static Pattern[] ALL_PATTERNS = new Pattern[]{
            Pattern.compile(STRING_PATTERN, Pattern.CASE_INSENSITIVE),
            Pattern.compile(INTEGER_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
            Pattern.compile(BINARY_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
            Pattern.compile(TIME_PATTERN_STRING, Pattern.CASE_INSENSITIVE)
    };

    public final static StringBuilder ERROR_MESSAGE = new StringBuilder("Acceptable types are (Numeric, String, Binary[:MajorType/MinorType] and Timestamp");

    public final static String[] TYPE_NAMES = new String[]{STRING_NAME, NUMERIC_NAME, BINARY_NAME, TIME_NAME};
    public final static String[] TYPE_SQL_NAMES = new String[]{STRING_SQL, NUMERIC_SQL, BINARY_SQL, TIME_SQL};
    public final static Byte[] TYPE_IDS = new Byte[]{STRING, NUMERIC, BINARY, TIME};


    public static byte convertSQLTypeToGSNType(byte sqlType) {
        // TODO: Hibernate Mapping.
        throw new GSNRuntimeException(new StringBuilder("The type *").append(sqlType).append("* is not recognized.").append(DataTypes.ERROR_MESSAGE).toString());
    }

    public static String convertGSNTypeNameToSQLTypeString(String type) {
        type = type.trim();
        for (byte i = 0; i < DataTypes.ALL_PATTERNS.length; i++)
            if (DataTypes.ALL_PATTERNS[i].matcher(type).matches()) {
                return TYPE_SQL_NAMES[i];
            }
        throw new GSNRuntimeException(new StringBuilder("The type *").append(type).append("* is not recognized.").append(DataTypes.ERROR_MESSAGE).toString());
    }

    public static byte convertGSNTypeNameToTypeID(String type) {
        type = type.trim();
        for (byte i = 0; i < DataTypes.ALL_PATTERNS.length; i++)
            if (DataTypes.ALL_PATTERNS[i].matcher(type).matches()) {
                return TYPE_IDS[i];
            }
        throw new GSNRuntimeException(new StringBuilder("The type *").append(type).append("* is not recognized.").append(DataTypes.ERROR_MESSAGE).toString());
    }
}


