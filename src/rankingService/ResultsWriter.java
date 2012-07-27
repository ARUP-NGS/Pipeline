package rankingService;

import java.io.PrintStream;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;

/**
 * Handles writing of output in a simple, user-friendly format 
 * @author brendan
 *
 */
public class ResultsWriter extends VariantPoolWriter  {

	public static final String header = "Gene	cDot	pDot	disease.potential	gene.relevance	overall.score	rsNumber	population.frequency	top.pubmed.hit	goterm.hits	interaction.score	summary.score";
	
	@Override
	public void writeHeader(PrintStream out) {
		out.println(header);
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream out) {
		out.println( rec.getAnnotation(VariantRec.GENE_NAME) + "\t" + rec.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + rec.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + rec.getPropertyOrAnnotation(VariantRec.EFFECT_PREDICTION2) + "\t" + rec.getPropertyOrAnnotation(VariantRec.GENE_RELEVANCE) + "\t" + rec.getPropertyOrAnnotation(VariantRec.GO_EFFECT_PROD) + "\t" + rec.getPropertyOrAnnotation(VariantRec.RSNUM) + "\t" + rec.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + rec.getPropertyOrAnnotation(VariantRec.PUBMED_HIT) + "\t" + rec.getPropertyOrAnnotation(VariantRec.GO_HITS) + "\t" + rec.getPropertyOrAnnotation(VariantRec.INTERACTION_SCORE) + "\t" + rec.getPropertyOrAnnotation(VariantRec.SUMMARY_SCORE));
	}

}
