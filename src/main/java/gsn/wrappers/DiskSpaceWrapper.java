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
* File: src/gsn/wrappers/DiskSpaceWrapper.java
*
* @author Mehdi Riahi
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.File;
import java.io.Serializable;

import org.apache.log4j.Logger;

public class DiskSpaceWrapper extends AbstractWrapper{
    
    private static final int            DEFAULT_SAMPLING_RATE       = 1000;
    
    private int                         samplingRate                = DEFAULT_SAMPLING_RATE;
    
    private final transient Logger      logger                      = Logger.getLogger(DiskSpaceWrapper.class);
    
    private static int                  threadCounter               = 0;
    
    private transient DataField[]       outputStructureCache        = new DataField[]{new DataField("FREE_SPACE", "bigint", "Free Disk Space")};

    private File[] roots;
    
      
    public boolean initialize() {
        logger.info("Initializing DiskSpaceWrapper Class");
        String javaVersion = System.getProperty("java.version");
        if(!javaVersion.startsWith("1.6")){
            logger.error("Error in initializing DiskSpaceWrapper because of incompatible jdk version: " + javaVersion + " (should be 1.6.x)");
            return false;
        }
        return true;
    }
    
    public void run(){
        while(isActive()){
            try{
                Thread.sleep(samplingRate);
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }
            roots = File.listRoots();
            long totalFreeSpace = 0;
            for (int i = 0; i < roots.length; i++) {
                totalFreeSpace += roots[i].getFreeSpace();
            }
            
            //convert to MB
            totalFreeSpace = totalFreeSpace / (1024 * 1024);
            StreamElement streamElement = new StreamElement(new String[]{"FREE_SPACE"}, new Byte[]{DataTypes.BIGINT}, new Serializable[] {totalFreeSpace
            },System.currentTimeMillis());
            postStreamElement(streamElement);
        }
    }
    
    public void dispose() {
        threadCounter--;
    }
    
    public String getWrapperName() {
        return "Free Disk Space";
    }
    
    public DataField[] getOutputFormat() {
        return outputStructureCache;
    }
    
}
