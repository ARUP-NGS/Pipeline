package util.reviewDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A class which represents two common log files in a review directory: a pipeinstance log and a pipeline_input.xml file. It will provide a handle on things like fastq paths,
 *  if a run used annovar or snpeff, and run time of all parameters, etc.
 * 
 * @author kevin
 *
 */
public class PipelineLogFiles {

	//Could potentially just have a map of all useful information. Using descriptive variables instead.
	//protected Map<String, String> logInfo = new HashMap<String, String>();

	private String fastq1;
	private String fastq2;

	private Boolean annotatedWithAnnovar;
	
	protected File pipeInstanceLog = null;
	protected File pipelineInputTemplate = null;


	PipelineLogFiles(String logPath, String pipelineInputTemplate) {
		this.pipeInstanceLog = new File(logPath);
		
		try {
			this.pipelineInputTemplate = new File(pipelineInputTemplate);
		} catch (Exception e) {
			System.out.println("Looks like " + pipelineInputTemplate + " doesnt exist.");
			e.getMessage();
		}
		
		try {
			this.processPipelineInputTemplate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
/*		try {
			this.processLogFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	/** The pipeline_input.xml file is usually much shorter and easier to parse. Get fastqs and if snpeff was used for annotation.
	 * 
	 */
	private void processPipelineInputTemplate() {
		Pattern fastq1 = Pattern.compile(".*"); //For fastqs.
		Pattern fastq2 = Pattern.compile(".*R2.*.fastq.gz.*"); //For fastqs.

		Pattern annovar = Pattern.compile(".*class=\"operator.annovar.GeneAnnotator\".*");
		
		try {
			for(Scanner scanner = new Scanner(this.pipelineInputTemplate); scanner.hasNextLine();) {
				//System.out.println(scanner.nextLine());
				String line = scanner.nextLine();
				if (fastq1 == null) {
					Matcher fastq1Matcher = fastq1.matcher(line);
					if(fastq1Matcher.find()) { //WWe got a match?
						System.out.println("HERE fast1");
						this.fastq1 = fastq1Matcher.group(0);
					}
				}
				if (fastq2 == null) {
					Matcher fastq2Matcher = fastq2.matcher(line);
					if(fastq2Matcher.find()) { //WWe got a match?
						this.fastq1 = fastq2Matcher.group(0);
					}
				}
				if (annotatedWithAnnovar == null) {
					Matcher annotationMatcher = annovar.matcher(line);
					if(annotationMatcher.find()) {
						annotatedWithAnnovar = true;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/** This method processes the given log file and extracts useful information.
	 * @return
	 * @throws FileNotFoundException 
	 */
	public void processLogFile() {
		Pattern fastq = Pattern.compile(".*R1.*fast[aq].*R2.*fast[aq].*"); //For fastqs.
		Pattern snpeff = Pattern.compile(".*snpeff.*");
		
		try {
			for(Scanner scanner = new Scanner(this.pipeInstanceLog); scanner.hasNextLine();) {
				//System.out.println(scanner.nextLine());
				String line = scanner.nextLine();
				Matcher fastqMatcher = fastq.matcher(line);
				Matcher annotationMatcher = snpeff.matcher(line);
				
				if(fastqMatcher.find()) { //WWe got a match?
					String[] fastqsLine = line.split("\\s+"); //Next line should be our fastqs.
					if (fastqsLine.length == 2) {
						this.fastq1 = fastqsLine[0];
						this.fastq2 = fastqsLine[1];
					} else {
						System.out.println("Unable to parse fastqs from log file.");
					}
				}
				if(annotationMatcher.find()) {
					annotatedWithAnnovar = true;
				}
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	public String[] getFastqNames() {
		return new String[]{fastq1, fastq2};
	}
	
	public Boolean getannotatedWithAnnovar() {
		return this.annotatedWithAnnovar;
	}
	
	public String getLogFileName() {
		return pipeInstanceLog.getName();
	}
}
