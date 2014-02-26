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
* File: src/ch/slf/FFTOutputPacket.java
*
* @author Ali Salehi
*
*/

package ch.slf;

import java.util.ArrayList;

public class FFTOutputPacket {
  private long timestamp;
  private double df;
  private ArrayList<Double> values = new ArrayList<Double>();
  
  public long getTimestamp() {
    return timestamp;
  }
  public ArrayList<Double> getValues() {
    return values;
  }
  public FFTOutputPacket(long timestamp) {
    this.timestamp = timestamp;
  }
  public void addValue(double val) {
    values.add(val);
  }
  public double getDf() {
    return df;
  }
  public void setdf(double df) {
    this.df = df;
  }
  public void addValues(double[] results) {
    for (double v : results)
      values.add(v);
  }
  public void setValues(double[] results) {
    values.clear();
    for (double v : results)
      values.add(v);
  }
  public void reset() {
    values.clear();
  }
  
  
}
