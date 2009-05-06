package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.wrappers.AbstractWrapper;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RemoteTimeBasedSlidingHandler implements SlidingHandler {

    private static final transient Logger logger = Logger.getLogger(RemoteTimeBasedSlidingHandler.class);
    private List<StreamSource> streamSources;
    private Map<StreamSource, Long> slidingHashMap;
    private AbstractWrapper wrapper;
    private long timediff;

    public RemoteTimeBasedSlidingHandler(AbstractWrapper wrapper) {
        streamSources = Collections.synchronizedList(new ArrayList<StreamSource>());
        slidingHashMap = Collections.synchronizedMap(new HashMap<StreamSource, Long>());
        this.wrapper = wrapper;
    }

    public void addStreamSource(StreamSource streamSource) {
        SQLViewQueryHandler rewriter = new SQLViewQueryHandler();
        rewriter.setStreamSource(streamSource);
        rewriter.initialize();
        if (streamSource.getWindowingType() != WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
            slidingHashMap.put(streamSource, -1L);
        }
        streamSources.add(streamSource);
    }

    public synchronized boolean dataAvailable(StreamElement streamElement) {
        boolean toReturn = false;
        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
                    toReturn = streamSource.getQueryHandler().dataAvailable(streamElement) || toReturn;
                } else {
                    long nextSlide = slidingHashMap.get(streamSource);
                    // this is the first stream element
                    if (nextSlide == -1) {
                        slidingHashMap.put(streamSource, streamElement.getTimeStamp() + streamSource.getParsedSlideValue());
                    } else {
                        long timeStamp = streamElement.getTimeStamp();
                        if (nextSlide <= timeStamp) {
                            // long timestampDiff = timeStamp - nextSlide;
                            // int slideValue =
                            // streamSource.getParsedSlideValue();
                            // nextSlide = nextSlide + (timestampDiff /
                            // slideValue + 1) * slideValue;
                            nextSlide = timeStamp + streamSource.getParsedSlideValue();
                            toReturn = streamSource.getQueryHandler().dataAvailable(streamElement) || toReturn;
                            slidingHashMap.put(streamSource, nextSlide);
                        }
                    }
                }
            }
        }
        return toReturn;
    }

    public void removeStreamSource(StreamSource streamSource) {
        streamSources.remove(streamSource);
        slidingHashMap.remove(streamSource);
        streamSource.getQueryHandler().finilize();
    }

    public void finilize() {
        synchronized (streamSources) {
            for (Iterator<StreamSource> iterator = streamSources.iterator(); iterator.hasNext();) {
                StreamSource streamSource = iterator.next();
                streamSource.getQueryHandler().finilize();
                iterator.remove();
                slidingHashMap.remove(streamSource);
            }
        }
    }

    public long getOldestTimestamp() {
        long timed1 = -1;
        long timed2 = -1;
        int maxTupleCount = 0;
        int maxSlideForTupleBased = 0;
        int maxWindowSize = 0;

        synchronized (streamSources) {
            for (StreamSource streamSource : streamSources) {
                if (streamSource.getWindowingType() != WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE) {
                    maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedWindowSize());
                } else {
                    maxSlideForTupleBased = Math.max(maxSlideForTupleBased, streamSource.getParsedSlideValue());
                    maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedWindowSize());
                }
                if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                    maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedWindowSize() + streamSource.getParsedSlideValue());
                }
            }
        }

        if (maxWindowSize > 0) {
            StringBuilder query = new StringBuilder();
            query.append("select max(timed) - ").append(maxWindowSize).append(" from ").append(wrapper.getDBAliasInStr());

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
        if (maxTupleCount > 0) {
            StringBuilder query = new StringBuilder();

            query.append(" select timed from ").append(wrapper.getDBAliasInStr()).append(" where timed <= ");
            query.append(System.currentTimeMillis() - timediff - maxSlideForTupleBased).append(" order by timed desc limit 1 offset ").append(
                    maxTupleCount - 1);

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

    public boolean isInterestedIn(StreamSource streamSource) {
        return WindowType.isTimeBased(streamSource.getWindowingType());
    }

}
