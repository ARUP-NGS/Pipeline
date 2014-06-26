package operator.qc.checkers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;

import buffer.CSVFile;

public class NoCallChecker implements QCItemCheck<CSVFile>  {

	private long captureExtent;
	
	public NoCallChecker(long extent) {
		this.captureExtent = extent;
	}
	
	@Override
	public QCCheckResult checkItem(CSVFile noCallCSV) {
		QCCheckResult result = new QCCheckResult();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(noCallCSV.getAbsolutePath()));
			String line = reader.readLine();
			int noCallIntervals = 0;
			int noCallPositions = 0;
			while(line != null) {
				String[] toks = line.split(" ");
				if (toks.length == 4) {
					if (! toks[3].equals("CALLABLE")) {
						
						noCallIntervals++;
						try {
							long startPos = Long.parseLong(toks[1]);
							long endPos = Long.parseLong(toks[2]);
							noCallPositions += (endPos - startPos);
						} catch (NumberFormatException nfe) {
							//dont stress it
						}
					}
				}
				line = reader.readLine();
			}
			
			reader.close();
			
			
			DecimalFormat formatter = new DecimalFormat("0.00");
			double fractionBasesNoCalls = (double)noCallPositions / (double)captureExtent ;
			if (fractionBasesNoCalls > 0.10) {
				result.result = QCItemCheck.ResultType.SEVERE;
				result.message = formatter.format(100.0 * fractionBasesNoCalls) + "% of bases were not callable";				
			}
			else {
				if (fractionBasesNoCalls > 0.05) {
					result.result = QCItemCheck.ResultType.WARNING;
					result.message = formatter.format(100.0 * fractionBasesNoCalls) + "% of bases were not callable";
				}
				else {
					result.result = QCItemCheck.ResultType.OK;
					result.message = formatter.format(100.0 * fractionBasesNoCalls) + "% of bases were not callable";
				}
			}
			
		}
		catch(Exception ex) {
			result.result = QCItemCheck.ResultType.UNKNOWN;
			result.message = "Exception occurred during processing: " + ex.getMessage();
		}
		return result;
	}

}
