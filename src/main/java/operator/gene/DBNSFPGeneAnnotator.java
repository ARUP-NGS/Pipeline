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
 */
public class DBNSFPGeneAnnotator extends AbstractGeneAnnotator {

	DBNSFPGene db;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		// TODO Auto-generated method stub
		if (db == null) {
			
			String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
			File dbFile = new File(pathToDBNSFP + "/dbNSFP2.0b4_gene");
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene looking to use file: " + dbFile.getAbsolutePath());
			try {
				db = DBNSFPGene.getDB(dbFile);
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
		String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
		if (pathToDBNSFP == null) {
			throw new IllegalArgumentException("No path to dbNSFP specified, cannot use dbNSFP gene annotator");
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
		File dbFile = new File(pathToDBNSFP + "/dbNSFP2.0b4_gene");
		if (! dbFile.exists()) {
			throw new IllegalArgumentException("DBNSFP file " + dbFile.getAbsolutePath() + " does not exist");
		}

	}
}
