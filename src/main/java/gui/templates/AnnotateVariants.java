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

public class AnnotateVariants extends TemplateConfigurator {

	public AnnotateVariants(PipelineWindow window) {
		super(window);

		generator = new PipelineGenerator( PipelineWindow.getResourceInputStream("templates/annotate_variants.xml"));
		initComponents();
	}

	protected void initComponents() {
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
		readsOnePanel = new FileSelectionPanel("Input variants (vcf) file:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("inputVCF", file.getAbsolutePath());
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
				updatePrefix();
				updateReference();
				window.beginRun(generator.getDocument());
			}
		});
		beginButton.setEnabled(false);
		beginButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(beginButton);
	}
	
}
