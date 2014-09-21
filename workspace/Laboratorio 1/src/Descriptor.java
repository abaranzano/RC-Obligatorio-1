import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Descriptor {

	private boolean isPersistent = false;
	private boolean usesProxy = false;
	private String proxy = null;
	private int profundidadMaxima = -1;
	private HashMap<String, String> links = null;
	private List<Pair<Integer, String>> aProcesar = null;	
	private boolean usesPozos = false;
	private String filePozos = null;
	private boolean usesMultilang = false;
	private String fileMultilang;
	private int cantHilos = 1;
	private int cantHilosFinalizados = 1; //Empieza en la misma cantidad que los Hilos por que el "revivo" resta.
	private ConnectionManager connectionManager = null;

	public Descriptor() {
		this.links = new HashMap<String,String>();
		this.aProcesar = new ArrayList<Pair<Integer, String>>();
		this.connectionManager = new ConnectionManager();
	}

	public synchronized void agregarURL(Integer profundidad, String url) {

		if (this.aProcesar.size() == 0) {
			this.aProcesar.add(new Pair<Integer, String>(profundidad, url));
		} else {
			int i = 0;
			while (i < this.aProcesar.size() && this.aProcesar.get(i).getDepth() < profundidad) {
				i++;				
			}

			this.aProcesar.add(i, new Pair<Integer, String>(profundidad, url));
		}
	}

	public void addLink(String link) {
		this.links.put(link, link);
	}

	public synchronized Pair<Integer, String> getData() {
		Pair<Integer, String> ret = null;
		if (aProcesar.size() > 0) {
			if (profundidadMaxima != -1) {
				if (aProcesar.get(0).getDepth() < profundidadMaxima) {
					ret = aProcesar.get(0);
					aProcesar.remove(0);
				} else {
					aProcesar.remove(0);
				}
			} else {
				ret = aProcesar.get(0);
				aProcesar.remove(0);
			}
		}
		return ret;
	}


	public boolean isPersistent() {
		return this.isPersistent;
	}


	public void setPersistent(boolean persistent) {
		this.isPersistent = persistent;
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

	public boolean getUsesProxy(){
		return this.usesProxy;
	}

	public String getProxy(){
		return this.proxy;
	}

	public int getProfundidadMaxima(){
		return this.profundidadMaxima;
	}

	public HashMap<String, String> getLinks(){
		return this.links;
	}

	public List<Pair<Integer, String>> getAProcesar(){
		return this.aProcesar;
	}

	public boolean getPozo(){
		return this.usesPozos;
	}

	public void setPozo(){
		this.usesPozos = true;
	}

	public String getFilePozo(){
		return this.filePozos;
	}

	public void setFilePozo(String path){
		this.filePozos = path;
	}

	public boolean getMultilang(){
		return this.usesMultilang;
	}

	public void setMultilang(){
		this.usesMultilang = true;
	}

	public String getFileMultilang(){
		return this.fileMultilang;
	}

	public void setFileMultilang(String path){
		this.fileMultilang = path;
	}

	public int getCantHilos() {
		return cantHilos;
	}

	public void setCantHilos(int cantHilos) {
		this.cantHilos = cantHilos;
		this.cantHilosFinalizados = cantHilos; //Arranca con la cantidad de hilos xq al revivir le resto.
	}

	public synchronized void finalizo() {
		this.cantHilosFinalizados++;
	}

	public synchronized void revivo() {
		this.cantHilosFinalizados--;
	}

	public synchronized int estadoHilos() {
		return this.cantHilosFinalizados;
	}

	public synchronized int getCantidadDatosAProcesar() {
		return this.aProcesar.size();
	}

	public synchronized Socket getConnection(String id, String host, int port, boolean keepAlive) throws IOException, HersonFensonException {
		return connectionManager.getConnection(id, host, port, keepAlive);
	}

	public synchronized void connectionClose(String id, Socket conn, boolean keepAlive) throws IOException {
		connectionManager.connectionClose(id, conn, keepAlive);
	}

}
