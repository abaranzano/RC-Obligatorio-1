import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;


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
		while (!salir) {
			this.descriptor.revivo();
			Pair<Integer, String> actual = this.descriptor.getData();		
			if (actual != null) {
				try {
					this.worker = new Worker(actual);
					worker.setDescriptor(this.descriptor);
					worker.doJob();
				} catch (IOException e) {
					System.err.println("Error original: " + e.getMessage());
				}
			} 		
			this.descriptor.finalizo();
			while (this.descriptor.getCantidadDatosAProcesar() == 0 && !salir) {
				if (this.descriptor.getCantHilos() == this.descriptor.estadoHilos()) {
					salir = true;
				}
			}
		}
	}

}
