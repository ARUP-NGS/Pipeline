package gui.widgets;

import java.awt.Font;

/**
 * Factory class for 'PrettyLabels'
 * @author brendan
 *
 */
public class LabelFactory {

	public static Font font = new Font("Sans", Font.PLAIN, 14);
	
	public static PrettyLabel makeLabel(String text) {
		return new PrettyLabel(text);
	}
	
	public static PrettyLabel makeLabel(String text, float fontSize) {
		PrettyLabel label = makeLabel(text);
		label.setFont( label.getFont().deriveFont(fontSize) );
		return label;
	}
	
}
