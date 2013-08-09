package operator.annovar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import pipeline.PipelineXMLConstants;
import util.VCFLineParser;
import buffer.AnnovarInputFile;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.SimpleLineReader;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Base class of things that use Annovar to annotate variant pools
 * @author brendan
 *
 */
public abstract class AnnovarAnnotator extends Annotator {

	public static final String BUILD_VER = "buildver";
	protected String annovarPath = "~/annovar/";
	protected String buildVer = "hg19";
	public static final String FORMAT = "format";
	protected String format = "vcf4"; //Default assumed input format
	protected String annovarPrefix = "annovar.output";

	protected AnnovarInputFile annovarInputFile = null;
	
	public void annotateVariant(VariantRec rec) {
		//Blank on purpose, annovar annotators do something else
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (annovarInputFile == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("No annovar input file found, creating one on the fly for annotator: " + getObjectLabel());
			
			annovarInputFile = new AnnovarInputFile(new File(getProjectHome() + "/." + getObjectLabel() + ".annovar.input"));
			try {
				annovarInputFile.getFile().createNewFile();
				createAnnovarInput(variants, annovarInputFile);
			} catch (IOException e) {
				Logger.getLogger(Pipeline.primaryLoggerName).severe("Could not create annovar input file for for annotator : " + getObjectLabel());
				e.printStackTrace();
			}
			
		}
		
		annovarInputFile.getFile().deleteOnExit();
		super.performOperation();
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String ranPrefix = "" + (int)Math.round(100000 * Math.random()); //Attach a random number here so if we're doing simultaneous annotations annovar output files dont clobber each other
		annovarPrefix = annovarPrefix + "." + ranPrefix;
		
		//Find annovar input file
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof AnnovarInputFile) {
					annovarInputFile = (AnnovarInputFile)obj;
				}

			}
		}
		
		
		Object path = getPipelineProperty(PipelineXMLConstants.ANNOVAR_PATH);
		if (path != null)
			annovarPath = path.toString();
		
		//User can override path specified in properties
		String userBuildVer = properties.get(BUILD_VER);
		if (userBuildVer != null) {
			buildVer = userBuildVer;
		}
				
		//User can override path specified in properties
		String userPath = properties.get(PipelineXMLConstants.PATH);
		if (userPath != null) {
			annovarPath = userPath;
		}
		
		annovarPrefix = getProjectHome() + annovarPrefix;
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
		logger.info(getObjectLabel() + " has completed ");
	}
	
	/**
	 * There have been consistent issues with matching variants found in annovar output files to
	 * those in a variant pool. This is partly because annovar seems to be inconsistent in how it represents
	 * insertions and deletions. For instance, sometimes a variant in a vcf that looks like 1 1025  T  TA
	 * will be converted to an annovar input record of 1  1026 - A, and sometimes 1  1025 - A. Thus, to 
	 * find variants we now look at both 1026 and 1025 (in the variant pool), to see if there's an alt 
	 * allele at either one that matches the one from the file.  
	 * @param contig
	 * @param pos
	 * @param ref
	 * @param alt
	 * @return
	 */
	protected VariantRec findVariant(String contig, int pos, String ref, String alt) {

		contig = contig.replace("chr", "");
		VariantRec rec = variants.findRecord(contig, pos, alt);
		if (rec != null)
			return rec;


		int modPos = pos;
		if (alt.length() != ref.length()) {
			//Remove initial characters if they are equal and add one to start position
			if (alt.charAt(0) == ref.charAt(0)) {
				alt = alt.substring(1);
				if (alt.length()==0)
					alt = "-";
				ref = ref.substring(1);
				if (ref.length()==0)
					ref = "-";
			}
		}
		
		if (alt.equals("-") || ref.equals("-"))
			modPos++;
		
		
		if (modPos > pos)
			rec = variants.findRecord(contig, modPos, alt);
		if (rec == null) {
			return null;
		}
		
		if (alt.equals(rec.getAlt())) {
			return rec;
		}
		else {
			System.err.println("Variant found at modified position, but alt allele is still wrong");
			return null;
		}
	}
	
	public void createAnnovarInput(VariantPool vars, AnnovarInputFile destination) {
		try {
			int count = 0;
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination.getAbsolutePath()));
			for(String contig : vars.getContigs()) {
				for (VariantRec var : vars.getVariantsForContig(contig)) {
					writeVariantToAnnovarInput(var, writer);	
					count++;
				}
			}
			Logger.getLogger(Pipeline.primaryLoggerName).info("Wrote " + count + " variants to temporary annovar input file");
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).severe("Error converting csv to annovar input: " + e.getMessage());
			e.printStackTrace();

		}
	}
	
	public void createAnnovarInput(VCFFile inputFile, AnnovarInputFile destination) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination.getAbsolutePath()));
			VCFLineParser reader = new VCFLineParser(inputFile.getFile());
			do {
				VariantRec rec = reader.toVariantRec();
				writeVariantToAnnovarInput(rec, writer);

			} while(reader.advanceLine());
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).severe("Error converting csv to annovar input: " + e.getMessage());
			e.printStackTrace();

		}
	}
	
	public void createAnnovarInput(CSVFile inputFile, AnnovarInputFile destination) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination.getAbsolutePath()));
			//CSVLineReader csvReader = new CSVLineReader(input.getFile());
			CSVLineReader csvReader = new SimpleLineReader(inputFile.getFile());
			do {
				VariantRec rec = csvReader.toVariantRec();
				writeVariantToAnnovarInput(rec, writer);
				
			} while(csvReader.advanceLine());
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).severe("Error converting csv to annovar input: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void writeVariantToAnnovarInput(VariantRec rec, Writer writer) throws IOException {
		String het = "het";
		if (!rec.isHetero())
			het = "hom";
		writer.write(rec.getContig() + "\t" + 
					 rec.getStart() + "\t" + 
					 (rec.getStart() + rec.getRef().length()-1) + "\t" + 
					 rec.getRef() + "\t" +
					 //"- \t" +
					 rec.getAlt() + "\t" +
					 //"A \t" +
					 rec.getQuality() + "\t" + 
					 rec.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t" + 
					 het + "\t" +
					 rec.getProperty(VariantRec.GENOTYPE_QUALITY) + "\n");
		writer.flush();
	}
}
