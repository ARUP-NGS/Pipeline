package ncbi;

/**
 * This class essentially just stores information about a gene as obtained from NCBI, and potentially
 * other sources. 
 * 
 * @author brendan
 *
 */
public class GeneRecord {

	private String symbol; //NCBI official symbol, eg. SMAD4
	private String geneID; //NCBI gene ID, eg 4089
	private String description;
	private String summary;
	
	GOTermList goFunctions = new GOTermList(GOTermList.GoType.FUNCTION);
	GOTermList goProcesses = new GOTermList(GOTermList.GoType.PROCESS);
	GOTermList goComponents = new GOTermList(GOTermList.GoType.COMPONENT);
	
	//Others: interactors, synonyms, GO terms, and RIFs ....
	
	public GeneRecord() {
		symbol = "?";
		geneID = "?";
		description = "?";
		summary = "?";
	}
	
	public GeneRecord(String symbol) {
		this();
		this.symbol = symbol;
	}
	
	/**
	 * Obtain the official NCBI symbol for this gene, e.g. SMAD4, or ENG
	 * @return
	 */
	public String getSymbol() {
		return symbol;
	}
	
	/**
	 * Obtain NCBI gene ID for this gene
	 * @return
	 */
	public String getGeneID() {
		return geneID;
	}
	
	/**
	 * Obtain the brief NCBI 'description' for this gene
	 * @return
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Get longer NCBI summary for this gene
	 * @return
	 */
	public String getSummary() {
		return summary;
	}
	
	/**
	 * Set the NCBI id of this gene. 
	 * @param id
	 */
	public void setID(String id) {
		this.geneID = id;
	}
	
	public void setDescription(String desc) {
		this.description = desc;
	}
	
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	public String toString() {
		return symbol + " (" + geneID + ") : " + description;
	}
}
