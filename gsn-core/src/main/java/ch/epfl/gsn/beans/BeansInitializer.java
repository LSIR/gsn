package ch.epfl.gsn.beans;


import java.util.ArrayList;

import org.apache.commons.collections.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.AddressBean;
import ch.epfl.gsn.beans.BeansInitializer;
import ch.epfl.gsn.beans.ContainerConfig;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.InputStream;
import ch.epfl.gsn.beans.SlidingConfig;
import ch.epfl.gsn.beans.StorageConfig;
import ch.epfl.gsn.beans.StreamSource;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.beans.WebInput;
import ch.epfl.gsn.utils.KeyValueImp;
import scala.collection.JavaConversions;
import scala.collection.Map;
import ch.epfl.gsn.config.*;


public class BeansInitializer {
  private transient static final Logger logger = LoggerFactory.getLogger( BeansInitializer.class );

  public static ContainerConfig container(GsnConf gsn){
	  SlidingConfig sliding = new SlidingConfig();
	  if (gsn.slidingConf().isDefined())
		  sliding.setStorage(storage(gsn.slidingConf().get()));
	  else sliding=null;
	 ContainerConfig con=new ContainerConfig(			 
			 gsn.monitorPort(),gsn.timeFormat(),			 
			 gsn.zmqConf().enabled(),gsn.zmqConf().proxyPort(),gsn.zmqConf().metaPort(),
			 storage(gsn.storageConf()),sliding);
	
	 return con;
  }
  public static StorageConfig storage(StorageConf st){
	 StorageConfig con=new StorageConfig();
	 if (st.identifier().isDefined())
	   con.setIdentifier(st.identifier().get());
	 else con.setIdentifier(null);
	 con.setJdbcDriver(st.driver());
	 con.setJdbcURL(st.url());
	 con.setJdbcUsername(st.user());
	 con.setJdbcPassword(st.pass());
	 return con;
  }
  
  public static DataField dataField(FieldConf fc){
	  DataField f=new DataField();
	  f.setName(fc.name().toLowerCase());
	  f.setType(fc.dataType());
	  f.setDescription(fc.description());
	  if (fc.unit().isDefined())
	    f.setUnit(fc.unit().get());
	  else f.setUnit(null);
	  return f;
  }
  
  public static WebInput webInput(WebInputCommand wi){
	  WebInput w=new WebInput();
	  DataField [] par=new DataField[(wi.params().size())];
	  for (int i=0;i<par.length;i++){
		  par[i]=dataField(wi.params().apply(i));
	  }
	  w.setParameters(par);
	  w.setName(wi.name());
	  return w;
  }
  
  public static StreamSource source(SourceConf sc){
	  StreamSource s = new StreamSource();
	  s.setAlias(sc.alias());
	  s.setSqlQuery(sc.query());
	  if (sc.slide().isDefined())
		  s.setRawSlideValue(sc.slide().get());
	  if (sc.samplingRate().isDefined())
		  s.setSamplingRate(((Double)sc.samplingRate().get()).floatValue());
	  if (sc.disconnectBufferSize().isDefined())
		  s.setDisconnectedBufferSize(((Integer)sc.disconnectBufferSize().get()));
	  if (sc.storageSize().isDefined())
		  s.setRawHistorySize(sc.storageSize().get());
	  AddressBean[] add=new AddressBean[sc.wrappers().size()];
	  int i=0;
	  for (WrapperConf w:JavaConversions.asJavaIterable(sc.wrappers())){
		  add[i]=address(w);
		  i++;
	  }
	  s.setAddressing(add);
	  return s;
  }
  
