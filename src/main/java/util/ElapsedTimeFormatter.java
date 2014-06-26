package util;

public class ElapsedTimeFormatter {

	public static String getElapsedTime(long start, long end) {
		long elapsedMillis = end - start;
		if (elapsedMillis < 1000) {
			return elapsedMillis + " ms";
		}
		int seconds = (int)Math.floor( elapsedMillis / 1000.0 );
		int minutes = (int)Math.floor( elapsedMillis / (1000.0 * 60.0) );
		int hours = (int)Math.floor( elapsedMillis / (1000.0 * 60.0 * 60.0));
		
		//I think this is right?
		seconds = seconds % 60;
		minutes  = minutes % 60;
		
		String secStr = "" + seconds;
		if (seconds < 10)
			secStr = "0" + secStr;
		
		String minStr = "" + minutes;
		if (minutes < 10)
			minStr = "0" + minStr;
		
		return hours + ":" + minStr + ":" + secStr;
	}
}
