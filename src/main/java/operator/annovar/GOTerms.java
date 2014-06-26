package operator.annovar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.NodeList;

import operator.OperationFailedException;
import operator.Operator;
import pipeline.ObjectHandler;
import pipeline.Pipeline;

/**
 * This class reads a bunch of gene ontology data from a file and stores it in memory
 * so that it can be quickly accessed, mostly for annotating variant pools. 
 * @author brendan
 *
 */
public class GOTerms extends Operator {

	public static final String GO_DIR = "goinfo.dir";
	File baseDir = null;
	
	//Store all known terms for each group
	Set<String> allProcesses = new HashSet<String>();
	Set<String> allFunctions = new HashSet<String>();
	Set<String> allComponents = new HashSet<String>();
	
	private Map<String, GeneInfo> goMap = new HashMap<String, GeneInfo>();

	public GOTerms(ObjectHandler handler) {
		setObjectHandler(handler);
		initialize(null);
	}

	@Override
	public void initialize(NodeList children) {
		String goDir = (String) getPipelineProperty(GO_DIR);
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Attempting to read GO terms from : " + goDir);

		if (goDir == null || goDir.length()==0) {
			throw new IllegalArgumentException("G.O. info base directory not specified (use goinfo.dir in pipelineprops file)");	
		}
		
		baseDir = new File(goDir);
		if (!baseDir.exists()) {
			throw new IllegalArgumentException("G.O. info base directory " + baseDir.getAbsolutePath() + " does not exist");			
		}
		if (!baseDir.isDirectory()) {
			throw new IllegalArgumentException("G.O. base directory " + baseDir.getAbsolutePath() + " is not a directory");
		}
		
		try {
			readData();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error reading GO terms from " + baseDir.getAbsolutePath() + ": " + e.getMessage());

		}
	}
	
	public List<String> getFunctionsForGene(String geneName) {
		GeneInfo info = goMap.get(geneName.toUpperCase());
		if (info == null)
			return new ArrayList<String>();
		else {
			List<String> function = info.functions;
			if (function == null)
				return new ArrayList<String>();
			return function;
		}
	}
	
	public List<String> getProcessesForGene(String geneName) {
		GeneInfo info = goMap.get(geneName.toUpperCase());
		if (info == null)
			return new ArrayList<String>();
		else {
			List<String> procs = info.processes;
			if (procs == null)
				return new ArrayList<String>();
			return procs;
		}
	}
	
	public List<String> getComponentsForGene(String geneName) {
		GeneInfo info = goMap.get(geneName.toUpperCase());
		if (info == null)
			return new ArrayList<String>();
		else {
			List<String> comps = info.components;
			if (comps == null)
				return new ArrayList<String>();
			return comps;
		}
	}
	
	/**
	 * Return set of all processes found in input files. 
	 * @return
	 */
	public Set<String> getAllProcesses() {
		return allProcesses;
	}
	
	public Set<String> getAllFunctions() {
		return allFunctions;
	}
	
	public Set<String> getAllComponents() {
		return allComponents;
	}

	private void readData() throws IOException {
		File homosapiensFile = new File(baseDir.getAbsolutePath() + "/Homo_sapiens.gene_info");
		
		//We first use a map keyed by geneID, which is common for GO stuff but not 
		//used elsewhere. When we're done we dump into another map that is keyed by
		//gene name
		Map<Integer, GeneInfo> tmpMap = new HashMap<Integer, GeneInfo>();
		
		BufferedReader reader = new BufferedReader(new FileReader(homosapiensFile));
		String line = reader.readLine();
		while(line != null && line.length()>0) {
			String[] toks = line.split("\t");
			String geneName = toks[2];
			Integer geneID = Integer.parseInt(toks[1]);
			GeneInfo info = new GeneInfo();
			info.geneID = geneID;
			info.geneName = geneName;
			tmpMap.put(geneID, info);
			line = reader.readLine();
		}
		
		
		//Now read the gene2go file and put function, process, etc. info into lists...
		File goFile = new File(baseDir.getAbsolutePath() + "/gene2go_human");
		reader = new BufferedReader(new FileReader(goFile));
		line = reader.readLine();
		while(line != null && line.length()>0) {
			String[] toks = line.split("\t");
			if (toks.length==1) {
				System.out.println("Ignoring line: " + line);
				line = reader.readLine();
				continue;
			}
			Integer id = Integer.parseInt(toks[1]);
			String goID = toks[2];
			String term = toks[5];
			String category = toks[7];
			
			GeneInfo info = tmpMap.get(id);
			
			if (info == null) {
				System.err.println("Couldn't find info object with id : " + id);
			} 
			else {
				//Put object in new map, OK to clobber existing values
				goMap.put(info.geneName, info);
				
				if (category.equals("Function")) {
					if (info.functions == null)
						info.functions = new ArrayList<String>(4);
					info.functions.add(term );
					allFunctions.add(term);
				}
				
				if (category.equals("Process")) {
					if (info.processes == null)
						info.processes = new ArrayList<String>(4);
					info.processes.add(term );
					allProcesses.add(term);
				}
				
				if (category.equals("Component")) {
					if (info.components == null)
						info.components = new ArrayList<String>(4);
					info.components.add(term );
					allComponents.add(term);
				}
			}
			
			line = reader.readLine();
		}

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Read in GO terms for : " + goMap.size() + " genes");
	}


	@Override
	public void performOperation() throws OperationFailedException {
		//Nothing to do, we do everything when we're initialized
	}
	
	class GeneInfo {
		int geneID;
		String geneName;
		List<String> functions;
		List<String> processes;
		List<String> components;
	}



}
