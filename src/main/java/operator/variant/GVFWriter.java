package operator.variant;

import java.io.PrintStream;
import java.util.Date;

import buffer.variant.VariantRec;

/**
 * Writes a variant pool in GVF format. Right now we make sure this is compatible with the
 * 'clinical' gvf format used in the ARUP clinical presentation stuff
 * @author brendan
 *
 */
public class GVFWriter extends VariantPoolWriter {

	public static final String lineSep = System.getProperty("line.separator");
	
	private String individualID = "unknown";
	
	
	
	public String getIndividualID() {
		return individualID;
	}

	public void setIndividualID(String individualID) {
		this.individualID = individualID;
	}

	@Override
	public void writeHeader(PrintStream out) {
		out.println("##gvf-version 1.06");
		out.println("##file-date " + new Date());
		out.println("##individual-id " + individualID);
		out.println("##technology-platform Platform_name=Illumina HiSeq 2000;");
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream out) {
		int start = rec.getStart();
		int end = rec.getStart() + rec.getRef().length()-1;
		out.print(rec.getContig() + "\t" + individualID + "\t" + getVarTypeSOTerm(rec) + "\t" + start + "\t" + end + "\t+\t.\t");
		
		String idStr = "ID=" + "chr" + rec.getContig() + ":" + start + ":" + end + ":" + getVarTypeSOTerm(rec);
		out.print(idStr + ";");
		
		String refSeq = "Reference_seq=" + rec.getRef();
		out.print(refSeq + ";");
		
		String altSeq = "Variant_seq=" + rec.getAlt();
		out.print(altSeq + ";");
		
		String zygoStr = "Genotype=" + getZygosityStr(rec);
		out.print(zygoStr + ";");
		
		String aliasStr = "Alias=" + getHGVSStr(rec);
		out.print(aliasStr + ";");
		
		String depthStr = "Total_reads=" + rec.getPropertyOrAnnotation(VariantRec.DEPTH);
		out.print(depthStr + ";");
		
		String varDepthStr = "Variant_reads=" + rec.getProperty(VariantRec.VAR_DEPTH);
		out.print(varDepthStr +";");
		
		
	}
	
	public String getHGVSStr(VariantRec rec) {
		String nmStr = rec.getAnnotation(VariantRec.NM_NUMBER);
		if (nmStr == null || nmStr.equals("-")) {
			return rec.getContig() + ":" + rec.getStart() + ":" + rec.getAlt();
		}
		String cdot = rec.getAnnotation(VariantRec.CDOT);
		return "HGVS:" + nmStr + ":" + cdot;
	}
	
	
	private static String getZygosityStr(VariantRec rec) {
		if (rec.isHetero()) {
			return "heterozygous";
		}
		else {
			return "homozygous";
		}
	}
	
	/**
	 * Obtain Sequence Ontology description for this variant type
	 * @return
	 */
	private static String getVarTypeSOTerm(VariantRec rec) {
		if (rec.isSNP()) {
			return "SNV";
		}
		if (rec.isInsertion()) {
			return "NUCLEOTIDE_INSERTION";
		}
		if (rec.isDeletion()) {
			return "NUCLEOTIDE_DELETION";
		}
		return "?";
	}

}
