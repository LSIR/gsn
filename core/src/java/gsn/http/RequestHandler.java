package gsn.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface RequestHandler {

    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException;

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}