  public static AddressBean address(WrapperConf w){
      KeyValueImp [] p=new KeyValueImp[w.params().size()];
      Iterable<String> keys=JavaConversions.asJavaIterable(w.params().keys());
      int i=0;
	  for (String k:keys){
		  p[i]=new KeyValueImp(k,w.params().apply(k));
		  i++;
	  }
      AddressBean a = new AddressBean(w.wrapper(),p);
      if(w.partialKey().isDefined()){
      a.setPartialOrderKey(w.partialKey().get());
      }
      DataField [] out=new DataField[(w.output().size())];
	  for (int j=0;j<out.length;j++){
		  out[j]=dataField(w.output().apply(j));
	  }
      a.setVsconfig(out);
	  return a;
  }
  
  public static InputStream stream(StreamConf s){
	  InputStream is = new InputStream();
	  is.setInputStreamName(s.name());
	  is.setCount(Long.valueOf(s.count()));
	  is.setRate(s.rate());
	  is.setQuery(s.query());
	  StreamSource[] ss=new StreamSource[s.sources().size()];
	  for (int j=0;j<ss.length;j++){
		  ss[j]=source(s.sources().apply(j));
	  }
	  is.setSources(ss);
	  return is;
  }
  
  public static VSensorConfig vsensor(VsConf vs){
	  VSensorConfig v=new VSensorConfig();
	  v.setMainClass(vs.processing().className());
	  v.setDescription(vs.description());
	  v.setName(vs.name());
	  v.setIsTimeStampUnique(vs.processing().uniqueTimestamp());
	  if (vs.poolSize().isDefined())
	    v.setLifeCyclePoolSize(((Integer)vs.poolSize().get()));
	  if (vs.processing().rate().isDefined())
	    v.setOutputStreamRate(((Integer)vs.processing().rate().get()));
	  v.setPriority(vs.priority());
	  KeyValueImp [] addr=new KeyValueImp[vs.address().size()];
      Iterable<String> keys=JavaConversions.asJavaIterable(vs.address().keys());
      int i=0;
	  for (String k:keys){
		  addr[i]=new KeyValueImp(k,vs.address().apply(k));
		  i++;
	  }
	  v.setAddressing(addr);
	  InputStream[] is=new InputStream[vs.streams().size()];
	  for (int j=0;j<is.length;j++){
		  is[j]=stream(vs.streams().apply(j));
	  }
	  v.setInputStreams(is);
	  if (vs.processing().webInput().isDefined()){
		  WebInputConf wic=vs.processing().webInput().get();
		  v.setWebParameterPassword(wic.password());
		  WebInput[] wi=new WebInput[wic.commands().size()];
		  for (int j=0;j<wi.length;j++){
			  wi[j]=webInput(wic.commands().apply(j));
		  }
		  v.setWebInput(wi);
	  }
	  DataField [] out=new DataField[(vs.processing().output().size())];
	  for (int j=0;j<out.length;j++){
		  out[j]=dataField(vs.processing().output().apply(j));
	  }
	  v.setOutputStructure(out);
	  Map<String,String> init=vs.processing().initParams();
	  ArrayList<KeyValue> ini=new ArrayList<KeyValue>();
      Iterable<String> initkeys=JavaConversions.asJavaIterable(init.keys());
	  for (String ik:initkeys){
		  logger.trace("keys:"+ik);
		  ini.add(new KeyValueImp(ik.toLowerCase(),init.apply(ik)));
	  }
	  v.setMainClassInitialParams(ini);
	  
	  StorageConfig st=new StorageConfig();
	  if (vs.storageSize().isDefined())
		  st.setStorageSize(vs.storageSize().get());
	  if (vs.storage().isDefined()){
		StorageConf sc=vs.storage().get();
		if (sc.identifier().isDefined())
		  st.setIdentifier(sc.identifier().get());
		st.setJdbcDriver(sc.driver());
		st.setJdbcURL(sc.url());
		st.setJdbcUsername(sc.user());
		st.setJdbcPassword(sc.pass());		
	  }
	  if (st.getStorageSize()!=null || st.getJdbcURL()!=null)
		v.setStorage(st);
	  return v;
  }
  
}
