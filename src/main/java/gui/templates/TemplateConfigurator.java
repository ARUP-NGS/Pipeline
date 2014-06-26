package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * User interface allowing configuration of pipeline template using a pipeline-generator. 
 * This is the base class and contains a few useful methods for all such template-configurators
 *  
 * @author brendan
 *
 */
public class TemplateConfigurator extends JPanel {

	protected JComboBox refBox; //Different references to use
	protected String[] refTypes = new String[]{"Human build 37", "Human build 36"};
	
	protected PipelineWindow window;
	protected PipelineGenerator generator;
	
	public TemplateConfigurator(PipelineWindow window) {
		this.window = window;
	}
	
	/**
	 * Inject reference sequence info into template, must happen before run begins
	 */
	protected void updateReference() {
		//TODO get path to resources directory from properties file
		String pathToResourceDirs = "/home/brendan/";
		if (refBox.getSelectedIndex()==0) {
			generator.inject("reference", pathToResourceDirs + "resources/human_g1k_v37.fasta");
		}
		if (refBox.getSelectedIndex()==1) {
			generator.inject("reference", pathToResourceDirs + "resources_b36/human_ref_b36.fasta");
		}
	}
	
	/**
	 * Inject the prefix information into all prefix tags in the xml file
	 */
	protected void updatePrefix() {
		String prefix = prefixField.getText();
		prefix = prefix.replaceAll(" ", "_");
		
		//If prefix does not start with a /, then assume we're talking about paths
		//relative to the execution directory, which is in this case user.dir
		if (! prefix.startsWith("/")) {
			String userDir  = System.getProperty("user.dir");
			prefix = userDir + "/" + prefix;
			System.out.println("Injecting prefix: " + prefix);
		}
		generator.injectMatchingTags(PipelineGenerator.PREFIX_TAG, prefix);
	}
	
	protected void initComponents() {
		//JPanel centerPanel = new JPanel();
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JTextArea descArea = new JTextArea("Description: " + generator.getDescription().replace('\n', ' '));
		descArea.setWrapStyleWord(true);
		descArea.setEditable(false);
		descArea.setOpaque(false);
		descArea.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
		descArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
		descArea.setLineWrap(true);
		descArea.setMaximumSize(new Dimension(1000, 100));
		descArea.setPreferredSize(new Dimension(400, 100));
		add(descArea, BorderLayout.NORTH );
		
		add(Box.createVerticalStrut(20));
		add(Box.createVerticalGlue());
		
		
		JPanel prefixPanel = new JPanel();
		prefixPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		prefixPanel.add(new JLabel("Result file prefix:"));
		prefixField = new JTextField("new_project");
		prefixField.setPreferredSize(new Dimension(200, 32));
		prefixField.setToolTipText("Prefix to use for result files");
		prefixPanel.add(prefixField);
		prefixPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(prefixPanel);
		
		chooser = new JFileChooser( System.getProperty("user.home"));
		readsOnePanel = new FileSelectionPanel("First read set:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("readsOne", file.getAbsolutePath());
					updateBeginButton();
				}
			}
		});
		
		readsTwoPanel = new FileSelectionPanel("Second read set:", "Choose file", chooser);
		readsTwoPanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("readsTwo", file.getAbsolutePath());
					updateBeginButton();
				}
			}
		});
		
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		readsTwoPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(readsOnePanel);
		add(readsTwoPanel);
		
		refBox = new JComboBox(refTypes);
		JPanel refPanel = new JPanel();
		refPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		refPanel.add(new JLabel("Reference:"));
		refPanel.add(refBox);
		refPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(refPanel);
		
		add(Box.createVerticalGlue());
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		add(new JSeparator(JSeparator.HORIZONTAL));
		
		beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePrefix();
				updateReference();
				window.beginRun(generator.getDocument());
			}
		});
		beginButton.setEnabled(false);
		beginButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(beginButton);
	}
	

	/**
	 * Update enabled status of the 'begin' button. It should only be enabled if 
	 * both input files have been selected
	 */
	protected void updateBeginButton() {
		if (readsOnePanel.hasSelectedFile() && readsTwoPanel.hasSelectedFile()) {
			beginButton.setEnabled(true);
		}
		else {
			beginButton.setEnabled(false);
		}
		beginButton.repaint();
	}


	protected JButton beginButton;
	protected FileSelectionPanel readsTwoPanel;
	protected FileSelectionPanel readsOnePanel;
	protected JFileChooser chooser;
	protected JTextField prefixField;
	
}
