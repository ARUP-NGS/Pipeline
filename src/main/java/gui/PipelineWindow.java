package gui;

import gui.templates.TemplateConfigurator;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;

public class PipelineWindow extends JFrame {

	FileSelectionPanel readsOnePanel;
	FileSelectionPanel readsTwoPanel;
	JFileChooser chooser;
	
	public PipelineWindow() {
		super("Pipeliner");
		
		try {
        	String plaf = UIManager.getSystemLookAndFeelClassName();
        	String gtkLookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        	//Attempt to avoid metal look and feel if possible
        	if (plaf.contains("metal")) {

        		UIManager.setLookAndFeel(gtkLookAndFeel);
        	}

        	UIManager.setLookAndFeel( plaf );
		}
        catch (Exception e) {
            System.err.println("Could not set look and feel, exception : " + e.toString());
        }	
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container contentPane = this.getContentPane();
		
		contentPane.setLayout(new BorderLayout());
				
		AnalysisBox analyses = new AnalysisBox(this);
		centerScrollPane = new JScrollPane(analyses);
		centerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
		centerScrollPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		contentPane.add(centerScrollPane, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(new JSeparator(JSeparator.HORIZONTAL));

		contentPane.add(bottomPanel, BorderLayout.SOUTH);
		this.setSize(500, 500);
		this.setPreferredSize(new Dimension(500,400));
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static InputStream getResourceInputStream(String url) {
		InputStream resource = null;
		try {
			URL resourceURL = PipelineWindow.class.getResource(url);
			resource = resourceURL.openStream();
			
		}
		catch (Exception ex) {
			System.out.println("Error loading file from resource " + url + "\n" + ex);
		}
		return resource;
	}
	
	/**
	 * Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static ImageIcon getIcon(String url) {
		ImageIcon icon = null;
		try {
			java.net.URL imageURL = PipelineWindow.class.getResource(url);
			icon = new ImageIcon(imageURL);
		}
		catch (Exception ex) {
			System.out.println("Error loading icon from resource : " + ex);
		}
		return icon;
	}
	
	/**
	 * Initiate a new Pipeline run that executes the given document
	 * @param doc
	 */
	public void beginRun(Document doc) {
		Transformer t;
		try {
			t = TransformerFactory.newInstance().newTransformer();

			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.STANDALONE, "yes");
			//t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");

			t.transform(new DOMSource( doc), new StreamResult(System.out));
			PrintStream outStream;
			try {
				outStream = new PrintStream(new File("pipeline.input.xml"));
				t.transform(new DOMSource(doc), new StreamResult(outStream));
			} catch (FileNotFoundException e) {
				System.err.println("Could not write input to a file, oh well.");
				e.printStackTrace();
			}
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		this.getContentPane().remove(centerScrollPane);
		Pipeline pipe = new Pipeline(doc);
		ProgressPanel progPanel = new ProgressPanel(pipe);
		centerScrollPane = new JScrollPane(progPanel);
		centerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
		centerScrollPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		this.getContentPane().add(centerScrollPane, BorderLayout.CENTER);
		validate();
		repaint();
		progPanel.executePipeline();
	}

	public void showAnalysisConfig(TemplateConfigurator fullAnalysisConfig) {
		
		centerScrollPane.setViewportView(fullAnalysisConfig);
		validate();
		repaint();
	}
	
	
	JScrollPane centerScrollPane;
}
