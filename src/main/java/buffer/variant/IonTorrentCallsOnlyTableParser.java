/**
 * 
 */
package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import util.vcfParser.VCFParser;
import util.vcfParser.VCFParser.GTType;

/** This class will read in the Torrent table allele calls only file and put them into a variant pool for annotating.
 * @author kevin
 *
 */
public class IonTorrentCallsOnlyTableParser extends CSVLineReader {

	public static final String DEPTH = "genotype";
	public static final String VCF_POS = "vcf.position";
	public static final String VCF_REF = "vcf.ref";
	public static final String VCF_ALT = "vcf.variant";

	private boolean headerHasBeenRead = false;

	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	protected void readHeader() throws IOException {
		// Ion torrent specific header reading...
		System.out.println("reading ion torrent header===========================");
		if (sourceFile == null) {
			String sourceFilePath = this.getAttribute("filename");
			if (sourceFilePath == null) {
				throw new IllegalArgumentException("No file specified");
			}
			sourceFile = new File(this.getProjectHome() + "/" + sourceFilePath);
		}

		reader = new BufferedReader(new FileReader(sourceFile));
		currentLine = reader.readLine();

		String[] rawToks = currentLine.split("\t");
		List<String> tokList = new ArrayList<String>();
		for(int i=0; i<rawToks.length; i++) {
			String trimmed_tok = rawToks[i].trim();
			if (trimmed_tok.length()>0) {
				//Else if statements looking for Ion Torrent specific header names that must be changed to standard names.
				if (trimmed_tok.equals("Coverage") ) {
					trimmed_tok = VariantRec.DEPTH;
				}
				else if (trimmed_tok.equals("Allele Cov") ) {
					trimmed_tok = VariantRec.VAR_DEPTH;
				}
				else if (trimmed_tok.equals("Frequency") ) {
					trimmed_tok = VariantRec.VAR_FREQ;
				}
				else if (trimmed_tok.equals("Allele Name") ) {
					trimmed_tok = VariantRec.COSMIC_ID;
				}

				tokList.add(trimmed_tok);
			}
		}
		headerToks = tokList.toArray(new String[]{});
		for(int i=0; i<tokList.size(); i++) {
			headerMap.put(tokList.get(i).trim(), i);
		}
		currentLine = reader.readLine();

		headerHasBeenRead = true;
	}

	/**
	 * @param csvFile
	 * @throws IOException
	 */
	public IonTorrentCallsOnlyTableParser(File csvFile) throws IOException {
		//super(csvFile);
	}

	/**
	 * 
	 */
	public IonTorrentCallsOnlyTableParser() {
	}

	private String getIonTorrentGenotypeString(String ref, String alt, String genotype) {
		genotype = genotype.toLowerCase();
		String geno = "";
		if (genotype.contains("het") ) {

			geno = ref + "/" + alt;
		}
		else if (genotype.contains("hom") ) {

			geno = alt + "/" + alt;
		}
		else {
			geno = "./.";
		}
		return geno;
	}

	private GTType getIonTorrentGTType(String genotype) {
		genotype = genotype.toLowerCase();
		if (genotype.contains("het") ) {
			return GTType.HET;
		}
		else if (genotype.contains("hom") ) {
			return GTType.HOM;
		}
		else if (genotype.contains("hemi") ) {
			return GTType.HEMI;
		}
		else {
			return GTType.UNKNOWN;
		}
	}

	@Override
	public VariantRec toVariantRec()  {
		if (! headerHasBeenRead) {
			try {
				readHeader();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		if (currentLine == null)
			return null;

		String[] toks = currentLine.split("\t");
		VariantRec rec = null;

		if (toks.length < 8) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine + " (at least 8 columns required)");
			return null;
		}

		try {
			String contig = toks[0].replace("chr", "");
			Integer start = Integer.parseInt(toks[1]);
			String ref = toks[2];
			String alt = toks[3];
			String call = toks[4];
			String derivedGenotype = getIonTorrentGenotypeString(ref, alt, call);
			GTType zygosity = getIonTorrentGTType(call);

			Double qual = Double.parseDouble(toks[7]);


			//boolean isHet = false;
			//new VariantRec()
			rec = new VariantRec(contig, start, start+ref.length(), ref, alt, qual, derivedGenotype, zygosity);
			rec = VCFParser.normalizeVariant(rec);

			//"pop.freq","exomes6500.frequency","rsnum","hgmd.hit","chrom","pos","ref","alt","zygosity","nm.number")
			rec.addProperty(VariantRec.VAR_FREQ, round(Double.parseDouble(toks[6]), 2) );
			rec.addProperty(VariantRec.DEPTH, Double.parseDouble(toks[18]));
			rec.addProperty(VariantRec.VAR_DEPTH, Double.parseDouble(toks[24]));
			rec.addAnnotation(VariantRec.COSMIC_ID, toks[11]);


			if (sourceFile != null)
				rec.addAnnotation(VariantRec.SOURCE, sourceFile.getName());
			System.out.println(rec);
		}
		catch (NumberFormatException nfe) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);

		}
		return rec;
	}
}
