package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ncbi.CachedGeneSummaryDB;
import operator.gene.HGMDB;
import operator.gene.HGMDB.HGMDInfo;
import disease.DiseaseInfo;
import disease.OMIMDB;
import disease.OMIMDB.OMIMEntry;

/**
 * Utility class to add additional info to the dbNSFP gene file, especially gene summaries
 * @author brendan
 *
 */
public class DBNSFPAugmenter {
	
	public void readDBNSFPGene(File sourceFile) throws IOException {
	
	}
	
	public static void main(String[] args) throws IOException {
			
		BufferedWriter writer = new BufferedWriter(new FileWriter("geneInfo.test.csv"));
		
		CachedGeneSummaryDB summaryDB = new CachedGeneSummaryDB();
		HGMDB hgmd = new HGMDB();
		hgmd.initializeMap(new File("/home/brendan/resources/hgmd_2012.1.csv"));
		OMIMDB omim = new OMIMDB(new File("/home/brendan/resources/OMIM/OMIM-Nov14-2012/"));
		
		BufferedReader reader = new BufferedReader(new FileReader("/home/brendan/resources/dbNSFP2.0b4/dbNSFP2.0b4_gene"));
		String line = reader.readLine();
		line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			String geneName = toks[0];
			
			String summary = summaryDB.getSummaryForGene(geneName);
			if (summary == null) 
				summary = ".";
			writer.write(line + "\t" + summary + "\t");
			
			List<HGMDInfo> hgmdHits = hgmd.getRecordsForGene(geneName);
			if (hgmdHits != null) {
				StringBuilder strB = new StringBuilder();
				for(HGMDInfo info : hgmdHits) {
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
					strB.append(info.condition+ "," + info.cDot + "," + assocType + ";");
				}
				writer.write(strB.toString() + "\t");
			}
			else {
				//no hgmd hits, write a "."
				writer.write(".\t");
			}
			
			List<OMIMEntry> entries = omim.getEntriesForGene(geneName);
			StringBuilder phenoStr = new StringBuilder();
			StringBuilder inheritanceStr = new StringBuilder();
			if (entries != null && entries.size()>0) {
				for(OMIMEntry entry : entries) {
					DiseaseInfo disInf = omim.getDiseaseInfoForID(entry.diseaseID);
					if (disInf != null) {
						inheritanceStr.append(disInf.getInheritance() + ",");
						for(String pheno : disInf.getPhenotypes())
							phenoStr.append(pheno + ",");
					}
				}
				
			}
			
			if (phenoStr.length()==0)
				phenoStr.append(".");
			if (inheritanceStr.length()==0) {
				inheritanceStr.append(".");
			}
			writer.write(phenoStr + "\t" + inheritanceStr + "\n");
			System.out.println("Processing gene " + geneName);
			line = reader.readLine();
		}
	
		
		reader.close();		
		writer.close();
		System.out.println("done");
		
	}

}
