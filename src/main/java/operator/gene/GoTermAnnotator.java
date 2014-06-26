package operator.gene;

import gene.Gene;

import java.util.List;

import operator.OperationFailedException;
import operator.annovar.GOTerms;

/**
 * Adds GO-term annotations to gene
 * @author brendan
 *
 */
public class GoTermAnnotator extends AbstractGeneAnnotator {

	GOTerms goTerms = null;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (goTerms == null)
			goTerms = new GOTerms(getObjectHandler());
		
		String gene = g.getName();
		if (gene == null) {
			return;
		}
		List<String> functions = goTerms.getFunctionsForGene(gene);
		String funcStr = combineStrings(functions);
		g.addAnnotation(Gene.GO_FUNCTION, funcStr);
		
		List<String> procs = goTerms.getProcessesForGene(gene);
		String procsStr = combineStrings(procs);
		g.addAnnotation(Gene.GO_PROCESS, procsStr);
		
		List<String> comps = goTerms.getComponentsForGene(gene);
		String compsStr = combineStrings(comps);
		g.addAnnotation(Gene.GO_COMPONENT, compsStr);
	}

	private static String combineStrings(List<String> strs) {
		if (strs.size() == 0) {
			return "-";
		}
		else {
			StringBuilder strB = new StringBuilder();
			for(int i=0; i<strs.size()-1; i++)
				strB.append(strs.get(i) + ", ");
			strB.append(strs.get(strs.size()-1));
			return strB.toString();
		}
	}

}
