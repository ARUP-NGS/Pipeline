package gui.templates;

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

import gui.PipelineGenerator;
import gui.PipelineWindow;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

public class CallVariants extends TemplateConfigurator {

	public CallVariants(PipelineWindow window) {
		super(window);

		generator = new PipelineGenerator( PipelineWindow.getFileResource("templates/call_variants.xml"));
		initComponents();
	}

	protected void initComponents() {
		//JPanel centerPanel = new JPanel();
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JTextArea descArea = new JTextArea("Description: " + generator.getDescription().replace('\n', ' '));
		descArea.setWrapStyleWord(true);
		descArea.setEditable(false);
		descArea.setOpaque(false);
		descArea.setBorder(BorderFactory.createLineBorder(Color.gray, 1, true));
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
		readsOnePanel = new FileSelectionPanel("Input BAM file:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("inputBAM", file.getAbsolutePath());
					beginButton.setEnabled(true);
					beginButton.repaint();
				}
			}
		});
		
		
		
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(readsOnePanel);
		add(Box.createVerticalGlue());
		
		refBox = new JComboBox(refTypes);
		JPanel refPanel = new JPanel();
		refPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		refPanel.add(new JLabel("<html><b>Reference:</b></html>"));
		refPanel.add(refBox);
		refPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(refPanel);
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		add(new JSeparator(JSeparator.HORIZONTAL));
		
		beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateReference();
				window.beginRun(generator.getDocument());
			}
		});
		beginButton.setEnabled(false);
		beginButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(beginButton);
	}
}
