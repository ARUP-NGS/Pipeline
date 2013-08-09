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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import javax.swing.JTextField;


/**
 * A horizontal string on a Figure
 * @author brendan
 *
 */
public class TextElement extends FigureElement {
	
	protected String text = "";
	protected Font font;
	protected JTextField configureTextField;
	
	
	public TextElement(String txt, Figure parent) {
		super(parent);
		font = new Font("Sans", Font.PLAIN, 14);
		this.text = txt;
	}
	
	
	public void setText(String txt) {
		this.text = txt;
	}
	
	public String getText() {
		return text;
	}
	
	public void popupConfigureTool(java.awt.Point pos) {
		configureTextField = new JTextField();
		Rectangle textBounds = new Rectangle();
		textBounds.x = round(bounds.x*xFactor);
		textBounds.y = round(bounds.y*yFactor);
		textBounds.width = round(Math.max(80, bounds.width*xFactor));
		textBounds.height = round(Math.max(30, bounds.height*yFactor));
		
		configureTextField.setBounds(textBounds);
		configureTextField.setText(text);
		parent.add(configureTextField);
		configureTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	doneEditingText();
            }
        });
		configureTextField.setVisible(true);
	}
	
	protected void doneEditingText() {
		configureTextField.setVisible(false);
		parent.remove(configureTextField);
		text = configureTextField.getText();
		parent.repaint();
	}


	public void setFont(Font newFont) {
		this.font = newFont;
	}
	
	public void setFontSize(int size) {
		font = new Font(font.getFamily(), Font.PLAIN, size);
	}
	
	public Rectangle2D getBounds(Graphics g) {
		Rectangle2D boundaries = new Rectangle2D.Double();
		
		FontMetrics fm = g.getFontMetrics();
		
		double width = fm.getStringBounds(text, 0, text.length(), g).getWidth()/xFactor; 
		double height = fm.getStringBounds(text, 0, text.length(), g).getHeight()/yFactor;
		boundaries.setFrame(bounds.x, bounds.y, width, height);
		return boundaries;
	}
	
	public void paint(Graphics2D g) {
		g.setFont(font);
		
	    Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, 0, text.length(), g);
	    
		bounds.width = (double)stringBounds.getWidth()/xFactor; 
		bounds.height = (double)stringBounds.getHeight()/yFactor;

		
		if (isSelected()) {
			g.setColor(highlightColor);
			g.setStroke(selectedStroke);
			g.drawRoundRect(toPixelX(0)-2, toPixelY(0)-2, (int)Math.round(bounds.width*xFactor)+4, (int)Math.round(bounds.height*yFactor)+4, 5, 5);
		}
		
		g.setStroke(normalStroke);
		g.setColor(normalColor);
		g.drawString(text, toPixelX(0), toPixelY(1.0));
				
	}
	
	protected static Stroke selectedStroke = new BasicStroke(1.75f);
	protected static Stroke normalStroke = new BasicStroke(1.0f);
	protected static Color normalColor = new Color(0.1f, 0.1f, 0.1f);
}
