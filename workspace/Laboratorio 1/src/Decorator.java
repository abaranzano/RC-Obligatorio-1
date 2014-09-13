import java.io.IOException;


public class Decorator implements Runnable {

	private Descriptor descriptor = null;
	private Worker worker = null;

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		boolean salir = false;
		this.worker = new Worker();
		worker.setDescriptor(this.descriptor);
		while (!salir) {
			this.descriptor.revivo();
			Pair<Integer, String> actual = this.descriptor.getData();		
			if (actual != null) {
				try {
					worker.initWorker(actual);					
					worker.doJob();
				} catch (IOException e) {
					System.err.println("Error original: " + e.getMessage());
				} catch (TimeoutException e) {
					System.err.println("Error: " + e.getMessage());
				}
			} 		
			this.descriptor.finalizo();
			//Polling, mientras no haya datos a procesar y no hayan terminado todos los hilos.
			while (this.descriptor.getCantidadDatosAProcesar() == 0 && !salir) {
				if (this.descriptor.getCantHilos() == this.descriptor.estadoHilos()) {
					salir = true;
				}
			}
		}
	}

}
