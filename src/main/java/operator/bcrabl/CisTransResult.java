package operator.bcrabl;

import java.io.Serializable;

public class CisTransResult implements Serializable {

	String message;
	boolean failed = false;
	int coverage;
	Double bothRefs;
	Double alt1Only;
	Double alt2Only;
	Double bothAlts;
	Double misc;
	Double transFrac;
	Double cisFrac;
	
	public boolean isFailed() {
		return failed;
	}

	public int getCoverage() {
		return coverage;
	}

	public void setCoverage(int coverage) {
		this.coverage = coverage;
	}
	
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	
	public Double getTransFrac() {
		return transFrac;
	}

	public void setTransFrac(Double transFrac) {
		this.transFrac = transFrac;
	}

	public Double getCisFrac() {
		return cisFrac;
	}

	public void setCisFrac(Double cisFrac) {
		this.cisFrac = cisFrac;
	}

	
	public CisTransResult() {
		//must have no-arg constructor
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Double getBothRefs() {
		return bothRefs;
	}

	public void setBothRefs(Double bothRefs) {
		this.bothRefs = bothRefs;
	}

	public Double getAlt1Only() {
		return alt1Only;
	}

	public void setAlt1Only(Double alt1Only) {
		this.alt1Only = alt1Only;
	}

	public Double getAlt2Only() {
		return alt2Only;
	}

	public void setAlt2Only(Double alt2Only) {
		this.alt2Only = alt2Only;
	}

	public Double getBothAlts() {
		return bothAlts;
	}

	public void setBothAlts(Double bothAlts) {
		this.bothAlts = bothAlts;
	}

	public Double getMisc() {
		return misc;
	}

	public void setMisc(Double misc) {
		this.misc = misc;
	}

	
	
}
