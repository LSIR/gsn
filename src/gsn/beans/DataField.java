package gsn.beans ;

import gsn.utils.GSNRuntimeException ;

import java.io.Serializable;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch) <br>
 *         Create : Apr 26, 2005 <br>
 */
public final class DataField implements Serializable {

    private String description = "Not Provided";

    private String fieldName;

    private int dataTypeID = - 1;

    private String type;

    private DataField() {
        /**
         * This constructor is needed by JIBX XML Parser.
         */
    }

    public DataField(String fieldName, String type, String description) throws GSNRuntimeException {
        this.fieldName = fieldName;
        this.type = type;
        this.dataTypeID = DataTypes.convertTypeNameToTypeID(type);
        this.description = description;
    }

    public DataField(String name, String type) {
        this.fieldName = name;
        this.type = type;
        this.dataTypeID = DataTypes.convertTypeNameToTypeID(type);
    }

    public String getDescription() {
        return this.description;
    }

    public String getFieldName() {
        return this.fieldName.toUpperCase();
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (! (o instanceof DataField))
            return false;

        final DataField dataField = (DataField) o;
        if (fieldName != null ? ! fieldName.equals(dataField.fieldName) : dataField.fieldName != null)
            return false;
        return true;
    }

    /**
     * @return Returns the dataTypeID.
     */
    public int getDataTypeID() {
        if (dataTypeID == - 1)
            this.dataTypeID = DataTypes.convertTypeNameToTypeID(type);
        return dataTypeID;
    }

    public int hashCode() {
        return (fieldName != null ? fieldName.hashCode() : 0);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[Field-Name:").append(fieldName).append(", Type:").append(DataTypes.TYPE_NAMES[getDataTypeID()]).append("[" + type
                + "]").append(", Decription:").append(description).append("]");
        return result.toString();
    }

    /**
     * @return Returns the type. This method is just used in the web interface
     *         for detection the output of binary fields.
     */
    public String getType() {
        return type;
    }

}
