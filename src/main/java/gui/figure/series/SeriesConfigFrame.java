/********************************************************************
*
* 	Copyright 2011 Brendan O'Fallon
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***********************************************************************/


package gui.figure.series;

import gui.figure.ColorSwatchButton;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A frame that allows the user to adjust some properties of the series. 
 * @author brendan
 *
 */
public class SeriesConfigFrame extends javax.swing.JFrame {

		XYSeriesElement seriesOwner;
		XYSeriesFigure parent;
		
		SeriesOptions initialOptions = null;
		
	    public SeriesConfigFrame(XYSeriesElement ser, XYSeriesFigure parent) {
	    	this.setTitle("Configure series");
	        this.seriesOwner = ser;
	        
	        this.parent = parent;
	        
	        initComponents();
	        setLocationRelativeTo(null);
	    }

	    /**
	     * We store the initial options so that if the user cancels we can restore everything
	     * to its initial state
	     */
	    private void setInitialOptions() {
	    	initialOptions = new SeriesOptions();
	    	initialOptions.name = nameField.getText();
	    	initialOptions.type = (String)styleBox.getSelectedItem();
	    	initialOptions.lineColor = lineColorButton.getColor();
	    	initialOptions.lineWidth = lineWidthSlider.getValue();
	    	initialOptions.markerSize = markerSizeSlider.getValue();
	    	initialOptions.markerType = (String)markerTypeBox.getSelectedItem();
	    }
	    
	    private void restoreInitialOptions() {
	    	seriesOwner.setOptions(initialOptions);	    	
	    }

