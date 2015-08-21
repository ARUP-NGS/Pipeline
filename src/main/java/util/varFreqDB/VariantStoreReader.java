package util.varFreqDB;

import java.util.Queue;

import util.varFreqDB.ComputeVarFreqs.SampleInfo;

public class VariantStoreReader implements Runnable {
	final SampleInfo info;
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
			info.getPool();
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
