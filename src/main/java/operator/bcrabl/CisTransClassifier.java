package operator.bcrabl;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.apache.log4j.Logger;

import buffer.BAMFile;
import buffer.variant.VariantRec;


/**
 * @author markebbert
 *
 */
public class CisTransClassifier  {

	private int permittedDist = 150;
	private int requiredMappingQual = 4;
	private int requiredBaseQual = 4;

	public int getPermittedDist() {
		return permittedDist;
	}



	public void setPermittedDist(int permittedDist) {
		Logger.getLogger(getClass()).info("Cis/trans setting required max distance to " + permittedDist);
		this.permittedDist = permittedDist;
	}



	public int getRequiredMappingQual() {
		return requiredMappingQual;
	}



	public void setRequiredMappingQual(int requiredMappingQual) {
		Logger.getLogger(getClass()).info("Cis/trans setting required mapping quality to " + requiredMappingQual);
		this.requiredMappingQual = requiredMappingQual;
	}



	public int getRequiredBaseQual() {
		return requiredBaseQual;
	}



	public void setRequiredBaseQual(int requiredBaseQual) {
		Logger.getLogger(getClass()).info("Cis/trans setting required base quality to " + requiredBaseQual);
		this.requiredBaseQual = requiredBaseQual;
	}

	public boolean closeEnoughToCompute(VariantRec var1, VariantRec var2) {
		int var1Pos = var1.getStart();
		int var2Pos = var2.getStart();
		int dist = Math.abs(var1Pos - var2Pos);
		if (dist > permittedDist) {
			return false;
		}
		return true;
	}

	public CisTransResult computeCisTransResult(BAMFile inputBAMFile, VariantRec var1, VariantRec var2) {
		//A few sanity checks:
				if (!var1.getContig().equals(var2.getContig())) {
					throw new IllegalArgumentException("Variants are not on the same chromosome");
				}

				
				
				
				
				int var1Pos = var1.getStart();
				int var2Pos = var2.getStart();
				char var1Ref = var1.getRef().charAt(0);
				char var2Ref = var2.getRef().charAt(0);
				char var1Alt = var1.getAlt().charAt(0);
				char var2Alt = var2.getAlt().charAt(0);

				// test whether positions are within a certain distance (~100bp)
				// If not, we cannot tell because no reads will span both positions

				int dist = Math.abs(var1Pos - var2Pos);
				if (dist > permittedDist) {
					throw new IllegalArgumentException("Variants are not close enough to compute (" + dist + " bases separate them)");
				}

				int startPos = Math.min(var1Pos, var2Pos) - permittedDist;
				int endPos = Math.max(var1Pos, var2Pos) + permittedDist;
				if (startPos < 1) 
					startPos = 1;
				
				// open bam file
				SAMFileReader inputBAM = new SAMFileReader(inputBAMFile.getFile());
				SAMRecordIterator sit = inputBAM.query(var1.getContig(), startPos, endPos, false);
				MappedRead mapped;
				char baseAtRef1, baseAtRef2;
				double bothAlts=0, bothRefs=0, alt1Only=0, alt2Only=0, misc=0,
						readCov=0;
				SAMRecord samRecord = sit.next();
				while(samRecord != null) {
					mapped = new MappedRead(samRecord);
					
					// If within distance, loop through reads and for each one that spans
					// both positions, count how many fit into one of four categories:
					// 1. Variants A and B on the read
					// 2. Variant A but not B on the read
					// 3. Variant B but not A on the read
					// 4. Neither variant on the read

					//Funky logic below addresses the fact that deletions don't have a base quality associated with them
					//So if we detect a deletion then ignore base quality constraint for that variant 
					if(mapped.containsPosition(var1Pos) && mapped.containsPosition(var2Pos)
							&& mapped.getMappingQuality() >= requiredMappingQual
							&& ((mapped.getBaseAtReferencePos(var1Pos)<0) || (mapped.getQualityAtReferencePos(var1Pos) >= requiredBaseQual) )
							&& ((mapped.getBaseAtReferencePos(var2Pos)<0) || (mapped.getQualityAtReferencePos(var2Pos) >= requiredBaseQual))){

						readCov++;

						//Be sure to catch / convert cases where the variant is a deletion to '-'
						baseAtRef1 = mapped.getBaseAtReferencePos(var1Pos)>-1 ? (char) mapped.getBaseAtReferencePos(var1Pos) : '-';
						baseAtRef2 = mapped.getBaseAtReferencePos(var2Pos)>-1 ? (char) mapped.getBaseAtReferencePos(var2Pos) : '-';
							 
								
						if(baseAtRef1 == var1Ref && baseAtRef2 == var2Ref){
							bothRefs++;
						}
						else if(baseAtRef1 == var1Alt && baseAtRef2 == var2Alt){
							bothAlts++;
						}
						else if(baseAtRef1 == var1Alt && baseAtRef2 != var2Alt){
							alt1Only++;
						}
						else if(baseAtRef1 != var1Alt && baseAtRef2 == var2Alt){
							alt2Only++;
						}
						else{
							//							System.out.println("Base at Ref1: " + baseAtRef1 + "\t" + "Base at Ref2: " + baseAtRef2);
							misc++;
						}
					}

					if (sit.hasNext()) {
						samRecord = sit.next();
					}
					else {
						samRecord = null;
					}

				}
				inputBAM.close();
				
				
				double minAlt = Math.min(alt1Only, alt2Only);
				
				
				CisTransResult result = new CisTransResult();
				if (readCov == 0) {
					result.setFailed(true);
					result.setMessage("No reads span both positions.");
				}
				else {
					result.setFailed(false);
					result.setCoverage((int)readCov);
					result.setBothRefs(new Double((bothRefs/readCov)*100) );
					result.setAlt1Only(new Double((alt1Only/readCov)*100) );
					result.setAlt2Only(new Double((alt2Only/readCov)*100) );
					result.setBothAlts(new Double((bothAlts/readCov)*100) );
					result.setMisc(new Double((misc/readCov)*100) );
					
					//Newer version: Use twice the minimum alt frequency
					result.setNewCisFrac(new Double( bothAlts/(bothAlts + 2.0*minAlt) *100));
					result.setNewTransFrac(new Double( 2.0*minAlt/(bothAlts+2.0*minAlt) *100));
					
					
					//Older version
					result.setOldTransFrac(new Double(( (alt1Only+alt2Only)/readCov)*100));
					result.setOldCisFrac(new Double((bothAlts/readCov)*100));
				}
				return result;

	}


}
