package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.variant.VariantRec;

public class BoostVarQualityFromPop extends Annotator {

	public static final String VCF_PATH = "pop.vcf";
	private boolean initialized = false;
	private TabixReader reader = null;
	
	//Modified quality score will be original quality * multiplier * frequency from vcf
	//For 1000 genomes data, something around 1000 might make sense - if a variant has been seen 
	//only once before, then its frequency will be near 0.001, and the multiplier will end up being 1.0
	//But if we've seen it ten times before, the multiplier will end up being 10. 
	private double multiplier = 1000.0;

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (reader == null) {
			throw new OperationFailedException("Could not initialize tabix reader", this);
		}
		
		String contig = var.getContig();
		Integer pos = var.getStart();
		
		String queryStr = contig + ":" + pos + "-" + (pos);
		//int count = 0;
		try {
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();
					while(val != null) {
						boolean ok = addAnnotationsFromString(var, val);
						if (ok) {
							break;
						}
						val = iter.next();
					}
				} catch (IOException e) {
					throw new OperationFailedException("Error reading dbSNP data file: " + e.getMessage(), this);
				}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}

	}
	
	private boolean addAnnotationsFromString(VariantRec var, String str) throws OperationFailedException {
		String[] toks = str.split("\t");
		if (! toks[0].equals(var.getContig())) {
			//We expect that sometimes we'll not get the right contig
			return false;
		}
		if (! toks[1].equals("" + var.getStart())) {
			//We expect that sometimes we'll not get the right position (not sure why exactly... tabix doesn't work perfectly I guess			return;
		}
		
		if (toks[4].equals(var.getAlt())) {
			//Found a match!
			String[] infos = toks[7].split(";");
			
			boolean validated = false;
			for(int i=0; i<infos.length; i++) {
				String inf = infos[i].trim();
								
				if (inf.startsWith("AF=")) {
					try {
						String valStr = inf.replace("AF=", "");
						Double freq = Double.parseDouble(valStr);
						double qualMultiplier = multiplier*freq;
//						if (qualMultiplier > 1.0) {
//							//System.out.println("For " + var + " found frequency " + freq + ", boosting quality from " + var.getQuality() + " to " + var.getQuality()*qualMultiplier);
//							var.setQuality( var.getQuality()*qualMultiplier);
//						}
					}
					catch (NumberFormatException nfe) {
						//don't stress it
					}
				}
			}
			
			if (validated) {
				var.addAnnotation(VariantRec.CLINVAR_VALIDATED, "yes");
			} 
			else {
				var.addAnnotation(VariantRec.CLINVAR_VALIDATED, "no");	
			}
			
			
		}
		
		return false;
	}

	private void initializeReader() {
		String filePath = this.getAttribute(VCF_PATH);
		if (filePath == null) {
			filePath = this.getPipelineProperty(VCF_PATH);
		}
		
		if (filePath == null) {
			throw new IllegalArgumentException("Path to population vcf data not specified, use " + VCF_PATH);
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing population VCF path using " + filePath);
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening population vcf data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//try to initialize reader here 
		initializeReader();
	}
	
	
	
}
