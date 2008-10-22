package gsn.http;

import gsn.Container;
import gsn.http.datarequest.DataRequestException;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class MultiDataDownload extends HttpServlet {

	private static final long serialVersionUID = 4249739276150343437L;
	
	private static transient Logger logger 			= Logger.getLogger(MultiDataDownload.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doPost(req, res);
	}

	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws IOException {
		try {
			gsn.http.datarequest.DownloadData dd = new gsn.http.datarequest.DownloadData (req.getParameterMap()) ;
			dd.process();
			if (dd.getOt() == gsn.http.datarequest.DownloadData.AllowedOutputType.csv) {
				res.setContentType("application/x-download");
				res.setHeader("content-disposition","attachment; filename=data.csv");
			}
			else {
				res.setContentType("text/xml");
			}
			dd.outputResult(res.getOutputStream(), false);
			res.getOutputStream().flush();
		} catch (DataRequestException e) {
			logger.error(e.getMessage());
			res.sendError(Container.ERROR_INVALID_VSNAME, e.getMessage());
			return;
		}
	}
}