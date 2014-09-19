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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
	private Descriptor descriptor = null;
	private long id = 0;


	Worker (long id) {
		this.id = id;
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
				this.path = (url.getPath() != null && !"".equalsIgnoreCase(url.getPath()) && !" ".equalsIgnoreCase(url.getPath())) ? url.getPath() : "/";
			}

			this.host = url.getHost();		
			this.port = (url.getPort() != -1) ? url.getPort() : 80;


		} catch (MalformedURLException e) {
			//No debería llegar nunca a entrar acá ya que todas las URL que trabajo me aseguro que tengan protocolo, lo controlo por programación defensiva.
			Log.error("Error. La url a procesar no tiene un protocolo válido. Error Original: " + e.getMessage());
		}
	}

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void doJob() throws UnsupportedEncodingException, IOException, TimeoutException { 
		this.socket = this.descriptor.getConnection(this.host, this.port, this.descriptor.isPersistent() && !connectionClosed);
		HTTPGet();
		String response = HTTPResponse();
		//creo un archivo que ocntiene el responde de la pag
		//el archivo queda almacenado en la carpeta Labratorio 1 del proyecto con nombre archivo, el nombre del host procesado
		File archivo=new File(host + ".txt");
		FileWriter escribir=new FileWriter(archivo,true);
		escribir.write(response);
		//Cerramos la conexion
		escribir.close();
		procesarRespuesta(response);
		this.descriptor.connectionClose(this.socket, this.descriptor.isPersistent() && !connectionClosed);
	}

	public void HTTPGet() throws UnsupportedEncodingException, IOException {

		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

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
		out.flush();

		//		out.println(httpGet);
		Log.debug("Se envio el siguiente mensaje: \n" + 
				httpGet + "\n" + "Fin del mensaje");



	}

	public String HTTPResponse() throws IOException, TimeoutException {

		BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));

		String line;	

		//String response = new String();
		String response = "";	//con null, lo toma como string y lo concatena al principio
		int timeout = 0;
		while (!in.ready()) {
			try {
				Thread.sleep(1000);
				timeout++;
				Log.error("Tiempo esperado:[" + timeout + "] Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				if (timeout > 30) {
					throw new TimeoutException("Timeout esperando por la respuesta. Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				}
			} catch (InterruptedException e) {
				//No debería entrar nunca acá, los hilos no los interrumpimos nunca.
				Log.error("Error inesperado. Error Original: " + e.getMessage());
			}
		}

		//Leer el Header hasta que encontramos /r/n/r/n
		line = in.readLine();
		while (line != null && line.length() != 0) { 
			response += line + "\n";
			line = in.readLine();

		}	

		if(!this.descriptor.isPersistent()){	//Es HTTP 1.0. El socket se cierra y el Buffer termina con null. Puede leerse sin problemas

			line = in.readLine();
			while (line != null) {  //line.length() > 0
				response += line + "\n";
				//Escribimos en el archivo con el metodo write
				line = in.readLine();
			}		

		}
		else {	//Es persistent. Puede pasar que venga el conente-length y leemeos hasta ahi, o Transfer Encodign: chunked y leemos de a partes
			if(response.contains("Transfer-Encoding: chunked")){

				String charset;
				if(response.contains("charset")){
					String [] aux = response.substring(response.indexOf("charset") + 8).split("\n"); 
					charset = aux [0];
				}
				else{	//Si no viene el charset, por defect se asume UTF-8
					charset = "UTF-8";
				}


				line = in.readLine();	//bytes del primer chunk
				int bytesChunk = Integer.parseInt(line, 16);
				boolean salir = false;
				while(!salir){ //bytesChunk != 0  TODO
					int bytesLeidos = 0;
					while(bytesLeidos < bytesChunk){
						byte[] b = in.readLine().getBytes();
						bytesLeidos++;
						line = new String (b, charset);
						response += line + "\n";
						int bytesLinea = b.length;
						if (bytesLinea != 0){
							bytesLeidos += b.length;
						}
					}
					byte[] b = in.readLine().getBytes();
					line = new String (b, charset);
					if (line.isEmpty())
						salir = true;
					else
						bytesChunk = Integer.parseInt(line, 16);

				}	

			}	//Chunked
			else{
				if(response.contains("Content-Length")) {
					line = in.readLine();
					while (line != null) { 
						response += line + "\n";
						line = in.readLine();
					}
				}	//Viene con Content-Length
			}	//No es chunked

		}	//Es persistent


		return response;
	}


	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = 
			Pattern.compile("[\\w\\.]+(?:@|\\s*at\\s*)[\\.\\w]+\\.[a-z]{2,6}", Pattern.CASE_INSENSITIVE);

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
			Log.error("Error:" + response + "Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
		}

		connectionClosed = (response.indexOf("Connection: Close") != -1) ? true : false;
		if (connectionClosed) {
			Log.debug("La conexion con Host:[" + this.host + "] Port:[" + this.port + "] se cerrará a pedido del Servidor (Connection: close encontrado en el Header)");
		}
		//		int indiceContentType = 0; //donde arranca el content type
		//		indiceContentType = response.indexOf("Content-Type");
		//obtengo el content type, proceso solo si es un html
		//		String contenttype = response.substring(indiceContentType + 14, indiceContentType + 23);

		//		if(contenttype.equals("text/html")) { 

		Log.debug("Procesando Url:[" + urlAProcesar.getUrl() + "] Status Code [" + statusCode + "]");
		if(statusCode.equals("200")) {

			//----------------------OBTENGO LOS EMAILS-----------------------------------------------------------------------------//
			Iterator<String> iter = getEmails(response).iterator();
			while(iter.hasNext()) {
				//JOptionPane.showMessageDialog(null, iter.next());
				String mail = iter.next();
				this.descriptor.addMail(mail);
				Log.console("MAIL: " + mail);
			}
			//---------------------------------------------------------------------------------------------------------------------//

			//-------------------------------OBTENGO LOS LINKS --------------------------------------------------------------------//
			HtmlExtractor htmlExtractor = new HtmlExtractor();
			Vector<HtmlLink> links = htmlExtractor.grabHTMLLinks(response);

			int cantLinks = links.size();
			if (cantLinks == 0 && this.descriptor.getPozo()) {
				Log.debug("Se encontró un pozo en URL[: " + this.urlAProcesar.getUrl() + "]");
				File archivo = new File(this.descriptor.getFilePozo());	//Pasarle la Ruta relativa dentro del proyecto
				try {
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				} catch (IOException e) {
					Log.error("No se pudo escribir el log de Pozos. Error original: " + e.getMessage());
				}
			}

			for (int i = 0; i < cantLinks; i++) {
				String link = links.elementAt(i).link;

				link = armarLink(link);

				//Esto lo hago igual por que tengo que verificar si tiene el path si es wellFormed.
				URL url = null;
				boolean error = false;
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
						Log.error("Error al verificar la URL. No se pudo generar el URI. El formato no respeta el RFC 2396 es incorrecto. Link:[" + link + "]");
					}
				} catch (MalformedURLException e) {
					//Solo si no tiene protocolo. No podría pasar por que lo controlo arriba
					error = true;
					Log.error("Error inesperado en url. Error original: " + e.getMessage());
				}


				//controlo existencia, si no existe, agrego
				if (!error) {
					if(!this.descriptor.getLinks().containsKey(link)) {				
						this.descriptor.addLink(link); //como es un hash si existe no lo agrega

						this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
						Log.console("LINK: " + links.elementAt(i).link + " VIENE DE: " + this.host + this.path + " AGREGO: " + link);
					} else {
						Log.debug("La url:[" + link + "] ya fue procesada. No se agrega a la lista de datos a procesar");
					}
				}


			}

			//Asumo que si viene el tag "Content-language:", l url esta publicada en mas de un lenguaje
			//Si no tiene este tag, esta en uno solo
			if(this.descriptor.getMultilang() && response.contains("Content-language")) {	 
				Log.debug("Se multilenguaje en URL[: " + this.urlAProcesar.getUrl() + "]");
				File archivo = new File(this.descriptor.getFileMultilang());	//Pasarle la Ruta relativa dentro del proyecto
				try{
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				}
				catch (IOException e){
					Log.error("No se pudo escribir el log de multilnag. Error original: " + e.getMessage());
				}

			}
		} else {	//Status Code <> 200
			if(statusCode.equals("301") || statusCode.equals("302") || statusCode.equals("303")) {	//Considero link de redireccion
				Log.debug("Se encontró redireccionamiento en URL:[" + this.urlAProcesar.getUrl() + "] Se procesará mas adelante");
				int posLocation = response.indexOf("Location");
				String link = response.substring(posLocation + 10);
				link = link.split("\n")[0];
				if(!this.descriptor.getLinks().containsKey(link)) {				
					this.descriptor.addLink(link); //como es un hash si existe no lo agrega
					this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
				}
			} else {
				Log.error("Error. Se recibio status code " + statusCode);
			}
		}
		//text/html
		//		}
	}

	private boolean wellFormedURL(String link) {
		URL url = null;
		boolean isWellFormed = true;
		try {
			if (!link.startsWith("http://") && !link.startsWith("ftp://") && !link.startsWith("https://")) {
				//Verifico que tenga protocolo. Si no lo tiene le agrego por defecto http.
				link = "http://" + link;
			}
			url = new URL(link);
			try {
				if (url.getPath() == null || url.getPath().isEmpty() || "".equalsIgnoreCase(url.getPath())) {
					//No tiene path. Lo agrego.
					link = link + "/";
					url = new URL(link);
				}
				url.toURI(); //Este metodo genera el URI por el RFC 2396. Si da error, el formato del link es incorrecto.
			} catch (URISyntaxException e) {
				isWellFormed = false;
				Log.debug("El formato no respeta el RFC 2396 es incorrecto. Link:[" + link + "] Se intentará armar el URL");
			}
		} catch (MalformedURLException e) {
			//Solo si no tiene protocolo. No podría pasar por que lo controlo arriba
			isWellFormed = false;
			Log.debug("Error inesperado en url. Se intentará armar el URL. Error original: " + e.getMessage());
		}
		return isWellFormed;
	}


	public static final Pattern IPAddress = Pattern.compile("\\b(?:(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
	private boolean checkTopLevelDomains(String link) {
		//Chequeo los Top Level Domains mas comunes
		return (link.contains(".com") || link.contains(".org") || link.contains(".net") 
				|| link.contains(".int") || link.contains(".edu") || link.contains(".gov") 
				|| link.contains(".mil") || link.contains(".academy") || link.contains(".aero") || link.contains(".agency")
				|| link.contains(".bar") || link.contains(".bargains") || link.contains(".bike") || link.contains(".biz")
				|| link.contains(".ceo") || link.contains(".education") || link.contains(".email") || link.contains(".holdings")
				|| link.contains(".info") || link.contains(".international") || link.contains(".link") || link.contains(".museum")
				|| link.contains(".photos") || link.contains(".properties") || link.contains(".social") || link.contains(".wiki"));
	}
	
	private String armarLink (String link) {

		if(link.contains("#")){
			//No me importan las Reference. Es la misma URL
			int posNum = link.indexOf("#");
			link = link.substring(0, posNum);
		}
		if (link.startsWith("//")) {
			//Wikipedia es una hija de puta. Tiene links de la forma //es.wikipedia.org
			link = link.substring(2);
		}

		if (!( checkTopLevelDomains(link) || IPAddress.matcher(link).find()))  {
			//No tiene nada. Es de la forma index.php
			if (!link.startsWith("/")) {
				link = "/" + link;
			}
		}
		if (link.startsWith("/")) {
			//No tiene nada. Es de la forma /index.php. Agrego el host que es en la misma pagina.
			if (this.descriptor.isUsesProxy()) {
				URL url = null;
				try {
					url = new URL(this.urlAProcesar.getUrl());
				} catch (MalformedURLException e) {
					//No debería entrar nunca acá, esta URL ya se proceso, lo pido solo para tomar el host de la URL de nuevo para formar la siguiente.
					Log.error("Error inesperado al armar la URL a procesar. Error original: " + e.getMessage());
				}
				link = (url.getPort() != -1) ? url.getHost() + ":" + url.getPort() + link : url.getHost() + link;;				
			} else {
				//Siempre va a estar por defeco en 80.
				link = (this.port != 80) ? this.host + ":" + this.port + link : this.host + link; //ya tiene path, concateno el host	
			}

		}

		if (!link.startsWith("http://") && !link.startsWith("ftp://") && !link.startsWith("https://")) {
			//Verifico que tenga protocolo. Si no lo tiene le agrego por defecto http.
			link = "http://" + link;
		}
		return link;
	}
} 