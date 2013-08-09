package operator.bamutils;

import java.util.Arrays;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

public class HaloplexSoftClipper extends BAMProcessor {
	
	private byte[] qualsToWrite = null;
	
	@Override
	public SAMRecord processRecord(SAMRecord samRecord) {
		
		if (samRecord.getReadName().contains("2114:20957:17172") || samRecord.getReadName().contains("1112:4858:9001")) {
			System.out.println("brasl");
		}
		
		Cigar cig = samRecord.getCigar();
		
		//If cigar is '=' then don't do anything
		List<CigarElement> cigars = cig.getCigarElements();
		if (cigars.size()==0 
				|| (cigars.size()==1 && cigars.get(0).getOperator() == CigarOperator.M)) {
			return samRecord;
		}
		else {
			
			
			//clip read by converting base quality to zero
			//byte[] qualities = samRecord.getBaseQualities();
			
			if (qualsToWrite == null || qualsToWrite.length != samRecord.getBaseQualities().length) {
				qualsToWrite = Arrays.copyOf(samRecord.getBaseQualities(), samRecord.getBaseQualities().length);

			}
			else {
				System.arraycopy(samRecord.getBaseQualities(), 0, qualsToWrite, 0, samRecord.getBaseQualities().length);
			}




			if (qualsToWrite.length>20) {

				CigarElement cigar = cigars.get(0);
				if (cigar.getOperator() == CigarOperator.X) {
					for (int i = 0; i<cigar.getLength(); i++) {
						qualsToWrite[i] = 0;
					}
					System.out.println("Trimming read " + cigar.getLength() + " bases from "+ samRecord.getReadName());
				}


				cigar = cigars.get( cigars.size()-1);
				if (cigar.getOperator() == CigarOperator.X) {
					for (int i = Math.max(qualsToWrite.length-cigar.getLength(), 0); i<qualsToWrite.length; i++) {
						qualsToWrite[i] = 0;
					}
					System.out.println("Trimming read " + cigar.getLength() + " bases from "+ samRecord.getReadName());
				}
			}


			samRecord.setBaseQualities(qualsToWrite);

		}
		
		
		return samRecord;
	}

}
