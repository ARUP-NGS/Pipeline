package operator.writer;

import java.io.PrintStream;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;
import util.vcfParser.VCFParser.GTType;

public class SimulConsultWriter extends VariantPoolWriter {

	@Override
	public void writeHeader(PrintStream outputStream) {
		outputStream.println("fileformat=generic");
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		outputStream.print(rec.getAnnotation(VariantRec.GENE_NAME) + "\t"); //1 hgncSymbol
		outputStream.print("\t"); //2 gene nane long
		outputStream.print("chr" + rec.getContig() +":" + rec.getStart() + "\t"); //3 chrPos
		outputStream.print("\t"); //4 cSeqAnnotation
		outputStream.print("\t"); //5 cPosition
		outputStream.print("\t"); //6 cRef
		outputStream.print("\t"); //7 cAlt
		outputStream.print("\t"); //8 pSeqAnnotaion
		outputStream.print("\t"); //9 pPosition
		outputStream.print("\t"); //10 pRef
		outputStream.print("\t"); //11 pAlt
		outputStream.print((""+rec.getAnnotation(VariantRec.RSNUM)).replace("null",  "") + "\t"); //12 rsid
		outputStream.print((rec.getGenotype() == GTType.HET ? "Het" : "Hom") + "\t"); //13 ZygP
		outputStream.print("\t"); //14 zygM
		outputStream.print("\t"); //15 zygF
		outputStream.print(rec.getAnnotation(VariantRec.VARIANT_TYPE) + "\t"); //16 type
		outputStream.print((""+rec.getProperty(VariantRec.POP_FREQUENCY)).replace("null",  "") + "\t"); //17 freq1
		outputStream.print("\t"); //18 freq2
		outputStream.print("\t"); //19 homoshares
		outputStream.print("\t"); //20 heteroshares
		outputStream.print("\t"); //21 omim#
		outputStream.print("\t"); //22 omim disease
		outputStream.print("\t"); //23 variant accession
		outputStream.print("\t"); //24 variant pathogenicity
		outputStream.print("\t"); //25 polyphen
		outputStream.print("\t"); //26 mutation taster
		outputStream.print("\t"); //27 sift
		outputStream.print("\t"); //28 gerp
		outputStream.print("\t"); //29 grantham 
		outputStream.print("\t"); //30 phat
		outputStream.print("\t"); //31 phast
		outputStream.print("\t"); //32 phyloP
		outputStream.print("\t"); //33 strand bias
		outputStream.print("\t"); //34 known splice
		outputStream.print(rec.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t"); //35 totDepth
		outputStream.print(rec.getPropertyOrAnnotation(VariantRec.VAR_DEPTH) + "\t"); //36 varDepth
		outputStream.print(rec.getQuality() + "\t"); //37 qualP
		outputStream.print("\t"); //38 totDepthM
		outputStream.print("\t"); //39 totDepthM
		outputStream.print("\t"); //40 totDepthM
		outputStream.print("\t"); //41 totDepthM
		outputStream.print("\t"); //42 totDepthM
		outputStream.print("\t"); //43 totDepthM
		outputStream.println();
	}

}

