import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Worker { 
	private String host = null;
	private Pair<Integer,String> urlAProcesar;
	private boolean connectionClosed = false; //Cuando el servidor manda un Connection: close. No puedo asumir persistencia
	private String path = null;
	private int port;
	private Socket socket = null;
	private Descriptor descriptor = null;
	private String id = "0";


	Worker (String id) {
		this.id = id;
	}

	public void initWorker(Pair<Integer, String> actual) {
		this.urlAProcesar = actual;
		Matcher m = null;

		//Si usa proxy, la URL entera pasa a ser el Path.
		if (this.descriptor.getUsesProxy()) {
			m = URL.matcher(this.descriptor.getProxy());
			m.matches();
			this.path = urlAProcesar.getUrl();
		} else {
			m = URL.matcher(urlAProcesar.getUrl());
			m.matches();
			String linkPath = m.group(PARSE_URL_PATH);
			this.path = (linkPath != null && !"".equalsIgnoreCase(linkPath) && !" ".equalsIgnoreCase(linkPath)) ? linkPath : "/";
		}


		String linkHost = m.group(PARSE_URL_HOST);
		String linkPort = m.group(PARSE_URL_PORT);
		this.host = linkHost;	
		this.port = (linkPort != null || "".equalsIgnoreCase(linkPort)) ? Integer.parseInt(linkPort) : 80;
	}

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void doJob() throws UnsupportedEncodingException, IOException, TimeoutException, HersonFensonException, Exception { 
		try {
			this.socket = this.descriptor.getConnection(this.id, this.host, this.port, this.descriptor.isPersistent() && !connectionClosed);
			HTTPGet();
			String response = HTTPResponse();
			procesarRespuesta(response);	
		} catch (Exception e) {
			throw e;
		}
		finally {
			if (this.socket != null) {
				this.descriptor.connectionClose(this.id, this.socket, this.descriptor.isPersistent() && !connectionClosed);			
			}
		}
	}

	private void HTTPGet() throws UnsupportedEncodingException, IOException {

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

		Log.debug(id, "Se envio el siguiente mensaje: \n" + 
				httpGet + "\n" + "Fin del mensaje\n");

	}

	private String HTTPResponse() throws IOException, TimeoutException {

		BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));

		String line;	
		String response = "";
		int timeout = 0;
		while (!in.ready()) {
			try {
				Thread.sleep(1000);
				timeout++;
				Log.console(id, "Tiempo esperado:[" + timeout + "] Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				if (timeout > 15) {
					throw new TimeoutException("Timeout esperando por la respuesta. Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "]");
				}
			} catch (InterruptedException e) {
				//No debería entrar nunca acá, los hilos no los interrumpimos nunca.
				Log.error(id, "Error inesperado. Error Original: " + e.getMessage());
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
				line = in.readLine();
			}		

		}
		else {	//Es persistent. Puede pasar que venga el Content-Length y leemeos hasta ahi, o Transfer Encoding: chunked y leemos de a partes
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
				
				//Controlamos que se respete el final del chunkeds. si al final no viene un cero seguido de un /r/n, reportamos un errorS
				String cero = "";
				if(in.ready()){
					cero = in.readLine();
				}
				if(in.ready() && cero.equals("0")){
					in.readLine(); 	//salto de linea
				}
				
				if(in.ready()){
					this.descriptor.connectionClose(id, this.socket, false);
					Log.error(id, "Error al procesar url con Transfer encoding chunked");
				}

			}	//Chunked
			else{
				if(response.contains("Content-Length")) {
					while (in.ready()) {
						line = in.readLine();
						response += line + "\n";						
					}
				}	//Viene con Content-Length
			}	//No es chunked

		}	//Es persistent


		return response;
	}

	private List<String> getEmails(String TextHTML) {

		Pattern p = VALID_EMAIL_ADDRESS_REGEX;
		List<String> emails = new ArrayList<String>();

		Matcher matcher = p.matcher(TextHTML);

		while (matcher.find()) {
			emails.add(matcher.group());
		}

		return emails;
	}

	private void procesarRespuesta(String response) {
		//Obtengo el codigo de respuesta, si es 200 esta todo bien, sino hacemos algo
		String statusCode = null;

		try {
			statusCode = response.substring(9,12);
		} catch (StringIndexOutOfBoundsException e) {
			Log.error(id, "Error al procesar respuesta de Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "] \n." + response + "\n Error Original: " + e.getMessage());
		}

		connectionClosed = (response.indexOf("Connection: Close") != -1) || (response.indexOf("HTTP/1.0") != -1) ? true : false;

		if (this.descriptor.isPersistent() && connectionClosed) {
			Log.debug(id, "La conexion con Host:[" + this.host + "] Port:[" + this.port + "] se cerrará a pedido del Servidor (Connection: close encontrado en el Header o contesto en HTTP/1.0)");
		}

		Log.debug(id, "Procesando Url:[" + urlAProcesar.getUrl() + "] Status Code [" + statusCode + "]");

		if(statusCode.equals("200")) {

			//----------------------OBTENGO LOS EMAILS-----------------------------------------------------------------------------//
			Iterator<String> iter = getEmails(response).iterator();
			while(iter.hasNext()) {
				String mail = iter.next();
				Log.console(id, "Se encontro un mail en URL[: " + this.urlAProcesar.getUrl() + "] MAIL:[" + mail + "]");
				File archivo = new File("mails.txt");
				try {
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(mail + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				} catch (IOException e) {
					Log.error(id, "No se pudo escribir el log mails. Error original: " + e.getMessage());
				}
			}
			//---------------------------------------------------------------------------------------------------------------------//

			//-------------------------------OBTENGO LOS LINKS --------------------------------------------------------------------//
			HtmlExtractor htmlExtractor = new HtmlExtractor();
			Vector<HtmlLink> links = htmlExtractor.grabHTMLLinks(response);

			int cantLinks = links.size();
			if (cantLinks == 0 && this.descriptor.getPozo()) {
				Log.debug(id, "Se encontró un pozo en URL[: " + this.urlAProcesar.getUrl() + "]");
				File archivo = new File(this.descriptor.getFilePozo());	//Pasarle la Ruta relativa dentro del proyecto
				try {
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				} catch (IOException e) {
					Log.error(id, "No se pudo escribir el log de Pozos. Error original: " + e.getMessage());
				}
			}

			for (int i = 0; i < cantLinks; i++) {
				String link = links.elementAt(i).link;

				link = armarLink(link);

				//controlo existencia, si no existe, agrego
				if(!this.descriptor.getLinks().containsKey(link)) {				
					this.descriptor.addLink(link); //como es un hash si existe no lo agrega

					this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
					Log.debug(id, "Se encontró link en:[" + this.urlAProcesar.getUrl() + "] Link:[" + links.elementAt(i).link + "] Valor luego de armarlo:[" + link + "]");
				} else {
					Log.debug(id, "La url:[" + link + "] ya fue procesada. No se agrega a la lista de datos a procesar");
				}
			}

			//Asumo que si viene el tag "Content-language:", la url esta publicada en mas de un lenguaje
			//Si no tiene este tag, esta en uno solo
			if(this.descriptor.getMultilang() && response.contains("Content-language")) {	 
				Log.debug(id, "Se multilenguaje en URL[: " + this.urlAProcesar.getUrl() + "]");
				File archivo = new File(this.descriptor.getFileMultilang());	//Pasarle la Ruta relativa dentro del proyecto
				try{
					FileWriter fw = new FileWriter(archivo, true);	//true para que haga append
					fw.write(this.urlAProcesar.getUrl() + "\r\n");  //Precisa los dos para saltar de linea
					fw.close();
				}
				catch (IOException e){
					Log.error(id, "No se pudo escribir el log de multilnag. Error original: " + e.getMessage());
				}

			}
		} else {	//Status Code <> 200
			if(statusCode.equals("301") || statusCode.equals("302") || statusCode.equals("303")) {	//Considero link de redireccion				
				int posLocation = response.indexOf("Location");
				String link = response.substring(posLocation + 10);
				link = link.split("\n")[0];
				Log.debug(id, "Se encontró redireccionamiento en URL:[" + this.urlAProcesar.getUrl() + "] Se procesará mas adelante. Link:[" + link + "].");
				if(!this.descriptor.getLinks().containsKey(link)) {				
					this.descriptor.addLink(link); //como es un hash si existe no lo agrega
					this.descriptor.agregarURL(this.urlAProcesar.getDepth() + 1,  link);
				}
			} else {
				Log.error(id, "Host:[" + this.host + "] Port:[" + this.port + "] Path:[" + this.path + "] Error. Se recibio status code " + statusCode);
			}
		}
		//text/html
		//		}
	}

	private String armarLink (String link) {

		Matcher m = URL.matcher(link);
		m.find();
		String linkSchema = m.group(PARSE_URL_SCHEMA);
		String linkHost = m.group(PARSE_URL_HOST);
		String linkPort = m.group(PARSE_URL_PORT);
		String linkPath = m.group(PARSE_URL_PATH);
		String linkQuery = m.group(PARSE_URL_QUERY);
		//String linkFragment = m.group(PARSE_URL_FRAGMENT); //No se lo agrego. No es necesario.
		String returnLink = null;


		Matcher urlActual = URL.matcher(urlAProcesar.getUrl());
		urlActual.find();

		returnLink = (linkSchema == null || "".equalsIgnoreCase(linkSchema)) ? "http://" : linkSchema + "//";

		if (linkHost == null || "".equalsIgnoreCase(linkHost)) {

			returnLink += urlActual.group(PARSE_URL_HOST);
		} else {
			returnLink += linkHost;
		}

		if (linkPort == null || "".equalsIgnoreCase(linkPort)) {
			String puerto = urlActual.group(PARSE_URL_PORT);
			returnLink += ((puerto == null || "".equalsIgnoreCase(puerto)) ? "" : ":" + puerto);
		} else {
			returnLink +=  ":" + linkPort;
		}

		String subPath = urlActual.group(PARSE_URL_PATH);
		if (subPath.lastIndexOf("/") != -1) {
			subPath =  subPath.substring(0,subPath.lastIndexOf("/"));
		}

		if (linkPath == null || "".equalsIgnoreCase(linkPath)) {
			returnLink += subPath + "/" ;
		} else {
			returnLink += (linkPath.startsWith("/")) ? subPath + linkPath : subPath + "/" + linkPath;
		}

		if (!(linkQuery == null || "".equalsIgnoreCase(linkQuery))) {
			returnLink += "?" + linkQuery;
		}

		return returnLink;
	}

	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = 
			Pattern.compile("[\\w\\.]+(?:@|\\s+at\\s+)[\\.\\w]+\\.[a-z]{2,6}", Pattern.CASE_INSENSITIVE);
	private static final Pattern URL = Pattern.compile("^((?:[^:/?#]+):)?(?://([^/?#:]*)(?::([^/?#]*))?)?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?");
	//	private static final Pattern URL = Pattern.compile("(http://)(.*)");
	private static final int PARSE_URL_SCHEMA = 1;
	private static final int PARSE_URL_HOST = 2;
	private static final int PARSE_URL_PORT = 3;
	private static final int PARSE_URL_PATH = 4;
	private static final int PARSE_URL_QUERY = 5;
	private static final int PARSE_URL_FRAGMENT = 6;
} 