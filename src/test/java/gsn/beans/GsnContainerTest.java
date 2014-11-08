package gsn.beans;

import gsn.config.GsnConf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GsnContainerTest {

	@Test
	public void testLoadVs() throws JiBXException, FileNotFoundException {
		String path="conf/gsn.xml";
		IBindingFactory bfact = BindingDirectory.getFactory ( ContainerConfig.class );
		IUnmarshallingContext uctx = bfact.createUnmarshallingContext ( );
		ContainerConfig c1 = ( ContainerConfig ) uctx.unmarshalDocument ( 
				new FileInputStream ( new File ( path ) ) , null );

		
		GsnConf cc=GsnConf.load(path);
		ContainerConfig c2=BeansInitializer.container(cc);
		assertEquals(c2.getWebName(),c1.getWebName());
		assertEquals(c2.getWebAuthor(),c1.getWebAuthor());
		assertEquals(c2.getWebEmail(),c1.getWebEmail());
		assertEquals(c2.getWebDescription(),c1.getWebDescription());
		assertEquals(c2.getContainerPort(),c1.getContainerPort());
		assertEquals(c2.isAcEnabled(),c1.isAcEnabled());
		assertEquals(c1.isAcEnabled(),false);
		//assertEquals(c2.getSSLPort(),c1.getSSLPort());
		assertEquals(c2.getSSLKeyPassword(),c1.getSSLKeyPassword());
		assertEquals(c2.getSSLKeyStorePassword(),c1.getSSLKeyStorePassword());
		assertEquals(c2.getTimeFormat(),c1.getTimeFormat());
		assertEquals(c2.isZMQEnabled(),c1.isZMQEnabled());
		assertEquals(c2.getZMQProxyPort(),c1.getZMQProxyPort());
		assertEquals(c2.getZMQMetaPort(),c1.getZMQMetaPort());
		assertNull(c1.getSliding());
		assertNull(c2.getSliding());
		assertEquals(c2.getStorage().getIdentifier(),c1.getStorage().getIdentifier());
		assertEquals(c2.getStorage().getJdbcDriver(),c1.getStorage().getJdbcDriver());
		assertEquals(c2.getStorage().getJdbcURL(),c1.getStorage().getJdbcURL());

	
	}	
}
