import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;


public class ConnectionManager {

	private HashMap<String, Socket> cachedConnections = null;

	ConnectionManager() {
		cachedConnections = new HashMap<String, Socket>();
	}

	public Socket getConnection(String id, String host, int port, boolean keepAlive) throws IOException {
		Socket conn = null; 
		if (!keepAlive) {
			conn = createNewConnection(id, host, port);
		} else {
			conn = getConnectionFromCache(id, host, port); 
		}
		return conn;
	}

	private Socket createNewConnection(String id, String host, int port) throws IOException {
		Socket socket = null;
		try {

			socket = new Socket(host, port);

			Log.debug(id,"Abro la conexion al host:[" + host + "] puerto:[" + port + "].");
		} catch (UnknownHostException e) {
			Log.error(id,"Error. No se reconoce el Host:[" + host + "].");
		} catch (IllegalArgumentException e) {
			Log.error(id,"Error. El puerto:[" + port + "] es inv�lido.");
		}
		return socket;
	}

	public Socket getConnectionFromCache(String id, String hostName, int port) throws IOException {
		Socket conn = cachedConnections.remove(hostName + ":" + port); 
		if (conn == null || !conn.isConnected() || conn.isClosed()) { 
			conn = createNewConnection(id, hostName, port); 
		} 
		return conn; 
	}

	public void connectionClose(String id, Socket conn, boolean keepAlive) throws IOException {
		if (conn != null && !conn.isClosed() && conn.isConnected()) {
			if (keepAlive) {
				cachedConnections.put(conn.getInetAddress().getHostName() + ":" + conn.getPort() , conn);
			} else {
				Log.debug(id,"Cierro conexion con host:[" + conn.getInetAddress().getHostName() + "] puerto:[" + conn.getPort() + "].");
				conn.close();				
			}
		}
	}

}
