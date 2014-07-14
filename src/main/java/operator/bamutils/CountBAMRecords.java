package operator.bamutils;

import java.io.File;

import net.sf.samtools.AbstractBAMFileIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.SAMFileReader;
import buffer.FileBuffer;

/*
 * Counts the number of BAM records in a given file. Can be rewritten to count the number of aligned, of aligned, etc.
 */

public class CountBAMRecords {

    public long CountRecords(FileBuffer inputBam) {
        File bamFile = inputBam.getFile();
 
        SAMFileReader sam = new SAMFileReader(bamFile,
                                 new File(bamFile.getAbsolutePath() + ".bai"));
 
        AbstractBAMFileIndex index = (AbstractBAMFileIndex) sam.getIndex();
 
        int count = 0;
        for (int i = 0; i < index.getNumberOfReferences(); i++) {
            BAMIndexMetaData meta = index.getMetaData(i);
            count += meta.getAlignedRecordCount();
            count += meta.getUnalignedRecordCount();
        }
        sam.close();
        return count;
    }
}