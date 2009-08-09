package gsn.operators;

import gsn.core.OpStateChangeListener;
import gsn.storage.SQLValidator;
import gsn.storage.StorageManager;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.picocontainer.MutablePicoContainer;
import gsn2.conf.OperatorConfig;

public class SQLValidatorIntegration implements OpStateChangeListener {
	
	private SQLValidator validator;
	
	public SQLValidatorIntegration(SQLValidator validator) throws SQLException {
		this.validator = validator;
	}
	

	private static final transient Logger logger = Logger.getLogger(SQLValidatorIntegration.class);

	public void opLoading(MutablePicoContainer config) {
//		try {
//			String ddl = StorageManager.getStatementCreateTable(config.getName(), config.getProcessingClassConfig().getOutputFormat(), validator.getSampleConnection()).toString();
//			validator.executeDDL(ddl);
//		}catch (Exception e) {
//			logger.error(e.getMessage(),e);
//		}
	}

	public void opUnLoading(MutablePicoContainer config) {
		try {
			String ddl = StorageManager.getStatementDropTable(config.getComponent(OperatorConfig.class).getIdentifier(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	public void dispose()  {
		validator.dispose();
	}
}
