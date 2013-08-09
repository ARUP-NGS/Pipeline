package gui.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * A label that runs a timer in the background and periodically updates its text to reflect the time elapsed
 * since 'start' was called
 * @author brendan
 *
 */
public class ElapsedTimeLabel extends JLabel implements ActionListener {

	private long elapsedMillis = 0;
	private long startTime = -1;
	private String prefix = "Time : ";
	private DecimalFormat formatter = new DecimalFormat("00.00");
	private DecimalFormat minutesFormat = new DecimalFormat("00");
	private Timer timer;
	
	public ElapsedTimeLabel() {
		setText(prefix + " 0");
		timer = new Timer(63, this);
		timer.setInitialDelay(0);
	}
	
	public void updateTime() {
		elapsedMillis = System.currentTimeMillis() - startTime;
		double elapsedSeconds = ((double)elapsedMillis / 1000.0) % 60;
		int elapsedMinutes = (int)Math.round( ((double)elapsedMillis / (1000.0*60)) % 60);
		int elapsedHours = (int)Math.round( ((double)elapsedMillis / (1000.0*60*60)) );
		if (elapsedHours > 0) {
			setText(prefix + " " +elapsedHours + ":" + minutesFormat.format(elapsedMinutes) + ": " + formatter.format(elapsedSeconds));
		}
		else {
			setText(prefix + " " + minutesFormat.format(elapsedMinutes) + ": " + formatter.format(elapsedSeconds));
		}
		revalidate();
		repaint();
	}
	
	public void start() {
		if (startTime < 0)
			startTime = System.currentTimeMillis();
		timer.start();
	}
	
	public void stop() {
		timer.stop();
		updateTime();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updateTime();
	}
	

}
