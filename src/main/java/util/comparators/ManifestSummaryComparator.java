package util.comparators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import util.comparators.CompareReviewDirs.ComparisonType;
import util.reviewDir.ReviewDirectory;

/**
 * Compares some sample information found in the sample manifest of two different review directories. Information like sample name, pipeline version, date run, run time, etc.
 * @author Kevin
 *
 */
public class ManifestSummaryComparator extends Comparator {

	public ManifestSummaryComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2);
		super.summaryTable = new ComparisonSummaryTable(analysisHeader, Arrays.asList("TRUTH", "TEST", "Notes"));
	}
	
	/** This performComparisonAndBuildSummaryTable function usually revolves around generating 3 arrays to input to the 
	 * 		outputTextTableSummary() function.  rowNames = An array that describes the information we will be comparing 
	 * 		for each review directory.
	 * 		c1 = An array of the given rowName information for the first review directory.
	 * 		c2 = An array of the given rowName information for the second review directory.
	 * 		c3 = A note about the comparison between c1 and c2, useful especially if there are inconsistencies to point out.
	 * @author Kevin
	 */
	@Override
	public void performComparison() throws IOException {
		//logger.info("Begin Manifest Summary Comparisons");

		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a");
		String date1 = sdf.format(Long.parseLong(rd1.getSampleManifest().getTime()));
		String date2 = sdf.format(Long.parseLong(rd2.getSampleManifest().getTime()));

		//Build table
		this.addNewEntry("sample.name", "Sample name", rd1.getSampleName(), rd2.getSampleName(), ComparisonType.NONE);
		this.addNewEntry("analysis.type", "Analysis Type", rd1.getAnalysisType() , rd2.getAnalysisType(), ComparisonType.NONE);
		this.addNewEntry("pipeline.version", "Pipeline Version", rd1.getSampleManifest().getPipelineVersion(), rd2.getSampleManifest().getPipelineVersion(), ComparisonType.NONE);
		
		this.addNewEntry("capture", "Capture", new File(rd1.getSampleManifest().getCapture()).getName(), new File(rd2.getSampleManifest().getCapture()).getName() , ComparisonType.TEXT);
		
		this.addNewEntry("run.date", "Run date", date1, date2 , ComparisonType.NONE);
		
		SimpleDateFormat sdfRunTime = new SimpleDateFormat("HH:mm:ss");
		
		Date run1Time;
		Date run2Time;
		try {
			run1Time = sdfRunTime.parse(this.getRunTimeFromLog(rd1.getSampleManifest().getLog()));
			run2Time = sdfRunTime.parse(this.getRunTimeFromLog(rd2.getSampleManifest().getLog())); // Set end date
			long duration  = run2Time.getTime() - run1Time.getTime();

			//long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
			//long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
			
			//String runTimeNotes = "Test run took " + diffInMinutes + " minutes longer.";
			this.addNewEntry("total.run.time", "Total Run Time", this.getRunTimeFromLog(rd1.getSampleManifest().getLog()), this.getRunTimeFromLog(rd2.getSampleManifest().getLog()), ComparisonType.TIME);
		} catch (ParseException e) {
			e.printStackTrace();
		} // Set start date
	}

	private String getRunTimeFromLog(File log) throws IOException {
		Vector<String> PipelineElapsedTime = new Vector<String>();

		BufferedReader br = new BufferedReader(new FileReader(log));  
		String line = br.readLine();  
		while (line != null)  
		{
			if(line.contains("Pipeline elapsed time")) {
				PipelineElapsedTime.add(line);
			}
			line = br.readLine();  
		}
		br.close();
		return PipelineElapsedTime.lastElement().split("time: ")[1];
	}
}
