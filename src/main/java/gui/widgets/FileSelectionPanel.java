package gui.widgets;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A JPanel with a text field and button that allows users to choose a file
 * @author brendan
 *
 */
public class FileSelectionPanel extends JPanel {
	
	private JLabel label;
	private JTextField field;
	private JButton browseButton;
	private JFileChooser fileChooser;
	private File selectedFile = null;
	
	private List<FileSelectionListener> listeners = new ArrayList<FileSelectionListener>();
	
	public FileSelectionPanel(String labelText, String fieldText, JFileChooser chooser) {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.fileChooser = chooser;
		label = new JLabel(labelText == null ? "" : labelText);
		add(label);
		field = new JTextField(fieldText);
		field.setMinimumSize(new Dimension(150, 10));
		field.setPreferredSize(new Dimension(150, 32));
		field.setMaximumSize(new Dimension(150, 1000));
		field.setEditable(false);
		add(field);
		
		browseButton = new JButton("Choose");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseFile();
			}
		});
		add(browseButton);
	}


	/**
	 * Add a new listener to be notified of file selection events
	 * @param listener
	 */
	public void addListener(FileSelectionListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	/**
	 * Alert all listeners that a new file has been selected
	 */
	private void fireFileSelectionEvent() {
		for(FileSelectionListener listener : listeners) {
			listener.fileSelected(selectedFile);
		}
	}
	
	protected void chooseFile() {
		int x = fileChooser.showOpenDialog(this);
		if (x == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			field.setText(selectedFile.getName());
			fireFileSelectionEvent();
		}
	}
	
	public File getSelectedFile() {
		return selectedFile;
	}
	
	public boolean hasSelectedFile() {
		return selectedFile != null;
	}
	
}
