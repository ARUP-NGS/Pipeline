package gui;

import gui.PipelineOpPanel.State;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import operator.OperationFailedException;
import operator.Operator;
import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;
import pipeline.PipelineListener;

/**
 * A panel that tracks the progress of the pipeline as it moves through the various operators
 * @author brendan
 *
 */
public class ProgressPanel extends JPanel implements PipelineListener {

	protected Pipeline pipeline;
	protected Operator currentOperator = null;
	
	private JProgressBar progressBar;
	
	protected List<PipelineOpPanel> opPanels = new ArrayList<PipelineOpPanel>();
	
	public ProgressPanel(Pipeline pipe) {
		this.pipeline = pipe;
		pipeline.addListener(this);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	public void executePipeline() {
		
		try {
			pipeline.initializePipeline();
			initializePanels();
			
			PipeWorker worker = new PipeWorker(pipeline);
			worker.execute();
		} catch (PipelineDocException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			haltOpPanel();
			ErrorWindow.showErrorWindow(e);
		} catch (ObjectCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			haltOpPanel();
			ErrorWindow.showErrorWindow(e);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			haltOpPanel();
			ErrorWindow.showErrorWindow(ex);
		}
	}
	
	public void haltOpPanel() {
		if (currentOperator != null) {
			PipelineOpPanel panel = panelForOperator(currentOperator);
			if (panel != null) {
				panel.updateStatus(State.Error);
			}
		}
	}
	
	private void initializePanels() {
		int count = 0;
		int total = pipeline.getOperatorList().size();
		for(Operator op : pipeline.getOperatorList()) {
			count++;
			PipelineOpPanel panel = new PipelineOpPanel(this, op, "Step " + count + " of " + total + " : ");
			panel.updateStatus(State.Uninitialized);
			panel.updateTime(0l);
			opPanels.add(panel);
			add(panel);
			add(Box.createVerticalStrut(10));
			//System.out.println("Adding panel : " + op.getObjectLabel() + " " + count + " of " + total);
		}
		revalidate();
		repaint();
	}

	@Override
	public void operatorCompleted(Operator op) {
		PipelineOpPanel panel = panelForOperator(op);
		if (panel != null){
			panel.updateStatus(State.Completed);
		}
	}
	
	private PipelineOpPanel panelForOperator(Operator op) {
		for(PipelineOpPanel panel : opPanels) {
			if (panel.getOperator() == op) {
				return panel;
			}
		}
		return null;
	}

	@Override
	public void operatorBeginning(Operator op) {
		currentOperator = op;
		PipelineOpPanel panel = panelForOperator(op);
		if (panel != null) {
			panel.updateStatus(State.Operating);

			//If we're in a scroll pane, scroll it so panel is visible
			panel.scrollRectToVisible(panel.getBounds());
			panel.getParent().repaint();
			panel.repaint();

		}
	}


	@Override
	public void message(String message) {
		System.out.println("Hmm, just got the message : " + message);
	}
	
	class PipeWorker extends SwingWorker {

		private Pipeline pipeline;
		
		public PipeWorker(Pipeline pipeline) {
			this.pipeline = pipeline;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			
			try {
				pipeline.execute();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
				haltOpPanel();
				ErrorWindow.showErrorWindow(ex);
			}
			
			return null;
		}
		
	}

	@Override
	public void pipelineFinished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void errorEncountered(OperationFailedException opEx) {
		// TODO Auto-generated method stub
		
	}
	
}
