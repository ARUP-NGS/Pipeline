package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import pipeline.Pipeline;

public class ValueFilter extends IOOperator {


	public static String FILTER = "filter";
	
	enum Comparison {LESS_THAN, EQUALTO, GREATER_THAN};
	
	public void performOperation() throws OperationFailedException {
		String filterStr = properties.get(FILTER);
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputFile = outputBuffers.get(0).getAbsolutePath();
		
		Filter filter = parseFilter(filterStr);
		Logger.getLogger(Pipeline.primaryLoggerName).info("Filter operator has target:" + filter.target + " comp: " + filter.type + " and filter level: " + filter.filterVal);
		
		//Right now we emit all lines that are 'comments' (which start with '#') and/or
		//lines which contain the expression in 'filter'
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputPath));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			String line = reader.readLine();
			int lineTotal = 0;
			int includedTotal = 0;
			while(line != null) {
				lineTotal++;
				if (line.startsWith("#") || filter.filter(line)) {
					//System.out.println("Line : " + line + " passed filter");
					includedTotal++;
					writer.write(line + "\n");
				}

				line = reader.readLine();
			}
			
			if (includedTotal==0) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Filter excluded ALL lines from output");
			}
			else 
				Logger.getLogger(Pipeline.primaryLoggerName).info("Filter included " + includedTotal + " lines out of " + lineTotal);
			writer.close();
			reader.close();
		} catch (IOException ex) {
			throw new OperationFailedException("Could not open input file " + inputPath + "\n" + ex.getLocalizedMessage(), this);
		}
	}
	
	private Filter parseFilter(String filterStr) {
		int eqIndex = filterStr.indexOf("=");
		int ltIndex = filterStr.indexOf("<");
		int gtIndex = filterStr.indexOf(">");
		int sum = 0;
		if (eqIndex >= 0)
			sum++;
		if (ltIndex >= 0)
			sum++;
		if (gtIndex >= 0)
			sum++;
		if (sum != 1) {
			throw new IllegalArgumentException("Unable to parse expression from filter " + filterStr + " (remember we can't handle <= or >=)");
		}
		
		Comparison filterType = Comparison.EQUALTO;
		int index = -1;
		if (eqIndex>=0) {
			filterType = Comparison.EQUALTO;
			index = eqIndex;
		}
		if (ltIndex>=0) {
			filterType = Comparison.LESS_THAN;
			index = ltIndex;
		}
		if (gtIndex>=0) {
			filterType = Comparison.GREATER_THAN;
			index = gtIndex;
		}
		
		String target = filterStr.substring(0, index).trim();
		String valStr = filterStr.substring(index+1).trim();
		Double val = Double.parseDouble(valStr);
		return new Filter(target, filterType, val);
	}

	/**
	 * Attempt to parse a Double value at the beginnig of this string
	 * @param str
	 * @return
	 */
	protected static Double parseValue(String str) {
		str = str.trim();
		//System.out.println("Trying to parse value from : " + str);
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<str.length(); i++) {
			Character c = str.charAt(i);
			if (Character.isDigit(c) || c.equals('.')) {
				buf.append(c);
			}
			else {
				break;
			}
		}
		
		
		Double val = null;
		try {
			val = Double.parseDouble( buf.toString() );
		}
		catch (NumberFormatException nfe) {
			//Should we throw an error here? Maybe just a warning
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Unable to parse value from first part of string " + str);
		}
		
		return val;
	}
	
	class Filter {
		final String target;
		final Comparison type;
		final Double filterVal;
		
		public Filter(String target, Comparison type, Double filterValue) {
			this.target = target;
			this.type = type;
			this.filterVal = filterValue;
		}
		
		public boolean filter(String expr) {
			
			//SPECIAL CASE ALERT : If target is 'QUAL', we just take the 5th token after breaking up by whitespace
			Double value;
			if (target.equals("QUAL")) {
				String[] toks = expr.split("\\s");
				value = Double.parseDouble(toks[5]);
				//System.out.println("Target is QUAL : parsed quality of : " + value);
			}
			else {
				int index = expr.indexOf(target);
				if (index < 0) {
					return false;
				}
				
				value = parseValue(expr.substring(index+target.length()+1, expr.length()));
			}
			
			if (type==Comparison.LESS_THAN) {
				return value < filterVal;
			}
			if (type==Comparison.EQUALTO) {
				return value == filterVal;
			}
			if (type==Comparison.GREATER_THAN) {
				return value > filterVal;
			}
			
			//Cant ever get here
			return false;
		}
	}
}
