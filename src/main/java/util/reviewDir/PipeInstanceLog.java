package util.reviewDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A class which represents a pipeinstance log. It will provide a handle on things like fastq paths, etc.
 * 
 * @author kevin
 *
 */
public class PipeInstanceLog {

	//Use these as keys to access various useful information in the logInfo map..
	public static final String FASTQ1 = "fastq1";
	public static final String FASTQ2 = "fastq2";


	protected Map<String, String> logInfo = new HashMap<String, String>();
	protected File logFile = null;

	PipeInstanceLog(String logPath) {
		this.logFile = new File(logPath);
		try {
			//this.processLogFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** This method processes the given log file and extracts useful information. Not implemented.
	 * @return
	 * @throws FileNotFoundException 
	 */
	public void processLogFile() {
	}
	
	public String[] getFastqNames() throws Exception {
		Pattern r = Pattern.compile(".*R1.*fast[aq].*R2.*fast[aq].*"); //For fastqs.

		for(Scanner scanner = new Scanner(this.logFile ); scanner.hasNextLine();) {
			//System.out.println(scanner.nextLine());
			String line = scanner.nextLine();
			Matcher m = r.matcher(line);
			if(m.find()) { //WWe got a match?
				String[] fastqs = line.split("\\s+"); //Next line should be our fastqs.
				if (fastqs.length == 2) {
					logInfo.put(FASTQ1, fastqs[0]);
					logInfo.put(FASTQ2, fastqs[1]);
					System.out.println("Successfully parsed Fastq path: " + this.logFile.getAbsolutePath());
				} else {
					throw new Exception("Unable to parse fastqs from log file.");
				}

			}
		}
		return new String[]{logInfo.get(FASTQ1), logInfo.get(FASTQ2)};
	}
	
	public String getLogFileName() {
		return logFile.getName();
	}
}
