package simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This just reads 'the' 1000Genomes vcf (often named 1000G.phase1.v3.20101123.sites.vcf)
 * and writes a new vcf that includes each variant from 1000 Genomes in proportion to the
 * variants frequency (from the AF field)
 * @author brendan
 *
 */
public class SampleFrom1000Genomes {

	public static void main(String[] args) throws IOException {
		
		File origVCF = new File(args[0]);
		
		BufferedReader reader = new BufferedReader(new FileReader(origVCF));
		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			String chr = toks[0];
			String pos = toks[1];
			String ref = toks[3];
			String alt = toks[4];
			
			int index = line.indexOf("AF=");
			if (index > 0) {
				int next = line.indexOf(";", index+1);
				String af = line.substring(index+3, next);
				Double val = Double.parseDouble(af);
				
				System.out.println(chr + "\t" + pos + "\t" + ref + "\t" + alt + "\t" + val);
			}
			line = reader.readLine();
		}
		
		reader.close();
		
	}
}
