package util.text;

public class StringUtils {

	public static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
	
	public static String randomStr(int length) {
		StringBuffer str = new StringBuffer();
		
		while(str.length() < length) {
			str.append( chars.charAt((int)(Math.random()*chars.length()) ));
		}
		return str.toString();
	}
	
}
