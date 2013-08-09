package util.flatFilesReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import buffer.variant.VariantRec;

/**
 * Small utility to write some values from dbNSFP over a region
 * @author brendan
 *
 */
public class DBNSFPEmitter {

	public static void emitRegion(DBNSFPReader reader, PrintStream writer, String contig, int startPos, int endPos) throws IOException {
		reader.advanceTo("" + contig, startPos);
		int curPos = reader.getCurrentPos();
		while(curPos < endPos) {
			for(int i = 0; i<bases.length; i++) {	
				String ref = reader.getRef();
				if (ref == null || ref.charAt(0) == bases[i]) {
					boolean hasNext = reader.advanceLine();
					if ((!hasNext) || curPos != reader.getCurrentPos()) {
						break;
					}
					continue;
				}

				String gene = reader.getString(DBNSFPReader.GENE);
				String tkgStr = reader.getString(DBNSFPReader.TKG);
				String afrFreq = reader.getString(DBNSFPReader.TKG_AFR);
				String amrFreq = reader.getString(DBNSFPReader.TKG_AMR);
				String eurFreq = reader.getString(DBNSFPReader.TKG_EUR);
				String mtStr = reader.getString(DBNSFPReader.MT);
				String ppStr = reader.getString(DBNSFPReader.PP);
				String ppHvar = reader.getString(DBNSFPReader.PP_HVAR);
				String siftStr = reader.getString(DBNSFPReader.SIFT);
				String gerpStr = reader.getString(DBNSFPReader.GERP);
				String gerpNRStr = reader.getString(DBNSFPReader.GERP_NR);
				String phylopStr = reader.getString(DBNSFPReader.PHYLOP);
				String siphyStr = reader.getString(DBNSFPReader.SIPHY);
				String lrtStr = reader.getString(DBNSFPReader.LRT);
				String slrStr = reader.getString(DBNSFPReader.SLR_TEST);
				String maStr = reader.getString(DBNSFPReader.MA);
				String codonPosStr = reader.getString(DBNSFPReader.CODON_POS);
				
//				if (! tkgStr.equals(".")) {
//					try {
//						Double tkgFreq = Double.parseDouble(tkgStr);
//						if (tkgFreq > 0.025) {
//							writer.println("-1" + "\t 1:" + Double.parseDouble(siftStr) + "\t2:" + Double.parseDouble(ppStr) + "\t3:" + Double.parseDouble(mtStr) + "\t4:" + Double.parseDouble(gerpStr)
//									+ "\t5:" + Double.parseDouble(phylopStr) + "\t6:" + Double.parseDouble(siphyStr) + "\t7:" + Double.parseDouble(lrtStr)
//									+ "\t8:" + Double.parseDouble(slrStr) + "\t9:" + Double.parseDouble(gerpNRStr) + "\t10:" + Double.parseDouble(ppHvar) + "\t11:" + Double.parseDouble(maStr) );
//						}
//					}
//					catch (NumberFormatException nfe) {
//						//ignore, this will happen sometimes
//					}
//				}
				
				if (! tkgStr.equals(".")) {
					//					Double freq = Double.parseDouble(popStr);
					VariantRec var = new VariantRec("" + contig, curPos, curPos+1, reader.getRef(), reader.getAlt() );
					//					var.addProperty(VariantRec.POP_FREQUENCY, freq);
					Double mtVal = Double.NaN;
					if (mtStr.length()>2)
						mtVal = Double.parseDouble(mtStr);
					if (tkgStr.equals(".")) {
						tkgStr = "0.0";
					}
					if (amrFreq.equals(".")) {
						amrFreq = "0.0";
					}
					if (eurFreq.equals(".")) {
						eurFreq = "0.0";
					}
					writer.println(var.toSimpleString() + "\t" + gene + "\t" + tkgStr + "\t" + afrFreq + "\t" + amrFreq + "\t" + eurFreq + "\t" + mtVal + "\t" + siftStr + "\t" + ppStr + "\t" + gerpStr + "\t" + phylopStr + "\t" + maStr + "\t" + slrStr);

					i = bases.length; //If we've found one for this position, skip all additional alts at this site
				}
				
				boolean hasNext = reader.advanceLine();
				if ((!hasNext) || curPos != reader.getCurrentPos()) {
					break;
				}
			}
			curPos = reader.getCurrentPos();
			if (curPos == -1) 
				break;
		}
	}
	
