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


package gui.figure;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StringUtils {

	static NumberFormat formatter = new DecimalFormat("0.0###");
	
	public static String format(double val) {
		if (Double.isNaN(val)) {
			return "?";
		}
		
		double absVal = Math.abs(val);
		
		if (absVal>1)
			formatter.setMaximumFractionDigits(1);
		else {
			double log = Math.log10(absVal);
			int dig = -1*(int)Math.round(log)+1; 
			formatter.setMaximumFractionDigits(dig);
		}
		
		
		return formatter.format(val);
	}
	
	public static String format(double val, int digits) {
		double absVal = Math.abs(val);
		
		if (absVal>1)
			formatter.setMaximumFractionDigits(digits);
		else {
			double log = Math.log10(absVal);
			int dig = -1*(int)Math.round(log)+digits; 
			formatter.setMaximumFractionDigits(dig);
		}
		
		
		return formatter.format(val);
	}
	
}
