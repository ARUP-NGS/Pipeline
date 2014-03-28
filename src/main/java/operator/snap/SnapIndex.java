package operator.snap;

import java.io.File;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;

/**
 * This looks for a SNAP index at snap.index.
 * If one exists, it exits.
 * If one does not exist, it runs snap index, building an index at
 * the specified location
 * Warning: the reference files produced by SNAP are not uniquely named.
 * In order to use a new reference (for example, custom reference files), set your snap.index location to be a subfolder
 * of snap_index. Currently, snap_index/full_genome holds the human_g1k_v37.fasta indices. 
 * @author daniel
 *
 */
public class SnapIndex extends IOOperator {

	public static final String HREF_PATH = "href.path";
	public static final String SNAP_PATH = "snap.path";
	public static final String SNAP_INDEX = "snap.index";
	public static final String HG19_SETTING = "hg.19";
		
	String samtoolsPath = null;
	String snapIndexPath = null;
	String snapPath = null;
	String hRefPath = null;
	String hg19_setting = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		String snapGenomeIndexHash = snapIndexPath + "/GenomeIndexHASH";
		if (new File(snapGenomeIndexHash).exists()) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Snap Index found. SnapIndex exiting");
			return;
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Snap Index not found. Building.");
		
		//If there is no directory at the location provided in pipeline_properties, it builds one
		if ((! (new File(snapIndexPath).exists())) && ( snapIndexPath != null)) {
			String commandBuildDir = "mkdir " + snapIndexPath;
			executeCommand(commandBuildDir);
		}
		//Builds index
		String command1 = snapPath + " index "
				+ hRefPath + " "
				+ snapIndexPath + " " + hg19_setting
				+ " -t " + threads;
		executeCommand(command1);
	}
	
	
	
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String snapPathAttr = this.getAttribute(SNAP_PATH);
		if (snapPathAttr == null) {
			snapPathAttr = this.getPipelineProperty(SNAP_PATH);
		}
		if (snapPathAttr == null) {
			throw new IllegalArgumentException("No path to SNAP found, please specify " + SNAP_PATH);
		}
		if (! (new File(snapPathAttr).exists())) {
			throw new IllegalArgumentException("No file found at Snap path path : " + snapPath);
		}
		this.snapPath = snapPathAttr;
		
		
		String snapIndexAttr = this.getAttribute(SNAP_INDEX);
		if (snapIndexAttr == null) {
			snapIndexAttr = this.getPipelineProperty(SNAP_INDEX);
		}
		if (snapIndexAttr == null) {
			throw new IllegalArgumentException("No path to SNAP index found, please specify " + SNAP_INDEX);
		}
		if (snapIndexAttr != null) {
			this.snapIndexPath = snapIndexAttr;
		}

		String hRefPathAttr = this.getAttribute(HREF_PATH);
		if (hRefPathAttr == null) {
			hRefPathAttr = this.getPipelineProperty(HREF_PATH);
		}
		if (hRefPathAttr == null) {
			throw new IllegalArgumentException("A reference genome fasta is required!");
		}
		
		String hg19Attr = this.getAttribute(HG19_SETTING);
		if (hg19Attr == null || hg19Attr == "true") {
			hg19Attr = this.getPipelineProperty(HG19_SETTING);
		}
		if (hg19Attr == null || hg19Attr == "true") {
			this.hg19_setting = "-hg19"; //default to hg19 speedup settings
		}
		else {
			this.hg19_setting = "";
		}
		
		
	}

}
