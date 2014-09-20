import java.io.IOException;


public class Decorator implements Runnable {

	private Descriptor descriptor = null;
	private Worker worker = null;
	private long id = 0; //Thread ID.
	
	Decorator(long id) {
		this.id = id;
	}

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		boolean salir = false;
		this.worker = new Worker(String.valueOf(this.id));
		worker.setDescriptor(this.descriptor);
		while (!salir) {
			this.descriptor.revivo();
			Pair<Integer, String> actual = this.descriptor.getData();		
			if (actual != null) {
				try {
					worker.initWorker(actual);					
					worker.doJob();
				} catch (IOException e) {
					Log.error(String.valueOf(id), "Error original: " + e.getMessage());
				} catch (TimeoutException e) {
					Log.error(String.valueOf(id), "Error: " + e.getMessage());
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
