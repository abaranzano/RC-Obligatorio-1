import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	public boolean HTTP11 = false;
	public boolean usesProxy = false;
	public String proxy = null;
	public int profundidadMaxima = -1;
	public HashMap<String, String> links = null;
	public List<String> mails = null;
	public List<Pair<Long, String>> aProcesar = null;

	public Descriptor() {
		this.links = new HashMap<String,String>();
		this.mails = new ArrayList<String>();
		this.aProcesar = new ArrayList<Pair<Long, String>>();
	}

	public void agregarURL(String url) {
		this.aProcesar.add(new Pair<Long, String>(new Long(0), url));
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
		return this.HTTP11;
	}


	public void setHTTP11(boolean hTTP11) {
		this.HTTP11 = hTTP11;
	}

	public int getProfundidadMaxima() {
		return profundidadMaxima;
	}

	public void setProfundidadMaxima(int profundidadMaxima) {
		this.profundidadMaxima = profundidadMaxima;
	}

	public boolean isUsesProxy() {
		return usesProxy;
	}

	public void setUsesProxy(boolean usesProxy) {
		this.usesProxy = usesProxy;
	}

	public String getProxy() {
		return proxy;
	}

	public void setProxy(String proxy) {
		this.proxy = proxy;
	}
	

}
