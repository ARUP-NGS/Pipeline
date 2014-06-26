package operator.qc;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

public class QCPageWriter {

	String prefix = null;
	
	public QCPageWriter() {
		
	}
	
	public QCPageWriter(String prefix) {
		this.prefix = prefix;
	}
	
	public void writePage(Writer writer, String content) throws IOException {
		writeHeader(writer);
		
		writer.write(content);
		
		writeFooter(writer);
	}
	
	
	private void writeHeader(Writer writer) throws IOException {
		//writer.write("<html>\n <head> \n <link rel=\"stylesheet\" type=\"text/css\" href=\"acg.css\" /> \n	</head> <body> \n");
		writer.write("<!DOCTYPE html>");
		writer.write("<html lang=\"en\">	<head>	<meta charset=\"utf-8\"> <title>Quality metrics</title> \n");
		writer.write("<link rel=\"stylesheet\" href=\"styles/style.css\" />  \n");
		writer.write("<!-- //////// Google Fonts //////// -->	<link rel=\"stylesheet\" type=\"text/css\" href=\"http://fonts.googleapis.com/css?family=Droid+Sans\"> \n");
		writer.write("</head>   <body>   	<div id=\"wrap\">    	\n");
		if (prefix != null && prefix.length() > 0)
			writer.write("<div id=\"header\"> <h1>Quality report for " + prefix + "</h1> </div> <!-- header --> \n");
		else
			writer.write("<div id=\"header\"> <h1>Quality report </h1> </div> <!-- header --> \n");
		
        writer.write("<div id=\"nav\">	<ul> \n");                 	     
        writer.write("<li><a href=\"qc-metrics.html\" title =\"Overview\">Overview</a><span>/</span></li> \n");
        writer.write("<li><a href=\"basequalities.html\" title =\"Base Qualities\">Base qualities</a><span>/</span></li> \n");
        writer.write("<li><a href=\"alignment.html\" title =\"Alignment Metrics\">Alignment metrics</a><span>/</span></li> \n");
        writer.write("<li><a href=\"variants.html\" title =\"Variant Metrics\">Variant Metrics</a><span>/</span></li> \n");
   		writer.write("<li><a href=\"log.html\" title =\"Run metrics\">Full log</a></li> \n");
		writer.write("</ul> </div> <!-- nav --> \n ");
		writer.write("<div id=\"maincontent\"> \n");
	}
	
	
	private void writeFooter(Writer writer) throws IOException {
		writer.write("\n</div> <!--end main content--> \n ");
		writer.write("\n<div id=\"footer\"> ARUP Labs Sequencing Quality Report, generated " + (new Date()).toString() + "</div>\n");
		writer.write("\n</div> <!--end wrap--> \n </body> \n</html>  ");
	}
}