	public static void emitHGMD(DBNSFPReader reader, PrintStream writer, File hgmdFile) throws IOException {
		BufferedReader hgmdReader = new BufferedReader(new FileReader(hgmdFile));
		String line = hgmdReader.readLine();
		int missed = 0;
		int novals = 0;
		
		while(line != null) {
			if (line.startsWith("#") || line.trim().length()==0) {
				line = hgmdReader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String contig = toks[0];
			Integer pos = Integer.parseInt(toks[1]);
			String cdot = toks[2];
			String type = toks[6];
			
			if (! type.equals("DM")) {
				line = hgmdReader.readLine();
				continue;
			}
			
			String alt = "";
			int index = cdot.indexOf(">");
			if (index > 0) {
				alt = cdot.substring(index+1);
			}
			
			reader.advanceTo(contig, pos);
			String strand = reader.getString(DBNSFPReader.STRAND);

			if (strand == null) {
				line = hgmdReader.readLine();
				missed++;
				continue;
			}
			
			//Compute actual alt, requires complementation if cds strand is 1
			if (strand.equals("-")) {
				if (alt.equals("A"))	alt = "T";
				if (alt.equals("C"))	alt = "G";
				if (alt.equals("G"))	alt = "C";
				if (alt.equals("T"))	alt = "A";
			}

			if (alt.length() == 0) {
				line = hgmdReader.readLine();
				continue;
			}
			reader.advanceTo(contig, pos, alt.charAt(0));
			String gene = reader.getString(DBNSFPReader.GENE);
			String tkgStr = reader.getString(DBNSFPReader.TKG);
			String popStr = reader.getString(DBNSFPReader.TKG_AMR);
			String eurStr = reader.getString(DBNSFPReader.TKG_EUR);
			String mtStr = reader.getString(DBNSFPReader.MT);
			String ppStr = reader.getString(DBNSFPReader.PP);
			String ppHvar = reader.getString(DBNSFPReader.PP_HVAR);
			String siftStr = reader.getString(DBNSFPReader.SIFT);
			String gerpStr = reader.getString(DBNSFPReader.GERP);
			String gerpNRStr = reader.getString(DBNSFPReader.GERP_NR);
			String phylopStr = reader.getString(DBNSFPReader.PHYLOP);
			String siphyStr = reader.getString(DBNSFPReader.SIPHY);
			String lrtStr = reader.getString(DBNSFPReader.LRT);
			String slrStr = reader.getString(DBNSFPReader.SLR_TEST);
			String maStr = reader.getString(DBNSFPReader.MA);
			String codonPosStr = reader.getString(DBNSFPReader.CODON_POS);
			
			if (reader.getCurrentPos() != pos) {
			//	System.out.println("Yikes, passed original location! aborting ");
				missed++;
				line = hgmdReader.readLine();
				continue;
			}
			try {
				writer.println("-1" + "\t 1:" + Double.parseDouble(siftStr) + "\t2:" + Double.parseDouble(ppStr) + "\t3:" + Double.parseDouble(mtStr) + "\t4:" + Double.parseDouble(gerpStr)
						+ "\t5:" + Double.parseDouble(phylopStr) + "\t6:" + Double.parseDouble(siphyStr) + "\t7:" + Double.parseDouble(lrtStr)
						+ "\t8:" + Double.parseDouble(slrStr) + "\t9:" + Double.parseDouble(gerpNRStr) + "\t10:" + Double.parseDouble(ppHvar) + "\t11:" + Double.parseDouble(maStr) );

			}
			catch(NumberFormatException nfe) {
				//ignore for now
				novals++;
			}
			
			line = hgmdReader.readLine();
		}
		hgmdReader.close();
		System.out.println("Missed " + missed + " variants");
		System.out.println("No vals " + novals + " variants");
	}
	
	public static void emitCloseGene(DBNSFPReader reader, PrintWriter writer, String contig, int startPos) throws IOException {
		
		for(int i=startPos+10; i<startPos+200; i++) {
				reader.advanceTo("" + contig, i);
				String gene = reader.getString(DBNSFPReader.GENE);
				System.out.println(contig + "\t" + startPos + "\t" + gene);
				return;	
		}
	}
	
	public static void main(String[] args) throws IOException {
		DBNSFPReader reader = new DBNSFPReader();
		
//		PrintStream out = new PrintStream(new FileOutputStream(new File("hgmd_vals.b4.csv")));
//		emitHGMD(reader, out, new File(args[0]));
//		return;
				
		PrintStream writer = new PrintStream(new FileOutputStream("/home/brendan/tkgdata.csv"));
		PrintStream out = writer; //System.out;
		out.println(VariantRec.getSimpleHeader() + "\tgene\ttkg.freq\tamr.freq\teur.freq\tmt.score\tsift.score\tpp.score\tgerp.score\tphylop.score\tmut.assessor.score\tslr.score");
		//String infilePath = args[0];
		
		String infilePath = "PTPN14.bed";
		//String infilePath = "/home/brendan/resources/SureSelect_XT_v4.sorted.bed";
		File inFile = new File(infilePath);
		BufferedReader fileReader = new BufferedReader(new FileReader(inFile));
		String line = fileReader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = fileReader.readLine();
				continue;
			}
			String[] toks = line.split("\t");
			String contig = toks[0];
			Integer startPos = Integer.parseInt(toks[1].replace("chr", ""));
			Integer endPos = Integer.parseInt(toks[2]);
			//System.out.println("Reading region chr" + contig + ":" + startPos );
			
			emitRegion(reader, out, contig, startPos, endPos);
			//emitCloseGene(reader, writer, "" + contig, startPos);
			line = fileReader.readLine();
		}
		out.flush();
		fileReader.close();
		
		
	}
	
	public static final char[] bases = new char[]{'A', 'C', 'G', 'T'};
}


