package gsn.beans;

import java.sql.Types;

public enum DataType {

    STRING("string", "String", "", "varchar", Types.VARCHAR, "STRING"),
    NUMERIC("numeric", "Numeric", "", "double", Types.NUMERIC, "NUMERIC"),
    BINARY("binary", "Binary", "", "blob", Types.BINARY, "BINARY\\s*(:.*)?\\s*"),
    TIMESTAMP("timestamp", "Timestamp", "", "bigint", Types.BIGINT, "TIMESTAMP");

    private String name;
    private String description;
    private String className;
    private String sqlName;
    private int sqlType;
    private String pattern;

    private DataType(String name, String description, String className, String sqlName, int sqlType, String pattern) {
        this.name = name;
        this.description = description;
        this.className = className;
        this.sqlName = sqlName;
        this.sqlType = sqlType;
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getClassName() {
        return className;
    }

    public String getSqlName() {
        return sqlName;
    }

    public int getSqlType() {
        return sqlType;
    }

    public String getPattern() {
        return pattern;
    }
}
