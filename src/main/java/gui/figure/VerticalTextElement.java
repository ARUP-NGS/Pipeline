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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.JTextField;


/**
 * An element that draws some text vertically. 
 * @author brendan
 *
 */
public class VerticalTextElement extends TextElement {

	AffineTransform transform;
	
	public VerticalTextElement(String txt, Figure parent) {
		super(txt, parent);
		transform = new AffineTransform();
		transform.rotate(Math.PI*1.5);
	}
	
	public Rectangle2D getBounds(Graphics g) {
		Rectangle2D boundaries = new Rectangle2D.Double();

		FontMetrics fm = g.getFontMetrics();
		
		double width = fm.getStringBounds(text, 0, text.length(), g).getWidth()/xFactor; 
		double height = fm.getStringBounds(text, 0, text.length(), g).getHeight()/yFactor;
		boundaries.setFrame(bounds.x, bounds.y, width, height);
		return boundaries;
	}
	
	public void popupConfigureTool(java.awt.Point pos) {
		configureTextField = new JTextField();
		Rectangle textBounds = new Rectangle();
		textBounds.x = round(bounds.x*xFactor);
		textBounds.y = round((bounds.y+bounds.height/2)*yFactor);
		textBounds.width = round(Math.max(80, bounds.height*xFactor));
		textBounds.height = round(Math.max(30, bounds.width*yFactor));
		
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
	
	public void paint(Graphics2D g) {
		if (text.length()==0)
			return;
		
		g.setColor(foregroundColor);
		g.setFont(font);
		
		// Create a rotation transformation for the font.
		
		Font theFont = g.getFont();
		Font theDerivedFont = theFont.deriveFont(transform);
		g.setFont(theDerivedFont);
		
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, 0, text.length(), g);
		bounds.height = (double)stringBounds.getWidth()/yFactor; 
		bounds.width = (double)stringBounds.getHeight()/xFactor;

		if (isSelected()) {
			g.setColor(highlightColor);
			g.setStroke(selectedStroke);
			g.drawRoundRect(toPixelX(0)-2, toPixelY(0)-2, (int)Math.round(bounds.width*xFactor)+4, (int)Math.round(bounds.height*yFactor)+4, 5, 5);
		}
		
		g.setStroke(normalStroke);
		g.setColor(normalColor);
		g.drawString(text, toPixelX(1.0), toPixelY(1.0));
		g.setFont(theFont);
	}

}
