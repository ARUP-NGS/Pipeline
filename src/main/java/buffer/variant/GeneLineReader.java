package buffer.variant;

import java.io.File;
import java.io.IOException;

/**
 * This line reader attempts to read gene-related information, such as gene name, nm number, and exon function
 * from a csv file. Right now, it's assumed that the csv file was generated in the standard csv-format
 * use bye this package
 * @author brendan
 *
 */
public class GeneLineReader extends CSVLineReader {

	String sourceStr = null;
	
	public GeneLineReader(File csvFile) throws IOException {
		super(csvFile);
		
		sourceStr = csvFile.getName();
		
		if (!hasHeader()) {
			throw new IOException("No header information found, cannot parse gene information");
		}
		
		if (!hasHeaderCol(VariantRec.GENE_NAME)) {
			throw new IOException("No header column found for " + VariantRec.GENE_NAME);
		}
		
		if (!hasHeaderCol(VariantRec.VARIANT_TYPE)) {
			throw new IOException("No header column found for " + VariantRec.VARIANT_TYPE);
		}
		
		if (!hasHeaderCol(VariantRec.EXON_FUNCTION)) {
			throw new IOException("No header column found for " + VariantRec.EXON_FUNCTION);
		}
		
	}

	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		
		String contig = getContig(toks);
		Integer start = getStart(toks);
		String ref = getRef(toks);
		String alt = getAlt(toks);
		
		Integer end = getEnd(toks);
		String geneName = getValueForHeader(VariantRec.GENE_NAME, toks);
		String varType = getValueForHeader(VariantRec.VARIANT_TYPE, toks);
		String varFunc = getValueForHeader(VariantRec.EXON_FUNCTION, toks);
		String popFreqStr =  getValueForHeader(VariantRec.POP_FREQUENCY, toks);
		String cDot = getValueForHeader(VariantRec.CDOT, toks);
		String pDot = getValueForHeader(VariantRec.PDOT, toks);
		
		Double popFreq = 0.0;
		if (popFreqStr != null && (!popFreqStr.equals("-"))) {
			popFreq = Double.parseDouble(popFreqStr);
		}
		
		VariantRec rec = new VariantRec(contig, start, start+ref.length(), ref, alt, 10.0, true);
		rec.addAnnotation(VariantRec.GENE_NAME, geneName);
		rec.addAnnotation(VariantRec.VARIANT_TYPE, varType);
		rec.addAnnotation(VariantRec.EXON_FUNCTION, varFunc);
		rec.addProperty(VariantRec.POP_FREQUENCY, popFreq);
		rec.addAnnotation(VariantRec.CDOT, cDot);
		rec.addAnnotation(VariantRec.PDOT, pDot);
		if (sourceStr != null)
			rec.addAnnotation(VariantRec.SOURCE, sourceStr);
			
		return rec;
	}
}
