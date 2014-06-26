package viewer;

import java.awt.EventQueue;

import javax.swing.JApplet;
import javax.swing.JLabel;


public class ViewerTest extends JApplet {

	public void init() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JLabel lab = new JLabel("Hello");
					add(lab);
					
				} catch (Exception e) {
					System.err.println("Caught exception : " + e);
					e.printStackTrace();
				}
			}
		});
	}
}
