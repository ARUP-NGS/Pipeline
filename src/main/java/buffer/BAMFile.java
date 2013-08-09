package buffer;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;

public class BAMFile extends FileBuffer implements HasMD5 {

	//Stores MD5Sum when computed
	protected String md = null;
	
	public BAMFile() {
		//blank on purpose
	}
	
	public BAMFile(File file) {
		super(file);
	}
	
	public BAMFile(File file, String contig) {
		super(file);
		setContig(contig);
	}
	
	public boolean isBinary() {
		return true;
	}
	
	
	
	@Override
	public String getTypeStr() {
		return "BAMFile";
	}

	@Override
	public String getMD5() {
		if (md == null) {
			computeMD5Sum();
		}
		return md;
	}

	private void computeMD5Sum() {
		if (getFile() == null || (!getFile().exists())) {
			setMD5Sum(null);
			return;
		}
		
		final SAMFileReader inputSam = new SAMFileReader(getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		MessageDigest digestor = null; 
		try {
			digestor = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			//This would be very weird and rare....
			setMD5Sum(null);
			return;
		}
		
		for (final SAMRecord samRecord : inputSam) {
			digestor.update( samRecord.getSAMString().getBytes() );
		}
		
		String mdStr = new String(digestor.digest()); 
		setMD5Sum(mdStr);
	}
	
	public void setMD5Sum(String sum) {
		this.md = sum;
	}

}
