package gsn.beans.windowing;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.GSNRuntimeException;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.TreeMap;

public class SQLViewQueryHandler extends QueryHandler {

    private static final transient Logger logger = Logger.getLogger(SQLViewQueryHandler.class);
    protected static StorageManager storageManager = StorageManager.getInstance();
    public static final CharSequence VIEW_HELPER_TABLE = Main.tableNameGeneratorInString("_SQL_VIEW_HELPER_".toLowerCase());
    private static DataField[] viewHelperFields = new DataField[]{new DataField("u_id", "varchar(17)")};

    static {
        try {
            if (storageManager.tableExists(VIEW_HELPER_TABLE)) {
                storageManager.executeDropTable(VIEW_HELPER_TABLE);
            }
            storageManager.executeCreateTable(VIEW_HELPER_TABLE, viewHelperFields, false);
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
                    new Serializable[]{streamSource.getUIDStr().toString()}, -1));

            storageManager.executeCreateView(streamSource.getUIDStr(), createViewSQL());
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public StringBuilder rewrite(String query) {
        if (streamSource == null) {
            throw new RuntimeException("Null Pointer Exception: streamSource is null");
        }
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
    public boolean dataAvailable(StreamElement streamElement) {
        long timestamp = streamElement.getTimeStamp();
        try {
            //TODO : can we use prepareStatement instead of creating a new query each time
            StringBuilder query = new StringBuilder("update ").append(VIEW_HELPER_TABLE);
            query.append(" set timed=").append(timestamp).append(" where u_id='").append(streamSource.getUIDStr());
            query.append("' ");
            storageManager.executeUpdate(query);
            if (storageManager.isThereAnyResult(new StringBuilder("select * from ").append(streamSource.getUIDStr()))) {
                if (logger.isDebugEnabled()) {
                    logger.debug(streamSource.getWrapper().getWrapperName() + " - Output stream produced/received from a wrapper " + streamSource.toString());
                }
                return streamSource.windowSlided();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public CharSequence createViewSQL() {
        if (cachedSqlQuery != null) {
            return cachedSqlQuery;
        }
        if (streamSource.getWrapper() == null) {
            throw new GSNRuntimeException("Wrapper object is null, most probably a bug, please report it !");
        }
        if (!streamSource.validate()) {
            throw new GSNRuntimeException("Validation of this object the stream source failed, please check the logs.");
        }
        CharSequence wrapperAlias = streamSource.getWrapper().getDBAliasInStr();
        int windowSize = streamSource.getParsedWindowSize();
        if (streamSource.getSamplingRate() == 0 || (streamSource.isStorageCountBased() && windowSize == 0)) {
            return cachedSqlQuery = new StringBuilder("select * from ").append(wrapperAlias).append(" where 1=0");
        }
        TreeMap<CharSequence, CharSequence> rewritingMapping = new TreeMap<CharSequence, CharSequence>(new CaseInsensitiveComparator());
        rewritingMapping.put("wrapper", wrapperAlias);
        StringBuilder toReturn = new StringBuilder(streamSource.getSqlQuery());
        if (streamSource.getSqlQuery().toLowerCase().indexOf(" where ") < 0) {
            toReturn.append(" where ");
        } else {
            toReturn.append(" and ");
        }

        if (streamSource.getSamplingRate() != 1) {
            toReturn.append(" ( timed - (timed / 100) * 100 < ").append(streamSource.getSamplingRate() * 100).append(") and ");
        }

        WindowType windowingType = streamSource.getWindowingType();
        if (windowingType == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
            toReturn.append("(wrapper.timed >= (select timed from ").append(VIEW_HELPER_TABLE).append(" where U_ID='").append(
                    streamSource.getUIDStr());
            toReturn.append("') - ").append(windowSize).append(") ");
        } else if (windowingType == WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE) {
            toReturn.append("timed >= (select distinct(timed) from ").append(wrapperAlias).append(" where timed in (select timed from ").append(wrapperAlias).append(" order by timed desc limit 1 offset ").append(windowSize - 1).append(
                    " ))");
        } else {
            if (windowingType == WindowType.TIME_BASED || windowingType == WindowType.TIME_BASED_WIN_TUPLE_BASED_SLIDE) {
                toReturn.append("timed in (select timed from ").append(wrapperAlias).append(" where timed <= (select timed from ").append(VIEW_HELPER_TABLE).append(" where U_ID='").append(Main.tableNameGeneratorInString(streamSource.getUIDStr())).append(
                        "') and timed >= (select timed from ").append(VIEW_HELPER_TABLE).append(
                        " where U_ID='").append(Main.tableNameGeneratorInString(streamSource.getUIDStr())).append("') - ").append(windowSize).append(" ) ");
            } else {
                toReturn.append("timed <= (select timed from ").append(VIEW_HELPER_TABLE).append(
                        " where U_ID='").append(streamSource.getUIDStr()).append("') and timed >= (select distinct(timed) from ");
                toReturn.append(wrapperAlias).append(" where timed in (select timed from ").append(wrapperAlias).append(
                        " where timed <= (select timed from ");
                toReturn.append(VIEW_HELPER_TABLE).append(" where U_ID='").append(streamSource.getUIDStr());
                toReturn.append("') ").append(" order by timed desc limit 1 offset ").append(windowSize - 1).append(" ))");
            }
        }

        toReturn.append(" order by timed desc ");
        toReturn = new StringBuilder(SQLUtils.newRewrite(toReturn, rewritingMapping));

        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuilder().append("The original Query : ").append(streamSource.getSqlQuery()).toString());
            logger.debug(new StringBuilder().append("The merged query : ").append(toReturn.toString()).append(" of the StreamSource ").append(streamSource.getAlias()).append(" of the InputStream: ").append(
                    streamSource.getInputStream().getInputStreamName()).append("").toString());
        }
        return cachedSqlQuery = toReturn;
    }
}
