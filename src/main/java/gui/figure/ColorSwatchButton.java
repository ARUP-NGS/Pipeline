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


package gui.figure;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;

public class ColorSwatchButton extends JButton implements MouseListener {
	
	public static final String SWATCH_COLOR_CHANGED = "Swatch Color";
	
	Color swatchColor;
	boolean mouseOver;
	
	public ColorSwatchButton(Color col) {
		super();
		swatchColor = col;
		this.addMouseListener(this);
		setPreferredSize(new Dimension(30, 30));
		setMinimumSize(new Dimension(20, 20));
		setText("."); //A dummy placeholder
	}
	
	public void setColor(Color newCol) {
		swatchColor = newCol;
	}
	
	public Color getColor() {
		return swatchColor;
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

			for(int i=0; i<getHeight(); i++) {
				g.setColor(new Color(245-2*i, 245-2*i, 245-2*i));
				g.drawLine(1, getHeight()-i, getWidth(), getHeight()-i);
			}

		if (mouseOver) {
			g2d.setStroke(new BasicStroke(2.0f));
			g.setColor(new Color(100, 100, 100));
			((Graphics2D)g).setStroke(new BasicStroke(1.25f));
		}
		else {
			g2d.setStroke(new BasicStroke(1.0f));
			g.setColor(new Color(180, 180, 180));
		}
		
		g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 5, 10);
		

		g.setColor(Color.LIGHT_GRAY);
		g.drawRect(3, 2, getWidth()-7, getHeight()-5);

		
		g.setColor(swatchColor);
		g.fillRect(4, 3, getWidth()-8, getHeight()-6);
	
	}

	
	public void mouseClicked(MouseEvent arg0) {
		Color prevColor = swatchColor;
		 Color newColor = JColorChooser.showDialog(this, "Choose color", swatchColor);
		 if (newColor != null) {
			 swatchColor = newColor;
			 this.firePropertyChange(SWATCH_COLOR_CHANGED, prevColor, swatchColor);
		 }
		
	}

	public void mouseEntered(MouseEvent e) {
		mouseOver = true;
		repaint();
	}

	public void mouseExited(MouseEvent e) {
		mouseOver = false;
		repaint();
	}


	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
