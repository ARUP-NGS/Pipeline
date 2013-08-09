package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.gene.HGMDB.HGMDInfo;
import pipeline.Pipeline;

/**
 * An annotator that uses he HGMD db to find diseases associated with individual variants. We just
 * query the db to see if it contains a record for a given contig:position, and if so add an annotation
 * to the variant in question
 * @author brendan
 *
 */
public class HGMDAnnotator extends AbstractGeneAnnotator {
	
	public static final String HGMDB_PATH = "hgmd.path";
	
	HGMDB db = null;
	
	/**
	 * Add info from HGMD to the given variant under the annotation HGMD_INFO, if
	 * there is a record associated with the variant's position in the HGMD db. Otherwise,
	 * don't add anything. 
	 * @param rec
	 * @throws OperationFailedException 
	 */
	public void annotateGene(Gene gene) throws OperationFailedException {
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
		
		List<HGMDInfo> infoList = db.getRecordsForGene( gene.getName() );
		if (infoList != null) {
			String infoStr = "" + infoList.size() + " HGMD hits: ";
			for(HGMDInfo info : infoList) {
				infoStr = infoStr + " " + info.condition.replace("$$", " ") + "," + info.nm + "," + info.cDot;
				gene.addAnnotation(Gene.HGMD_INFO, infoStr);
			}
		}
	}

}
