
public class Pair<Long, String> {
	
	public Long depth;
	public String url;
	
	Pair(Long depth, String url) {
		this.depth = depth;
		this.url = url;
	}
	
	public String getUrl(){
		return this.url;
	}
	
	public Long getDepth(){
		return this.depth;
	}

}
