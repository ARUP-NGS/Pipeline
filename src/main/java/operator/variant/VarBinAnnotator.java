package operator.variant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.VCFLineParser;
import buffer.BAMFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class VarBinAnnotator extends Annotator {

	public static final String VARBIN_PATH = "varbin.path";
	BAMFile bamFile = null;
	VCFFile vcfFile = null;
	String varbinScriptPath = null;
	
	private File varbinFinalTable = null; //Gets set after varbin execution in prepare()
	
	protected void prepare() throws OperationFailedException {
		if (varbinScriptPath == null) {
			throw new OperationFailedException("Varbin path not specified", this);
		}
		
		//We need a VCF to input, but we only want the variants present in the variantpool
		String destVars = "varbin.input." + ("" + System.currentTimeMillis()).substring(6) + ".vcf"; 
		try {
			makeFilteredVCF(vcfFile, variants, destVars);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String command = "bash " + varbinScriptPath + " -v " + destVars + " -b " + bamFile.getAbsolutePath() + " -a " + "varbin" + " -p varbin";
		
		//We execute varbin from here and block until it completes...
		executeCommand(command);
		
		//Now read in table and annotate variants

		File finalTable = new File("varbin.final.table");
		try {
			annotateVarsFromTable(finalTable);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void annotateVarsFromTable(File tableFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(tableFile));
		String line = reader.readLine();
		line = reader.readLine(); //Skip first line
		while(line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length < 4) {
				line = reader.readLine();
				continue;
			}
			String contig = toks[0];
			Integer pos = Integer.parseInt(toks[1]);
			String binStr = toks[3];
			//System.out.println("Adding bin #" + binStr + " to variant : " + contig + ":" + pos);
			try {
				Integer bin = Integer.parseInt(binStr);
				VariantRec var = variants.findRecord(contig, pos);
				if (var != null) {
					var.addProperty(VariantRec.VARBIN_BIN, new Double(bin));
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not find variant to associate with varbin annotation at position " + contig + ":" + pos);
				}
			//	System.out.println("Adding bin #" + bin + " to variant : " + var.toSimpleString());
			}
			catch (NumberFormatException nfe) {
				
			}
			line = reader.readLine();
		}
		
		reader.close();
	}
	
	/**
	 * Create a new VCF file that contains all variants in the sourceVCF that are present in the vars variant pool
	 * @param sourceVCF
	 * @param vars
	 * @param outputFilename
	 * @throws IOException
	 */
	private static void makeFilteredVCF(VCFFile sourceVCF, VariantPool vars, String outputFilename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
		VCFLineParser reader = new VCFLineParser(sourceVCF);
		
		//Emit header of source VCF
		writer.write(reader.getHeader());
		
		do {
			VariantRec var = reader.toVariantRec();
			if (vars.contains(var.getContig(), var.getStart())) {
				writer.write(reader.getCurrentLine() + "\n");
			}
		} while(reader.advanceLine());
		
		
		writer.close();
	}
	
	
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		//OK, we actually don't do anything in here.... all annotations take place in 'prepare'
	}
	
	protected void executeCommand(String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		Process p;
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info(getObjectLabel() + " executing command : " + command);
		try {
			p = r.exec(command);

			try {
				if (p.waitFor() != 0) {
					logger.info("Task with command " + command + " for object " + getObjectLabel() + " exited with nonzero status");
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//BAM file is a required arg
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof BAMFile) {
					bamFile = (BAMFile)obj;
				}
				
				if (obj instanceof VCFFile) {
					vcfFile = (VCFFile)obj;
				}

			}
		}
		
		//Check to see if the required attributes have been specified
		varbinScriptPath = this.getAttribute(VARBIN_PATH);
		if (varbinScriptPath == null) {
			varbinScriptPath = this.getPipelineProperty(VARBIN_PATH);
			if (varbinScriptPath == null) {
				throw new IllegalArgumentException("No path to varbin script specified");
			}
		}
		
		//Test to see if varbin script is really there
		File varbinFile = new File(varbinScriptPath);
		if (! varbinFile.exists()) {
			throw new IllegalArgumentException("No file found on varbin script path: " + varbinScriptPath);
		}
	}

	public static void main(String[] args) throws IOException {
		VarBinAnnotator vb = new VarBinAnnotator();
		
		vb.annotateVarsFromTable(new File("varbin.final.table"));
	}
}
