package operator;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import json.AnnotatedVarsJsonConverter;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import operator.qc.QCReport;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.InstanceLogFile;
import buffer.MultiFileBuffer;
import buffer.TextBuffer;
import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

/**
 * Create directories and copy files to the directory where GenomicsReviewApp can see them
 * @author brendan
 *
 */
public class ReviewDirGenerator extends Operator {

	public static final String DEST_DIR = "destination.dir";
	
	String sampleName = "unknown";
	String submitter = "unknown";
	String analysisType = "unknown";
	String rootPath = null;
	VCFFile variantFile = null;
	BAMFile finalBAM = null;
	CSVFile annotatedVariants = null;
	InstanceLogFile logFile = null;
	MultiFileBuffer fastqs1 = null;
	MultiFileBuffer fastqs2 = null;
	QCReport qcReport = null;
	TextBuffer qcJsonFile = null;
	BEDFile capture = null;
	private boolean createJSONVariants = true; //If true, create a compressed json variants file
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger.getLogger(Pipeline.primaryLoggerName).info("Creating GenomicsReview directory in " + rootPath);

		//Create directory structure
		createDir("", rootPath);
		createDir(rootPath, "bam");
		createDir(rootPath, "var");
		createDir(rootPath, "log");
		createDir(rootPath, "depth");
		createDir(rootPath, "qc");
		createDir(rootPath, "report");
		createDir(rootPath, "fastq");
		createDir(rootPath, "array");
		createDir(rootPath, "bed");
		

		Map<String, String> manifest = new HashMap<String, String>();
		
		//This should happen before files get moved around
		createSampleManifest("sampleManifest.txt");
		
		if (qcReport != null) {
			File dest = new File(rootPath +"/qc/");
			moveFile(qcReport.getOutputDir(), dest);
			qcReport.setOutputDir(new File(dest.getAbsolutePath()));
		}
		
		if (qcJsonFile != null) {
			File dest = new File(rootPath +"/qc/");
			moveFile(qcJsonFile, dest);
		}
		
