package operator.variant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Writes a variant pool to a CSV file, in the format expected by most
 * of the tools used in Pipeline 
 * @author brendan
 *
 */
public class VariantPoolToFile extends VariantPoolWriter {


	String[] toInclude = new String[]{VariantRec.GENE_NAME, 
								VariantRec.VARIANT_TYPE,
								VariantRec.EXON_FUNCTION,
								VariantRec.POP_FREQUENCY,
								VariantRec.EXOMES_FREQ,
								VariantRec.SIFT_SCORE,
								VariantRec.POLYPHEN_SCORE,
								VariantRec.MT_SCORE,
								VariantRec.PHYLOP_SCORE,
								VariantRec.GERP_SCORE,
								VariantRec.EFFECT_PREDICTION };
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		outputStream.print("#contig \t start \t end \t ref \t alt \t quality \t depth \t zygosity \t genotype.quality ");
		
		for(int i=0; i<toInclude.length; i++) {
			outputStream.print("\t" + toInclude[i]);
		}
		
		outputStream.println();
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		String depthStr = "-";
		Double depth = rec.getProperty(VariantRec.DEPTH);
		if (depth != null)
			depthStr = "" + depth;
		
		String hetStr = "het";
		if (! rec.isHetero())
			hetStr = "hom";
		
		String gqStr = "-";
		Double gq = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
		if (gq != null)
			gqStr = "" + gq;
		
		outputStream.print(rec.getContig() + "\t" + rec.getStart() + "\t" + rec.getEnd() + "\t" + rec.getRef() + "\t" + rec.getAlt() + "\t" + rec.getQuality() + "\t" + depthStr + "\t" + hetStr + "\t" + gqStr);
		
		for(int i=0; i<toInclude.length; i++) {
			String str = rec.getPropertyOrAnnotation(toInclude[i]);
			outputStream.print("\t" + str);
		}
		
		outputStream.println();
	}

}
