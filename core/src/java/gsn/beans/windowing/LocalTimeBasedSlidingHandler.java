package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.wrappers.AbstractWrapper;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
        SQLViewQueryHandler rewriter = new SQLViewQueryHandler();
        rewriter.setStreamSource(streamSource);
        rewriter.initialize();
        if (streamSource.getWindowingType() != WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
            int oldTimerTick = timerTick;
            if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                slidingHashMap.put(streamSource, streamSource.getParsedSlideValue() - streamSource.getParsedWindowSize());
                if (timerTick == -1) {
                    timerTick = GCD(streamSource.getParsedWindowSize(), streamSource.getParsedSlideValue());
                } else {
                    timerTick = GCD(timerTick, GCD(streamSource.getParsedWindowSize(), streamSource.getParsedSlideValue()));
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
//                        streamSource.getQueryHandler().dataAvailable(System.currentTimeMillis());
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
                    toReturn = streamSource.getQueryHandler().dataAvailable(streamElement) || toReturn;
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
                maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedWindowSize());
            }
        }

        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                    maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedWindowSize() + streamSource.getParsedSlideValue());
                } else {
                    maxSlideForTupleBased = Math.max(maxSlideForTupleBased, streamSource.getParsedSlideValue());
                    maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedWindowSize());
                }
            }
        }

        if (maxWindowSize > 0) {
            timed1 = System.currentTimeMillis() - maxWindowSize;
        }

        if (maxTupleCount > 0) {
            StringBuilder query = new StringBuilder();
            query.append(" select timed from ").append(wrapper.getDBAliasInStr()).append(" where timed <= ");
            query.append(System.currentTimeMillis() - maxSlideForTupleBased).append(" order by timed desc limit 1 offset ").append(
                    maxTupleCount - 1);

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
        streamSource.getQueryHandler().finilize();
        updateTimerTick();
    }

    private void updateTimerTick() {
        int oldTimerTick = timerTick;
        // recalculating timer tick
        timerTick = -1;
        synchronized (slidingHashMap) {
            for (StreamSource streamSource : slidingHashMap.keySet()) {
                if (streamSource.getWindowingType() == WindowType.TIME_BASED) {
                    slidingHashMap.put(streamSource, streamSource.getParsedSlideValue() - streamSource.getParsedWindowSize());
                    if (timerTick == -1) {
                        timerTick = GCD(streamSource.getParsedWindowSize(), streamSource.getParsedSlideValue());
                    } else {
                        timerTick = GCD(timerTick, GCD(streamSource.getParsedWindowSize(), streamSource.getParsedSlideValue()));
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
        return WindowType.isTimeBased(streamSource.getWindowingType());
    }

}
