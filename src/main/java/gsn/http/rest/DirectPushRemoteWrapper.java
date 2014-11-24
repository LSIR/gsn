/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* File: src/gsn/http/rest/DirectPushRemoteWrapper.java
*
* @author Julien Eberle
*
*/

package gsn.http.rest;

import gsn.beans.DataField;
import org.apache.log4j.Logger;

/**
 * Same as PushRemoteWrapper, but no registration needed to initialize the push stream.
 * The notification id serves as the key to identify the pushed data and the structure has to be defined in the 
 * virtual sensor xml file.
 * This is mostly useful for getting data from network enabled sensors that are not always connected and that may change their 
 * ip address frequently. For example curl can be used to push data. See online documentation for more details.
 * @author jeberle
 *
 */
public class DirectPushRemoteWrapper extends PushRemoteWrapper {

    private final transient Logger logger = Logger.getLogger(DirectPushRemoteWrapper.class);

    private double uid = -1; //statically set from the parameters

    /**
     * get parameters from the address bean
     */
    @Override
    public boolean initialize() {
        try {
            uid = Double.parseDouble(getActiveAddressBean().getPredicateValueWithException(PushDelivery.NOTIFICATION_ID_KEY));
            structure = registerAndGetStructure();
            NotificationRegistry.getInstance().addNotification(uid, this);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            NotificationRegistry.getInstance().removeNotification(uid);
            return false;
        }
        return true;
    }

    @Override
    public String getWrapperName() {
        return "Direct Push-Remote Wrapper";
    }

    /**
     * if the structure is not defined in the xml file, throws an exception.
     */
    @Override
    public DataField[] registerAndGetStructure() throws RuntimeException{ 
    	DataField[] s = getActiveAddressBean().getOutputStructure();
    	if (s == null){throw new RuntimeException("Direct Push wrapper has an undefined output structure.");}
    	return s;
    }
    
    /**
     * Passive push. We don't actively query the node for new elements.
     */
    @Override
    public void run() {}

}
