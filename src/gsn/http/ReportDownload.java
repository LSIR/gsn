package gsn.http;

import gsn.Container;
import gsn.http.datarequest.DataRequestException;
import gsn.http.datarequest.DownloadReport;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * eg. /report?vsname=ss_mem_vs:heap_memory_usage&reportclass=report-default
 */
public class ReportDownload extends HttpServlet {

	private static final long serialVersionUID = 4352188989988156763L;

	private static transient Logger logger = Logger.getLogger(ReportDownload.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doPost(req, res);
	}

	public void doPost (HttpServletRequest req , HttpServletResponse res) throws IOException {
		try {			
			DownloadReport rpd = new DownloadReport (req.getParameterMap()) ;
			rpd.process();
			res.setContentType("application/pdf");
			res.setHeader("content-disposition","attachment; filename=gsn_data.pdf");
			rpd.outputResult(res.getOutputStream(), false);
			res.getOutputStream().flush();
		} catch (DataRequestException e) {
			logger.error(e.getMessage());
			res.sendError(Container.ERROR_INVALID_VSNAME, e.getMessage());
			return;
		}
	}
}
