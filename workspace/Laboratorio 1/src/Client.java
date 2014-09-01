import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class Client { 
	public static void main(String args[]) throws Exception 
	{ 

		Socket socket = new Socket("www.google.com", 80);

		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		
		// Creo el HTTP GET de prueba
		out.write("GET /intl/en/policies/privacy/ HTTP/1.1\r\n");
		out.write("\r\n");
		//Esto manda los caracteres al stream. Al ser este el stream de un socket, lo que hace es enviar el mensaje
		out.flush();
		
		// Leo el HTTP Response y lo imprimo
		System.out.println("\n * Response");

		String line;
		//El in.readLine, lee una linea del buffer, como el stream es de un socket, si no mande un mensaje, se va a quedar esperando hasta recibirlo en vez de decir q es nulo
		while ((line = in.readLine()) != null) {
			System.out.println(line);
		}
		
		out.close();
		in.close();
	}
} 