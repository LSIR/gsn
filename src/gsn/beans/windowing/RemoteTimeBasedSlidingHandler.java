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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class RemoteTimeBasedSlidingHandler implements SlidingHandler {

	private static final transient Logger logger = Logger.getLogger(RemoteTimeBasedSlidingHandler.class);

	private ArrayList<StreamSource> streamSources;

	private HashMap<StreamSource, Long> slidingHashMap;

	private AbstractWrapper wrapper;

	public RemoteTimeBasedSlidingHandler(AbstractWrapper wrapper) {
		streamSources = new ArrayList<StreamSource>();
		slidingHashMap = new HashMap<StreamSource, Long>();
		this.wrapper = wrapper;
	}

	public void addStreamSource(StreamSource streamSource) {
		SQLViewQueryRewriter rewriter = new RTBSQLViewQueryRewriter();
		rewriter.setStreamSource(streamSource);
		streamSource.setQueryRewriter(rewriter);
		rewriter.initialize();
		if (streamSource.getWindowingType() != WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
			slidingHashMap.put(streamSource, -1L);
		}
		streamSources.add(streamSource);
	}

	public boolean dataAvailable(StreamElement streamElement) {
		synchronized (streamSources) {
			for (StreamSource streamSource : streamSources) {
				if (streamSource.getWindowingType() == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE)
					streamSource.getQueryRewriter().dataAvailable(streamElement.getTimeStamp());
				else {
					long nextSlide = slidingHashMap.get(streamSource);
					// this is the first stream element
					if (nextSlide == -1) {
						if (streamSource.getWindowingType() == WindowType.TIME_BASED)
							slidingHashMap.put(streamSource, streamElement.getTimeStamp() + streamSource.getParsedStorageSize());
						else
							slidingHashMap.put(streamSource, streamElement.getTimeStamp() + streamSource.getParsedSlideValue());
					} else {
						long timeStamp = streamElement.getTimeStamp();
						if (nextSlide <= timeStamp) {
//							long timestampDiff = timeStamp - nextSlide;
//							int slideValue = streamSource.getParsedSlideValue();
//							nextSlide = nextSlide + (timestampDiff / slideValue + 1) * slideValue;
							nextSlide = timeStamp + streamSource.getParsedSlideValue();
							streamSource.getQueryRewriter().dataAvailable(timeStamp);
							slidingHashMap.put(streamSource, nextSlide);
						}
					}
				}
			}
		}
		return true;
	}

	public void removeStreamSource(StreamSource streamSource) {
		streamSources.remove(streamSource);
		slidingHashMap.remove(streamSource);
		streamSource.getQueryRewriter().finilize();
	}

	public void finilize() {
		synchronized (streamSources) {
			for (Iterator<StreamSource> iterator = streamSources.iterator(); iterator.hasNext();) {
				StreamSource streamSource = iterator.next();
				streamSource.getQueryRewriter().finilize();
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
				if (streamSource.getWindowingType() != WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE)
					maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedStorageSize());
				else {
					maxSlideForTupleBased = Math.max(maxSlideForTupleBased, streamSource.getParsedSlideValue());
					maxTupleCount = Math.max(maxTupleCount, streamSource.getParsedStorageSize());
				}
				if (streamSource.getWindowingType() == WindowType.TIME_BASED)
					maxWindowSize = Math.max(maxWindowSize, streamSource.getParsedStorageSize() + streamSource.getParsedSlideValue());
			}
		}

		if (maxWindowSize > 0) {
			StringBuilder query = new StringBuilder();
			query.append("select max(timed) - ").append(maxWindowSize).append(" from ").append(wrapper.getDBAliasInStr());

			if (logger.isDebugEnabled()) {
				logger.debug("Query1 for getting oldest timestamp : " + query);
			}

			try {
				ResultSet resultSet = StorageManager.getInstance().executeQueryWithResultSet(query);
				if (resultSet.next())
					timed1 = resultSet.getLong(1);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (maxTupleCount > 0) {
			StringBuilder query = new StringBuilder();
			if (StorageManager.isHsql() || StorageManager.isMysqlDB()) {
				query.append(" select min(timed) from (select timed from ").append(wrapper.getDBAliasInStr()).append(" where timed <= ");
				query.append(System.currentTimeMillis() - maxSlideForTupleBased).append(" order by timed desc limit 1 offset ").append(
						maxTupleCount).append(") as X ");
			} else if (StorageManager.isSqlServer()) {
				query.append(" select min(timed) from (select top ").append(maxTupleCount).append(" * ").append(" from ").append(
						wrapper.getDBAliasInStr()).append(" where timed <= ").append(System.currentTimeMillis() - maxSlideForTupleBased)
						.append(" order by timed desc) as X  ");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Query2 for getting oldest timestamp : " + query);
			}

			try {
				ResultSet resultSet = StorageManager.getInstance().executeQueryWithResultSet(query);
				if (resultSet.next())
					timed2 = resultSet.getLong(1);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}

		if (timed1 >= 0 && timed2 >= 0)
			return Math.min(timed1, timed2);
		return (timed1 == -1) ? timed2 : timed1;
	}

	public boolean isInterestingIn(StreamSource streamSource) {
		return WindowType.isTimeBased(streamSource.getWindowingType());
	}

	private class RTBSQLViewQueryRewriter extends SQLViewQueryRewriter {
		@Override
		public CharSequence createViewSQL() {
			if (cachedSqlQuery != null)
				return cachedSqlQuery;
			if (streamSource.getWrapper() == null)
				throw new GSNRuntimeException("Wrapper object is null, most probably a bug, please report it !");
			if (streamSource.validate() == false)
				throw new GSNRuntimeException("Validation of this object the stream source failed, please check the logs.");
			CharSequence wrapperAlias = streamSource.getWrapper().getDBAliasInStr();
			int windowSize = streamSource.getParsedStorageSize();
			if (streamSource.getSamplingRate() == 0 || (streamSource.isStorageCountBased() && windowSize == 0))
				return cachedSqlQuery = new StringBuilder("select * from ").append(wrapperAlias).append(" where 1=0");
			TreeMap<CharSequence, CharSequence> rewritingMapping = new TreeMap<CharSequence, CharSequence>(new CaseInsensitiveComparator());
			rewritingMapping.put("wrapper", wrapperAlias);
			StringBuilder toReturn = new StringBuilder(streamSource.getSqlQuery());
			if (streamSource.getSqlQuery().toLowerCase().indexOf(" where ") < 0)
				toReturn.append(" where ");
			else
				toReturn.append(" and ");
			// Applying the ** START AND END TIME ** for all types of windows
			// based
			// windows
			toReturn.append(" wrapper.timed >=").append(streamSource.getStartDate().getTime()).append(" and timed <=").append(
					streamSource.getEndDate().getTime());

			if (streamSource.getSamplingRate() != 1)
				toReturn.append(" and ( mod( timed , 100)< ").append(streamSource.getSamplingRate() * 100).append(")");

			toReturn.append(" and ");

			WindowType windowingType = streamSource.getWindowingType();
			if (windowingType == WindowType.TIME_BASED_SLIDE_ON_EACH_TUPLE) {
				toReturn.append("(wrapper.timed >= (select timed from ").append(VIEW_HELPER_TABLE).append(" where UID='").append(
						streamSource.getUIDStr());
				toReturn.append("') - ").append(windowSize).append(") ");
				if (StorageManager.isHsql() || StorageManager.isMysqlDB())
					toReturn.append(" order by timed desc ");
			} else {
				if (windowingType == WindowType.TIME_BASED) {
					toReturn.append("timed in (select timed from ").append(wrapperAlias).append(" where timed <= (select timed from ")
							.append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(" where UID='").append(streamSource.getUIDStr()).append(
									"') and timed >= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(
									" where UID='").append(streamSource.getUIDStr()).append("') - ").append(windowSize).append(" ) ");
					if (StorageManager.isHsql() || StorageManager.isMysqlDB())
						toReturn.append(" order by timed desc ");
				} else {// WindowType.TUPLE_BASED_WIN_TIME_BASED_SLIDE
					if (StorageManager.isHsql() || StorageManager.isMysqlDB())
						toReturn.append("timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE).append(
								" where UID='").append(streamSource.getUIDStr()).append("') order by timed desc limit ").append(windowSize);
					else if (StorageManager.isSqlServer())
						toReturn.append("timed in (select TOP ").append(windowSize).append(" timed from ").append(wrapperAlias).append(
								" where timed <= (select timed from ").append(SQLViewQueryRewriter.VIEW_HELPER_TABLE)
								.append(" where UID='").append(streamSource.getUIDStr()).append("') order by timed desc ) ");
				}
			}

			toReturn = new StringBuilder(SQLUtils.newRewrite(toReturn, rewritingMapping));

			if (logger.isDebugEnabled()) {
				logger.debug(new StringBuilder().append("The original Query : ").append(streamSource.getSqlQuery()).toString());
				logger.debug(new StringBuilder().append("The merged query : ").append(toReturn.toString()).append(" of the StreamSource ")
						.append(streamSource.getAlias()).append(" of the InputStream: ").append(
								streamSource.getInputStream().getInputStreamName()).append("").toString());
			}
			return cachedSqlQuery = toReturn;
		}
	}
}
