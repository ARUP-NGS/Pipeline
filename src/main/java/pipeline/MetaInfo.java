package pipeline;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Access to a few pieces of meta-info, including the last modified date and creation date of the
 * jar's MANIFEST file
 * @author brendan
 *
 */
public class MetaInfo {

	/**
	 * Return absolute path to jar file containing Pipeline.class
	 * @return
	 */
	public static String getJarFilePath() {
		return Pipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}
	
	/**
	 * Provides time in ms since the  META-INF/MANIFEST.MF file in the jar file in which Pipeline.class
	 * is located was modified. This provides a nice way to query the compile time of this jar.  
	 * @return
	 */
	public static long getManifestModifiedTime() {
		File jarFile = new File(Pipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		System.out.println("file path: " + jarFile.getAbsolutePath());
		
		URL jarURL;
		try {
			jarURL = new URL("jar:file:" + jarFile.getAbsolutePath() + "!/");
			System.out.println("Jar url: " + jarURL);
			JarURLConnection jarConnection = (JarURLConnection)jarURL.openConnection();
			long modTime = jarConnection.getJarFile().getEntry("META-INF/MANIFEST.MF").getTime();
			return modTime;
		} catch (MalformedURLException e1) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not obtain jar url, no way find its creation date: " + e1.getLocalizedMessage());
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error loading jar url: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		return -1l;
	}
	
	
	public static void main(String[] args) {
		Date modTime = new Date(MetaInfo.getManifestModifiedTime());
		System.out.println("Jar file modified time: " + modTime);
		
	}
}
