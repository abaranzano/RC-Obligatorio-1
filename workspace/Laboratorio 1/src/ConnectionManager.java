import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;


public class ConnectionManager {

	private HashMap<String, Socket> cachedConnections = null;

	ConnectionManager() {
		cachedConnections = new HashMap<String, Socket>();
	}

	public Socket getConnection(String host, int port, boolean keepAlive) throws IOException {
		Socket conn = null; 
		if (!keepAlive) {
			conn = createNewConnection(host, port);
		} else {
			conn = getConnectionFromCache(host, port); 
		}
		return conn;
	}

	private Socket createNewConnection(String host, int port) throws IOException {
		Socket socket = null;
		try {

			socket = new Socket(host, port);

			Log.debug("Abro la conexion al host:[" + host + "] puerto:[" + port + "].");
		} catch (UnknownHostException e) {
			Log.error("Error. No se reconoce el Host:[" + host + "].");
		} catch (IllegalArgumentException e) {
			Log.error("Error. El puerto:[" + port + "] es inválido.");
		}
		return socket;
	}

	public Socket getConnectionFromCache(String hostName, int port) throws IOException {
		Socket conn = cachedConnections.remove(hostName + ":" + port); 
		if (conn == null || !conn.isConnected() || conn.isClosed()) { 
			conn = createNewConnection(hostName, port); 
		} 
		return conn; 
	}

	public void connectionClose(Socket conn, boolean keepAlive) throws IOException {
		if (conn != null && !conn.isClosed() && conn.isConnected()) {
			if (keepAlive) {
				cachedConnections.put(conn.getInetAddress().getHostName() + ":" + conn.getPort() , conn);
			} else {
				Log.debug("Cierro conexion con host:[" + conn.getInetAddress().getHostName() + "] puerto:[" + conn.getPort() + "].");
				conn.close();				
			}
		}
	}

}
