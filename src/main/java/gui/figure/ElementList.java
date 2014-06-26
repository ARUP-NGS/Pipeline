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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Maintains a list of figure elements in ascending z order.
 * This doesn't listen for changes to z-order, so you gotta call resort() 
 * after any z-order changes to ensure things are still sorted. 
 * @author brendan
 *
 */
public class ElementList implements Iterable {

	ArrayList<FigureElement> elements;
	Comparator elementComparator;
	
	public ElementList() {
		elements = new ArrayList<FigureElement>();
		elementComparator = new ElementComparator();
	}
	
	public void add(FigureElement el) {
		elements.add(el);
		Collections.sort(elements, elementComparator);
	}
	
	public void resort() {
		Collections.sort(elements, elementComparator);
	}
	
	public FigureElement get(int index) {
		return elements.get(index);
	}


	public Iterator iterator() {
		return elements.iterator();
	}

	public boolean contains(FigureElement el) {
		return elements.contains(el); 
	}
	
	public void remove(FigureElement toRemove) {
		elements.remove(toRemove);
	}
	
	public void removeAll() {
		elements.clear();
	}
	
	public int size() {
		return elements.size();
	}
	
	/**
	 * Returns true if at least one element is selected
	 */
	public boolean somethingIsSelected() {
		for(FigureElement el : elements) {
			if (el.isSelected())
				return true;
		}
		return false;
	}
	/**
	 * Return the number of currently selected elements
	 * @return
	 */
	public int getNumSelectedElements() {
		int count = 0;
		for(FigureElement el : elements) {
			if (el.isSelected())
				count++;
		}
		return count;
	}
	
	/**
	 * Return references to all selected elements in a list
	 * @return
	 */
	public List<FigureElement> getSelectedElements() {
		ArrayList<FigureElement> list = new ArrayList<FigureElement>();
		for(FigureElement el : elements) {
			if (el.isSelected())
				list.add(el);
		}
		return list;
	}
	
	class ElementComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			if (arg0 instanceof FigureElement && arg1 instanceof FigureElement) {
				return ((FigureElement)arg0).getZPosition() - ((FigureElement)arg1).getZPosition();
			}
			
			return 0;
		}
		
	}

	


}
