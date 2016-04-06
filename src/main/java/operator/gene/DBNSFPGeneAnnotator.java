package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.variant.DBNSFPAnnotator;
import operator.variant.DBNSFPGene;
import operator.variant.DBNSFPGene.GeneInfo;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;

/**
 * Provides a handful of  gene annotations obtained from the dbNSFP database, including
 * disease description, OMIM numbers, functional description, and tissue expression info
 * @author brendan
 *
 * Mods my Nix to support 3.1a, March 2016
 */
public class DBNSFPGeneAnnotator extends AbstractGeneAnnotator {

	DBNSFPGene db;
	private File dbFile;
	public static final String DBNSFPGENE_PATH = "dbnsfp.gene.path";
	private String dbsnfpVersion = null; 
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		// TODO Auto-generated method stub
		if (db == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene looking to use file: " + dbFile.getAbsolutePath());
			try {
				//support for 2.0, or 3.1a
				if (dbsnfpVersion == null) db = DBNSFPGene.getDB(dbFile);
				else db = new DBNSFPGene( dbFile, dbsnfpVersion);
				
			} catch (IOException e) {
				throw new OperationFailedException("Could not initialize dbNSFP gene file " + dbFile.getAbsolutePath() + " : " + e.getMessage(), this);
			}
		}
		
		GeneInfo info = db.getInfoForGene( g.getName() );
		if (info == null)
			return;
		
		g.addAnnotation(Gene.DBNSFP_DISEASEDESC, info.diseaseDesc);
		g.addAnnotation(Gene.DBNSFP_FUNCTIONDESC, info.functionDesc);
		g.addAnnotation(Gene.DBNSFP_MIMDISEASE, info.mimDisease);
		g.addAnnotation(Gene.EXPRESSION, info.expression);
	}

	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathToDBNSFPGene = this.getPipelineProperty(DBNSFPGENE_PATH);
		if (pathToDBNSFPGene == null) throw new IllegalArgumentException("No path to dbNSFP specified, cannot use dbNSFP gene annotator");
		Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFPGene);
		
		
		//look for version, might be null
		dbsnfpVersion = getAttribute(DBNSFPAnnotator.DBNSFP_VERSION);
	    if (dbsnfpVersion == null) dbsnfpVersion= getPipelineProperty (DBNSFPAnnotator.DBNSFP_VERSION);
		
	    //set file
	    if (dbsnfpVersion != null && dbsnfpVersion.equals("3.1a")) dbFile = new File(pathToDBNSFPGene + "/dbNSFP3.1_gene");
	    else dbFile = new File(pathToDBNSFPGene + "/dbNSFP2.0b4_gene");
	    
		if (! dbFile.exists()) throw new IllegalArgumentException("DBNSFP file " + dbFile.getAbsolutePath() + " does not exist");

	}
}
