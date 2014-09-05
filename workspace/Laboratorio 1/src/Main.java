
public class Main {

	public static void main(String args[]) throws Exception 
	{ 
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
