package gui;

import gui.templates.AlignDedupRealignCall;
import gui.templates.AlignReadsDedup;
import gui.templates.AnnotateVariants;
import gui.templates.CallVariants;
import gui.templates.FullAnalysisConfig;
import gui.widgets.BorderlessButton;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

public class AnalysisBox extends JPanel {
	
	public static final Color backgroundColor = new Color(0.9f, 0.9f, 0.9f);
	
	private final PipelineWindow window;
	
	public AnalysisBox(PipelineWindow window) {
		this.window = window;
		setBackground(backgroundColor);
		setLayout(new GridLayout(0, 2, 25, 25));
		
		addAnalysisTypes();
		
	}

	/**
	 * Add a button with an icon for each different analysis type. When a button is clicked, we create a 
	 * new class that is a subclass of TemplateConfigurator, each of which uses a different template that
	 * defines it's pipeline (templates are all stored in the gui.templates package)
	 */
	private void addAnalysisTypes() {
//		BorderlessButton firstType = new BorderlessButton("Test analysis", PipelineWindow.getIcon("icons/pipe_icon2.png"));
//		firstType.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				window.showAnalysisConfig( new FullAnalysisConfig(window) );
//			}
//		});
//		add(firstType);
		
		BorderlessButton alignDedup = new BorderlessButton("Align reads &\n remove duplicates", PipelineWindow.getIcon("icons/pipe_fastq_bam.png"));
		alignDedup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.showAnalysisConfig( new AlignReadsDedup(window) );
			}
		});
		add(alignDedup);
		
		BorderlessButton alignDedupRealignCall = new BorderlessButton("Align, remove dups,\n realign, call variants", PipelineWindow.getIcon("icons/pipe_fastq_vcf.png"));
		alignDedupRealignCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.showAnalysisConfig( new AlignDedupRealignCall(window) );
			}
		});
		add(alignDedupRealignCall);
		
		BorderlessButton callVariants = new BorderlessButton("Remove duplicates,\n realign & call variants", PipelineWindow.getIcon("icons/pipe_bam_vcf.png"));
		callVariants.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.showAnalysisConfig( new CallVariants(window) );
			}
		});
		add(callVariants);
		
		BorderlessButton annotateVariants = new BorderlessButton("Annotate variants", PipelineWindow.getIcon("icons/pipe_bam_vcf.png"));
		annotateVariants.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.showAnalysisConfig( new AnnotateVariants(window) );
			}
		});
		add(annotateVariants);
	}

}
