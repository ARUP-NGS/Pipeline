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
	public static final String HGMDB_INDEL_PATH = "hgmd.indel.path";
	
	HGMDB db = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (db == null) {
			db = new HGMDB();
			Object hgmdSNVObj =  getPipelineProperty(HGMDB_PATH);
			if (hgmdSNVObj== null) {
				throw new OperationFailedException("Could not initialize HGMD db, no path to db file specified (use " + HGMDB_PATH + ")", this);
			}
			File snvFile = new File(hgmdSNVObj.toString());
			if (! snvFile.exists()) {
				throw new OperationFailedException("HGMD db file at path " + hgmdSNVObj.toString() + " does not exist", this);
			}
			
			
			Object hgmdIndelObj =  getPipelineProperty(HGMDB_INDEL_PATH);
			if (hgmdIndelObj== null) {
				throw new OperationFailedException("Could not initialize HGMD INDEL db, no path to db file specified (use " + HGMDB_INDEL_PATH + ")", this);
			}
			File indelFile = new File(hgmdIndelObj.toString());
			if (! indelFile.exists()) {
				throw new OperationFailedException("HGMD db indel file at path " + hgmdIndelObj.toString() + " does not exist", this);
			}
			
			
			
			Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing hgmd db from file: " + snvFile.getAbsolutePath());
			try {
				db.initializeMap(snvFile, indelFile);
			} catch (IOException e) {
				throw new OperationFailedException("Error reading HGMD db file at path " + hgmdSNVObj.toString() + " : " + e.getMessage(), this);
			}
			
		}
		
		HGMDInfo info_exact = db.getRecordRefAlt(var.getContig(), var.getStart(), var.getRef(), var.getAlt());
		if (info_exact != null) {
			var.addAnnotation(VariantRec.HGMD_HIT_EXACT, info_exact.condition + ", " + info_exact.assocType + " (" + info_exact.cDot + ",  " + info_exact.citation + ")");
		}

		HGMDInfo info = db.getRecord(var.getContig(), var.getStart());
		if (info != null) {
			String variant_class = info.assocType;
			var.addAnnotation(VariantRec.HGMD_CLASS, variant_class);
			var.addAnnotation(VariantRec.HGMD_HIT, info.condition + ", " + info.assocType + " (" + info.cDot + ",  " + info.citation + ")");
		}
		
	}

}
