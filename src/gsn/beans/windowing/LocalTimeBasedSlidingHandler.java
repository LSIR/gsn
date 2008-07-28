package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.SQLUtils;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.GSNRuntimeException;
import gsn.wrappers.AbstractWrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class LocalTimeBasedSlidingHandler implements SlidingHandler {

    private static final transient Logger logger = Logger.getLogger(LocalTimeBasedSlidingHandler.class);
    private static int timerCount = 0;
    private List<StreamSource> streamSources;
    private AbstractWrapper wrapper;
    private Timer timer;
    private int timerTick = -1;
    private Map<StreamSource, Integer> slidingHashMap;

    public LocalTimeBasedSlidingHandler(AbstractWrapper wrapper) {
        streamSources = Collections.synchronizedList(new ArrayList<StreamSource>());
        slidingHashMap = Collections.synchronizedMap(new HashMap<StreamSource, Integer>());
        timer = new Timer("LocalTimeBasedSlidingHandlerTimer" + (++timerCount));
        this.wrapper = wrapper;
    }

    public void addStreamSource(StreamSource streamSource) {
        SQLViewQueryRewriter rewriter = new LTBSQLViewQueryRewriter();
        rewriter.setStreamSource(streamSource);
        rewriter.initialize();
        if (streamSource.getWindowingType() != WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
            int oldTimerTick = timerTick;
            if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                slidingHashMap.put(streamSource, streamSource.getParsedSlideValue() - streamSource.getParsedStorageSize());
                if (timerTick == -1) {
                    timerTick = GCD(streamSource.getParsedStorageSize(), streamSource.getParsedSlideValue());
                } else {
                    timerTick = GCD(timerTick, GCD(streamSource.getParsedStorageSize(), streamSource.getParsedSlideValue()));
                }
            } else {
                slidingHashMap.put(streamSource, 0);
                if (timerTick == -1) {
                    timerTick = streamSource.getParsedSlideValue();
                } else {
                    timerTick = GCD(timerTick, streamSource.getParsedSlideValue());
                }
            }
            if (oldTimerTick != timerTick) {
                timer.cancel();
                timer = new Timer();
                if (logger.isDebugEnabled()) {
                    logger.debug("About to schedule new timer task at period " + timerTick + "ms in the " + wrapper.getDBAliasInStr() + " wrapper");
                }
                timer.schedule(new LTBTimerTask(), 500, timerTick);
            }
        } else {
            streamSources.add(streamSource);
        }
    }

    public int GCD(int a, int b) {
        return WindowingUtil.GCD(a, b);
    }

    private class LTBTimerTask extends TimerTask {

        @Override
        public void run() {
            synchronized (slidingHashMap) {
                for (StreamSource streamSource : slidingHashMap.keySet()) {
                    int slideVar = slidingHashMap.get(streamSource) + timerTick;
                    if (slideVar >= streamSource.getParsedSlideValue()) {
                        slideVar = 0;
                        streamSource.getQueryRewriter().dataAvailable(System.currentTimeMillis());
                    }
                    slidingHashMap.put(streamSource, slideVar);
                }
            }
        }
    }

    public boolean dataAvailable(StreamElement streamElement) {
        boolean toReturn = false;
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
                    toReturn = streamSource.getQueryRewriter().dataAvailable(streamElement.getTimeStamp()) || toReturn;
                }
            }
        }
        return toReturn;
    }

    public long getOldestTimestamp() {
        long timed1 = -1;
        long timed2 = -1;
        int maxTupleCount = 0;
        int maxSlideForTupleBased = 0;
        int maxWindowSize = 0;

        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedStorageSize());
            }
        }

        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                    maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedStorageSize() + streamSource.getParsedSlideValue());
                } else {
                    maxSlideForTupleBased = Math.max(maxSlideForTupleBased, streamSource.getParsedSlideValue());
                    maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedStorageSize());
                }
            }
        }

        if (maxWindowSize > 0) {
            timed1 = System.currentTimeMillis() - maxWindowSize;
        }

        if (maxTupleCount > 0) {
            StringBuilder query = new StringBuilder();
            if (StorageManager.isH2() || StorageManager.isMysqlDB()) {
                query.append(" select timed from ").append(wrapper.getDBAliasInStr()).append(" where timed <= ");
                query.append(System.currentTimeMillis() - maxSlideForTupleBased).append(" order by timed desc limit 1 offset ").append(
                        maxTupleCount - 1);
            } else if (StorageManager.isSqlServer()) {
                query.append(" select min(timed) from (select top ").append(maxTupleCount).append(" * ").append(" from ").append(
                        wrapper.getDBAliasInStr()).append(" where timed <= ").append(System.currentTimeMillis() - maxSlideForTupleBased).append(" order by timed desc) as X  ");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Query for getting oldest timestamp : " + query);
            }
            ResultSet resultSet = null;
            try {
                resultSet = StorageManager.getInstance().executeQueryWithResultSet(query);
                if (resultSet.next()) {
                    timed2 = resultSet.getLong(1);
                } else {
                    return -1;
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            } finally {
                StorageManager.close(resultSet);
            }
        }

        if (timed1 >= 0 && timed2 >= 0) {
            return Math.min(timed1, timed2);
        }

        return (timed1 == -1) ? timed2 : timed1;
    }

    public void removeStreamSource(StreamSource streamSource) {
        streamSources.remove(streamSource);
        slidingHashMap.remove(streamSource);
        streamSource.getQueryRewriter().finilize();
        updateTimerTick();
    }

    private void updateTimerTick() {
        int oldTimerTick = timerTick;
        // recalculating timer tick
        timerTick = -1;
        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                    slidingHashMap.put(streamSource, streamSource.getParsedSlideValue() - streamSource.getParsedStorageSize());
                    if (timerTick == -1) {
                        timerTick = GCD(streamSource.getParsedStorageSize(), streamSource.getParsedSlideValue());
                    } else {
                        timerTick = GCD(timerTick, GCD(streamSource.getParsedStorageSize(), streamSource.getParsedSlideValue()));
                    }
                } else {
                    slidingHashMap.put(streamSource, 0);
                    if (timerTick == -1) {
                        timerTick = streamSource.getParsedSlideValue();
                    } else {
                        timerTick = GCD(timerTick, streamSource.getParsedSlideValue());
                    }
                }
            }
        }
        if (oldTimerTick != timerTick && timerTick > 0) {
            timer.cancel();
            timer = new Timer();
            if (logger.isDebugEnabled()) {
                logger.debug("About to schedule new timer task at period " + timerTick + "ms in the " + wrapper.getDBAliasInStr() + " wrapper");
            }
            timer.schedule(new LTBTimerTask(), 500, timerTick);
        }
    }

    public void finilize() {
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                streamSource.getQueryRewriter().finilize();
            }
            streamSources.clear();
        }
        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                streamSource.getQueryRewriter().finilize();
            }
            slidingHashMap.clear();
        }
    }

    public boolean isInterestedIn(StreamSource streamSource) {
        return WindowType.isTimeBased(streamSource.getWindowingType());
    }

    private class LTBSQLViewQueryRewriter extends SQLViewQueryRewriter {

        @Override
        public CharSequence createViewSQL() {
            if (cachedSqlQuery != null) {
                return cachedSqlQuery;
            }
            if (streamSource.getWrapper() == null) {
                throw new GSNRuntimeException("Wrapper object is null, most probably a bug, please report it !");
            }
            if (streamSource.validate() == false) {
                throw new GSNRuntimeException("Validation of this object the stream source failed, please check the logs.");
            }
            CharSequence wrapperAlias = streamSource.getWrapper().getDBAliasInStr();
            int windowSize = streamSource.getParsedStorageSize();
            if (streamSource.getSamplingRate() == 0 || windowSize == 0) {
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
                if (StorageManager.isH2()) {
                    toReturn.append("( timed - (timed / 100) * 100 < ").append(streamSource.getSamplingRate() * 100).append(") and ");
                } else {
                    toReturn.append("( mod( timed , 100)< ").append(streamSource.getSamplingRate() * 100).append(") and ");
                }
            }

            WindowType windowingType = streamSource.getWindowingType();
            if (windowingType == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {

                toReturn.append("(wrapper.timed >");
                if (StorageManager.isH2()) {
                    toReturn.append(" (NOW_MILLIS()");
                } else if (StorageManager.isMysqlDB()) {
                    toReturn.append(" (UNIX_TIMESTAMP()*1000");
                } else if (StorageManager.isSqlServer()) {
                    // NOTE1 : The value retuend is in seconds (hence 1000)
                    // NOTE2 : There is no time in the date for the epoch, maybe
                    // doesn't match with the current system time, needs
                    // checking.
                    toReturn.append(" (convert(bigint,datediff(second,'1/1/1970',current_timestamp))*1000 )");
                }

                long timeDifferenceInMillis = storageManager.getTimeDifferenceInMillis();
                // System.out.println(timeDifferenceInMillis);
                toReturn.append(" - ").append(windowSize).append(" - ").append(timeDifferenceInMillis).append(" )");
                if (StorageManager.isH2() || StorageManager.isMysqlDB()) {
                    toReturn.append(") order by timed desc ");
                }

            } else {
                if (windowingType == WindowType.TIME_BASED) {

                    toReturn.append("timed in (select timed from ").append(wrapperAlias).append(" where timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(" where UID='").append(streamSource.getUIDStr()).append(
                            "') and timed >= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(
                            " where UID='").append(streamSource.getUIDStr()).append("') - ").append(windowSize).append(" ) ");
                    if (StorageManager.isH2() || StorageManager.isMysqlDB()) {
                        toReturn.append(" order by timed desc ");
                    }

                } else {// WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE

                    if (StorageManager.isMysqlDB()) {
                        toReturn.append("timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(
                                " where UID='").append(streamSource.getUIDStr()).append("') and timed >= (select timed from ");
                        toReturn.append(wrapperAlias).append(" where timed <= (select timed from ");
                        toReturn.append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(" where UID='").append(streamSource.getUIDStr());
                        toReturn.append("') ").append(" order by timed desc limit 1 offset ").append(windowSize - 1).append(" )");
                        toReturn.append(" order by timed desc ");
                    } else if (StorageManager.isH2()) {
                        toReturn.append("timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(
                                " where UID='").append(streamSource.getUIDStr()).append("') and timed >= (select distinct(timed) from ");
                        toReturn.append(wrapperAlias).append(" where timed in (select timed from ").append(wrapperAlias).append(
                                " where timed <= (select timed from ");
                        toReturn.append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(" where UID='").append(streamSource.getUIDStr());
                        toReturn.append("') ").append(" order by timed desc limit 1 offset ").append(windowSize - 1).append(" ))");
                        toReturn.append(" order by timed desc ");
                    } else if (StorageManager.isSqlServer()) {
                        toReturn.append("timed in (select TOP ").append(windowSize).append(" timed from ").append(wrapperAlias).append(
                                " where timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(" where UID='").append(streamSource.getUIDStr()).append("') order by timed desc ) ");
                    }
                }
            }

            toReturn = new StringBuilder(SQLUtils.newRewrite(toReturn, rewritingMapping));

            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuilder().append("The original Query : ").append(streamSource.getSqlQuery()).toString());
                logger.debug(new StringBuilder().append("The merged query : ").append(toReturn.toString()).append(" of the StreamSource ").append(streamSource.getAlias()).append(" of the InputStream: ").append(
                        streamSource.getInputStream().getInputStreamName()).append("").toString());
            }
            return cachedSqlQuery = toReturn;
        }
    }
}
