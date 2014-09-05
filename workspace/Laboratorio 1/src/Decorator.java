import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class Decorator implements Runnable {

	Descriptor descriptor = null;
	Worker worker = null;

	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		String urlAProcesar = this.descriptor.getData();	

		if (urlAProcesar != null) {
			try {
				if (!(this.descriptor.links.containsKey(urlAProcesar))) {
				//TODO: Las URL tienen que tener http:// y esas cosas sino el new URL da excepcion
				URL url = new URL (urlAProcesar);
				//TODO: Chequear que la url.getPath() de el camino correcto completo, fijarse si tiene alguna variable o referencia (estilo ?name=algo o #Ref) 
				this.worker = new Worker(url.getHost(), url.getPort(), url.getPath());
				worker.setDescriptor(this.descriptor);
				worker.doJob();
				} else {
					//TODO: Encontre un ciclo
				}
			} catch (MalformedURLException e) {
				System.err.println("Error. La url a procesar no tiene un protocolo válido. Error Original: " + e.getMessage());
			} catch (IOException e) {
				System.err.println("Error original: " + e.getMessage());
			}
		} 		


	}

}
