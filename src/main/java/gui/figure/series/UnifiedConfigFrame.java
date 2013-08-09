package gui.figure.series;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public class UnifiedConfigFrame extends JFrame {

	final XYSeriesFigure fig;
	AutoFieldPanel xMaxPanel = new AutoFieldPanel("Max X:", 1.0);
	AutoFieldPanel xMinPanel = new AutoFieldPanel("Min X:", 0.0);
	AutoFieldPanel yMaxPanel = new AutoFieldPanel("Max Y:", 1.0);
	AutoFieldPanel yMinPanel = new AutoFieldPanel("Min Y:", 0.0);
	AutoFieldPanel xTicksPanel = new AutoFieldPanel("X tick spacing:", 0.2);
	AutoFieldPanel yTicksPanel = new AutoFieldPanel("Y tick spacing:", 0.2);
	JSpinner fontSizeSpinner;
	JCheckBox showXGridBox;
	JCheckBox showYGridBox;
	
	public UnifiedConfigFrame(XYSeriesFigure fig) {
		this.fig = fig; 
		initComponents(); 
		readSettingsFromFigure();
		pack();
		setLocationRelativeTo(fig);
		this.getRootPane().setDefaultButton(doneButton);
		this.setVisible(true);
	}
	
	public void readSettingsFromFigure() {
		xMaxPanel.setFieldValue( fig.getXMax() );
		xMinPanel.setFieldValue( fig.getXMin() );
		yMaxPanel.setFieldValue( fig.getYMax() );
		yMinPanel.setFieldValue( fig.getYMin() );
		
		xMaxPanel.setAutoBox( fig.getAxes().isAutoXMax() );
		xMinPanel.setAutoBox( fig.getAxes().isAutoXMin() );
		yMaxPanel.setAutoBox( fig.getAxes().isAutoYMax() );
		yMinPanel.setAutoBox( fig.getAxes().isAutoYMin() );
		
		xTicksPanel.setFieldValue(fig.getAxes().getXTickSpacing() );
		yTicksPanel.setFieldValue(fig.getAxes().getYTickSpacing() );
		
		if (fig.getAxes().showXGrid())
			showXGridBox.setSelected(true);
		

		if (fig.getAxes().showYGrid())
			showYGridBox.setSelected(true);
		
		int fontSize = fig.getAxes().getFontSize();
		fontSizeSpinner.setValue(fontSize);
		
	}
	
	protected void writeSettingsToFigure() {
		if (showXGridBox.isSelected())
			fig.getAxes().setDrawXGrid(true);
		else
			fig.getAxes().setDrawXGrid(false);

		if (showYGridBox.isSelected())
			fig.getAxes().setDrawYGrid(true);
		else
			fig.getAxes().setDrawYGrid(false);

		
		if (! xMaxPanel.isAutoOn()) {
			fig.getAxes().setXMax( xMaxPanel.getFieldValue() );
			fig.getAxes().setAutoXMax(false);
		}
		else {
			fig.getAxes().setAutoXMax(true);
		}
		
		if (! xMinPanel.isAutoOn()) {
			fig.getAxes().setXMin( xMinPanel.getFieldValue() );
			fig.getAxes().setAutoXMin(false);
		}
		else {
			fig.getAxes().setAutoXMin(true);
		}
		
		
		if (! yMaxPanel.isAutoOn()) {
			fig.getAxes().setYMax( yMaxPanel.getFieldValue() );
			fig.getAxes().setAutoYMax(false);
		}
		else {
			fig.getAxes().setAutoYMax(true);
		}
		
		if (! yMinPanel.isAutoOn()) {
			fig.getAxes().setYMin( yMinPanel.getFieldValue() );
			fig.getAxes().setAutoYMin(false);
		}
		else {
			fig.getAxes().setAutoYMin(true);
		}
	}
	
	protected void cancel() {
		//don't do anything with settigs, they'll be re-written next time 
		this.setVisible(false);
	}


	protected void done() {
		writeSettingsToFigure();
		this.setVisible(false);
	}




	public void initComponents() {
		this.getRootPane().setLayout(new BorderLayout());
		
		JTabbedPane tabPane = new JTabbedPane();
		this.getRootPane().add(tabPane, BorderLayout.CENTER);
		
		///Panel for adjust axes values / ticks
		JPanel axesPanel = new JPanel();
		axesPanel.setLayout(new BoxLayout(axesPanel, BoxLayout.Y_AXIS));
		axesPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
		tabPane.addTab("Axes", axesPanel);
		
		axesPanel.add(Box.createVerticalStrut(20));
		axesPanel.add(xMaxPanel);
		axesPanel.add(xMinPanel);
		axesPanel.add(Box.createVerticalStrut(10));
		axesPanel.add(yMaxPanel);
		axesPanel.add(yMinPanel);
		axesPanel.add(Box.createVerticalStrut(10));
		axesPanel.add(xTicksPanel);
		axesPanel.add(yTicksPanel);
		
		
		
		//Panel for font size, color, and grid lines..
		
		JPanel colorsPanel = new JPanel();
		colorsPanel.setLayout(new BoxLayout(colorsPanel, BoxLayout.Y_AXIS));
		colorsPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
		colorsPanel.add(Box.createVerticalStrut(20));
		tabPane.addTab("Fonts & Colors", colorsPanel);
		
		
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(12, 1, 72, 1);		
		fontSizeSpinner = new JSpinner(spinnerModel);
		JPanel sizePanel = new JPanel();
		sizePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		sizePanel.add(new JLabel("Font size:"));
		sizePanel.add(fontSizeSpinner);
		sizePanel.setAlignmentX(LEFT_ALIGNMENT);
		colorsPanel.add(sizePanel);
		
		showXGridBox = new JCheckBox("Show X grid");
		colorsPanel.add(showXGridBox);
		showXGridBox.setAlignmentX(LEFT_ALIGNMENT);
		
		showYGridBox = new JCheckBox("Show Y grid");
		colorsPanel.add(showYGridBox);
		showYGridBox.setAlignmentX(LEFT_ALIGNMENT);
		colorsPanel.add(Box.createVerticalGlue());
		
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		doneButton = new JButton("Done");
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				done();
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancel();
			}
		});
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(cancelButton);
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(doneButton);
		bottomPanel.add(Box.createHorizontalGlue());
		this.getRootPane().add(bottomPanel, BorderLayout.SOUTH);
		
	}
	
	/**
	 * An oft-used panel with a label, text field, and checkbox
	 * @author brendano
	 *
	 */
	class AutoFieldPanel extends JPanel {
		
		final JTextField field;
		final JCheckBox checkBox;
		final Dimension fieldSize = new Dimension(80, 32);
		
		public AutoFieldPanel(String label, double initVal) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			JLabel lab = new JLabel(label);
			
			field = new JTextField("" + initVal);
			field.setEnabled(false);
			field.setPreferredSize(fieldSize);
			field.setMaximumSize(fieldSize);
			field.setMinimumSize(fieldSize);
			checkBox = new JCheckBox("Auto", true);
			checkBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (checkBox.isSelected())
						field.setEnabled(false);
					else
						field.setEnabled(true);
					repaint();
				}
			});
			
			this.add(lab);
			this.add(field);
			this.add(checkBox);
			
			this.setAlignmentX(RIGHT_ALIGNMENT);
		}
		
		/**
		 * Return a double parsed from the text field
		 */
		public Double getFieldValue() {
			return Double.parseDouble(field.getText());
		}
		
		public void setFieldValue(double val) {
			field.setText("" + val);
		}
		
		public void setAutoBox(boolean autoOn) {
			if (autoOn)
				checkBox.setSelected(true);
			else
				checkBox.setSelected(false);
		}
		
		/**
		 * True if the 'auto' checkbox is selected
		 * 
		 * @return
		 */
		public boolean isAutoOn() {
			return checkBox.isSelected();
		}
	}
	
	private JButton doneButton;
}
