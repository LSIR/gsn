package gsn.beans;

import gsn.config.VsConf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VsLoadingTest {

	@Test
	public void loadAll() throws FileNotFoundException{
		String path="virtual-sensors/samples/";
		File dir=new File(path);
		for (String f :dir.list()){
			System.out.println(f);
			//loadVs(path+f);
		}
	}
	
	@Test
	public void LoadDemoVs(){
		VsConf cc=VsConf.load("virtual-sensors/replay.xml");
		System.out.println(cc.streams().size());
		VSensorConfig v2=BeansInitializer.vsensor(cc);
		v2.validate();
		System.out.println(v2.getInputStreams().size());
		assertEquals(v2.getInputStreams().iterator().next().getCount().longValue(),Long.MAX_VALUE);
	}
	
	/*public void loadVs(String path) throws FileNotFoundException, JiBXException{
		//String path="virtual-sensors/replay.xml";
		IBindingFactory bfact = BindingDirectory.getFactory ( VSensorConfig.class );
		IUnmarshallingContext uctx = bfact.createUnmarshallingContext ( );
		VSensorConfig v1 = ( VSensorConfig ) uctx.unmarshalDocument ( 
				new FileInputStream ( new File ( path ) ) , null );

		VsConf cc=VsConf.load(path);
		VSensorConfig v2=BeansInitializer.vsensor(cc);
		
		assertEquals(v2.getName(),v1.getName());
		assertEquals(v2.getDescription(),v2.getDescription());
		assertEquals(v2.getIsTimeStampUnique(),v1.getIsTimeStampUnique());
		assertEquals(v2.getOutputStreamRate(),v1.getOutputStreamRate());
		assertEquals(v2.getPriority(),v1.getPriority());
		assertEquals(v2.getProcessingClass(),v2.getProcessingClass());		
		assertEquals(v2.getStorageHistorySize(),v1.getStorageHistorySize());
		assertEquals(v2.getWebParameterPassword(),v1.getWebParameterPassword());
		for (int i=0;i<v2.getAddressing().length;i++){
			assertEquals(v2.getAddressing()[i].getKey(),v1.getAddressing()[i].getKey());
			assertEquals(v2.getAddressing()[i].getValue(),v1.getAddressing()[i].getValue());
		}
		Iterator<InputStream> is2 =v2.getInputStreams().iterator();
		Iterator<InputStream> is1 =v1.getInputStreams().iterator();
		while (is2.hasNext()){		
		  compareStreams(is1.next(),is2.next());
		}
		if (v2.getWebinput()!=null){
			for (int i=0;i<v2.getWebinput().length;i++){
				compareWebInput(v1.getWebinput()[i],v2.getWebinput()[i]);
			}
		}
		else assertNull(v1.getWebinput());
		for (int i=0;i<v2.getOutputStructure().length;i++){
			compareFields(v1.getOutputStructure()[i],v2.getOutputStructure()[i]);
		}	
		if (v2.getStorage()!=null){
			assertEquals(v2.getStorage().getIdentifier(),v1.getStorage().getIdentifier());
			assertEquals(v2.getStorage().getJdbcDriver(),v1.getStorage().getJdbcDriver());
			assertEquals(v2.getStorage().getJdbcURL(),v1.getStorage().getJdbcURL());
		}
		else assertNull(v1.getStorage());
	}*/
	
	private void compareStreams(InputStream is1,InputStream is2){
		assertEquals(is2.getCount(),is1.getCount());
		assertEquals(is2.getInputStreamName(),is1.getInputStreamName());
		assertEquals(is2.getQuery(),is1.getQuery());
		assertEquals(is2.getRate(),is1.getRate());
		for (int i=0;i<is2.getSources().length;i++){
			compareSources(is1.getSources()[i],is2.getSources()[i]);
		}
	}
	
	private void compareSources(StreamSource s1,StreamSource s2){
		assertEquals(s2.getAlias(),s1.getAlias());
		assertEquals(s2.getDisconnectedBufferSize(),s1.getDisconnectedBufferSize());
		assertEquals(s2.getParsedSlideValue(),s1.getParsedSlideValue());		
	    assertEquals(s2.getParsedStorageSize(),s1.getParsedStorageSize());
	    assertEquals(s2.getRawHistorySize(),s1.getRawHistorySize());
	    assertEquals(s2.getSamplingRate(),s1.getSamplingRate(),0);
	    assertEquals(s2.getSlideValue(),s1.getSlideValue());
	    assertEquals(s2.getSqlQuery(),s1.getSqlQuery());
	    assertEquals(s2.getStorageSize(),s1.getStorageSize());
	    for (int i=0;i<s2.getAddressing().length;i++){
	    	compareWrappers(s1.getAddressing()[i], s2.getAddressing()[i]);
	    }
	}
	private void compareWrappers(AddressBean w1,AddressBean w2){
		assertEquals(w2.getWrapper(),w1.getWrapper());
		for (int i=0;i<w2.getPredicates().length;i++){
			assertEquals(w2.getPredicates()[i].getKey(),w1.getPredicates()[i].getKey());
			assertEquals(w2.getPredicates()[i].getValue(),w1.getPredicates()[i].getValue());
		}
	}
	
	private void compareFields(DataField d1,DataField d2){
		assertEquals(d2.getName(),d1.getName());		
		//assertEquals(d2.getDataTypeID(),d1.getDataTypeID());
		assertEquals(d2.getDescription(),d1.getDescription());
		assertEquals(d2.getType(),d1.getType());		
		assertEquals(d2.getUnit(),d1.getUnit());
	}
	private void compareWebInput(WebInput w1,WebInput w2){
		assertEquals(w2.getName(),w1.getName());
		for (int i=0;i<w2.getParameters().length;i++){
			compareFields(w1.getParameters()[i],w2.getParameters()[i]);
		}	
	}
}
