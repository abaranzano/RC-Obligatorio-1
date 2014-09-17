import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;


public class Main {

	public static void main(String args[]) throws Exception 
	{ 
		CommandLineParser parser  = null;  
		CommandLine       cmdLine = null; 
		Options options = new Options();

		//		boolean salir = false;
		int cantidadHilos = 1;

		//nombre del switch, tiene argumentos?, descripcion   
		options.addOption("d", false, "debug");
		options.addOption("depth", true, "profundidad");
		options.addOption("persistent", false, "protocolo");
		options.addOption("pozos", true, "pozos");
		options.addOption("multilang", true, "multilang");
		options.addOption("p", true, "hilos");
		options.addOption("prx", true, "proxy");

		//Para llamarlo seria por ejemplo asi: redbot -pozos 5 -d www.google.com. 
		//Para obtener el 5 del switch pozos asi: cmdLine.getOptionValue("pozos")
		Descriptor descriptor = new Descriptor();
		try {  
			//Parseamos la entrada con la configuración establecida    

			parser  = new BasicParser();  
			cmdLine = parser.parse(options, args);             
			if (cmdLine.getArgs().length != 0) {
				descriptor.addLink(cmdLine.getArgs()[0]); //Agrego como procesada, ya que la primera la proceso siempre.
				descriptor.agregarURL(0, cmdLine.getArgs()[0]);
			} else {
				Log.error("Error con la cantidad de argumentos ingresados, se esperaba [1] se encontraron:[" + cmdLine.getArgs().length + "]"); 
			}

			if(cmdLine.hasOption("d")){
				Log.init(); // Setea verbosity en debug.
			}

			if(cmdLine.hasOption("depth")){
				try {
					descriptor.setProfundidadMaxima(Integer.parseInt(cmdLine.getOptionValue("depth")));
				} catch (NumberFormatException e) {
					Log.error("Error en el valor de profundidad. Se utiliza valor por defecto. Error Original: " + e.getMessage());
				}

			}

			if(cmdLine.hasOption("persistent")){
				descriptor.setPersistent(true);
			}

			if(cmdLine.hasOption("pozos")){
				descriptor.setPozo();
				descriptor.setFilePozo(cmdLine.getOptionValue("pozos"));
			}	

			if(cmdLine.hasOption("multilang")){
				descriptor.setMultilang();
				descriptor.setFileMultilang(cmdLine.getOptionValue("multilang"));
			}

			if(cmdLine.hasOption("p")) {
				try {
					cantidadHilos = Integer.valueOf(cmdLine.getOptionValue("p"));
				} catch (NumberFormatException e) {
					Log.error("Error en el valor de cantidad de hilos. Se utiliza valor por defecto. Error Original: " + e.getMessage());
				}
			}
			if (cmdLine.hasOption("prx")) {
				descriptor.setUsesProxy(true);
				String proxy = cmdLine.getOptionValue("prx");
				if (proxy.startsWith("http://")) {
					descriptor.setProxy(proxy);
				} else {
					descriptor.setProxy("http://" + proxy);
				}            	 
			}

		} catch (org.apache.commons.cli.ParseException ex){  
			Log.error(ex.getMessage());                  
		}  


		Thread[] t = new Thread[cantidadHilos];
		descriptor.setCantHilos(cantidadHilos);

		//		while (!salir) {

		Log.debug("Parametros del Descriptor.\n depth:[" + descriptor.getProfundidadMaxima() + "] persistent:[" + descriptor.isPersistent() + 
					"] pozos:[" + descriptor.getPozo() + "|" + descriptor.getFilePozo() + 
					"] multilang:[" + descriptor.getMultilang() + "] p:[" + descriptor.getCantHilos() + 
					"] prx:[" + descriptor.getUsesProxy() + "|" + descriptor.getProxy() + "]");
		for (int i = 0; i < cantidadHilos; i++) { 

			Decorator decoratorInstance = new Decorator(i);
			decoratorInstance.setDescriptor(descriptor);

			t[i] = new Thread(decoratorInstance);
			t[i].start();
		}
		try {
			for(int i = 0; i < cantidadHilos; i++) {
				t[i].join();
			}
		} catch (InterruptedException e) {
			Log.error("Error al esperar por el fin de un hilo. Se interrumpio la ejecucion. Error Original: " + e.getMessage());
		}
		//			if (descriptor.getAProcesar().size() == 0) {
		//				//Terminaron todos los hilos y no quedan urls por procesar.
		//				salir = true;
		//			}
		//		}

	}

}
