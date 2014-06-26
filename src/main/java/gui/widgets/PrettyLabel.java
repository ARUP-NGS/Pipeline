package gui.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class PrettyLabel extends JPanel {

	public enum TextAlignment {LEFT, CENTER, RIGHT};
	private String text;
	private TextAlignment textAln = TextAlignment.CENTER;
	private Font font = new Font("Sans", Font.PLAIN, 12);
	public static final Color textColor = new Color(0.2f, 0.2f, 0.2f);
	public static final Color shadow = new Color(0.9f, 0.9f, 0.9f, 0.6f);
	
	
	public PrettyLabel(String text) {
		this(text, TextAlignment.CENTER);
	}
	
	public PrettyLabel(String text, TextAlignment textAln) {
		this.text = text;
		this.textAln = textAln;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		int estimatedWidth = (int)Math.round( font.getSize() * text.length() *0.65 );
		add(Box.createRigidArea(new Dimension(estimatedWidth, 30)));
	}
	
	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public void setText(String text) {
		this.text = text;
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);	
		
		g2d.setFont(getFont());
		int strWidth = g2d.getFontMetrics().stringWidth(text);
		int leftEdge = 1;
		int bottomEdge = getHeight()/2 + g2d.getFontMetrics().getHeight()/2+4;
		if (textAln == TextAlignment.LEFT) {
			leftEdge = 1;
		}
		if (textAln == TextAlignment.CENTER) {
			leftEdge = Math.max(1, getWidth()/2 - strWidth/2);
		}
		if (textAln == TextAlignment.RIGHT) {
			leftEdge = Math.max(1, getWidth() - strWidth - 1);
		}
		
		g2d.setColor(shadow);
		g2d.drawString(text, leftEdge+1, bottomEdge+1);
		g2d.setColor(textColor);
		g2d.drawString(text, leftEdge, bottomEdge);
		
	}

	
	
}
