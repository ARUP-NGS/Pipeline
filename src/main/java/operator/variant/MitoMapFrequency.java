package operator.variant;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import buffer.variant.VariantRec;
import operator.OperationFailedException;

public class MitoMapFrequency extends AbstractTabixAnnotator {

	public static final String MITOMAPFREQ_PATH = "mitomap.freq.db.path";
	protected int totalAlleles;
	
	
    @Override
    protected String getPathToTabixedFile() {
    	return this.searchForAttribute(MITOMAPFREQ_PATH);
    }

	/**
	 * This overrides the 'prepare' method in the base Annotator class. It is always called 
	 * prior to the first call to annotateVariant, and gives us a chance to do a little
	 * initialization. 
	 * @throws OperationFailedException 
	 */
	@Override
	protected void prepare() throws OperationFailedException {
		//initializeReader( getPathToTabixedFile());
		//get total alleles from freq vcf header
		this.totalAlleles = getTotalAlleles();
	}
	
	

	@Override
	public boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) throws OperationFailedException {
		

		//parse freq vcf line
		String[] vcfCols = val.split("\t");
		String[] infos = vcfCols[7].split(";");
		if (! infos[0].startsWith("AC=")) {
				throw new OperationFailedException("The MitoMap frequency vcf has unexpected form. INFO field should start with AC info.", this);
		}
		
		//get values for annotations
		String[] acs = infos[0].substring(3).split(",");
		int ac = Integer.parseInt(acs[altIndex]);
		String[] alts = vcfCols[4].split(",");
		String alt = alts[altIndex];
		int pos = Integer.parseInt(vcfCols[1]);
		String ref = vcfCols[3];

		//generate fraction
		double freq = (double)ac / this.totalAlleles;
		//use BigDecimal number class to force 6 decimal points of precision then recast to double
		//so that text file formatting will only have 6 rather than many more decimal points
		Double freqRnd = BigDecimal.valueOf(freq)
				.setScale(6, RoundingMode.HALF_UP)
				.doubleValue();

		//generate allele change
		String alleleChange = ref + pos + alt;
		
		//annotate
		var.addProperty(VariantRec.MITOMAP_FREQ, freqRnd);
		var.addAnnotation(VariantRec.MITOMAP_ALLELE_ID, alleleChange);

		return true;
	}

	protected int getTotalAlleles() throws OperationFailedException {
		Integer totalAlleles = null;

		String pathToTabix = getPathToTabixedFile();
		try {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(pathToTabix));
			BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
			String line = br.readLine();
		    while (line != null) {
		    	//test if line is the allele count definition line
		    	if (line.startsWith("##INFO=<ID=AC")) {
		    		Matcher matcher = Pattern.compile("\\d+").matcher(line);
		    		matcher.find();
		    		if (matcher.group() != null) {
		    			totalAlleles = Integer.valueOf(matcher.group());
		    			break;
		    		}
		    	}
		        line = br.readLine();
		    }
		    br.close();
		} catch (FileNotFoundException e) {
				throw new OperationFailedException("The MitoMap frequency vcf file was not found (to find total sequences in data set in file header).", this);
		} catch (IOException e) {
				throw new OperationFailedException("The MitoMap frequency vcf file had IOException (to find total sequences in data set in file header).", this);
		}
		
		if (totalAlleles == null) {
			throw new OperationFailedException("Could not find total sequences in the MitoMap frequency vcf file header.", this);
		}
		return totalAlleles;
	}
	


}
