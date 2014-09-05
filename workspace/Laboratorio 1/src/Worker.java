import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

class Worker { 

	String host = null;
	String path = null;
	int port = 80;
	Socket socket = null;
	BufferedWriter out = null;
	BufferedReader in = null;
	Descriptor descriptor = null;

	public Worker (String host, int port, String path) throws IOException {
		
		this.host = host;		
		if (port != -1) {
			this.port = port;
		}
		this.path = path;
		try {
			this.socket = new Socket(this.host, this.port);
		} catch (UnknownHostException e) {
			System.err.println("Error. No se reconoce el Host:[" + this.host + "].");
		} catch (IllegalArgumentException e) {
			System.err.println("Error. El puerto:[" + this.port + "] es inválido.");
		}
	}


	public Descriptor getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void doJob() throws UnsupportedEncodingException, IOException {
		HTTPGet();
		String response = HTTPResponse();
		close();
		//Cierro el Socket antes de procesar la respuesta. No hay necesidad de mantenerlo abierto.
		procesarRespuesta(response);

	}

	public void HTTPGet() throws UnsupportedEncodingException, IOException {
		out = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		String httpGet = "GET " + this.path;
		if (getDescriptor().isHTTP11()) {
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

		return response;
	}

	public void close() throws IOException {
		out.close();
		in.close();
	}

	public void procesarRespuesta(String response) {
		//TODO: Procesar la respuesta bien.
		getDescriptor().addLink(response); //Solo quiero probar que funque la estructura
		getDescriptor().addMail(response);
	}
} 