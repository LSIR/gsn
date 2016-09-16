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
* File: src/ch/epfl/gsn/wrappers/ProxiedWrapper.java
*
* @author Ali Salehi
*
*/

package ch.epfl.gsn.wrappers;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;

import org.slf4j.Logger;

public class ProxiedWrapper {
  
  private final static transient Logger      logger         = LoggerFactory.getLogger( ProxiedWrapper.class );
  
  String remoteHost ;
  String remotePort;
  String wrapperName;
  AddressBean wrapperParams;
  
  public ProxiedWrapper(String remoteHost, String remotePort, String wrapperName, AddressBean wrapperParams) {
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.wrapperName = wrapperName;
    this.wrapperParams = wrapperParams;
  }
  
  
  
}
