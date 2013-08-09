package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;

public class AlignReadsDedup extends TemplateConfigurator {

	
	public AlignReadsDedup(PipelineWindow window) {
		super(window);
		generator = new PipelineGenerator( PipelineWindow.getResourceInputStream("templates/align_dedup.xml"));

		initComponents();
	}


}
