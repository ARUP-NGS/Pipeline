package buffer.variant;

import util.VCFLineParser;

/**
 * Alters the way in which Depth and VariantDepth are computed from the vcf. Briefly, IonTorrent vcfs like
 * to put the depth and variant depth in the FDP and FAO format fields, not the usual DP and AO fields, so 
 * those functions are overridden here.  
 * @author brendan
 *
 */
public class IonTorrentVCFParser extends VCFLineParser {

	
	/**
	 * Read depth from INFO column, tries to identify depth by looking for a DP string, then reading
	 * the following number
	 * @return
	 */
	public Integer getDepth() {
		return getDepthFromInfo();
	}
	
	/**
	 * Depth may appear in format OR INFO fields, this searches the latter for depth
	 * @return
	 */
	public Integer getDepthFromInfo() {
		updateFormatIfNeeded();
		
		
		if (fdpCol < 0)
			return null;
		
		String[] formatValues = lineToks[getSampleColumn()].split(":");
		String dpStr = formatValues[fdpCol];
		return Integer.parseInt(dpStr);
	}
	
	/** 
	 * Returns the depth of the first variant allele, as parsed from the INFO string for this sample
	 * @return
	 */
	public Integer getVariantDepth() {
		return getVariantDepth(0);
	}
	
	/**
	 * Returns the depth of the whichth variant allele, as parsed from the INFO string for this sample
	 * @return
	 */
	public Integer getVariantDepth(int which) {
		updateFormatIfNeeded();
		
		String[] formatValues = lineToks[getSampleColumn()].split(":");
		
		//Confusing logic below to parse var depth (alt depth) from both new (FAO) and old (AD) IonTorrent VCFs
		boolean useAD = false;
		int index = faoCol;
		if (index == -1) {
			index = adCol;
			useAD = true;
		}
		if (index == -1) {
			return null;
		}
		
		String adStr = formatValues[index];
		try {
			String[] depths = adStr.split(",");
			Integer altReadDepth = null;
			if (useAD) {
				altReadDepth = Integer.parseInt(depths[which+1]);
			}
			else {
				altReadDepth = Integer.parseInt(depths[which]);
			}
			return altReadDepth;
			
			
		}
		catch (NumberFormatException ex) {
			System.err.println("Could not parse alt depth from " + adStr);
			return null;
		}
	}
}
