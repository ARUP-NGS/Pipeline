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


package gui.figure.series;

import gui.figure.Figure;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A figure that displays one or more series of data. These will generally contain an Axes element which actually
 * contains the series to display, a legend, and perhaps some axes labels. Subclasses are specialized to display
 * categorical or x-y data.
 * @author brendan
 *
 */
public abstract class SeriesFigure extends Figure {

	protected ArrayList<SeriesElement> seriesElements;
	
	protected List<SeriesListener> seriesListeners;
	
	public SeriesFigure() {
		seriesListeners = new ArrayList<SeriesListener>();
	}
	
	/**
	 * Add a new listener to be notified of when a series is changed
	 * @param listener
	 */
	public void addSeriesListener(SeriesListener listener) {
		if (!seriesListeners.contains(listener))
			seriesListeners.add(listener);
	}
	
	/**
	 * Remove the given listener from the list of listeners
	 * @param removeMe
	 */
	public void removeSeriesListener(SeriesListener removeMe) {
		seriesListeners.remove(removeMe);
	}
	
	/**
	 * Notify all listeners of a series removal
	 * @param removedSeries
	 */
	protected void fireSeriesRemovedEvent(AbstractSeries removedSeries) {
		for (SeriesListener sl : seriesListeners) {
			sl.seriesRemoved(removedSeries);
		}
	}
	
	/**
	 * Notify all listeners of a change to a series
	 * @param changedSeries
	 */
	protected void fireSeriesChangedEvent(AbstractSeries changedSeries) {
		for (SeriesListener sl : seriesListeners) {
			sl.seriesChanged(changedSeries);
		}
	}
	
	public void removeSeriesByName(String name) {
		//Must remove from both seriesElements and elements
		if (seriesElements == null || seriesElements.size()==0) {
			return;
		}
		
		SeriesElement toRemove = null;
		for(SeriesElement ser : seriesElements) {
			if (ser.getSeries().getName().equals(name))
				toRemove = ser;
		}
		
		if (toRemove != null) {
			seriesElements.remove(toRemove);
		}
		
		elements.remove(toRemove);
		fireSeriesRemovedEvent(toRemove.getSeries());
	}

	
	/**
	 * Clears all the series - removes everything in seriesElements from elements,
	 * then clears seriesElements  
	 */
	public void removeAllSeries() {
		for(SeriesElement ser : seriesElements) {
			elements.remove(ser);
			fireSeriesRemovedEvent(ser.getSeries());
		}		
		seriesElements.clear();
	}
	
	/**
	 * Clears all series elements but does not fire any series removed events 
	 */
	public void removeAllSeriesSilently() {
		for(SeriesElement ser : seriesElements) {
			elements.remove(ser);
		}		
		seriesElements.clear();
	}
	
	public ArrayList<SeriesElement> getSeriesElements() {
		return seriesElements;
	}
	
	/**
	 * Returns the series with the given name
	 * @param name
	 * @return
	 */
	public AbstractSeries getSeries(String name) {
		for(SeriesElement ser : seriesElements) {
			if (ser.getSeries().getName().equals(name))
				return ser.getSeries();
		}
		return null;
	}
	
	/**
	 * Returns true if a series with the given name exists in SeriesElements
	 * @param name
	 * @return
	 */
	public boolean containsSeries(String name) {
		for(SeriesElement ser : seriesElements) {
			if (ser.getSeries().getName().equals(name))
				return true;
		}
		return false;
	}
	
	/**
	 * Remove the series element associated with the given series (also removes element from parent Figure)
	 * @param series
	 */
	public void removeSeries(AbstractSeries series) {
		SeriesElement toRemove = null;
		for(SeriesElement el : seriesElements) {
			if (el.getSeries() == series)
				toRemove = el;
		}
		if (toRemove != null) {
			seriesElements.remove(toRemove);
			elements.remove(toRemove);
			fireSeriesRemovedEvent(toRemove.getSeries());
		}
	}
	
	/**
	 * Return a list of all AbstractSeries used in this figure
	 * @return
	 */
	public ArrayList<AbstractSeries> getAllSeries() {
		ArrayList<AbstractSeries> all = new ArrayList<AbstractSeries>();
		for(SeriesElement ser : seriesElements) {
			all.add(ser.getSeries());
		}
		return all;
	}

	public AbstractSeries getSeries(int which) {
		return seriesElements.get(which).getSeries();
	}
	
	/**
	 * Sets the mode of series to mode, if series is currently associated with the figure. In this
	 * case true is returned. Otherwise, nothing is set and false is returned.
	 * 
	 * @param series The series to set
	 * @param mode The new mode of the series
	 * @return True if series was found, false otw
	 */
	public boolean setSeriesMode(AbstractSeries series, String mode) {
		for(SeriesElement el : seriesElements) {
			if (el.getSeries()==series) {
				el.setMode(mode);
				fireSeriesChangedEvent(series);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the line color of the series, if series is currently associated with the figure. In this
	 * case true is returned. Otherwise, nothing is set and false is returned.
	 * 
	 * @param series The series to set
	 * @param Color the new line color
	 * @return True if series was found, false otw
	 */
	public boolean setSeriesLineColor(AbstractSeries series, Color lineColor) {
		for(SeriesElement el : seriesElements) {
			if (el.getSeries()==series) {
				el.setLineColor(lineColor);
				fireSeriesChangedEvent(series);
				return true;
			}
		}
		return false;
	}
	
	

	/**
	 * Set the line width for the given series 
	 * @param ser Series to set options for
	 * @param width Line width of series
	 */
	public void setSeriesLineWidth(AbstractSeries ser, float width) {
		for(SeriesElement l : seriesElements) {
			if (l.getSeries()==ser) {
				l.setLineWidth(width);
			}
		}
		fireSeriesChangedEvent(ser);
	}
	
}
