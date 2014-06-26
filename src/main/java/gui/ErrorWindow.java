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


package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import gui.widgets.SpinArrow;

public class ErrorWindow extends JFrame implements PropertyChangeListener {

		JPanel mainPanel;
		JLabel mainMessageLabel;
		JButton okButton;
		JPanel cardPanel;
		JPanel emptyPanel;
		JPanel fillerPanel;
		
		JPanel stackPanel;
		JScrollPane stackScrollPane;
		JTextArea stackTA;
		SpinArrow showButton;
		
		Font stackFont = new Font("Sans", Font.PLAIN, 11);
		Dimension textAreaSize = new Dimension(300, 200);
		
		public ErrorWindow(Exception e) {
			super("Error");
			setLayout(new BorderLayout());
			mainPanel = new JPanel();

			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 4));
			this.add(mainPanel);
			mainPanel.add(Box.createVerticalStrut(10));
			mainMessageLabel = new JLabel("Sorry, but an error occurred, and it may not be possible to complete this operation");
			mainMessageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			mainPanel.add(mainMessageLabel);
			mainPanel.add(Box.createVerticalStrut(10));

			JLabel messageLabel = new JLabel("<html><strong>Message</strong><html> : " + e.getLocalizedMessage());
			messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			mainPanel.add(messageLabel);

			String exStr = e.getClass().toString();
			if (exStr.contains("."))	
				exStr = exStr.substring(exStr.lastIndexOf(".")+1);
			JLabel typeLabel = new JLabel("<html><strong>Type</strong><html> : " + exStr);
			typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

			mainPanel.add(typeLabel);
			
			showButton = new SpinArrow(" Show details ");
			showButton.addPropertyChangeListener(this);
			showButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			fillerPanel = new JPanel();
			fillerPanel.setLayout(new BoxLayout(fillerPanel, BoxLayout.PAGE_AXIS));
			fillerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

			stackPanel = new JPanel();
			stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.PAGE_AXIS));
			stackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			stackTA = new JTextArea();
			stackTA.setFont(stackFont);
			stackTA.setEditable(false);
			for(int i=0; i<e.getStackTrace().length; i++) {
				stackTA.append( e.getStackTrace()[i].toString() + "\n");	
			}
			stackScrollPane = new JScrollPane(stackTA);
			stackPanel.add(stackScrollPane);
			stackScrollPane.setPreferredSize(textAreaSize);
			stackScrollPane.setMaximumSize(new Dimension(1000, 200));
			stackScrollPane.setBorder(BorderFactory.createEmptyBorder());
			stackScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
			mainPanel.add(Box.createVerticalStrut(10));
			mainPanel.add(showButton);
			mainPanel.add(fillerPanel);
			
			okButton = new JButton("  OK  ");		

			mainPanel.add(Box.createVerticalStrut(10));
			mainPanel.add(okButton);
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					okButtonClicked();
				}
			});
			
			pack();
		}

		
		protected void showButtonPressed() {
			fillerPanel.add(stackScrollPane);
			fillerPanel.setPreferredSize(textAreaSize);
			pack();
			repaint();
		}

		public void okButtonClicked() {
			setVisible(false);
		}
		
		/**
		 * Pop open an error window and write the exception data to the specified logger
		 * @param e
		 * @param logger
		 */
		public static void showErrorWindow(Exception e, Logger logger) {
			ErrorWindow window = new ErrorWindow(e);
			window.setLocationRelativeTo(null);
			window.setVisible(true);	
			if (logger!=null)
				logger.warning(e.getMessage());
		}
		
		public static void showErrorWindow(Exception e, String extraMessage) {
			ErrorWindow window = new ErrorWindow(e);
			window.mainMessageLabel.setText(extraMessage);
			window.setLocationRelativeTo(null);
			window.setVisible(true);	
		}
		
		/**
		 * Pop open an error window and write the exception to the usual logger
		 * @param e
		 */
		public static void showErrorWindow(Exception e) {
			showErrorWindow(e, "Sorry, but an error occurred :");	
		}


		public void propertyChange(PropertyChangeEvent propEvt) {
			if (propEvt.getPropertyName().equals(SpinArrow.SPIN_ARROW_PROPERTY) && ((Boolean)propEvt.getOldValue())==false) {
				showButtonPressed();
			}
			
		}
}

