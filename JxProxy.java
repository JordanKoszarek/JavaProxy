import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

public class JxProxy extends Thread {
	//statics
	public static int 				myOutPort 				= 80;
	//ints
	private int 					myBytesRead 			= 0;
	private	int 					myPort 					= 6601;
	private	int 					mySize 					= 1024*8;
	//url
	private							URL myURL 				= null;
	
	//sockets and streams
	private 	ServerSocket 		myServerSocket 			= null;
	private 	Socket 				servSock 				= null;
	private 	InputStream 		in 						= null;
	private 	OutputStream 		out 					= null;
	private		Socket 				myClientSock 			= null;
	//file
	private 	PrintWriter 		myFile 					= null;
	// bools
	private boolean 				isAlive 				= false;
	private boolean 				isChunked 				= false;
	//string
	String 							myheader			 	= null;

	public JxProxy(Socket aClient, ServerSocket aServerSock,PrintWriter aFile) throws IOException {
		myClientSock = aClient;
		myServerSocket = aServerSock;
		myFile = aFile;
	}

    private int threadCnt=0;
    
	public void runProxy() throws IOException, SocketException {
		servSock = null;
		byte[] readIn = new byte[mySize];
		try {
				int end = 0;
				// read from client sock to receive header
				myBytesRead = myClientSock.getInputStream().read(readIn);
				
				myFile.println("GET\r\n");
				print(getHeader(readIn));
				
				if (myBytesRead < 0 ){return;}// failed to read from browser
				
				// Convert buffer to string
				String mystring = new String(readIn, 0, myBytesRead);
				int zero;

				if (mystring.contains("GET")) {zero = mystring.indexOf("GET ") + 4;}
				else {zero = mystring.indexOf("POST ") + 5;}
				int theEnd = mystring.indexOf(" HTTP/1.1", zero);
				String hostName = mystring.substring(zero, theEnd);
				URL myURL = new URL(hostName);
				
				try {
					servSock = new Socket(myURL.getHost(), myOutPort);
					servSock.setSoTimeout(15000);
				} catch (UnknownHostException e) {
					closeALL();
					System.out.println("" + e.toString());
					return;
				}
				if (servSock!=null && servSock.isConnected()) {
					servSock.getOutputStream().write(readIn,0,myBytesRead);
					servSock.getOutputStream().flush();

					 in = servSock.getInputStream();
					 out = myClientSock.getOutputStream();
					
					byte[] buffer = new byte[mySize];
					int contentLength  = 0;
					int len = 0;
					int totalRead = 0;
					
					if((len = servSock.getInputStream().read(buffer)) < 0){return;}
					myFile.println("Header\r\n");
					print(getHeader(buffer));
					String header = null;
						totalRead = len;
						isChunked = isChunked(buffer);
						contentLength = getContentL(buffer);
						if(contentLength >= 100){
							isAlive = true;
						}else{
							isAlive = false;
						}
						out.write(buffer, 0, len);
						out.flush();
						if(contentLength < 75 && contentLength > 1){closeALL();return;}
						try{
						if(isChunked == true ){
							while ((len = in.read(buffer)) > 0){
								myClientSock.getOutputStream().write(buffer, 0, len);
								myClientSock.getOutputStream().flush();
								totalRead += len;
									
							}
							print("-------finished chunked" +" From" + hostName);
							closeALL();
							return;
						}

						
							
							while ((len = in.read(buffer)) > 0 && ((totalRead <= contentLength)||(isAlive == true))) {
								totalRead += len;								
								myClientSock.getOutputStream().write(buffer, 0, len);
								myClientSock.getOutputStream().flush();
								
							}
						}catch(Exception e ){
							closeALL();
							return;
						}
						myClientSock.getOutputStream().flush();
						closeALL();
				}
				closeALL();

		} catch (IOException e) {
			e.printStackTrace();
			closeALL();
		}

	}
	private String getHeader(byte[] aBuff){
		String split[] = new String(aBuff).split("\r\n\r\n");
		System.out.print(split[0]);
		myFile.println(split[0]+"\r\n");
		myFile.flush();
		return split[0];
	}
	
	private void closeALL() throws IOException{
		
		if(in != null){in.close();}
		if(out != null){out.close();}
		if(myClientSock != null){myClientSock.close();}
		if(servSock != null){servSock.close();}
	}
	
	private int getContentL(byte[]aBuff){
		int i = 0;
		String split[] = new String(new String(aBuff)).split("\r\n");
		for(String b : split){
			if(b.contains("Content-Length:")){
				String[] a = split[i].split(" ");
				return Integer.parseInt(a[1]);
			}
			i++;
		}
		return 0;
	}
	
	private boolean isKeepALive(byte[] aBuff){
		Pattern contentPat = Pattern.compile("Connection: keep-alive");
		Matcher contentMatch = contentPat.matcher(new String(aBuff));
		if(contentMatch.find() == true){return true;}
		return false;
	}
	
	private boolean isChunked(byte[] aBuff){
		Pattern contentPat = Pattern.compile("Transfer-Encoding: chunked");
		Matcher contentMatch = contentPat.matcher(new String(aBuff));
		if(contentMatch.find() == true){return true;}
		return false;
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			runProxy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void print(String aValue) {
		System.out.println(aValue);
	}
}
