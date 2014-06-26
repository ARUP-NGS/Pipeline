package util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class QueuedLogHandler extends Handler {

	public static int defaultSize = 1000; //Number of records to store
	private List<LogRecord> records = new ArrayList<LogRecord>();
	private int maxSize = defaultSize;
	
	public QueuedLogHandler() {
		//Create a handler with the default size
	}
	
	/**
	 * Creat a handler that will contain at most the given number of records (default 1000)
	 * @param size
	 */
	public QueuedLogHandler(int size) {
		this.maxSize = size;
	}
	

	@Override
	public void publish(LogRecord record) {
		records.add(record);
		
		if (records.size() > maxSize) {
			records.remove(0);
		}
	}
	
	public int getRecordCount() {
		return records.size();
	}
	
	public LogRecord getRecord(int index) {
		return records.get(index);
	}
	
	@Override
	public void flush() {
		//Nothing do to - we don't write to a stream
	}

	@Override
	public void close() throws SecurityException {
		//Nothing do to - we don't write to a stream
	}

}
