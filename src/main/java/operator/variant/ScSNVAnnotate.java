package operator.variant;


import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

/**
 * Provides information on the variants ability to impact splicing.
 * 
 * Uses a tabix-index file.
 * 
 * Tabix file is created from a db [tab delimited] obtained form [https://sites.google.com/site/jpopgen/dbNSFP] v2.9
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
 * 15	ada_score ****VALUE ADDED TO VARIANT RECORD
 * 16	rf_score  ****VALUE ADDED TO VARIANT RECORD
 * 
 * this cat'd file is tabix index using the following commands
 * bgzip -c dbscSNV_cat_1-22XY.tab >> dbscSNV_cat_1-22XY.tab.bgz
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
 * TESTING
 * A truncated database was utilized; see TestScSNV for details.
 * 
 * @author Keith Simmon
 * @date February 24th 2015
 *
 */

public class ScSNVAnnotate extends AbstractTabixAnnotator {
	
	public static final String dbScSNV_PATH = "dbScSNV.path"; 
	
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(dbScSNV_PATH);
	}

	@Override 
	protected boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) {
		String[] toks = val.split("\t");
		String _ada = toks[14];
		String _rf = toks[15];

		try {		
			var.addProperty(VariantRec.scSNV_ada, Double.parseDouble(_ada));
		}
		catch (NumberFormatException ex){ 			
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}
		try {
			var.addProperty(VariantRec.scSNV_rf, Double.parseDouble(_rf));
		}
		 catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", rf:"+_rf);
		}
		return true;
	}
	
	/**
	 * Parses variants from the given VCF line (appropriately handling multiple alts) and compare each variant tot he
	 * 'varToAnnotate'. If a perfect match (including both chr, pos, ref, and alt) 
	 * @param varToAnnotate
	 * @param vcfLine
	 * @return
	 */
	protected int findMatchingVariant(VariantRec varToAnnotate, String vcfLine) {
		String[] toks = vcfLine.split("\t");
		String[] alts = toks[3].split(",");
		for(int i=0; i<alts.length; i++) {
			VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1])+toks[1].length(), toks[2], alts[i]);
			queryResultVar = VCFParser.normalizeVariant(queryResultVar);

			if (queryResultVar.getContig().equals(varToAnnotate.getContig())
					&& queryResultVar.getStart() == varToAnnotate.getStart()
					&& queryResultVar.getRef().equals(varToAnnotate.getRef())
					&& queryResultVar.getAlt().equals(varToAnnotate.getAllAlts()[i])) { //change to loop through all alts

				//Everything looks good, so go ahead and annotate		
				boolean ok = addAnnotationsFromString(varToAnnotate, vcfLine, i);
				if (ok) {
					return i;
				}
			} //if perfect variant match

		}//Loop over alts	
		return -1;
	}
}
