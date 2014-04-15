import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;


public class JxProxyThread {
	static int myPort = 6601;
	
	public static void main(String args[]) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
		PrintWriter myFile = new PrintWriter("HeaderLog.txt","UTF-8");
		try {
			ServerSocket theServerSock = new ServerSocket(myPort);
			while(true) {
				Socket client = theServerSock.accept();
				JxProxy newProxy = new JxProxy(client, theServerSock,myFile);
				newProxy.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
}
}
