package util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;
import pipeline.Pipeline;
import pipeline.PipelineListener;

/**
 * A listener that keeps track of when each operator starts and ends so we can have a handy
 * summary of how long each one took
 * @author brendan
 *
 */
public class OperatorTimeSummary implements PipelineListener {

	List<TimeRecord> times = new ArrayList<TimeRecord>();
	
	@Override
	public void operatorCompleted(Operator op) {
		TimeRecord rec = getRecordForName(op.getObjectLabel());
		rec.endTime = System.currentTimeMillis();
	}
	
	private TimeRecord getRecordForName(String name) {
		for(TimeRecord rec : times) {
			if (rec.name.equals(name))
				return rec;
		}
		return null;
	}

	public String getSummary() {
		long totalTime = 0;
		for(TimeRecord rec : times) {
			totalTime += rec.endTime-rec.startTime;
		}
		
		StringBuffer buf = new StringBuffer();
		DecimalFormat formatter = new DecimalFormat("00.00");
		for(TimeRecord rec: times) {
			buf.append(rec.name + "\t" + ElapsedTimeFormatter.getElapsedTime(rec.startTime, rec.endTime) + "\t" + formatter.format(100.0* (rec.endTime-rec.startTime)/totalTime) + "\n");
		}
		
		return buf.toString();
	}
	
	@Override
	public void operatorBeginning(Operator op) {
		TimeRecord rec = new TimeRecord();
		rec.name = op.getObjectLabel();
		rec.startTime = System.currentTimeMillis();
		times.add(rec);
	}

	@Override
	public void message(String message) {
		// TODO Auto-generated method stub
		
	}

	
	class TimeRecord {
		String name;
		long startTime;
		long endTime;
	}


	@Override
	public void pipelineFinished() {
		Logger.getLogger(Pipeline.primaryLoggerName).info( getSummary() );
		System.out.println( getSummary() );
	}

	@Override
	public void errorEncountered(OperationFailedException op) {
		// TODO Auto-generated method stub
		
	}
}
