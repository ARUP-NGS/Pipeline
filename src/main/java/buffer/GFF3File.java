package buffer;

/**
 * A GFF3 file - see http://www.sequenceontology.org/gff3.shtml for details
 * @author brendan
 *
 */
public class GFF3File extends FileBuffer {

	@Override
	public String getTypeStr() {
		return "GFF3File";
	}

}
