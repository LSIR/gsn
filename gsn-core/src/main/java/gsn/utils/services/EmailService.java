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
* File: src/gsn/utils/services/EmailService.java
*
* @author Timotee Maret
*
*/

package gsn.utils.services;

import gsn.utils.Utils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.mail.Session;
import java.util.ArrayList;

/**
 * This class provides an access to Email Notification services.
 * The implementation is based on the Apache commons Email library @see http://commons.apache.org/email/
 * Prior to use this service, you MUST configure the SMTP server (postfix, gmail, ...) which will send your emails.
 * The smtp parameters are configured in the conf/emails.properties.
 */
public class EmailService {

    private static final transient Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String SMTP_FILE = "conf/emails.properties";

    /**
     * This method cover most of the cases of sending a simple text email. If the email to be sent has to be configured
     * in a way which is not possible whith the parameters provided (such as html email, or email with attachement, ..),
     * please use the {@link #sendCustomEmail(org.apache.commons.mail.Email)} method and refer the API
     * {@see http://commons.apache.org/email/}.
     *
     * @param to      A set of destination email address of the email. Must contain at least one address.
     * @param object  The subject of the email.
     * @param message The msg of the email.
     * @return true if the email has been sent successfully, false otherwise.
     */
    public static boolean sendEmail(ArrayList<String> to, String object, String message) {
        Email email = new SimpleEmail();
        try {
            email.setSubject(object);
            email.setMsg(message);
            if (to != null)
                for (String _to : to)
                    email.addTo(_to);
            sendCustomEmail(email);
            return true;
        }
        catch (EmailException e) {
            logger.warn("Please, make sure that the SMTP server configuration is correct in the file: " + SMTP_FILE);
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * This method send a user configured email {@link org.apache.commons.mail.Email} after having updated the
     * email session from the property file.
     *
     * @param email
     * @return true if the email has been sent successfully, false otherwise.
     * @throws org.apache.commons.mail.EmailException
     *
     */
    public static void sendCustomEmail(org.apache.commons.mail.Email email) throws EmailException {
        email.setMailSession(Session.getInstance(Utils.loadProperties(SMTP_FILE)));
        email.setDebug(true);
        email.send();
    }
}
