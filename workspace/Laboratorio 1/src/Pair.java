
public class Pair<Integer, String> {
	
	private Integer depth;
	private String url;
	
	Pair(Integer depth, String url) {
		this.depth = depth;
		this.url = url;
	}
	
	public String getUrl(){
		return this.url;
	}
	
	public Integer getDepth(){
		return this.depth;
	}

}
