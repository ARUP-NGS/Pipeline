package gui.widgets;
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

/**
 * A label with button-like functionality but no border
 * @author brendano
 *
 */
public class BorderlessButton extends JPanel {

	ImageIcon icon = null;
	String[] text = null;
	private boolean drawBorder = false;
	private boolean clicking = false;
	
	//Allows nudgeing of image a bit so its in right spot
	private int yDif = 0;
	private int xDif = 0;
	
	//Pixels between icon and text
	private int iconGap = 5;
	private int yStart = 1; //y-position to start drawing text
	

	List<ActionListener> actionListeners = new ArrayList<ActionListener>();
	
	public BorderlessButton(String label) {
		this(label, null);
	}
	
	public BorderlessButton(String label, ImageIcon icon) {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setOpaque(false);
		this.text = label.split("\\n");
		this.icon = icon;
		
		setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
		int pWidth = 0;
		int pHeight = 0;
		if (icon != null) {
			pWidth += icon.getIconWidth()+3;
			pHeight += icon.getIconHeight()+5;
			yStart = pHeight+4;
		}
		if (label != null) {
			pWidth = Math.max(text[0].length()*10, pWidth+3);
			pHeight += 15;
		}
		
		
		setPreferredSize(new Dimension(pWidth, pHeight));
		this.add(Box.createRigidArea(new Dimension(pWidth, pHeight)));
		Listener listener = new Listener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
	}
	
	public BorderlessButton(ImageIcon icon) {
		this(null, icon);
	}
	
	public int getIconGap() {
		return iconGap;
	}

	public void setIconGap(int iconGap) {
		this.iconGap = iconGap;
	}
	
	public void fireActionEvent(MouseEvent me) {
		ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Button pressed");
		for(ActionListener listener : actionListeners ) {
			listener.actionPerformed(evt);
		}
	}
	
	public void addActionListener(ActionListener listener) {
		if (!actionListeners.contains(listener))
			actionListeners.add(listener);
	}
	
	public void removeActionListener(ActionListener listener) {
		actionListeners.remove(listener);
	}
	
	public void setDrawBorder(boolean drawIt) {
		this.drawBorder = drawIt;
		repaint();
	}
	
	class Listener extends MouseInputAdapter {
		
		public void mouseClicked(MouseEvent me) {
			if (isEnabled())
				fireActionEvent(me);
		}
		
		public void mousePressed(MouseEvent me) {
			if (isEnabled()) {
				clicking = true;
				repaint();
			}
		}
		
		public void mouseReleased(MouseEvent me) {
			if (isEnabled()) {
				clicking = false;
				repaint();
			}
		}
		
		public void mouseEntered(MouseEvent me) {
			setDrawBorder(true);
		}
		
		public void mouseExited(MouseEvent me) {
			setDrawBorder(false);
		}
		
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		if (this.isEnabled() && drawBorder) {

			GradientPaint gp;
			if (clicking)
				gp = new GradientPaint(1, 0, new Color(0.79f, 0.79f, 0.79f), 3, getHeight(), new Color(0.88f, 0.88f, 0.88f));
			else
				gp = new GradientPaint(1, 0, new Color(1f, 1f, 1f), 3, getHeight(), new Color(0.88f, 0.88f, 0.88f));
			g2d.setPaint(gp);
			g2d.fillRoundRect(1, 1, getWidth()-2, getHeight()-3, 5, 2);
		}
		else {
			super.paintComponent(g);
		}
		
		int dx = 1;
		if (icon != null) {
			g2d.drawImage(icon.getImage(), Math.max(0, getWidth()/2-icon.getIconWidth()/2), 4 , null);
		}
		if (text != null) {
			g2d.setFont(getFont());
			for(int i=0; i<text.length; i++) {
				int strWidth = g2d.getFontMetrics().stringWidth(text[i]);
				g2d.setColor(new Color(0.99f, 0.99f, 0.99f, 0.4f));
				g2d.drawString(text[i], Math.max(1, getWidth()/2-strWidth/2+1), yStart + (i+1)*14+1 /*getHeight()-(i+1)*13 */);
				g2d.setColor(new Color(0.2f, 0.2f, 0.2f));
				g2d.drawString(text[i], Math.max(0, getWidth()/2-strWidth/2), yStart + (i+1)*14 /*getHeight()-(i+1)*14 */);	
			}

		}
		
		if (this.isEnabled() && drawBorder) {
			g2d.setColor(new Color(0.99f, 0.99f, 0.99f, 0.35f));
			g2d.drawRoundRect(1, 1, getWidth()-2, getHeight()-3, 7, 7);
			
			g2d.setColor(new Color(0.69f, 0.69f, 0.69f, 0.90f));
			g2d.drawRoundRect(0, 0, getWidth()-2, getHeight()-3, 7, 7);
		}
	}


	public int getYDif() {
		return yDif;
	}

	public void setYDif(int yDif) {
		this.yDif = yDif;
	}

	public int getXDif() {
		return xDif;
	}

	public void setXDif(int xDif) {
		this.xDif = xDif;
	}
	
}

