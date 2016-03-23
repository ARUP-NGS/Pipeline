package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for info from DBNSFP_GENE database
 * @author brendan
 *
 * Modified by Nix March 2016
 index	v3.1a
0	Gene_name
1	Ensembl_gene
2	chr
3	Gene_old_names
4	Gene_other_names
5	Uniprot_acc
6	Uniprot_id
7	Entrez_gene_id
8	CCDS_id
9	Refseq_id
10	ucsc_id
11	MIM_id
12	Gene_full_name
13	Pathway(Uniprot)
14	Pathway(BioCarta)_short
15	Pathway(BioCarta)_full
16	Pathway(ConsensusPathDB)
17	Pathway(KEGG)_id
18	Pathway(KEGG)_full
19	Function_description
20	Disease_description
21	MIM_phenotype_id
22	MIM_disease
23	Trait_association(GWAS)
24	GO_biological_process
25	GO_cellular_component
26	GO_molecular_function
27	Tissue_specificity(Uniprot)
28	Expression(egenetics)
29	Expression(GNF/Atlas)
	Lots more
	
index	v2.0b4
0	Gene_name
1	Ensembl_gene
2	chr
3	Gene_old_names
4	Gene_other_names
5	Uniprot_acc(HGNC/Uniprot)
6	Uniprot_id(HGNC/Uniprot)
7	Entrez_gene_id
8	CCDS_id
9	Refseq_id
10	ucsc_id
11	MIM_id
12	Gene_full_name
13	Pathway
14	Function_description
15	Disease_description
16	MIM_phenotype_id
17	MIM_disease
18	Trait_association(GWAS)
19	Expression(egenetics)
20	Expression(GNF/Atlas)
21	Interactions(IntAct)
22	Interactions(BioGRID)
23	P(HI)
24	P(rec)
25	Known_rec_info

 */
public class DBNSFPGene {
	
	private static DBNSFPGene db = null;
	private Map<String, GeneInfo> map = null;
	
	//defaulting it to 2.0 to keep bad behavior
	private String version = "2.0";

	public static DBNSFPGene getDB() {	
		return db;
	}
	
	public static DBNSFPGene getDB(File sourceFile) throws IOException {
		if (db == null) db = new DBNSFPGene(sourceFile);
		return db;
	}

	private DBNSFPGene(File sourceFile) throws IOException {
		readFile(sourceFile);
	}
	
	/**Constructor specifying version*/
	public DBNSFPGene(File sourceFile, String version) throws IOException{
		this.version = version;
		readFile(sourceFile);
	}
	
	/**
	 * Obtain a geneInfo object for the gene with the given name
	 * @param geneName
	 * @return
	 */
	public GeneInfo getInfoForGene(String geneName) {
		return map.get(geneName);
	}
	

	private void readFile(File sourceFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		
		map = new HashMap<String, GeneInfo>();
		
		String line = reader.readLine();
		line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			GeneInfo info = new GeneInfo();
			
			//version 3.1a
			if (version.equals("3.1a")) {
				info.geneName = toks[0];
				info.mimDisease = toks[21];
				info.diseaseDesc = toks[20];
				info.functionDesc = toks[19];
				info.expression = toks[28] + ";" + toks[29];
			}
			//version 2.0
			else if (version.equals("2.0")){
				info.geneName = toks[0];
				info.mimDisease = toks[16];
				info.diseaseDesc = toks[15];
				info.functionDesc = toks[14];
				info.expression = toks[19] + ";" + toks[20];
			}
			else throw new IOException ("Unsupported dbNSFPGene version "+version);
			map.put(info.geneName, info);
			line = reader.readLine();
		}
	
		System.err.println("Initialized dbNSFP2.0-gene database with " + map.size() + " elements");
		reader.close();
	}
	
	/**
	 * Return all gene names for which there's an entry
	 * @return
	 */
	public Collection<String> getGeneNames() {
		return map.keySet();
	}
	
	public class GeneInfo {
		public String geneName = null;
		public String mimDisease =  null; //Column 16
		public String diseaseDesc = null; //Column 15
		public String functionDesc = null; //Column 14
		public String expression = null; //Columns 19 and 20
	}
	
}
