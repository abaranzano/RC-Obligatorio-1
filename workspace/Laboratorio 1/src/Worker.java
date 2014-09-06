import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

	public Worker (String host, int port, String path) {
		
		this.host = host;		
		if (port != -1) {
			this.port = port;
		}
		this.path = path;
	}

	public void abrirSocket() throws IOException  {
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
		abrirSocket();
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
			httpGet += " HTTP/1.1";
		} else {
			httpGet += " HTTP/1.0";
		}
		httpGet += "\r\n";
		
		httpGet +="Accept: text/plain, text/html, text/*\r\n";
		httpGet +="Host: " + host + ":" + port + "\r\n";
		httpGet +="\r\n";
		
		//httpGet += "\n";
		
		out.write(httpGet);

		System.out.println("Se envio el siguiente mensaje: " );
		System.out.println(httpGet);
		System.out.println("Fin del mensaje");
		System.out.println("HTTP Get Message: ");
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
			response += line + "\n";
			//System.out.println(line);
		}		

		//creo un archivo que ocntiene el responde de la pag
		//el archivo queda almacenado en la carpeta Labratorio 1 del proyecto con nombre archivo, el nombre del host procesado
		File archivo=new File(host + ".txt");
		FileWriter escribir=new FileWriter(archivo,true);
		//Escribimos en el archivo con el metodo write
		escribir.write(response);
		//Cerramos la conexion
		escribir.close();
				
	    System.out.println(response);
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