	    private void initComponents() {
	    	this.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
	        jLabel1 = new javax.swing.JLabel();
	        jLabel2 = new javax.swing.JLabel();
	        nameField = new javax.swing.JTextField();
	        styleBox = new javax.swing.JComboBox();
	        jLabel5 = new javax.swing.JLabel();	        
	        jLabel3 = new javax.swing.JLabel();
	        lineWidthSlider = new javax.swing.JSlider();

	        lineColorButton = new ColorSwatchButton(Color.blue);
	        removeButton = new JButton("Remove");
	        jLabel6 = new javax.swing.JLabel();
	        markerSizeSlider = new javax.swing.JSlider();
	        jLabel7 = new javax.swing.JLabel();
	        markerTypeBox = new javax.swing.JComboBox();
	        cancelButton = new javax.swing.JButton();
	        doneButton = new javax.swing.JButton();

	        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

	        jLabel1.setText("Series Name :");

	        jLabel2.setText("Style :");

	        nameField.setText("jTextField1");

	        styleBox.setModel(new javax.swing.DefaultComboBoxModel(XYSeriesElement.styleTypes));

	        jLabel5.setText("Line color :");

	        lineColorButton.setText("----");

	        jLabel3.setText("Line width :");


	        markerSizeSlider.setMaximum(20);
	        lineWidthSlider.setMaximum(20);

	        jLabel6.setText("Marker size :");

	        jLabel7.setText("Marker type :");

	        String[] markerTypes = XYSeriesElement.markerTypes;
	        markerTypeBox.setModel(new javax.swing.DefaultComboBoxModel(markerTypes));

	        lineWidthSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updateOptions();
				}	        	
	        });
	        
	        markerSizeSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updateOptions();
				}
	        });
	        
	        cancelButton.setText("Cancel");
	        cancelButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                cancelButtonActionPerformed(evt);
	            }
	        });

	        doneButton.setText("Accept"); 
	        doneButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                doneButtonActionPerformed(evt);
	            }
	        });
	        
	        removeButton.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                removeButtonActionPerformed(evt);
	            }
	        });

	        lineColorButton.addActionListener(new java.awt.event.ActionListener() {
	        	public void actionPerformed(java.awt.event.ActionEvent evt) {
	        		chooseLineColorAction();   
	        	}
	        });
	     
	        
	        
	        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
	        getContentPane().setLayout(layout);
	        layout.setHorizontalGroup(
	            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	            .addGroup(layout.createSequentialGroup()
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	                    .addGroup(layout.createSequentialGroup()
	                        .addContainerGap()
	                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
	                            .addComponent(jLabel7)
	                            .addComponent(jLabel2)
	                            .addComponent(jLabel1)
	                            .addComponent(jLabel5)
	                            .addComponent(jLabel3)
	                            .addComponent(jLabel6))
	                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
	                            .addComponent(styleBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	                            .addComponent(nameField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
	                            .addComponent(lineColorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
	                            .addComponent(lineWidthSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
	                            .addComponent(markerSizeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
	                            .addComponent(markerTypeBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
	                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
	                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
	                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
	                        .addComponent(removeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
	                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
	                        .addComponent(doneButton, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)))
	                .addContainerGap())
	        );
	        layout.setVerticalGroup(
	            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	            .addGroup(layout.createSequentialGroup()
	                .addContainerGap()
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
	                    .addComponent(jLabel1)
	                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
	                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	                    .addComponent(jLabel2)
	                    .addComponent(styleBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
	                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
	                    .addComponent(lineColorButton)
	                    .addComponent(jLabel5))
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	                    .addGroup(layout.createSequentialGroup()
	                        .addGap(3, 3, 3)
	                        .addComponent(lineWidthSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
	                    .addGroup(layout.createSequentialGroup()
	                        .addGap(18, 18, 18)
	                        .addComponent(jLabel3)))
	                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
	                    .addComponent(markerSizeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
	                    .addComponent(jLabel6))
	                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
	                    .addComponent(jLabel7)
	                    .addComponent(markerTypeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
	                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
	                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
	                    .addComponent(cancelButton)
	                    .addComponent(removeButton)
	                    .addComponent(doneButton)))
	        );

	        pack();
	        this.getRootPane().setDefaultButton(doneButton);
	    }
	    
	    protected void removeButtonActionPerformed(ActionEvent evt) {
			parent.removeSeries(seriesOwner.getSeries());
			parent.inferBoundsFromCurrentSeries();
			doneButtonActionPerformed(null);
			repaint();
		}


		protected void chooseLineColorAction() {
			Color newColor = JColorChooser.showDialog(this, "Line Color", lineColorButton.getColor());
			lineColorButton.setColor(newColor);
			updateOptions();
			repaint();
		}


		public void display(String name, 
							String style, 
							Color lineColor, 
							int lineWidth, 
							int markerSize, 
							String markerType) {
	    	nameField.setText(name);
	    	
	    	try {
	    		styleBox.setSelectedItem(style);
	    	}
	    	catch (IllegalArgumentException ex) {
	    		System.out.println("Can't set the series style to : " + style);
	    	}
	    	
	    	try {
	    		markerTypeBox.setSelectedItem(markerType);
	    	}
	    	catch (IllegalArgumentException ex) {
	    		System.out.println("Can't set the marker type to : " + markerType);
	    	}
	    	
	    	lineWidthSlider.setValue(lineWidth);
	    	markerSizeSlider.setValue(markerSize);

	    	lineColorButton.setColor(lineColor);
	    	setInitialOptions();
	    	setVisible(true);
	    }

	    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
	    	restoreInitialOptions();
	    	setVisible(false);
	    }

	    private void updateOptions() {
	    	SeriesOptions ops = new SeriesOptions();
	    	ops.name = nameField.getText();
	    	ops.type = (String)styleBox.getSelectedItem();
	    	ops.lineColor = lineColorButton.getColor();
	    	ops.lineWidth = lineWidthSlider.getValue();
	    	ops.markerSize = markerSizeSlider.getValue();
	    	ops.markerType = (String)markerTypeBox.getSelectedItem();
	    	seriesOwner.setOptions(ops);
	    	parent.repaint();
	    }
	    
	    private void doneButtonActionPerformed(java.awt.event.ActionEvent evt) {
	    	updateOptions();
	    	setVisible(false);
	    }

	    
	    class SeriesOptions {
	    	
	    	public String name;
	    	public String type;
	    	public int lineWidth;
	    	public int markerSize;
	    	public String markerType;
	    	public Color lineColor;
	    	
	    	public SeriesOptions() {
	    		
	    	}
	    }

	    private javax.swing.JButton cancelButton;
	    private javax.swing.JButton doneButton;
	    private javax.swing.JButton removeButton;
	    private javax.swing.JLabel jLabel1;
	    private javax.swing.JLabel jLabel2;
	    private javax.swing.JLabel jLabel3;
	    private javax.swing.JLabel jLabel5;
	    private javax.swing.JLabel jLabel6;
	    private javax.swing.JLabel jLabel7;
	    private ColorSwatchButton lineColorButton;
	    private javax.swing.JSlider lineWidthSlider;
	    private javax.swing.JSlider markerSizeSlider;
	    private javax.swing.JComboBox markerTypeBox;
	    private javax.swing.JTextField nameField;
	    private javax.swing.JComboBox styleBox;

}

