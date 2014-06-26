package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.List;

import operator.OperationFailedException;

import org.apache.log4j.Logger;

import pipeline.Pipeline;
import disease.DiseaseInfo;
import disease.OMIMDB;
import disease.OMIMDB.OMIMEntry;

/**
 * This class adds OMIM - based information to each gene
 * @author brendan
 *
 */
public class OMIMAnnotator extends AbstractGeneAnnotator {

	public static final String OMIM_DIR = "omim.dir";
	OMIMDB omim = null;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (omim == null) {
			String pathToOMIM = this.getAttribute(OMIM_DIR);
			if (pathToOMIM == null) {
				pathToOMIM = this.getPipelineProperty(OMIM_DIR);
			}
			if (pathToOMIM == null) {
				throw new OperationFailedException("No path to OMIM specified, use omim.dir", this);
			}
			
			try {
				omim = new OMIMDB(new File(pathToOMIM));
			} catch (IOException e) {
				throw new OperationFailedException("Error opening omim directory", this);
			}
		}
		

		List<OMIMEntry> list = omim.getEntriesForGene(g);
		if (list != null) {
			int count = 0;
			for (OMIMEntry entry : list) {
				g.appendAnnotation(Gene.OMIM_DISEASES, entry.diseaseName);
				g.appendAnnotation(Gene.OMIM_NUMBERS, entry.diseaseID);
	
				DiseaseInfo disInf = omim.getDiseaseInfoForID(entry.diseaseID);
				
				//See if we can get a diseaseInfo object for the omim #			
				if (disInf != null) {
					g.addAnnotation(Gene.OMIM_INHERITANCE, disInf.getInheritance());
					List<String> phenotypes = disInf.getPhenotypes();
					String phenoStr = "";
					String sep = "";
					for(String pheno : phenotypes) {
						phenoStr = phenoStr + sep + pheno;
						sep = ", ";
					}
					if (phenoStr.length()>0)
						g.appendAnnotation(Gene.OMIM_PHENOTYPES, phenoStr);
					else {
						g.appendAnnotation(Gene.OMIM_PHENOTYPES, "(none)");
					}
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).warn("No disease info found for in DB for OMIM ID #" + entry.diseaseID);
				}
							
			}
		}
	}
	
	

}

