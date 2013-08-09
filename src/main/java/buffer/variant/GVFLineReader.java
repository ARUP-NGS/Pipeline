package buffer.variant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GVFLineReader implements VariantLineReader {
	
	public static final String REFERENCE_SEQ = "Reference_seq=";
	public static final String VARIANT_SEQ = "Variant_seq=";
	public static final String ZYGOSITY = "Zygosity=";
	public static final String VAR_EFFECT = "Variant_effect=";
	public static final String TOT_READS = "Total_reads=";
	public static final String ALLELE_FREQ = "Allele_freq=";
	public static final String GENE = "Intersecting_genes=";
	
	protected BufferedReader reader;
	protected String currentLine = null;
	
	public GVFLineReader(BufferedReader reader) throws IOException {
		this.reader = reader;
		currentLine = reader.readLine();
		currentLine = reader.readLine();
	}
	
	public GVFLineReader(File file) throws FileNotFoundException, IOException {
		this(new BufferedReader(new FileReader(file)));
	}
	
	public GVFLineReader(GVFFile file) throws FileNotFoundException, IOException {
		this(file.getFile());
	}
	
	@Override
	public boolean advanceLine() throws IOException {
		currentLine = reader.readLine();
		//Skip zero-length lines
		while (currentLine != null && currentLine.length()==0)
			currentLine = reader.readLine();
		return currentLine != null;
	}
	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		String contig = toks[0].replace("chr", "");
		String startStr = toks[3];
		String endStr = toks[4];
		
		Integer start = Integer.parseInt(startStr);
		Integer end = Integer.parseInt(endStr);
		
		String[] attrs = toks[8].split(";");
		String ref = getRef(attrs);
		String alt = getAlt(ref, attrs);
		Double quality = Double.parseDouble(toks[5]); 
		boolean het = isHet(attrs);
		
		VariantRec rec = new VariantRec(contig, start, end,  ref, alt, quality, het );
		
		Double totDepth = new Double(getDepth(attrs));
		rec.addProperty(VariantRec.DEPTH, totDepth);
		
		Double varDepth = getVarDepth(attrs, totDepth);
		rec.addProperty(VariantRec.VAR_DEPTH, varDepth);
		
		String varType = toks[2];
		rec.addAnnotation(VariantRec.VARIANT_TYPE, varType);
		
		String[] varEffect = getVarEffect(attrs);
		
		if (varEffect.length>1) {
			String nmnum = varEffect[1];
			rec.addAnnotation(VariantRec.NM_NUMBER, nmnum);
		}
		
		if (varEffect.length>2) {
			String cdot = varEffect[2];
			int index = cdot.indexOf(" ");
			if (index>-1)
				cdot = cdot.substring(0, index);
			rec.addAnnotation(VariantRec.CDOT, cdot);
		}
		
		String gene = getGene(attrs);
		if (gene != null)
			rec.addAnnotation(VariantRec.GENE_NAME, gene);
		
		return rec;
	}
	
	private String getGene(String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(GENE)) {
				return attrs[i].replace(GENE, "");
				
			}
		}
		return null;
	}
	
	private Double getVarDepth(String[] attrs, Double totDepth) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(ALLELE_FREQ)) {
				String reads = attrs[i].replace(ALLELE_FREQ, "");
				if (reads.length()>0)
					return totDepth*Double.parseDouble(reads);
			}
		}
		return 0.0;
	}

	private String[] getVarEffect(String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(VAR_EFFECT)) {
				String[] eff = attrs[i].replace(VAR_EFFECT, "").split(":");
				return eff;
			}
		}
		return new String[]{};
	}

	private Integer getDepth(String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(TOT_READS)) {
				String reads = attrs[i].replace(TOT_READS, "");
				if (reads.length()>0)
					return Integer.parseInt(reads);
			}
		}
		return -1;
	}
	
	private String getRef(String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(REFERENCE_SEQ))
				return attrs[i].replace(REFERENCE_SEQ, "");
		}
		return "?";
	}
	
	private String getAlt(String ref, String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(VARIANT_SEQ)) {
				String[] vars = attrs[i].replace(VARIANT_SEQ, "").split(",");
				for(int j=0; j<vars.length; j++) {
					if (! vars[j].equals(ref))
						return vars[j];
				}
			}
		}
		return "?";
	}
	
	private boolean isHet(String[] attrs) {
		for(int i=0; i<attrs.length; i++) {
			if (attrs[i].contains(ZYGOSITY)) {
				return attrs[i].contains("het") || attrs[i].contains("Het");
			}
		}
		
		return false;
	}
	
	
	public static void main(String[] args) {
		
		File inputFile = new File("/home/brendan/marc-test/12020129453.dbxref.gvf");
		
		try {
			GVFLineReader reader = new GVFLineReader(inputFile);
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/home/brendan/marc-test/marc.annovar.input")));

			do {
				VariantRec rec = reader.toVariantRec();
				String cDot = rec.getAnnotation(VariantRec.CDOT);
				if (cDot != null)
					System.out.println(rec.toSimpleString() + "\t" + cDot);
				String gene = rec.getAnnotation(VariantRec.GENE_NAME);
				writer.write(rec.toSimpleString() + "\t" + cDot + "\t" + gene + "\n");
				
			} while( reader.advanceLine());
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getCurrentLine() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
