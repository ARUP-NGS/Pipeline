package util.coverage;

import java.io.File;

import util.bamWindow.BamWindow;

public class CoverageCalcTest {

	public static void main(String[] args) {
		
		BamWindow bam = new BamWindow(new File(args[0]));
		int colon = args[1].indexOf(":");
		int hyphen = args[1].indexOf("-");
		String chr = args[1].substring(0, colon).replace("chr", "");
		int start = Integer.parseInt(args[1].substring(colon+1, hyphen).trim());
		int end = Integer.parseInt(args[1].substring(hyphen+1).trim());
		
		int advance = 2;
		
		bam.advanceTo(chr, start);
		
		boolean cont = bam.hasMoreReadsInCurrentContig();
		while(cont && bam.getCurrentPosition() < end) {
			System.out.println(chr + ":" + bam.getCurrentPosition() + "\t" + bam.size());
			cont = bam.advanceBy(advance);
		}
		
		bam.close();
		
		
	}
}
