import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

class Client { 

	String host = null;
	String path = null;
	int port = 80;
	Socket socket = null;
	BufferedWriter out = null;
	BufferedReader in = null;
	Descriptor descriptor = null;
	
	public Client (String url, int port, Descriptor d) throws UnknownHostException, IOException {
		int pos = url.indexOf("/");
		if (pos != -1) {
			this.host = url.substring(0, pos);
			this.path = url.substring(pos, url.length());
		} else {
			this.host = url;
			this.path = "/";
		}
		
		this.port = port;
		this.socket = new Socket(host, port);
		this.descriptor = d;
	}
	
	
	public void HTTPGet() throws UnsupportedEncodingException, IOException {
		out = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		String httpGet = "GET " + this.path;
		if (Descriptor.isHTTP11) {
			httpGet += " HTTP/1.1\r\n";
		} else {
			httpGet += " HTTP/1.0\r\n";
		}
		httpGet += "\r\n";
		out.write(httpGet);
		
		System.out.println("\n HTTP Get Message: ");
		System.out.println(httpGet);
		
		out.flush();
		
	}
	
	public String HTTPResponse() throws IOException {
		in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		String line;
		
		System.out.println("\n HTTP Response Message: ");
		String response = null;
		while ((line = in.readLine()) != null) {
			response += line;
			System.out.println(line);
		}		
		
		descriptor.addLink(response); //Solo quiero probar que funque la estructura
		descriptor.addMail(response);
		return response;
	}

	public void close() throws IOException {
		out.close();
		in.close();
	}
} 