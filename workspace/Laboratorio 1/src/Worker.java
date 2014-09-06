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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

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
			System.err.println("Error. El puerto:[" + this.port + "] es inv�lido.");
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
		String response = ""; 	//Con null concatena un null adelante
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

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = 
			Pattern.compile("[-\\w\\.]+@[\\.\\w]+\\.\\w+", Pattern.CASE_INSENSITIVE);

	 List<String> getEmails(String TextHTML) {
		         
				Pattern p = VALID_EMAIL_ADDRESS_REGEX;
				List<String> emails = new ArrayList<String>();
		        Matcher matcher = p.matcher(TextHTML);
				while (matcher.find()) {
					emails.add(matcher.group());
				}
		        return emails;
		}
		
	public void procesarRespuesta(String response) {
		//TODO: Procesar la respuesta bien.
		String statusCode = response.substring(9,12);
		JOptionPane.showMessageDialog(null, statusCode);
		int indiceContentType = 0;
		indiceContentType = response.indexOf("Content-Type");
		
		String contenttype = response.substring(indiceContentType + 14, indiceContentType + 23);
		if(contenttype.equals("text/html"))
			JOptionPane.showMessageDialog(null, contenttype);
		
		getDescriptor().addLink(response); //Solo quiero probar que funque la estructura
		Iterator<String> iter = getEmails(response).iterator();
		while(iter.hasNext()){
			//JOptionPane.showMessageDialog(null, iter.next());
			this.descriptor.addMail(iter.next());
		}
		
	}
} 