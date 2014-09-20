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
	
	public static void error(String log) {
		System.err.println(log);
		File archivo=new File("errors.txt");
		FileWriter escribir;
		try {
			escribir = new FileWriter(archivo,true);
			escribir.write(log + "\n");
			//Cerramos la conexion
			escribir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void debug(String log) {
		if (Log.getInstance().verbosity == DEBUG) {
			System.out.println("[debug] " + log);
		}
	}

	public static void console(String log) {
		System.out.println(log);
	}
}
