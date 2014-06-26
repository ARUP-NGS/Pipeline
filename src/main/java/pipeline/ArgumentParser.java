/********************************************************************
*
* 	Copyright 2011 Brendan O'Fallon
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***********************************************************************/


package pipeline;

import java.util.HashMap;
import java.util.Map;

/**
 * A tiny class to handle the parsing of an argument list. Very little functionality here 
 * @author brendano
 *
 */
public class ArgumentParser {
	
	//Stores arguments that have been supplied
	Map<String, String> suppliedArgs = new HashMap<String, String>();
	
	public ArgumentParser() {
		//potentialArgs.put("length", Integer.class);
		//potentialArgs.put("frequency", Integer.class);
	}
	
	
	public void parse(String[] args) {
		for(int i=0; i<args.length; i++) {
			if (args[i].startsWith("-")) {
				String key = args[i].substring(1);
				String valStr = "1";
				if (i+1 < args.length) {
					valStr = args[i+1];
				}
				suppliedArgs.put(key, valStr);
			}
		}
	}
	
	
	public Integer getIntegerOp(String key) {
		String op = suppliedArgs.get(key);
		if (op == null)
			return null;
		try {
			Integer opInt = Integer.parseInt(op);
			return opInt;
		}
		catch (NumberFormatException nfe) {
			System.out.println("Could not parse integer value from argument : " + key);
			System.exit(1);
		}
		
		return null;
	}
	
	public String getStringOp(String key) {
		return suppliedArgs.get(key);
	}
	
	
	public Long getLongOp(String key) {
		String op = suppliedArgs.get(key);
		if (op == null)
			return null;
		try {
			Long opL = Long.parseLong(op);
			return opL;
		}
		catch (NumberFormatException nfe) {
			System.out.println("Could not parse value from argument : " + key);
			System.exit(1);
		}
		
		return null;
	}


	public Double getDoubleOp(String key) {
		String op = suppliedArgs.get(key);
		if (op == null)
			return null;
		try {
			Double opD = Double.parseDouble(op);
			return opD;
		}
		catch (NumberFormatException nfe) {
			System.out.println("Could not parse value from argument : " + key);
			System.exit(1);
		}
		
		return null;
	}
}
