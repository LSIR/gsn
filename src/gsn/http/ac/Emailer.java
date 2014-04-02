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
* File: src/gsn/http/ac/Emailer.java
*
* @author Behnaz Bostanipour
*
*/

package gsn.http.ac;

import gsn.utils.services.EmailService;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Behnaz Bostanipour
 * Date: Apr 27, 2010
 * Time: 7:03:49 PM
 * To change this template use File | Settings | File Templates.
 */

/* This class is used to send E-mails from a AC servlet, the Email sent encrypted,
  we are using EPFL SMTP server, so the parameters are fixed for that server */

public class Emailer
{
    private static transient Logger logger                             = Logger.getLogger( Emailer.class );

    public void sendEmail( String senderAlias, String receiverName,String receiverEmail,String subject, String msgHead, String msgBody, String msgTail)
    {

        ArrayList<String> to = new ArrayList<String>();
        to.add(receiverEmail);
        EmailService.sendEmail(to, subject,  msgHead + msgBody + msgTail);

    }



}