		if (annotatedVariants != null) {
			if (createJSONVariants) {
				try {
					createJSONVariants(annotatedVariants, new File(rootPath + "/var/") );
				} catch (JSONException e) {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Error creating annotated vars json: " + e.getLocalizedMessage());
				} catch (IOException e) {
					Logger.getLogger(Pipeline.primaryLoggerName).warning("Error creating annotated vars json: " + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			
			moveFile(annotatedVariants, new File(rootPath + "/var/"));
		}
		
		try {
			File varDestination = new File(rootPath + "/var/" + variantFile.getFilename());
			copyTextFile(variantFile.getFile(), varDestination);

			File inputDestination = new File(rootPath + "/log/pipeline_input.xml");
			copyTextFile(this.getPipelineOwner().getSourceFile(), inputDestination);
			
			if (capture != null) {
				File bedDestination = new File(rootPath + "/bed/" + capture.getFilename());
				copyTextFile(capture.getFile(), bedDestination);
			}
			
			File logDestination = new File(rootPath + "/log/" + logFile.getFilename());
			copyTextFile(logFile.getFile(), logDestination);

			File newBAMLocation = new File(rootPath + "/bam/");
			String indexPath = finalBAM.getAbsolutePath() + ".bai";
			File indexFile = new File(indexPath);
			if (indexFile.exists()) {
				moveFile(indexFile, newBAMLocation);
			}
			moveFile(finalBAM, newBAMLocation);
			
			
			File newFQLocation = new File(rootPath + "/fastq/");
			if (fastqs1 != null) {
				moveFiles(fastqs1, newFQLocation);
			}
			
			if (fastqs2 != null) {
				moveFiles(fastqs2, newFQLocation);
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Error moving files to review dir destination : " + e.getMessage(), this);
		}
		
	}
	

	private void createJSONVariants(CSVFile inputVars, File destDir) throws JSONException, IOException {
		JSONObject jsonResponse = new JSONObject();
		String destFilename = inputVars.getFilename().replace(".csv", ".json.gz");
		File dest = new File(destDir.getAbsolutePath() + "/" + destFilename);
		
		VariantLineReader varReader = new CSVLineReader(inputVars.getFile());
		AnnotatedVarsJsonConverter converter = new AnnotatedVarsJsonConverter();

		JSONArray jsonVarList = new JSONArray();
		
		//Trim, etc
		List<String> map = new ArrayList<String>();
		String[] headerToks =  varReader.getHeader().trim().replace("#",  "").split("\t");
		for(int i=0; i<headerToks.length; i++) {
			map.add(headerToks[i].trim().replace(" ", ""));
		}
		converter.setKeys(map);
		
		//Danger: could create huge json object if variant list is big
		VariantRec var = varReader.toVariantRec();
		while(var != null) {
			jsonVarList.put( converter.toJSON(var) );
			varReader.advanceLine();
			var = varReader.toVariantRec();
		}
		
		jsonResponse.put("variant.list", jsonVarList);

		//Get the json string, then compress it to a byte array
		String str = jsonResponse.toString();			
		byte[] bytes = compressGZIP(str);

		if (dest.exists()) {
			throw new IOException("Destination file already exists");
		}

		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();

	}


	/**
	 * GZIP compress the given string to a byte array
	 * @param str
	 * @return
	 */
	private static byte[] compressGZIP(String str){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try{
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gzipOutputStream.write(str.getBytes("UTF-8"));
			gzipOutputStream.close();
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return byteArrayOutputStream.toByteArray();
	}

	
	
	private void createSampleManifest(String filename) {
		File manifestFile = new File(rootPath + "/" +filename);
		try {
			manifestFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFile));
			
			writer.write("sample.name=" + sampleName + "\n");
			writer.write("pipeline.version=" +  Pipeline.PIPELINE_VERSION + "\n");
			writer.write("submitter=" +  submitter + "\n");
			writer.write("analysis.type=" +  analysisType + "\n");
			if (annotatedVariants != null) {
				writer.write("annotated.vars=var/" + annotatedVariants.getFilename() + "\n");
				if (createJSONVariants) {
					writer.write("json.vars=var/" + annotatedVariants.getFilename().replace(".csv", ".json.gz") + "\n");
				}
			}
			if (variantFile != null) {
				writer.write("vcf.file=var/" + variantFile.getFilename() + "\n");
				//WARNING: Bad code here. This should be updated to make sure we're getting the correct
				//link location from the status finalizer, which actually creates this link
				writer.write("vcf.link=results/" + variantFile.getFilename() + "\n");
			}
			if (finalBAM != null) {
				writer.write("bam.file=bam/" + finalBAM.getFilename() + "\n");
				//WARNING: Bad code here. This should be updated to make sure we're getting the correct
				//link location from the status finalizer, which actually creates this link
				writer.write("bam.link=results/" + finalBAM.getFilename() + "\n");
			}
			if (qcReport != null) {
				writer.write("qc.dir=" +  qcReport.getOutputDir() + "\n");
				//WARNING: Bad code here. This should be updated to make sure we're getting the correct
				//link location from the qc writer and/or status finalizer that creates the link. Changes
				//in those classes will not be automatically reflected here
				writer.write("qc.link=" +  "results/" + sampleName + "-QC/qc-report/qc-metrics.html" + "\n");
			}
			
			if (qcJsonFile != null) {
				writer.write("qc.json=qc/" + qcJsonFile.getFilename() + "\n");
			}
			writer.write("analysis.start.time=" +  this.getPipelineOwner().getStartTime().getTime() + "\n");
			writer.write("current.time=" +  (new Date()).getTime() + "\n");
			if (capture != null) {
				writer.write("capture=" +  capture.getAbsolutePath() + "\n");
			}
			writer.write("working.dir=" +  this.getProjectHome() + "\n");
			if (fastqs1 != null) {
				for(FileBuffer fqBuf : fastqs1.getFileList()) {
					writer.write("fastq.1.src=" +  fqBuf.getAbsolutePath() + "\n");
				}
			}
			if (fastqs2 != null) {
				for(FileBuffer fqBuf : fastqs2.getFileList()) {
					writer.write("fastq.2.src=" +  fqBuf.getAbsolutePath() + "\n");
				}
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	public String getRootPath() {
		return rootPath;
	}

	/**
	 * Copy the given file to the new destination - this will only work with text files
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	private void copyTextFile(File source, File dest) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source));
		if (! dest.exists()) {
			dest.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String line = reader.readLine();
		while(line != null) {
			writer.write( line + "\n");
			line =reader.readLine();
		}
		
		writer.close();
		reader.close();
	}
	
	/**
	 * Move all files in the multi-file-buffer to the new parent dir
	 * @param sourceFiles
	 * @param newParentDir
	 */
	private void moveFiles(MultiFileBuffer sourceFiles, File newParentDir) {
		for(int i=0; i<sourceFiles.getFileCount(); i++) {
			moveFile(sourceFiles.getFile(i), newParentDir);
		}
	}
	
	/**
	 * Move the given file to the new destination directory, preserving the short file name
	 * @param sourceFile
	 * @param newParentDir
	 */
	private void moveFile(FileBuffer sourceFile, File newParentDir) {
		File destinationFile = new File(newParentDir + "/" + sourceFile.getFilename());
		Logger.getLogger(Pipeline.primaryLoggerName).info("Renaming " + sourceFile.getFile().getAbsolutePath() + " to " + destinationFile.getAbsolutePath());
		sourceFile.getFile().renameTo(destinationFile);
		sourceFile.setFile(destinationFile);
	}
	
	/**
	 * Move the given file to the new destination directory, preserving the short file name
	 * @param sourceFile
	 * @param newParentDir
	 */
	private void moveFile(File sourceFile, File newParentDir) {
		File destinationFile = new File(newParentDir + "/" + sourceFile.getName());
		Logger.getLogger(Pipeline.primaryLoggerName).info("Renaming " + sourceFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath());
		sourceFile.renameTo(destinationFile);
	}
	
	private boolean createDir(String parent, String dirName) {
		File dir = new File(parent + "/" + dirName);
		boolean ok = dir.mkdirs();
		return ok;
	}
	
	
	
	@Override
	public void initialize(NodeList children) {
		
		rootPath = properties.get(DEST_DIR);
		if (rootPath == null) {
			throw new IllegalArgumentException("No root path specified, not sure where to make files");
		}
		if (!rootPath.startsWith("/")) {
			throw new IllegalArgumentException("Root path MUST be absolute");
		}
		
		
		sampleName = this.getAttribute("sample");
		submitter = this.getAttribute("submitter");
		analysisType = this.getAttribute("analysis.type");
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					finalBAM = (BAMFile)obj;
				}
				
				if (obj instanceof VCFFile) {
					variantFile = (VCFFile)obj;
				}
				
				if (obj instanceof InstanceLogFile) {
					logFile = (InstanceLogFile)obj;
				}
				
				if (obj instanceof MultiFileBuffer) {
					if (fastqs1 == null)
						fastqs1 = (MultiFileBuffer)obj;
					else
						fastqs2 = (MultiFileBuffer)obj;
				}
				
				if (obj instanceof CSVFile) {
					annotatedVariants = (CSVFile)obj;
				}
				
				if (obj instanceof QCReport) {
					qcReport = (QCReport)obj;
				}
				
				if (obj instanceof TextBuffer) {
					qcJsonFile = (TextBuffer)obj;
				}
				
				if (obj instanceof BEDFile) {
					capture = (BEDFile)obj;
				}
			}
		}
		
		if (finalBAM == null) {
			throw new IllegalArgumentException("No BAM file specified to ReviewDirGenerator");
		}
		if (variantFile == null) {
			throw new IllegalArgumentException("No variant file specified to ReviewDirGenerator");
		}
		if (logFile == null) {
			throw new IllegalArgumentException("No log file specified to ReviewDirGenerator");
		}
	}

}
