package util.varFreqDB;

import java.util.Queue;

import util.varFreqDB.ComputeVarFreqs.SampleInfo;

/**
 * These just provide a Runnable for reading VCFs into VariantPools for a sampleInfo object. For exomes
 * this can take a long time, so it's nice to do a bunch of these in parallel  
 * @author brendan
 *
 */
public class VariantStoreReader implements Runnable {
	final SampleInfo info;
	
	//When the pool is done being read, the sampleInfo object that contains it is pushed onto this Queue. Queue MUST be concurrent-safe!
	final Queue<SampleInfo> finishedQueue; 
	Exception ex = null;
	
	public VariantStoreReader(SampleInfo info, Queue<SampleInfo> queue) {
		this.info = info;
		this.finishedQueue = queue;
	}
	
	@Override
	public void run() {
		System.err.println("Reading pool for " + info.source.getName());
		try {
			info.getPool(); //Loads the vcf file into memory, that's it.
			boolean added = finishedQueue.offer(info);
			while(! added) {
				System.err.println("Queue at capacity, variant pool reading thread for " + info.source.getName() + " is sleeping");
				Thread.sleep(1000);
				added = finishedQueue.offer(info);
			}
			System.err.println("Done reading pool for " + info.source.getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Yikes, errored out reading " + info.source.getName() + " Error: " + e.getLocalizedMessage());
			e.printStackTrace();
			this.ex = e;
		} 
			
	}
	
	public Exception getException() {
		return ex;
	}
	
}
