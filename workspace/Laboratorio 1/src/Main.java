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
        
        try {  
            //Parseamos la entrada con la configuración establecida    
         
             parser  = new BasicParser();  
             cmdLine = parser.parse(options, args);             
             
             if (cmdLine.hasOption("d"))
             	JOptionPane.showMessageDialog(null, "Tiene debug");
             
             if (cmdLine.hasOption("pozos"))
             	JOptionPane.showMessageDialog(null, "tiene pozos " + "arg " + cmdLine.getOptionValue("pozos"));
               
         } catch (org.apache.commons.cli.ParseException ex){  
             System.out.println(ex.getMessage());  
                
         }  
        
        
        
		boolean salir = false;
		int cantidadHilos = 5;
		String urlInicial = "http://www.google.com/doodles";
		Descriptor descriptor = new Descriptor(urlInicial);
		
		//Agrego más links antes de prueba para ver si toma uno cada hilo
//		descriptor.aProcesar.add("http://www.fing.edu.uy/inco/cursos/compil/");
//		descriptor.aProcesar.add("http://www.fing.edu.uy/inco/inicio");
		Thread[] t = new Thread[cantidadHilos];
		while (!salir) {
			/*
			 * Recreo hilos cada vez que los 5 terminan de procesar una URL.
			 * Esto no es muy efectivo xq tengo que recrearlos cada vez.
			 * Además, si 4 hilos terminaron, esperan por el 5to que termine para empezar a procesar nuevamente
			 * No se me ocurrio todavía como hacer para que un hilo siga procesando esperando por un posible url nuevo, 
			 * por que la condición de que sea 0 la lista de a procesar no sirve
			 * ya que podría pasar que 4 hilos terminen y quede ejecutando 1 solo todo el resto.
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
			if (descriptor.aProcesar.size() == 0) {
				//Terminaron todos los hilos y no quedan urls por procesar.
				salir = true;
			}
		}
		
	}

}
