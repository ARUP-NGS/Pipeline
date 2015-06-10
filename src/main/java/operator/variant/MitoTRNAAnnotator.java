package operator.variant;

import gene.MitoAnnoLookupContainer;
import gene.MitoAnnoLookupContainer.MitoAnnoInfo;

import java.io.File;
import java.io.IOException;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

/**
 * An annotator that does mito tRNAs since SnpEff doesn't. This just affects variants with contig == "M" or "MT",
 * and only appends a 'gene name' that is actually the name of the tRNA intersected. 
 * @author brendan
 *
 */
public class MitoTRNAAnnotator extends Annotator {

	public static final String GBK_PATH = "gbk.path";
	private MitoAnnoLookupContainer annoLookup = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (annoLookup == null) {
			initializeContainer();
			
			if (annoLookup == null){
				throw new OperationFailedException("Could not create mito anno lookup container", this);
			}
		}
		
		if (var.getContig().equals("M") || var.getContig().equals("MT")) {
			Object[] hits;
			if (var.isIndel()) {
				hits = annoLookup.getIntervalObjectsForRange("MT", var.getStart(), var.getIndelLength());
			}
			else {
				hits = annoLookup.getIntervalObjectsForRange("MT", var.getStart(), var.getStart()+1);
			}
			
			for(int i=0; i<hits.length; i++) {
				MitoAnnoInfo info = (MitoAnnoInfo)hits[i];
				appendAnnotation(var, VariantRec.GENE_NAME, info.featureName);
				
				//Overwrite 'intergenic_region' if it exists
				String vType = var.getAnnotation(VariantRec.VARIANT_TYPE);
				if (vType == null || vType.length()==0 || vType.equals("intergenic_region")) {
					var.addAnnotation(VariantRec.VARIANT_TYPE, info.featureType);
				} else {
					appendAnnotation(var, VariantRec.VARIANT_TYPE, info.featureType);
				}
			}
		}
	}

	
	private void initializeContainer() {
		
		String pathToGBK = this.getAttribute(GBK_PATH);
		if (pathToGBK == null) {
			throw new IllegalArgumentException("No path to mito gbk file specified, cannot load mito anno info");
		}
		if (! pathToGBK.startsWith("/")) {
			pathToGBK = this.getProjectHome() + "/" + pathToGBK;
		}
		File gbkFile = new File(pathToGBK);
		annoLookup = new MitoAnnoLookupContainer();
		try {
			annoLookup.readIntervals(gbkFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not read mito gbk file at path " + gbkFile.getAbsolutePath() + ", cannot load mito anno info");
		}
		
		
		
	}

}
