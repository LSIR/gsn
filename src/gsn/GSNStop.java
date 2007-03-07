package gsn;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public class GSNStop {
	public static void main(String[] args) throws UnknownHostException, IOException {
		stopGSN();
	}
	public static void stopGSN() throws IOException, UnknownHostException {
		Socket socket = new Socket(InetAddress.getLocalHost(), gsn.GSNController.GSN_CONTROL_PORT);
		PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
		writer.println(gsn.GSNController.GSN_CONTROL_SHUTDOWN);
		writer.flush();
	}
}
