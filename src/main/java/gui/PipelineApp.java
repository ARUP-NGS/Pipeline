package gui;

import java.awt.EventQueue;

public class PipelineApp {

	static PipelineApp pipelineApp;
	
	protected PipelineWindow window;
	
	public static void showMainWindow() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PipelineWindow window = new PipelineWindow();
					window.setVisible(true);
				} catch (Exception e) {
					System.err.println("Caught exception : " + e);
					e.printStackTrace();
				}
			}
		});
		
	}
	
	private void loadProperties() {
		
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		showMainWindow();
	}
	
}
