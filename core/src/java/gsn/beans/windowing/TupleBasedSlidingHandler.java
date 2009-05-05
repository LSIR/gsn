package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.wrappers.AbstractWrapper;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TupleBasedSlidingHandler implements SlidingHandler {

    private static final transient Logger logger = Logger.getLogger(TupleBasedSlidingHandler.class);
    private List<StreamSource> streamSources; //only holds WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE types of stream sources
    private Map<StreamSource, Integer> slidingHashMap;
    private AbstractWrapper wrapper;

    public TupleBasedSlidingHandler(AbstractWrapper wrapper) {
        streamSources = Collections.synchronizedList(new ArrayList<StreamSource>());
        slidingHashMap = Collections.synchronizedMap(new HashMap<StreamSource, Integer>());
        this.wrapper = wrapper;
    }

    public void addStreamSource(StreamSource streamSource) {
        if (streamSource.getWindowingType() != WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE) {
            if (streamSource.getWindowingType() == WindowType.TUPLE_BASED) {
                slidingHashMap.put(streamSource, streamSource.getParsedSlideValue() - streamSource.getParsedWindowSize());
            } else {
                slidingHashMap.put(streamSource, 0);
            }
        } else {
            streamSources.add(streamSource);
        }
        SQLViewQueryHandler rewriter = new SQLViewQueryHandler();
        rewriter.setStreamSource(streamSource);
        rewriter.initialize();
    }

    public boolean dataAvailable(StreamElement streamElement) {
        boolean toReturn = false;
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                toReturn = streamSource.getQueryHandler().dataAvailable(streamElement) || toReturn;
            }
        }
        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                int slideVar = slidingHashMap.get(streamSource) + 1;
                if (slideVar == streamSource.getParsedSlideValue()) {
                    toReturn = streamSource.getQueryHandler().dataAvailable(streamElement) || toReturn;
                    slideVar = 0;
                }
                slidingHashMap.put(streamSource, slideVar);
            }
        }
        return toReturn;
    }

    public long getOldestTimestamp() {
        long timed1 = -1;
        long timed2 = -1;
        int maxTupleCount = 0;
        int maxTupleForTimeBased = 0;
        int maxWindowSize = 0;

        //WindowType.TUPLE_BASED_SLIDE_ON_EACH_TUPLE sliding windows are saved in streamSources list
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedWindowSize());
            }
        }

        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                if (streamSource.getWindowingType() == WindowType.TUPLE_BASED) {
                    maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedWindowSize() + streamSource.getParsedSlideValue());
                } else {
                    maxTupleForTimeBased = Math.max(maxTupleForTimeBased, streamSource.getParsedSlideValue());
                    maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedWindowSize());
                }
            }
        }

        if (maxTupleCount > 0) {
            StringBuilder query = new StringBuilder();

            query.append(" select timed from ").append(wrapper.getDBAliasInStr());
            query.append(" order by timed desc limit 1 offset ").append(maxTupleCount - 1);

            if (logger.isDebugEnabled()) {
                logger.debug("Query1 for getting oldest timestamp : " + query);
            }
            ResultSet resultSet = null;
            try {
                resultSet = StorageManager.getInstance().executeQueryWithResultSet(query);
                if (resultSet.next()) {
                    timed1 = resultSet.getLong(1);
                } else {
                    return -1;
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            } finally {
                StorageManager.close(resultSet);
            }
        }

        if (maxWindowSize > 0) {
            StringBuilder query = new StringBuilder();

            query.append(" select timed - ").append(maxWindowSize).append(" from ").append(wrapper.getDBAliasInStr()).append(
                    " where timed in (select timed from ").append(wrapper.getDBAliasInStr());
            query.append(" order by timed desc limit 1 offset ").append(maxTupleForTimeBased - 1).append(" ) ");

            if (logger.isDebugEnabled()) {
                logger.debug("Query2 for getting oldest timestamp : " + query);
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
        streamSource.getQueryHandler().finilize();
    }

    public void finilize() {
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                streamSource.getQueryHandler().finilize();
            }
            streamSources.clear();
        }
        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                streamSource.getQueryHandler().finilize();
            }
            slidingHashMap.clear();
        }
    }

    public boolean isInterestedIn(StreamSource streamSource) {
        return WindowType.isTupleBased(streamSource.getWindowingType());
    }
    
}
