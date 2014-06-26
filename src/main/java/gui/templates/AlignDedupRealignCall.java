package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;

public class AlignDedupRealignCall extends TemplateConfigurator {


	public AlignDedupRealignCall(PipelineWindow window) {
		super(window);
		generator = new PipelineGenerator( PipelineWindow.getResourceInputStream("templates/align_dedup_realign_call.xml"));

		initComponents();
	}	
}
