import javax.swing.JOptionPane;

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
        
		boolean salir = false;
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
            //Parseamos la entrada con la configuraci�n establecida    
         
             parser  = new BasicParser();  
             cmdLine = parser.parse(options, args);             
             if (cmdLine.getArgs().length != 0) {
            	 descriptor.agregarURL(0, cmdLine.getArgs()[0]);
             } else {
            	 System.err.println("Error con la cantidad de argumentos ingresados, se esperaba [1] se encontraron:[" + cmdLine.getArgs().length + "]"); 
             }
             
             if(cmdLine.hasOption("d")){
             	descriptor.setDebug();		//Setea en true el debug del descriptor
             }
             
             if(cmdLine.hasOption("depth")){
            	 descriptor.setProfundidadMaxima(Integer.parseInt(cmdLine.getOptionValue("depth")));
             }
             
             if(cmdLine.hasOption("persistent")){
            	 descriptor.setHTTP11(true);
             }
             
             if(cmdLine.hasOption("pozos")){
             	descriptor.setPozo();
             	descriptor.setFilePozo(cmdLine.getOptionValue("pozos"));
             }	
             
             if(cmdLine.hasOption("multilang")){
            	 descriptor.setMultilang();
            	 descriptor.setFileMultilang(cmdLine.getOptionValue("multilang"));
             }
             
             if(cmdLine.hasOption("p")){
            	 //TODO
             }
             
             if(cmdLine.hasOption("prx")){
            	 descriptor.setUsesProxy(true);
            	 descriptor.setProxy(cmdLine.getOptionValue("prx"));
             }
             
             if(cmdLine.hasOption("p")) {
            	 cantidadHilos = Integer.valueOf(cmdLine.getOptionValue("p"));
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
             System.err.println(ex.getMessage());                  
         }  
        

		//Agrego m�s links antes de prueba para ver si toma uno cada hilo
//		descriptor.aProcesar.add("http://www.fing.edu.uy/inco/cursos/compil/");
//		descriptor.aProcesar.add("http://www.fing.edu.uy/inco/inicio");
		Thread[] t = new Thread[cantidadHilos];
		
		int cuantasvecesbusque = 0;
		while (!salir) {
			cuantasvecesbusque ++;
			
			/*
			 * Recreo hilos cada vez que los 5 terminan de procesar una URL.
			 * Esto no es muy efectivo xq tengo que recrearlos cada vez.
			 * Adem�s, si 4 hilos terminaron, esperan por el 5to que termine para empezar a procesar nuevamente
			 * No se me ocurrio todav�a como hacer para que un hilo siga procesando esperando por un posible url nuevo, 
			 * por que la condici�n de que sea 0 la lista de a procesar no sirve
			 * ya que podr�a pasar que 4 hilos terminen y quede ejecutando 1 solo todo el resto.
			 * Necesito saber si terminaron de procesar todos los hilos para estar seguro que termine de procesar todo
			 */
			for (int i = 0; i < cantidadHilos; i++) { 
			
				Decorator decoratorInstance = new Decorator();
				decoratorInstance.setDescriptor(descriptor);

				t[i] = new Thread(decoratorInstance);
				t[i].start();
			}
			try {
				for(int i = 0; i < cantidadHilos; i++) {
					t[i].join();
				}
			} catch (InterruptedException e) {
				System.err.println("Error al esperar por el fin de un hilo. Error Original: " + e.getMessage());
			}
			if (descriptor.getAProcesar().size() == 0) {
				//Terminaron todos los hilos y no quedan urls por procesar.
				salir = true;
			}
		}
		
		System.out.println("Cuantas quise procesar: " + cuantasvecesbusque);
		
	}

}
