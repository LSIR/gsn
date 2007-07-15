package gsn.beans.windowing;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public abstract class SQLViewQueryRewriter extends QueryRewriter {
	private static final transient Logger logger = Logger.getLogger(SQLViewQueryRewriter.class);

	protected static StorageManager storageManager = StorageManager.getInstance();

	public static final String VIEW_HELPER_TABLE = "__SQL_VIEW_HELPER_TABLE__".toLowerCase();

	private static DataField[] viewHelperFields = new DataField[] { new DataField("UID", "varchar(17)") };

	static {
		try {
			if (storageManager.tableExists(VIEW_HELPER_TABLE))
				storageManager.executeDropTable(VIEW_HELPER_TABLE);
			storageManager.executeCreateTable(VIEW_HELPER_TABLE, viewHelperFields);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	protected StringBuilder cachedSqlQuery;

	@Override
	public boolean initialize() {
		if (streamSource == null) {
			throw new RuntimeException("Null Pointer Exception: streamSource is null");
		}
		try {
			// Initializing view helper table entry for this stream source
			storageManager.executeInsert(VIEW_HELPER_TABLE, viewHelperFields, new StreamElement(viewHelperFields,
					new Serializable[] { streamSource.getUIDStr().toString() }, -1));

			storageManager.executeCreateView(streamSource.getUIDStr(), createViewSQL());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public StringBuilder rewrite(String query) {
		if (streamSource == null)
			throw new RuntimeException("Null Pointer Exception: streamSource is null");
		return SQLUtils.newRewrite(query, streamSource.getAlias(), streamSource.getUIDStr());
	}

	@Override
	public void finilize() {
		if (streamSource == null) {
			throw new RuntimeException("Null Pointer Exception: streamSource is null");
		}
		try {
			storageManager.executeDropView(streamSource.getUIDStr());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean dataAvailable(long timestamp) {
		try {
			//TODO : use preparedStatement instead of creating a new query each time
			StringBuilder query = new StringBuilder("update ").append(VIEW_HELPER_TABLE);
			query.append(" set timed=").append(timestamp).append(" where UID='").append(streamSource.getUIDStr());
			query.append("' ");
			storageManager.executeUpdate(query);
			if (storageManager.isThereAnyResult(new StringBuilder("select * from ").append(streamSource.getUIDStr()))) {
				if (logger.isDebugEnabled())
					logger.debug(streamSource.getWrapper().getWrapperName() + " - Output stream produced/received from a wrapper "
							+ streamSource.toString());
				return streamSource.windowSlided();
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	public abstract CharSequence createViewSQL();

}
