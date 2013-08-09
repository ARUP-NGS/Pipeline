package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import operator.gene.HGMDB;
import operator.gene.HGMDB.HGMDInfo;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Determines if a variant has an exact HGMD match - not just a variant in a gene with 
 * an HGMD entry, but an entry at the exact same position in HGMD
 * @author brendan
 *
 */
public class HGMDVarAnnotator extends Annotator {

	
	public static final String HGMDB_PATH = "hgmd.path";
	
	HGMDB db = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (db == null) {
			db = new HGMDB();
			Object hgmdObj =  getPipelineProperty(HGMDB_PATH);
			if (hgmdObj== null) {
				throw new OperationFailedException("Could not initialize HGMD db, no path to db file specified (use " + HGMDB_PATH + ")", this);
			}
			File dbFile = new File(hgmdObj.toString());
			if (! dbFile.exists()) {
				throw new OperationFailedException("HGMD db file at path " + hgmdObj.toString() + " does not exist", this);
			}
			Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing hgmd db from file: " + dbFile.getAbsolutePath());
			try {
				db.initializeMap(dbFile);
			} catch (IOException e) {
				throw new OperationFailedException("Error reading HGMD db file at path " + hgmdObj.toString() + " : " + e.getMessage(), this);
			}
			
		}
		
		HGMDInfo info = db.getRecord(var.getContig(), var.getStart());
		if (info != null) {
			String assocType = "?";
			if (info.assocType.equals("DM")) {
				assocType = "Disease-causing";
			}
			if (info.assocType.equals("DP")) {
				assocType = "Disease-associated polymorphism";
			}
			if (info.assocType.equals("DFP")) {
				assocType = "Disease-associated polymorphism with functional evidence";
			}
			if (info.assocType.equals("FP")) {
				assocType = "Functional polymorphism with in vitro evidence";
			}
			if (info.assocType.equals("FTV")) {
				assocType = "Frameshifting or truncating variant";
			}
			var.addAnnotation(VariantRec.HGMD_HIT, info.condition + ", " + assocType + " (" + info.cDot + ",  " + info.citation + ")");
		}
		
	}

}
