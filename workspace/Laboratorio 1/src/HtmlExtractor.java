import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HtmlExtractor {

	private Pattern patternTag, patternLink;
	private Matcher matcherTag, matcherLink;
 
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = 
		"\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
 
 
	public HtmlExtractor() {
		patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
		patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
	}
 
	public Vector<HtmlLink> grabHTMLLinks(final String html) {
 
		Vector<HtmlLink> result = new Vector<HtmlLink>();
 
		matcherTag = patternTag.matcher(html);
 
		while (matcherTag.find()) {
 
			String href = matcherTag.group(1); // href
			String linkText = matcherTag.group(2); // link text
 
			matcherLink = patternLink.matcher(href);
 
			while (matcherLink.find()) {
 
				String link = matcherLink.group(1); // link
				HtmlLink obj = new HtmlLink();
				obj.setLink(link);
				obj.setLinkText(linkText);
 
				result.add(obj);
 
			}
 
		}
 
		return result;
 
	}
 
}
