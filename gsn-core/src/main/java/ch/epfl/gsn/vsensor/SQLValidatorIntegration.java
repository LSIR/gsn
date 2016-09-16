/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/vsensor/SQLValidatorIntegration.java
*
* @author Ali Salehi
* @author Timotee Maret
*
*/

package ch.epfl.gsn.vsensor;

import java.sql.SQLException;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.VSensorStateChangeListener;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.storage.SQLValidator;
import ch.epfl.gsn.storage.StorageManager;
import ch.epfl.gsn.vsensor.SQLValidatorIntegration;

import org.slf4j.Logger;

public class SQLValidatorIntegration implements VSensorStateChangeListener{
	
	private SQLValidator validator;
	
	public SQLValidatorIntegration(SQLValidator validator) throws SQLException {
		this.validator = validator;
	}
	

	private static final transient Logger logger = LoggerFactory.getLogger(SQLValidatorIntegration.class);

	public boolean vsLoading(VSensorConfig config) {
		try {
            String ddl = Main.getValidationStorage().getStatementCreateTable(config.getName(), config.getOutputStructure(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return true;
	}

	public boolean vsUnLoading(VSensorConfig config) {
		try {
			String ddl = Main.getValidationStorage().getStatementDropTable(config.getName(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	public void release() throws Exception {
		validator.release();
		
	}
}
