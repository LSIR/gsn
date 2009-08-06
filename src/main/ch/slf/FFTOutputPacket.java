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
