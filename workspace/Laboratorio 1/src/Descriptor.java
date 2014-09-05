import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	public boolean HTTP11 = false;
	public int profundidadMaxima = -1;
	public HashMap<String, String> links = new HashMap<String,String>();
	public List<String> mails = new ArrayList<String>();
	public List<Pair<Long, String>> aProcesar = new ArrayList<Pair<Long, String>>();

	public Descriptor(String urlInicial) {
		aProcesar.add(new Pair<Long, String>(new Long(0), urlInicial));
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
			if (profundidadMaxima != -1) {
				if (aProcesar.get(0).depth < profundidadMaxima) {
					ret = aProcesar.get(0).url;
					aProcesar.remove(0);
				}
			} else {
				ret = aProcesar.get(0).url;
				aProcesar.remove(0);
			}
		}
		return ret;
	}


	public boolean isHTTP11() {
		return HTTP11;
	}


	public void setHTTP11(boolean hTTP11) {
		HTTP11 = hTTP11;
	}

	public int getProfundidadMaxima() {
		return profundidadMaxima;
	}

	public void setProfundidadMaxima(int profundidadMaxima) {
		this.profundidadMaxima = profundidadMaxima;
	}

}
