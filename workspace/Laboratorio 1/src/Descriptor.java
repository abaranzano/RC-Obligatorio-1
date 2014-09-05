import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	public boolean HTTP11 = false;
	public HashMap<String, String> links = new HashMap<String,String>();
	public List<String> mails = new ArrayList<String>();
	public List<String> aProcesar = new ArrayList<String>();

	public Descriptor(String urlInicial) {
		aProcesar.add(urlInicial);
	}

	public void addLink(String link) {
		this.links.put(link, link);
	}

	public void addMail(String mail) {
		this.mails.add(mail);
	}

	public synchronized String getData() {
		String ret = null;
		if (aProcesar.size() > 0) {
			ret = aProcesar.get(0);
			aProcesar.remove(0);
		}
		return ret;
	}


	public boolean isHTTP11() {
		return HTTP11;
	}


	public void setHTTP11(boolean hTTP11) {
		HTTP11 = hTTP11;
	}



}
