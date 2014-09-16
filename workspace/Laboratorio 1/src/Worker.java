import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Worker { 

	private String host = null;
	private Pair<Integer,String> urlAProcesar;
	//	private boolean persist = false;
	private boolean connectionClosed = false; //Cuando el servidor manda un Connection: close. No puedo asumir persistencia
	private String path = null;
	private int port;
	private Socket socket = null;
	private BufferedWriter out = null;
	private BufferedReader in = null;
	private Descriptor descriptor = null;


	public Worker () {

	}

	public void initWorker(Pair<Integer, String> actual) {
		this.urlAProcesar = actual;
		URL url = null;
		try {
			//Si usa proxy, la URL entera pasa a ser el Path.
			if (this.descriptor.getUsesProxy()) {
				url = new URL(this.descriptor.getProxy());
				this.path = urlAProcesar.getUrl();
			} else {
				url = new URL(urlAProcesar.getUrl());
				this.path = url.getPath();
			}

			this.host = url.getHost();		
			this.port = (url.getPort() != -1) ? url.getPort() : 80;


		} catch (MalformedURLException e) {
			//No debería llegar nunca a entrar acá ya que todas las URL que trabajo me aseguro que tengan protocolo, lo controlo por programación defensiva.
			System.err.println("Error. La url a procesar no tiene un protocolo válido. Error Original: " + e.getMessage());
		}
	}

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void doJob() throws UnsupportedEncodingException, IOException, TimeoutException { 
		this.socket = this.descriptor.getConnection(this.host, this.port, this.descriptor.isPersistent() && !connectionClosed);
		HTTPGet();
		String response = HTTPResponse();
		procesarRespuesta(response);
		this.descriptor.connectionClose(this.socket, this.descriptor.isPersistent() && !connectionClosed);
	}

	public void HTTPGet() throws UnsupportedEncodingException, IOException {

		out = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

		String httpGet = "GET " + this.path;
		if (this.descriptor.isPersistent()) {
			httpGet += " HTTP/1.1";
		} else {
			httpGet += " HTTP/1.0";
		}
		httpGet += "\r\n";

		httpGet +="Accept: text/html\r\n";
		httpGet +="Host: " + host + ":" + port + "\r\n";
		httpGet +="\r\n";

		out.write(httpGet);

		if (descriptor.usesDebug()){
			System.out.println("[debug] Se envio el siguiente mensaje: " );
			System.out.println("[debug] " + httpGet);
			System.out.println("[debug] Fin del mensaje");
		}

		out.flush();


	}

	public String HTTPResponse() throws IOException, TimeoutException {

		in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));


		String line;

		String response = "";	//con null, lo toma como string y lo concatena al principio
		int timeout = 0;
		while (!in.ready()) {
			try {
				Thread.sleep(1000);
				timeout++;
				System.err.println("Tiempo esperado:[" + timeout + "] Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				if (timeout > 30) {
					throw new TimeoutException("Timeout esperando por la respuesta. Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

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

		return response;
	}

	/*
	 * Cierra la conexión siempre y cuando sea distinto de null (Se haya activado en algun momento)
	 */
	public void close() throws IOException {
		if (out != null) {
			out.close();
		}
		if (in != null) {
			in.close();
		}
		if (socket != null) {
			socket.close();
			if (this.descriptor.usesDebug()) {
				System.out.println("[debug] Cierro conexion con host:[" + this.host + "] puerto:[" + this.port + "].");
			}
		}		

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
		//Obtengo el codigo de respuesta, si es 200 esta todo bien, sino hacemos algo
		String statusCode = null;
		try {
			statusCode = response.substring(9,12);
		} catch (StringIndexOutOfBoundsException e) {
			System.err.println("Error:" + response + "Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
		}

		connectionClosed = (response.indexOf("Connection: Close") != -1) ? true : false;
		//		int indiceContentType = 0; //donde arranca el content type
		//		indiceContentType = response.indexOf("Content-Type");
		//obtengo el content type, proceso solo si es un html
		//		String contenttype = response.substring(indiceContentType + 14, indiceContentType + 23);

		//		if(contenttype.equals("text/html")) { 


		if(statusCode.equals("200")) {

			//----------------------OBTENGO LOS EMAILS-----------------------------------------------------------------------------//
			Iterator<String> iter = getEmails(response).iterator();
			while(iter.hasNext()) {
				//JOptionPane.showMessageDialog(null, iter.next());
				String mail = iter.next();
				this.descriptor.addMail(mail);
				System.out.println("MAIL: " + mail);
			}
			//---------------------------------------------------------------------------------------------------------------------//

			//-------------------------------OBTENGO LOS LINKS --------------------------------------------------------------------//
			HtmlExtractor htmlExtractor = new HtmlExtractor();
			Vector<HtmlLink> links = htmlExtractor.grabHTMLLinks(response);

			int cantLinks = links.size();
			if (cantLinks == 0 && this.descriptor.getPozo()) {
				File archivo = new File(this.descriptor.getFilePozo());	//Pasarle la Ruta relativa dentro del proyecto
				try {
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				} catch (IOException e) {
					System.err.println("No se pudo escribir el log de Pozos. Error original: " + e.getMessage());
				}
			}

			for (int i = 0; i < cantLinks; i++) {
				String link = links.elementAt(i).link;

				link = validarLink(link);

				boolean error = false;
				URL url = null;
				try {
					url = new URL(link);
					try {
						if (url.getPath() == null || url.getPath().isEmpty() || "".equalsIgnoreCase(url.getPath())) {
							//No tiene path. Lo agrego.
							link = link + "/";
							url = new URL(link);
						}
						url.toURI(); //Este metodo genera el URI por el RFC 2396. Si da error, el formato del link es incorrecto.
					} catch (URISyntaxException e) {
						error = true;
						System.err.println("No se pudo generar el URI. El formato no respeta el RFC 2396 es incorrecto. Link:[" + link + "]");
					}
				} catch (MalformedURLException e) {
					//Solo si no tiene protocolo. No podría pasar por que lo controlo arriba
					error = true;
					System.err.println("Error inesperado en url");
				}


				//controlo existencia, si no existe, agrego
				if (!error) {
					if(!this.descriptor.getLinks().containsKey(link)) {				
						this.descriptor.addLink(link); //como es un hash si existe no lo agrega

						this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
						System.out.println("LINK: " + links.elementAt(i).link + " VIENE DE: " + this.host + this.path + " AGREGO: " + link);
					}
				}


			}

			if (this.descriptor.usesDebug()) {
				System.out.println("[debug] Url:" + urlAProcesar.getUrl() + " Status Code " + statusCode);
			}

			//Asumo que si viene el tag "Content-language:", l url esta publicada en mas de un lenguaje
			//Si no tiene este tag, esta en uno solo
			if(this.descriptor.getMultilang() && response.contains("Content-language")) {	 
				File archivo = new File(this.descriptor.getFileMultilang());	//Pasarle la Ruta relativa dentro del proyecto
				try{
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				}
				catch (IOException e){
					System.err.println("No se pudo escribir el log de multilnag. Error original: " + e.getMessage());
				}

			}
		} else {	//Status Code <> 200
			if(statusCode.equals("301") || statusCode.equals("302") || statusCode.equals("303")) {	//Considero link de redireccion
				int posLocation = response.indexOf("Location");
				String link = response.substring(posLocation + 10);
				link = link.split("\n")[0];
				if(!this.descriptor.getLinks().containsKey(link)) {				
					this.descriptor.addLink(link); //como es un hash si existe no lo agrega
					this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
				}
			} else {
				System.err.println("Error: se recibio status code " + statusCode);
			}
		}
		//text/html
		//		}
	}

	private String validarLink (String link) {
		if(link.contains("#")){
			//No me importan las Reference. Es la misma URL
			int posNum = link.indexOf("#");
			link = link.substring(0, posNum);
		}
		if (link.startsWith("//")) {
			//Wikipedia es una hija de puta. Tiene links de la forma //es.wikipedia.org
			link = link.substring(2);
		}

		if (!(link.contains("www") || link.contains(".com") || link.contains(".org") || link.contains(".edu") || link.contains(".net")))  {
			//No tiene nada. Es de la forma index.php
			if (!link.startsWith("/")) {
				link = "/" + link;
			}
		}
		if (link.startsWith("/")) {
			//No tiene nada. Es de la forma /index.php. Agrego el host que es en la misma pagina.
			link = this.host + link; //ya tiene path, concateno el host					
		}

		if (!link.startsWith("http://") && !link.startsWith("ftp://") && !link.startsWith("https://")) {
			//Verifico que tenga protocolo. Si no lo tiene le agrego por defecto http.
			link = "http://" + link;
		}
		return link;
	}
} 