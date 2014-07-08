package operator.bamutils;

import java.io.IOException;
import json.JSONException;

import operator.IOOperator;
import operator.OperationFailedException;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;

/*
 * Returns a BAM file containing all reads in the input BAM file which
 * intersect with the input BED file.
 * 
 * @author daniel
 * 
*/

public class RegionFilter extends IOOperator{
	public static final String SAMTOOLS_PATH="samtools.path";
	String samtoolsPath="samtools";
		@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		String inBam = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		FileBuffer bedRegion = this.getInputBufferForClass(BEDFile.class);
		FileBuffer outFile = this.getOutputBufferForClass(BAMFile.class);
		String samAttr= this.getPipelineProperty(SAMTOOLS_PATH);
		if(samAttr != null) {
			samtoolsPath=samAttr;
		}
		String command_str=samtoolsPath + " view -L " + bedRegion.getAbsolutePath() + " -b -o " + outFile.getAbsolutePath() + " " + inBam;
		executeCommand(command_str);
		return;
	}
	
	
}
