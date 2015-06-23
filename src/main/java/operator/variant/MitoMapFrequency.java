package operator.variant;

import buffer.variant.VariantRec;

public class MitoMapFrequency extends AbstractTabixAnnotator {

	public static final String MITOMAPFREQ_PATH = "mitomap.freq.db.path";
	
	
	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(MITOMAPFREQ_PATH);
	}

	@Override
	public boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) {
		String[] toks = val.split("\t");
		String info = toks[6];
		Double freq = Double.parseDouble(info);
		System.out.println("String val: "+toks[0]+" "+toks[1]+" "+toks[2]+" "+toks[3]+" "+toks[4]+" "+toks[5]+" "+toks[6]);
		System.out.println("VAR: "+var.getContig()+" "+var.getStart()+" "+var.getRef()+" "+var.getAlt());
		//String[] infoTok= info.split(";");
		//System.out.println("info: "+info);
		
		var.addProperty(VariantRec.MITOMAP_FREQ, freq);
		
		return true;
	}



}
