package simulation;

import java.io.File;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

/**
 * Removes reads from a .bam file based on gc content
 * @author brendan
 *
 */
public class ApplyGCBias {

	//Random number generator
	static RandomEngine rng = new MersenneTwister( (int)System.currentTimeMillis() );

	public static void main(String[] args) {
		if (args.length==0) {
			System.out.println("Remove reads from a .bam file based on gc content of read");
			System.out.println(" Usage : inputfile.bam outputfile.bam [intercept of line] [slope of line]");
			System.out.println(" Removal probability is intercept + slope*gc, where gc is GC content");
			return;
		}
		
		double intercept = -0.1;
		double slope = 2.0;
				
		File inputBAM = new File(args[0]);
		File outputBAM = new File(args[1]);
		
		if (args.length>2)
			intercept = Double.parseDouble(args[2]);
		if (args.length>3)
			slope = Double.parseDouble(args[3]);
		
		System.out.println("Using intercept: " + intercept + ", slope = " + slope);
		
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		final SAMFileReader inputSam = new SAMFileReader(inputBAM);
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		final SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMOrBAMWriter(inputSam.getFileHeader(),
				true, outputBAM);

		Uniform uniGen = new Uniform(rng);
		
		
		
		int readCount = 0;
		int outReadCount = 0;
		for (final SAMRecord samRecord : inputSam) {
			readCount++;
			byte[] bases = samRecord.getReadBases();
			int count = 0;
			for(int i=0; i<bases.length; i++) {
				if ((char)bases[i]=='G' || (char)bases[i]=='C' || (char)bases[i]=='g' || (char)bases[i]=='c')
					count++;
			}
			double gcFrac = count / (double) bases.length;
			
			double transformedValue = intercept + slope * gcFrac;
			
			if (uniGen.nextDouble() < transformedValue) {
				outputSam.addAlignment(samRecord);
				outReadCount++;
			}
		}

		outputSam.close();
		inputSam.close();

		System.out.println("Wrote " + outReadCount + " of " + readCount + " total reads");
	}
}
