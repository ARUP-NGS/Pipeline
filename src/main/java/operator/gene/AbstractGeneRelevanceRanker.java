package operator.gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.TextBuffer;

/**
 * Base class of objects that annotate Genes with a gene relevance sub-score. Right now
 * these relevance subscores are implemented as plain-old Gene properties, but we may
 * wish to be fancier about them in the future.
 * 
 * @author brendan
 *
 */
public abstract class AbstractGeneRelevanceRanker extends AbstractGeneAnnotator {
	
	protected Map<String, Integer> rankingMap = null;
	protected TextBuffer termsFile = null; //Text file keeping ranking terms and their scores 
	protected int examined = 0; //Number of gene so far examined
	protected int scored = 0; //Number of genes with nonzero relevance scores
	
	public abstract String getScoreKey();
	
	
	protected void buildRankingMap() throws IOException {
		rankingMap = new HashMap<String, Integer>();
		
		BufferedReader reader = new BufferedReader(new FileReader(termsFile.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length<2) {
				System.out.println("Cannot parse summary ranking term for this line: " + line);
				line = reader.readLine();
				continue;
			}
			Integer score = Integer.parseInt(toks[1].trim());
			String key = toks[0].trim().toLowerCase();
			rankingMap.put(key, score);
			line = reader.readLine();
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("Read " + rankingMap.size() + " terms in from search terms in file: " + termsFile.getAbsolutePath());
		reader.close();
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
		
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to GeneSummaryRanker");
		
		if (termsFile == null)
			throw new IllegalArgumentException("No term file provided to GeneSummaryRanker");
		
	}

	public Map<String, Integer> getRankingMap() {
		return rankingMap;
	}
	
	public void setRankingMap(Map<String, Integer> newMap) {
		this.rankingMap = newMap;
	}
}
