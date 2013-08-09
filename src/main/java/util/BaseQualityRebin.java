package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.zip.GZIPInputStream;

/**
 * Utility for rebinning of base qualities 
 * @author brendan
 *
 */
public class BaseQualityRebin {

	double bins = 4;
	double maxQuality = 40.0;
	File source = null;

	private char[] newQualities = null;
	private FastQRead read = null;
	
	public BaseQualityRebin(int bins, File inputFQ) {
		this.bins = bins;
		this.source = inputFQ;
	}
	
	public BaseQualityRebin(File inputFQ) {
		this.source = inputFQ;
	}
	
	public void rebinAll(PrintStream output) throws IOException {
		maxQuality = inferMaxQuality(source);
		BufferedReader reader = getReader(source);
		FastQRead read = nextRead(reader);
		while(read != null) {
			rebinRead(read);
			output.println(read.name);
			output.println(read.bases);
			output.println(read.something);
			output.println(read.qualities);
			read = nextRead(reader);
		}
		
	}
	
	private static BufferedReader getReader(File source) throws FileNotFoundException, IOException {
		if (source.getName().endsWith(".gz")) {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(source))));
		}
		else {
			return new BufferedReader(new FileReader(source));
		}
	}
	
	public int inferMaxQuality(File fqFile) throws IOException {
		BufferedReader reader = getReader(fqFile); 
		int count = 0;
		int max = 0;
		FastQRead read = nextRead(reader);
		while(count < 1000 && read != null) {
			for(int i=0; i<read.qualities.length(); i++) {
				int qual = (int)read.qualities.charAt(i);
				if (qual > max)
					max = qual;
			}
			read = nextRead(reader);
			count++;
		}
		
		reader.close();
		return max;
	}
	
	
	public FastQRead nextRead(BufferedReader reader) {
		if (read == null)
		 read = new FastQRead();
		try {
			read.name = reader.readLine();
			read.bases = reader.readLine();
			read.something = reader.readLine();
			read.qualities = reader.readLine();
		} catch (IOException e) {
			return null;
		}
		if (read.name == null || read.bases == null || read.something == null || read.qualities == null) {
			return null;
		}
		return read;
	}
	
	public void rebinRead(FastQRead read) {
		if (newQualities == null || newQualities.length != read.qualities.length())
			newQualities = new char[read.qualities.length()];
		
		for(int i=0; i<read.bases.length(); i++) {
			int oldQuality = ((int)read.qualities.charAt(i)-33);
			double fracQuality = oldQuality / maxQuality;
			fracQuality = fracQuality > 1.0 ? 1.0 : fracQuality;
			int bin = (int)(Math.floor(bins*fracQuality+0.5));
			int newQuality = (int)Math.floor( (double)bin / (double)bins * maxQuality);
			newQualities[i] = (char)(newQuality+33);
		}
		read.qualities = new String(newQualities);
	}
	
	public static void listRead(FastQRead read) {
		System.out.println("Read name: " + read.name);
		for(int i=0; i<read.bases.length(); i++) {
			System.out.println(read.bases.charAt(i) + "\t" + read.qualities.charAt(i) + "\t" + ((int)read.qualities.charAt(i)-33) );
		}
	}
	
	static class FastQRead {
		String name = null;
		String bases = null;
		String something = null;
		String qualities = null;
	}
	
	public static void main(String[] args) throws IOException {
		BaseQualityRebin rebinner = new BaseQualityRebin(Integer.parseInt(args[0]), new File(args[1]));
		rebinner.rebinAll(System.out);
	}
}
