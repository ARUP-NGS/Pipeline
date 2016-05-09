package util.prereviewDataGen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import util.ReviewDirTool;

/**
 * Just a thin wrapper for the qcJsonReader that we use to 
 * collate qc data across a number of samples in prep for uploading to NGSWeb
 * @author brendan
 *
 */
public class PrereviewDataGenerator {

	
	public static void main(String[] args) {
		
		List<String> paths = new ArrayList<String>();
		for(int i=0; i<args.length; i++) {
			File file = new File(args[i]);
			if (file.exists() && file.isDirectory() && file.canRead()) {
				paths.add(args[i]);
			}
			
		}
		
		ReviewDirTool.performQCList(paths, System.out, new AnalysisTypeConverter());
		
		
	}
	
}
