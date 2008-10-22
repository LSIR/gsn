package gsn.http.datarequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class OutputInputStream {

	private OISInputStream 	oisi = null;
	private OISOutputStream	oiso = null;
	private boolean oisiClosed = false;
	private boolean oisoClosed = false;
	private ArrayBlockingQueue<Integer> circularBuffer = null;


	public OutputInputStream (int bufferSize) {
		circularBuffer = new ArrayBlockingQueue<Integer> (bufferSize) ;		
	}

	public void _close () throws IOException {
		if (oisi != null) oisi.close();
		if (oiso != null) oiso.close();
		circularBuffer = null;
	}

	public InputStream getInputStream () {
		if (oisi == null) oisi = new OISInputStream () ;
		return oisi;
	}

	public OutputStream getOutputStream () {
		if (oiso == null) oiso = new OISOutputStream () ;
		return oiso;
	}

	private class OISOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			try {
				circularBuffer.put(b);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void close () throws IOException {
			super.close();
			oisoClosed = true;
		}
	}

	private class OISInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			int nextValue = -1;
			try {
				if ( ! oisoClosed ) nextValue = circularBuffer.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return nextValue;
		}
		@Override
		public void close () throws IOException {
			super.close();
			oisiClosed = true;
		}
	}

	public interface OISOutputStreamIF extends Runnable { }

	public static void main (String[] args) {
		final OutputInputStream ois = new OutputInputStream (4) ;
		new Thread(
				new Runnable () {
					public void run () {
						try {
							ois.getOutputStream().write("ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes());
							Thread.sleep(4000);
							System.out.println("closing stream");
							ois.getOutputStream().close();
							ois._close();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
		).start();
		InputStream is = ois.getInputStream();
		int nextValue = -1;
		try {
			while ((nextValue = is.read()) != -1) {
				System.out.println("read: " + nextValue);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
