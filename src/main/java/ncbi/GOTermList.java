package ncbi;

import java.util.ArrayList;
import java.util.List;

public class GOTermList {

	enum GoType {FUNCTION, PROCESS, COMPONENT}
	
	
	private List<GOTerm> terms;
	final GoType type;
	
	public GOTermList(GoType type) {
		this.type = type;
	}
	
	public void addTerm(int id, String desc) {
		if (terms == null)
			terms = new ArrayList<GOTerm>(8);
		
		GOTerm term = new GOTerm();
		term.goID = id;
		term.description = desc;
		terms.add( term );
	}
	
	/**
	 * The number of entries in this list of GOTerms
	 * @return
	 */
	public int size() {
		if (terms == null)
			return 0;
		else 
			return terms.size();
	}
	
	class GOTerm {
		int goID;
		String description;
	}
}
