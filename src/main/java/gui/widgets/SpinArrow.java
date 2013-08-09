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


package gui.widgets;

import gui.PipelineWindow;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class SpinArrow extends JPanel implements MouseListener, ActionListener {

	public static final String SPIN_ARROW_PROPERTY = "spin.arrow.change";
	
	static Image closedHighlight;
	static ImageIcon one;
	static ImageIcon two;
	static ImageIcon three;
	static ImageIcon four;
	static ImageIcon five;
	static ImageIcon six;
	static Image openHighlight;
	static Image[] triArray;
	static boolean initialized = false;
	
	javax.swing.Timer timer;

	int openState = 0;
	boolean open = false;
	
	Image currentImage;
	String text;
	
	public SpinArrow() {
		if (!initialized) {
			initializeImages();
		}

		text = "";
		currentImage = triArray[openState];
		int delay = 40; //milliseconds
		timer = new javax.swing.Timer(delay, this);
		addMouseListener(this);
		this.setMinimumSize(new Dimension(15, 14));
		this.setPreferredSize(new Dimension(15, 16));
		this.setMaximumSize(new Dimension(15, 18));
	}
	
	public SpinArrow(String label) {
		this();
		text = label;
		
		int strWidth = this.getFontMetrics(getFont()).stringWidth(text);
		this.setMinimumSize(new Dimension(15+strWidth+2, 14));
		this.setPreferredSize(new Dimension(15+strWidth+2, 16));
		this.setMaximumSize(new Dimension(15+strWidth+2, 18));
	}
	
	public static ImageIcon getIcon(String url) {
		ImageIcon icon = null;
		try {
			java.net.URL imageURL = SpinArrow.class.getResource(url);
			icon = new ImageIcon(imageURL);
		}
		catch (Exception ex) {
			System.err.println("Error loading spin arrow image : " + ex);
		}
		return icon;
	}
	
	
	private void initializeImages() {
		closedHighlight = PipelineWindow.getIcon("icons/triangle_highlight.png").getImage();
		one = PipelineWindow.getIcon("icons/triangle_1.png");
		two = PipelineWindow.getIcon("icons/triangle_2.png");
		three = PipelineWindow.getIcon("icons/triangle_3.png");
		four = PipelineWindow.getIcon("icons/triangle_4.png");
		five = PipelineWindow.getIcon("icons/triangle_5.png");
		six = PipelineWindow.getIcon("icons/triangle_highlight6.png");
		triArray = new Image[6];
		triArray[0] = one.getImage();
		triArray[1] = two.getImage();
		triArray[2] = three.getImage();
		triArray[3] = four.getImage();
		triArray[4] = five.getImage();
		
		triArray[5] = six.getImage();
		openHighlight = PipelineWindow.getIcon("icons/triangle_highlight6.png").getImage();
		initialized = true;
	}
	
	public void mouseClicked(MouseEvent arg0) {
			timer.start();
	}

	/**
	 * Cause this spin arrow to 'open', which means the arrow points down
	 */
	public void open() {
		if (! isOpen())
		timer.start();
	}
	
	/**
	 * Cause this spinarror to 'close, which means the arrow points to the right.
	 */
	public void close() {
		if (isOpen())
			timer.start();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.drawImage(currentImage, 1, 1, null);
		g.setFont(getFont());
		g.setColor(getForeground());
		int strHeight = getFontMetrics(getFont()).getHeight();
		g.drawString(text, 16, getHeight()-(getHeight()-strHeight)/2-4);
	}
	
	/**
	 * Called by the timer to update the 'open state'
	 */
	public void actionPerformed(ActionEvent arg0) {
		if (! isOpen()) { //We're currently not all the way open, so open more
			openState++;
			currentImage = triArray[openState];
			repaint();
			if (openState == 5) {
				timer.stop();
				open = true;
				firePropertyChange(SPIN_ARROW_PROPERTY, false, true);
			}
		}
		else { //we're currently open, so start closing
			openState--;
			currentImage = triArray[openState];
			repaint();
			if (openState == 0) {
				timer.stop();
				open = false;
				firePropertyChange(SPIN_ARROW_PROPERTY, true, false);
			}
		}
	}
	
	public boolean isOpen() {
		return open;
	}


	public void mouseEntered(MouseEvent arg0) {
		if (isOpen()) {
			currentImage = openHighlight;
			repaint();
		}
		else {
			currentImage = closedHighlight;
			repaint();			
		}
	}

	public void mouseExited(MouseEvent arg0) {
		if (isOpen()) {
			currentImage = openHighlight;
			repaint();
		}
		else {
			currentImage = triArray[0];
			repaint();			
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
