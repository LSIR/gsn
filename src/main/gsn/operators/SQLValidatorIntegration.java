package gsn.operators;

import gsn.core.VSensorStateChangeListener;
import gsn.storage.SQLValidator;
import gsn.storage.StorageManager;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import gsn2.conf.OperatorConfig;

public class SQLValidatorIntegration implements VSensorStateChangeListener{
	
	private SQLValidator validator;
	
	public SQLValidatorIntegration(SQLValidator validator) throws SQLException {
		this.validator = validator;
	}
	

	private static final transient Logger logger = Logger.getLogger(SQLValidatorIntegration.class);

	public boolean vsLoading(OperatorConfig config) {
//		try {
//			String ddl = StorageManager.getStatementCreateTable(config.getName(), config.getProcessingClassConfig().getOutputFormat(), validator.getSampleConnection()).toString();
//			validator.executeDDL(ddl);
//		}catch (Exception e) {
//			logger.error(e.getMessage(),e);
//		}
		return true;
	}

	public boolean vsUnLoading(OperatorConfig config) {
		try {
			String ddl = StorageManager.getStatementDropTable(config.getIdentifier(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	public void dispose()  {
		validator.dispose();
	}
}
