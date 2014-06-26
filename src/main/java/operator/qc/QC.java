package operator.qc;

import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.DOCMetrics;
import buffer.variant.VariantPool;

/**
 * A wrapper for a collection of objects that hold information about QC metrics
 * @author brendan
 *
 */
public class QC {
	
	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMMetrics rawBAMMetrics = null;
	BAMMetrics finalBAMMetrics = null;
	VariantPool variantPool = null;
	CSVFile noCallCSV = null;
	BEDFile captureBed = null;

}
