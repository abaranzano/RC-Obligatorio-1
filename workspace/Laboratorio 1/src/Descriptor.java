import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	private boolean HTTP11 = false;
	private boolean usesProxy = false;
	private String proxy = null;
	private int profundidadMaxima = 1;
	private int canthijosAProcesar = 2; //esto despues sw puede ir
	private HashMap<String, String> links = null;
	private List<String> mails = null;
	private List<Pair<Integer, String>> aProcesar = null;

	public Descriptor() {
		this.links = new HashMap<String,String>();
		this.mails = new ArrayList<String>();
		this.aProcesar = new ArrayList<Pair<Integer, String>>();
	}

	public void agregarURL(Integer profundidad, String url) {
		this.aProcesar.add(new Pair<Integer, String>(profundidad, url));
	}
	
	public void addLink(String link) {
		this.links.put(link, link);
	}

	public void addMail(String mail) {
		this.mails.add(mail);
	}

	public synchronized Pair<Integer, String> getData() {
		Pair ret = null;
		if (aProcesar.size() > 0) {
			if (profundidadMaxima != -1) {
				if (aProcesar.get(0).getDepth() < profundidadMaxima) {
					ret = aProcesar.get(0);
					aProcesar.remove(0);
				}
			} else {
				ret = aProcesar.get(0);
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

	

	public void setProfundidadMaxima(int profundidadMaxima) {
		this.profundidadMaxima = profundidadMaxima;
	}

	public boolean isUsesProxy() {
		return usesProxy;
	}

	public void setUsesProxy(boolean usesProxy) {
		this.usesProxy = usesProxy;
	}


	public void setProxy(String proxy) {
		this.proxy = proxy;
	}
	
	public boolean getHTTP11(){
		return this.HTTP11;
	}
	
	public boolean getusesProxy(){
		return this.usesProxy;
	}
	
	public String getProxy(){
		return this.proxy;
	}
	
	public int getProfundidadMaxima(){
		return this.profundidadMaxima;
	}
	
	public int getCanthijosAProcesar(){
		return this.canthijosAProcesar;
	}
	
	public HashMap<String, String> getLinks(){
		return this.links;
	}
	
	public List<String> getMails(){
		return this.mails;
	}
	 
	public List<Pair<Integer, String>> getAProcesar(){
		return this.aProcesar;
	}

}
