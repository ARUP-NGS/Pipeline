package operator.annovar;

import java.io.File;

import buffer.variant.GVFFile;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import pipeline.Pipeline;

public class DBVarAnnotator extends Annotator {

	public static final String DBVAR_PATH = "dbVar.path";
	
	public DBVarAnnotator() {
		//Read initial list of structural variants...
		String path = (String) getPipelineProperty(DBVAR_PATH);
		if (path == null)
			throw new IllegalArgumentException("Could not initialize DBVarAnnotator, DBVAR_PATH not specified");
		
		//GVFFile gvfFile = new GVFFile(new File(path));
		
		//ARG, not done. File format apparently does not contain chromosomes. 
	}
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void annotateVariant(VariantRec var) {
		// TODO Auto-generated method stub
		
	}

	

}
