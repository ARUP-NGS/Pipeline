package operator.gene;

import gene.Gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.TextBuffer;

/**
 * Computes the "go.score" annotation for genes
 * @author brendan
 *
 */
public class GOTermRanker extends AbstractGeneAnnotator {

	TextBuffer termsFile = null;
	Map<String, Integer> rankingMap = null;
	

	@Override
	public void annotateGene(Gene g) {
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		double goScore = computeScore(g);
		g.addProperty(Gene.GO_SCORE, goScore);
	}
	
	private double computeScore(Gene g) {

		String goComponent = g.getAnnotation(Gene.GO_COMPONENT);
		if (goComponent != null) 
			goComponent = goComponent.toLowerCase();
		String goProcess = g.getAnnotation(Gene.GO_PROCESS);
		if (goProcess != null)
			goProcess = goProcess.toLowerCase();
		
		String goFunction = g.getAnnotation(Gene.GO_FUNCTION);
		if (goFunction != null)
			goFunction = goFunction.toLowerCase();
		
		double score = 0;
		
		StringBuilder hitStr = new StringBuilder();
		for(String term : rankingMap.keySet()) {
			if (goComponent != null && goComponent.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
			
			if (goProcess != null && goProcess.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
			
			if (goFunction != null && goFunction.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
		}
		String hits = hitStr.toString();
		if (hits.length()>2)
			hits = hits.substring(0, hits.length()-2);
		g.addAnnotation(Gene.GO_HITS, hits);
		return score;
	}

	private void buildRankingMap() throws IOException {
		rankingMap = new HashMap<String, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(termsFile.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length < 2) {
				System.err.println("Warning : could not parse GO term from this line: " + line);
			}
			else {
				Integer score = Integer.parseInt(toks[1].trim());
				rankingMap.put(toks[0].trim().toLowerCase(), score);
			}
			line = reader.readLine();
		}
	}

	public void initialize(NodeList children) {
		super.initialize(children);

		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof TextBuffer) {
					termsFile = (TextBuffer)obj;
				}
			}
		}
	}

	public void setRankingMap(Map<String, Integer> searchTerms) {
		this.rankingMap = searchTerms;
	}
	



}
