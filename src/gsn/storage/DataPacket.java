package gsn.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DataPacket {
    private final static transient Logger logger = Logger
	    .getLogger(DataPacket.class);

    public static final DataEnumerator EMPTY_ENUM = new DataEnumerator();

    /**
         * This method Only returns {@link DataEnumerator}.
         * 
         * @param preparedStatement
         * @return Returns {@link DataEnumerator} or EMPTY_ENUM.
         */
    public static DataEnumerator resultSetToStreamElements(
	    PreparedStatement preparedStatement) {
	if (preparedStatement == null) {
	    if (logger.isDebugEnabled())
		logger.debug(new StringBuilder().append(
			"resultSetToStreamElements").append(
			" is supplied with null input.").toString());
	    return EMPTY_ENUM;
	}
	try {
	    ResultSet resultSet = preparedStatement.executeQuery();
	    return new DataEnumerator(resultSet);
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return EMPTY_ENUM;
	}
    }
}
