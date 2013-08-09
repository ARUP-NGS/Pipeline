package alnGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlignmentGenerator {

	protected File referenceFile;
	List<SampleReader> sampleReaders = new ArrayList<SampleReader>();
	
	public AlignmentGenerator(File referenceFile) {
		this.referenceFile = referenceFile;
	}
	
	public void addSampleReader(SampleReader reader) {
		this.sampleReaders.add(reader);
	}
	
	/**
	 * Obtain a list of proto-sequences that 
	 * @param contig
	 * @param startPos
	 * @param endPos
	 * @return
	 * @throws IOException
	 */
	public List<ProtoSequence> getAlignment(String contig, int startPos, int endPos) throws IOException {
		List<ProtoSequence> seqs = new ArrayList<ProtoSequence>();

		FastaReader refReader = new FastaReader(referenceFile);
		StringBuilder refSeq = new StringBuilder();

		Integer intContig = FastaReader.getIntegerTrack(contig);

		for(int i=startPos; i<endPos; i++) {
			char base = refReader.getBaseAt(intContig, i);
			refSeq.append(base);
		}

		System.out.println(">reference");
		System.out.println(refSeq.toString());
		
		for(SampleReader reader : sampleReaders) {
			ProtoSequence seq = new ProtoSequence(refSeq.toString(), startPos);
			seq.setSampleName(reader.getSampleName());
			reader.advanceTo(contig, startPos);
			Variant var = reader.getVariant();
			while(var.getContig().equals("" + contig) && var.getPos() < endPos) {
				System.out.println(var);
				seq.applyVariant(var, reader.getPhase());
				reader.advance();
				var = reader.getVariant();
			}
			seqs.add(seq);
			
			System.out.println(seq.toString());
		}

		return seqs;
	}
	
}
