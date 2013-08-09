package gui.widgets;

import java.io.File;

/**
 * Interface for things that listen to file selection events, currently generated only by FileSelectionPanels
 * @author brendan
 *
 */
public interface FileSelectionListener {

	public void fileSelected(File file);
	
}
