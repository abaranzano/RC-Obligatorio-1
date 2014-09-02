import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	public final static boolean isHTTP11 = false;
	public HashMap<String, String> links = new HashMap<String,String>();
	public List<String> mails = new ArrayList<String>();
	public List<String> aProcesar = new ArrayList<String>();

	public Descriptor(String urlInicial) {
		aProcesar.add(urlInicial);
	}


	public void execute() {
		//		String url = args[args.length-1];
		Client prueba;
		try {
			prueba = new Client(getData(), 80, this);
			prueba.HTTPGet();
			String response = prueba.HTTPResponse();
			prueba.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addLink(String link) {
		this.links.put(link, link);
	}
	
	public void addMail(String mail) {
		this.mails.add(mail);
	}

	public synchronized String getData() {
		String ret = aProcesar.get(0);
		aProcesar.remove(0);
		return ret;
	}


}
