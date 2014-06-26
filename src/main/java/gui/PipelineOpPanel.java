package gui;

import gui.widgets.ElapsedTimeLabel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.BevelBorder;

import operator.Operator;

public class PipelineOpPanel extends JPanel {

	public static enum State {Uninitialized, Operating, Completed, Error};
	
	protected final ProgressPanel parentPanel;
	protected final Operator operator;
	
	private JLabel opNameLabel;
	private JLabel statusLabel;
	private ElapsedTimeLabel timeLabel;
	private JProgressBar progressBar;
	
	
	public PipelineOpPanel(ProgressPanel parentPanel, Operator op, String prefix) {
		this.parentPanel = parentPanel;
		this.operator = op;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setOpaque(false);
		this.setBorder(BorderFactory.createEmptyBorder(8,6,7,6));
		
		opNameLabel = new JLabel("<html><b>" + prefix + " " + op.getObjectLabel() + "</b></html>");
		opNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(opNameLabel);
		add(Box.createVerticalStrut(4));
		
		statusLabel = new JLabel("Status : Uninitialized");
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(statusLabel);
		
		timeLabel = new ElapsedTimeLabel();
		timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(timeLabel);
		
		progressBar = new JProgressBar();
		progressBar.setValue(0);
		add(Box.createVerticalStrut(6));
		add(progressBar);
		this.setPreferredSize(new Dimension(200, 100));
	}
	
	public void updateStatus(State newState) {
		switch (newState) {
		case Uninitialized : 
			statusLabel.setText("Status : Uninitialized");
			break;
		case Operating :
				statusLabel.setText("Status : Operating");
				progressBar.setIndeterminate(true);
				timeLabel.start();
				break;
		case Completed :
			statusLabel.setText("Status : Completed");
			progressBar.setIndeterminate(false);
			progressBar.setValue(100);
			timeLabel.stop();
			break;
		case Error : 
			statusLabel.setText("Status : Error");
			progressBar.setIndeterminate(false);
			progressBar.setValue(50);
			timeLabel.stop();
			break;
		}
		statusLabel.revalidate();
		statusLabel.repaint();
	}
	
	public void updateTime(Long elapsedMs) {
		//TODO Format it nicely...
		timeLabel.setText("Time : " + elapsedMs);
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2d.setColor(new Color(0.99f, 0.99f, 0.99f, 0.35f));
		g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-3, 7, 7);
		
		g2d.setColor(new Color(0.69f, 0.69f, 0.69f, 0.90f));
		g2d.drawRoundRect(0, 0, getWidth()-2, getHeight()-3, 7, 7);
	}
}
