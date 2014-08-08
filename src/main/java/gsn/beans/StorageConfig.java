/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
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
* File: src/gsn/beans/StorageConfig.java
*
* @author Timotee Maret
*
*/

package gsn.beans;

public class StorageConfig {

    private String jdbcDriver;

    private String jdbcUsername;

    private String jdbcPassword;

    private String jdbcURL;

    private String identifier;

    private String storageSize;

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcURL() {
        return jdbcURL;
    }

    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }

    public String getStorageSize() {
        return storageSize;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setStorageSize(String storageSize) {
        this.storageSize = storageSize;
    }

    public boolean isStorageSize() {
        return storageSize != null;
    }

    public boolean isJdbcDefined() {
        return jdbcDriver != null
                && jdbcPassword != null
                && jdbcURL != null
                && jdbcUsername != null;
    }

    public boolean isIdentifierDefined() {
        return identifier != null;
    }

    public boolean isDefined() {
        return isJdbcDefined() || isIdentifierDefined();
    }

    

}
