package operator.variant;


import java.io.IOException;

import org.broad.tribble.readers.TabixReader;

import operator.OperationFailedException;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

/**
 * Provides information on the variants ability top impact splicing.
 * 
 * Uses a tabix-index file.
 * 
 * Tabix file is created from db [tab] obtained form [https://sites.google.com/site/jpopgen/dbNSFP] v2.9
 * the file [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbscSNV.zip] dbscSNv.zip unpacks 
 * to 24 files (chr 1 - 22, X, Y).  These files are cat together with the bash command below.
 * 
 * for i in {1..22} X Y; do tail -n +2 dbscSNV.chr$i >> dbscSNV_cat_1-22XY.tab ; done 
 * #tail to remove the header
 * 
 * columns
 * 1	chr     
 * 2	pos
 * 3	ref
 * 4	alt
 * 5	RefSeq?
 * 6	Ensembl?
 * 7	RefSeq_region
 * 8	RefSeq_gene
 * 9	RefSeq_functional_consequence
 * 10	RefSeq_id_c.change_p.change
 * 11	Ensembl_region  
 * 12	Ensembl_gene
 * 13	Ensembl_functional_consequence
 * 14	Ensembl_id_c.change_p.change
 * 15	ada_score
 * 16	rf_score
 * 
 * this cat'd file is tabix index using the following command
 * tabix -s 1 -b 2 -e 2 dbscSNV_cat_1-22XY.tab
 * 
 * the resulting files are:
 * 	dbscSNV_cat_1-22XY.tab.bgz  334M Feb 24
 * 	dbscSNV_cat_1-22XY.tab.bgz.tbi 688K Feb 24
 * 
 * dbscSNV includes all potential human SNVs within splicing consensus regions 
 *   (−3 to +8 at the 5’ splice site and −12 to +2 at the 3’ splice site), i.e. scSNVs, 
 *   related functional annotations and two ensemble prediction scores for        
 *   predicting their potential of altering splicing.
 *   
 * FROM 
 * X. Jian, E. Boerwinkle, and X. Liu.In silico prediction of splice-altering single 
 * nucleotide variants in the human genome. Nucl. Acids Res. 
 * (16 December 2014) 42 (22): 13534-13544. doi: 10.1093/nar/gku1206 
 * 
 * All variants were annotated using ANNOVAR, a software package that performs 
 *  functional annotation of genetic variants from high-throughput sequencing data
 *   and based on human reference sequence assembly GRCh37/hg19.
 *  
 * 
 * @author Keith Simmon
 * @date February 24th 2015
 *
 */


public class ScSncAnnotate extends AbstractTabixAnnotator{
	
	private boolean initialized = false;
	private TabixReader reader = null;
	
	
	public static final String dbScSNV_PATH = "dbScSNV.path"; 
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(dbScSNV_PATH);
	}
	
	protected void initializeReader(String filePath) {
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}

	

	@Override //TODO
	protected boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
		//TODO
		
		//should probably wrap in a try catch.
		Double ada_score = Double.parseDouble(toks[14]);
		Double rf_score = Double.parseDouble(toks[15]);
		
		var.addProperty(VariantRec.scSNV_ada, ada_score);
		var.addProperty(VariantRec.scSNV_rf, rf_score);
		//var.addProperty(VariantRec.scSNV_gene, toks[7]);
		
		//THIS IS WHAT IS ADDED TO VariantRec.java
		//public static final String scSNV_gene = "scSNV.RefSeq_gene";
		//public static final String scSNV_ada = "scSNV.ada_score";
		//public static final String scSNV_rf = "scSNV.rf_score";
		
		
	
		
		return true;
	}
	
	
	
	@Override
	public void annotateVariant(VariantRec varToAnnotate) throws OperationFailedException {
		if (! initialized) {
			throw new OperationFailedException("Failed to initialize", this);
		}
		
		if (reader == null) {
			throw new OperationFailedException("Tabix reader not initialized", this);
		}
		//System.out.println( "XXXXXXXXXXXRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");

		String contig = varToAnnotate.getContig();
		Integer pos = varToAnnotate.getStart();
		//System.out.println(contig + " " + pos);
		String queryStr = contig + ":" + (pos) + "-" + (pos); 
		
		try {
			//Perform the lookup
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();
					
					while(val != null) {
						String[] toks = val.split("\t"); //length of the array 16
						//System.out.println(toks.length);
						for (int i =0; i < toks.length; i++){
							//System.out.print(toks[i] + " ");
						}

						
						if (toks.length > 15) {

							
							//Convert the result (which is a line of a VCF file) into a variant rec
							// call the constructer and set variants
							VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1]), toks[2], toks[3]);
							//Important: Normalize the record so that it will match the 
							//variants in the variant pool that we want to annotate
							queryResultVar = VCFParser.normalizeVariant(queryResultVar);
							
							//Make sure the (normalized) variant we get from the tabix query matches the
							//variant we want to annotate
							
							if (queryResultVar.getContig().equals(varToAnnotate.getContig())
									&& queryResultVar.getStart() == varToAnnotate.getStart()
									&& queryResultVar.getRef().equals(varToAnnotate.getRef())
									&& queryResultVar.getAlt().equals(varToAnnotate.getAlt())) {
								//Everything looks good, so go ahead and annotate
								boolean ok = addAnnotationsFromString(varToAnnotate, val);
								
								if (ok)
									break;
							}
						}
						val = iter.next();
					}
				} catch (IOException e) {
					throw new OperationFailedException("Error reading data file: " + e.getMessage(), this);
				}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
	
	}
}
