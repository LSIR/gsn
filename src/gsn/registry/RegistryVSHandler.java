/**
 * @author m_jost
 */

package gsn.registry;

import gsn.vsensor.http.RequestHandler;
import java.io.IOException;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;

public class RegistryVSHandler implements RequestHandler {

	private static final transient Logger logger = Logger.getLogger( Registry.class );

	public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		String searchRequest = request.getParameter( "search" );
		if (logger.isDebugEnabled( )) logger.debug("Searching : "+searchRequest  );
		response.setStatus( HttpServletResponse.SC_OK );
		StringBuilder sb = new StringBuilder();
		Hits hits=null;
		try{
			hits = Registry.getRegistryRefresh().doQuery(searchRequest);
		} catch (Exception e){
			logger.error( e.getMessage( ) , e );
			response.sendError(SearchServlet.UNSUPPORTED_REQUEST_ERROR, e.getMessage());
		}
		if(hits!=null){
			Iterator it = hits.iterator();
			while(it.hasNext()){
				Hit hit= (Hit) it.next();
				Document doc = hit.getDocument();
				sb.append( doc.getField( Registry._GUID ).stringValue() ).append( "\n" );
			}
		}
		response.getWriter( ).write( sb.toString( ) );
	}

	public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		return true;
	}
}
