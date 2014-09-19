package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

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
	
	public static String getGitCommitTag() throws IOException {
		Map<String, String> props = getGitProperties();
		return props.get("git.commit.id.abbrev");
	}
	
	public static String getCompileDateStr() throws IOException {
		Map<String, String> props = getGitProperties();
		return props.get("git.build.time");
	}
	
	
	
	private static Map<String, String> getGitProperties() throws IOException {
		File jarFile = new File(Pipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		
		URL jarURL;
		
			jarURL = new URL("jar:file:" + jarFile.getAbsolutePath() + "!/");
			JarURLConnection jarConnection = (JarURLConnection)jarURL.openConnection();
			ZipEntry entry = jarConnection.getJarFile().getEntry("git.properties");
			InputStream propsInputStream = jarConnection.getJarFile().getInputStream(entry);
		
		Map<String, String> props = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(propsInputStream));
		String line = reader.readLine();
		while(line != null) {
			if (! line.startsWith("#")) {
				String[] toks = line.split("=", 2);
				props.put(toks[0], toks[1]);
			}
			line = reader.readLine();
		}
		return props;
	}
	
		
	/**
	 * Provides time in ms since the  META-INF/MANIFEST.MF file in the jar file in which Pipeline.class
	 * is located was modified. This provides a nice way to query the compile time of this jar.  
	 * @return
	 */
	public static long getManifestModifiedTime() {
		File jarFile = new File(Pipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		//System.out.println("file path: " + jarFile.getAbsolutePath());
		
		URL jarURL;
		try {
			jarURL = new URL("jar:file:" + jarFile.getAbsolutePath() + "!/");
			//System.out.println("Jar url: " + jarURL);
			JarURLConnection jarConnection = (JarURLConnection)jarURL.openConnection();
			long modTime = jarConnection.getJarFile().getEntry("META-INF/MANIFEST.MF").getTime();
			return modTime;
		} catch (MalformedURLException e1) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not obtain jar url, no way find its creation date: " + e1.getLocalizedMessage());
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error loading jar url: " + e.getLocalizedMessage());
		}
		catch(Exception ex) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error loading jar info: " + ex.getLocalizedMessage());
		}
		
		return -1l;
	}
	
	
	public static void main(String[] args) {
//		Date modTime = new Date(MetaInfo.getManifestModifiedTime());
//		System.out.println("Jar file modified time: " + modTime);
	
		try {
			System.out.println("Git commit tag : " + MetaInfo.getGitCommitTag());
			System.out.println("Git build time : " + MetaInfo.getCompileDateStr());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
