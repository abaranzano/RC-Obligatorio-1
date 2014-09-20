import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Log {
	private static Log instance = null;
	public final static int DEBUG = 1;
	public final static int STANDARD = 0;
	private static int verbosity = STANDARD;

	protected Log(int verbosity) {
		this.verbosity = verbosity;
	}

	public static void init() {
		if(instance == null) {
			instance = new Log(DEBUG);
		}
	}
	
	private static Log getInstance() {
		if (instance == null) {
			instance = new Log(STANDARD);
		}
		return instance;
	}
	
	public static void error(String threadID, String log) {
		System.err.println("[error] ThreadID:[" + threadID + "] " + log);
		File archivo = new File("errors.txt");
		FileWriter escribir;
		try {
			escribir = new FileWriter(archivo,true);
			escribir.write("[error] ThreadID:[" + threadID + "] " + log + "\n");
			//Cerramos la conexion
			escribir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void debug(String threadID, String log) {
		if (Log.getInstance().verbosity == DEBUG) {
			System.out.println("[debug] ThreadID:[" + threadID + "] " + log);
		}
		File archivo = new File("debug.txt");
		FileWriter escribir;
		try {
			escribir = new FileWriter(archivo,true);
			escribir.write("[debug] ThreadID:[" + threadID + "] " + log + "\n");
			//Cerramos la conexion
			escribir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void console(String threadID, String log) {
		System.out.println("[console] ThreadID:[" + threadID + "] " + log);
		File archivo = new File("console.txt");
		FileWriter escribir;
		try {
			escribir = new FileWriter(archivo,true);
			escribir.write("[console] ThreadID:[" + threadID + "] " + log + "\n");
			//Cerramos la conexion
			escribir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
