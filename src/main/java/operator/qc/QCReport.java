package operator.qc;

import gene.ExonLookupService;
import gui.figure.FigureFactory;
import gui.figure.heatMapFigure.HeatMapFigure;
import gui.figure.series.XYSeriesFigure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import math.Histogram;
import math.LazyHistogram;
import operator.OperationFailedException;
import operator.Operator;
import operator.qc.checkers.BAMMetricsChecker;
import operator.qc.checkers.CoverageChecker;
import operator.qc.checkers.MQChecker;
import operator.qc.checkers.NoCallChecker;
import operator.qc.checkers.QCItemCheck;
import operator.qc.checkers.QCItemCheck.QCCheckResult;
import operator.qc.checkers.VariantPoolChecker;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.MetaInfo;
import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.ElapsedTimeFormatter;
import util.QueuedLogHandler;
import util.StringWriter;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.DOCMetrics;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Builds a small html report containing some QC information. 
 * @author brendan
 *
 */
public class QCReport extends Operator {

	public static final String QC_STYLE_SHEET = "qc.style.sheet";
	
	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMMetrics rawBAMMetrics = null;
	BAMMetrics finalBAMMetrics = null;
	VariantPool variantPool = null;
	BEDFile captureBed = null;
	BEDFile secondaryCapture = null;
	CSVFile noCallCSV = null;
	File outputDir = null; //Directory containing qc info

	/**
	 * Return directory containing qc output, will be null before report is written
	 * @return
	 */
	public File getOutputDir() {
		return outputDir;
	}
	
	/**
	 * Set the directory to which results will be written. 
	 * Note: This is currently used only by the ReviewDirGenerator,
	 * which moves the QC dir to a new location
	 * @return
	 */
	public void setOutputDir(File newDir) {
		this.outputDir = newDir;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		File homeDir = new File( getProjectHome() );
		if ( (! homeDir.exists()) || (! homeDir.isDirectory()) ) {
			throw new OperationFailedException("Could not open project home directory : " + homeDir.getAbsolutePath(), this);
		}
	
		if (variantPool == null) {
			throw new OperationFailedException("No variant pool specified", this);
		}
		
		if (rawBAMMetrics == null) {
			logger.warning(getObjectLabel() + " Raw BAM Metrics object is null!");
		}
		if (finalBAMMetrics == null) {
			logger.warning(getObjectLabel() + " Final BAM Metrics object is null!");
		}
		logger.info("Creating qc report for raw bam file:" + rawBAMMetrics.path + "\n final BAM: " + finalBAMMetrics.path + " variant pool with:" + variantPool.size() + " variants");
		
		String projHome = getProjectHome();				
		
		outputDir = new File(projHome + "qc-report");
		outputDir.mkdir();
			
		String outputPath = outputDir.getAbsolutePath();
			
		QCPageWriter pageWriter = new QCPageWriter( this.getAttribute("sample"));
			
		//Write summary (index) page
		StringWriter summary = new StringWriter();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/qc-metrics.html"));
			writeSummary(summary, getPipelineOwner());
			pageWriter.writePage(writer, summary.toString());
			writer.close();	
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing  QC summary page : " + rex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing  QC summary page : " + e.getMessage());
		}


		//Write base qualities page...
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/basequalities.html"));
			StringWriter basequalities = new StringWriter();
			writeBaseQualities(basequalities, rawBAMMetrics, outputDir);
			pageWriter.writePage(writer, basequalities.toString());
			writer.close();
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing base quality QC page : " + rex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing base quality QC page : " + e.getMessage());
		}



		//Write alignment metrics page...


		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/alignment.html"));
			StringWriter alnWriter = new StringWriter();
			writeBAMMetricsBlockNew(alnWriter, rawBAMMetrics, rawCoverageMetrics, finalBAMMetrics, finalCoverageMetrics, outputDir);
			pageWriter.writePage(writer, alnWriter.toString());
			writer.close();
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing BAM metrics QC page : " + rex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing BAM metrics QC page : " + e.getMessage());
		}


		//Writer variant report

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/variants.html"));
			StringWriter variants = new StringWriter();
			writeVariantReport(variants, variantPool, outputDir);
			pageWriter.writePage(writer, variants.toString());
			writer.close();
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing variant QC page : " + rex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing variant QC page : " + e.getMessage());
		}
			

			
			
			
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/log.html"));
			StringWriter log = new StringWriter();
			writeLogPage(log, outputDir);
			pageWriter.writePage(writer, log.toString());
			writer.close();
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing log QC page : " + rex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Error writing log QC page : " + e.getMessage());
		}


		//Finally, copy style sheet to directory...
		String styleSheetPath = getPipelineProperty(QC_STYLE_SHEET);

		if (styleSheetPath != null) {

			File styleDir = new File(outputPath + "/styles");
			styleDir.mkdir();

			File styleSheetSrc = new File(styleSheetPath);
			if (! styleSheetSrc.exists()) {
				logger.warning("QC style sheet at path " + styleSheetSrc.getAbsolutePath() + " does not exist");
				return;
			}

			File styleSheetDest = new File(styleDir.getAbsolutePath() + "/style.css");
			try {
				copyFile(styleSheetSrc, styleSheetDest);
			} catch (IOException e) {
				e.printStackTrace();
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Error copying style sheet to destination : " + e.getMessage());
			}
		}


	}
	
	private void writeLogPage(StringWriter writer, File outputDir) throws IOException {
		writer.write("<h2> Complete log </h2>");
		
		Calendar cal = Calendar.getInstance();
		TableWriter table = new TableWriter(3);
		table.setColumnWidth(1, 150);
		QueuedLogHandler logHandler = getPipelineOwner().getLogHandler();
		for(int i=0; i<logHandler.getRecordCount(); i++) {
			LogRecord rec = logHandler.getRecord(i);
			
			cal.setTimeInMillis( rec.getMillis() );
			List<String> recRow = new ArrayList<String>(4);
			
			if (rec.getLevel() == Level.WARNING || (rec.getLevel() == Level.SEVERE)) {
				recRow.add("<div id=\"error\">" + rec.getLevel() + "</div>");
			}
			else {
				recRow.add(rec.getLevel() + "");
			}
			recRow.add((cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR) + "  " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "." + ("" + (cal.get(Calendar.MILLISECOND)/1000.0)).replace("0.",  "") );
			
			recRow.add(rec.getMessage());
				
			table.addRow(recRow);	
		}
		
		writer.write( table.getHTML() );
	}

	private void writeVariantReport(StringWriter writer, VariantPool varPool, File outputDir) throws IOException {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		DecimalFormat smallFormatter = new DecimalFormat("#0.0000");
		String lineSep = System.getProperty("line.separator");
		
		writer.write("<h2> Variant summary </h2>");
		if (captureBed != null) {
			writer.write("<p> Using target regions from file : " + captureBed.getFilename() + "</p>" );
			writer.write("<p> Target region covers : " + captureBed.getExtent() + " sites in " + captureBed.getIntervalCount() + " intervals </p>" );
		}
		
		TableWriter table = new TableWriter(8);
		table.setCellPadding("4");
		table.setID("vartable");
		List<String> row = new ArrayList<String>();
		row.add("");
		row.add("Total");
		row.add("Contigs");
		row.add("SNPs");
		row.add("Insertions");
		row.add("Deletions");
		row.add("Het %");
		row.add("Ts / Tv");
		table.addRow(row);
		
		row.clear();
		row.add("Raw variants");
		row.add("" + varPool.size());
		row.add("" + varPool.getContigCount());
		row.add("" + varPool.countSNPs());
		row.add("" + varPool.countInsertions());
		row.add("" + varPool.countDeletions());
		row.add( formatter.format(varPool.countHeteros() / (double)varPool.size()) );
		row.add( formatter.format(varPool.computeTTRatio()) );
		table.addRow(row);
				
		if (captureBed != null) {
			varPool = varPool.filterByBED(captureBed);
		}
		
		VariantPool novels = new VariantPool();
		VariantPool knowns = new VariantPool();
		for(String contig : varPool.getContigs()) {
			for(VariantRec var : varPool.getVariantsForContig(contig)) {
				Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
				String rsID = var.getPropertyOrAnnotation(VariantRec.RSNUM);
				if ( (freq != null && freq > 1e-5) || (rsID!=null && rsID.length()>2))
					knowns.addRecordNoSort(var);
				else {
					novels.addRecordNoSort(var);
				}
			}
		}
				
	
		if (captureBed != null) {
			row.clear();
			row.add("Variants in target");
			row.add("" + varPool.size());
			row.add("" + varPool.getContigCount());
			row.add("" + varPool.countSNPs());
			row.add("" + varPool.countInsertions());
			row.add("" + varPool.countDeletions());
			row.add( formatter.format(varPool.countHeteros() / (double)varPool.size()) );
			row.add( formatter.format(varPool.computeTTRatio()) );
			table.addRow(row);
		}

		row.clear();
		row.add("Known variants");
		row.add("" + knowns.size());
		row.add("" + knowns.getContigCount());
		row.add("" + knowns.countSNPs());
		row.add("" + knowns.countInsertions());
		row.add("" + knowns.countDeletions());
		row.add( formatter.format(knowns.countHeteros() / (double)knowns.size()) );
		row.add( formatter.format(knowns.computeTTRatio()) );
		table.addRow(row);
		
		
		row.clear();
		row.add("Novel variants");
		row.add("" + novels.size());
		row.add("" + novels.getContigCount());
		row.add("" + novels.countSNPs());
		row.add("" + novels.countInsertions());
		row.add("" + novels.countDeletions());
		row.add( formatter.format(novels.countHeteros() / (double)novels.size()) );
		row.add( formatter.format(novels.computeTTRatio()) );
		table.addRow(row);
		
		writer.write("<div id=\"vartablewrap\">" +  table.getHTML() + "\n</div><!-- table wrapper -->\n");
		
		
		writer.write("<p> Novel variant % : " + formatter.format( 100*novels.size()/(novels.size()+knowns.size()))) ;
		writer.write("<p> Variants per sequenced base : " + smallFormatter.format( (double)(novels.size()+knowns.size()) / (double)captureBed.getExtent() ) ) ;
		
		
		Histogram varDepthHisto = new Histogram(0, 1, 50);
		Histogram knownVarDepthHisto = new Histogram(0, 1, 50);
		Histogram novelVarDepthHisto = new Histogram(0, 1, 50);
		LazyHistogram overallQualHisto = new LazyHistogram(100);
		LazyHistogram snpQualHisto = new LazyHistogram(100);
		LazyHistogram hetQualHisto = new LazyHistogram(100);
		LazyHistogram homoQualHisto = new LazyHistogram(100);
		LazyHistogram indelQualHisto = new LazyHistogram(100);
		
		for(String contig : varPool.getContigs()) {
			for(VariantRec var : varPool.getVariantsForContig(contig)) {
				Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				Double depth = var.getProperty(VariantRec.DEPTH);
				Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
				String rsID = var.getPropertyOrAnnotation(VariantRec.RSNUM);
				
				if (varDepth != null && depth != null) {
					Double frac = varDepth / depth;
					varDepthHisto.addValue(frac);
					
					if ( (freq != null && freq > 1e-5) || (rsID!=null && rsID.length()>2)) {
						knownVarDepthHisto.addValue(frac);
					}
					else {
						novelVarDepthHisto.addValue(frac);
					}
				}
				
				overallQualHisto.addValue(var.getQuality());
				if (var.isSNP()) {
					snpQualHisto.addValue(var.getQuality());
				}
				if (var.isIndel())
					indelQualHisto.addValue(var.getQuality());
				if (var.isHetero()) 
					hetQualHisto.addValue(var.getQuality());
				else {
					homoQualHisto.addValue(var.getQuality());
				}
			}
		}
		
		String figName = "vardepthfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
		String figPath = outputDir.getAbsolutePath() + "/" + figName;
		File destFile = new File(figPath);
		
		System.out.println("Creating var depth histo, orig histo is : " + varDepthHisto.toString());
		//List<Point2D> allData = histoToPointList(varDepthHisto);
		List<Point2D> knownData = histoToPointList(knownVarDepthHisto);
		List<Point2D> novelData = histoToPointList(novelVarDepthHisto);
		
		List< List<Point2D> > dataLists = new ArrayList< List<Point2D>>();
		dataLists.add(knownData);
		dataLists.add(novelData);
		
		List<String> names = Arrays.asList(new String[]{"Known variants", "Novel variants"});
		List<Color> colors = Arrays.asList(new Color[]{Color.blue, Color.red});
		
		
		XYSeriesFigure fig = FigureFactory.createFigure("Variant allele frequency", "Count", dataLists, names, colors);
		
		FigureFactory.saveFigure(new Dimension(500, 500), fig, destFile);
		
		writer.write("<div id=\"separator\">  </div>");
		writer.write("<h2> Distribution of variant depths: " + " </h2>" +lineSep);
		writer.write("<img src=\"" + figName + "\">");
		
		
		List<List<Point2D>> histos = new ArrayList< List<Point2D> >();
		histos.add( histoToPointList( overallQualHisto.getHistogram()) );
		histos.add(histoToPointList( snpQualHisto.getHistogram() ));
		histos.add(histoToPointList( indelQualHisto.getHistogram() ));
		histos.add(histoToPointList( hetQualHisto.getHistogram()));
		histos.add(histoToPointList( homoQualHisto.getHistogram()));
		
		List<String> labels = new ArrayList<String>();
		labels.add("All variants");
		labels.add("SNPs");
		labels.add("Indels");
		labels.add("Heterozygotes");
		labels.add("Homozygotes");
		
		colors = new ArrayList<Color>();
		colors.add(Color.blue);
		colors.add(Color.red);
		colors.add(Color.green);
		colors.add(Color.orange);
		colors.add(Color.magenta);
		
		if (varDepthHisto.getCount() > 0) {
			figName = "varqualfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
			figPath = outputDir.getAbsolutePath() + "/" + figName;
			
			XYSeriesFigure qualityFig = FigureFactory.createFigure("Quality (phred-scaled)", "Frequency", histos, labels, colors);
			qualityFig.getAxes().setXMax(500);
			qualityFig.getAxes().setNumXTicks(5);
			FigureFactory.saveFigure(new Dimension(500, 500), qualityFig, new File(figPath));
			
			writer.write("<div id=\"separator\">  </div>");
			writer.write("<h2> Distribution of variant qualities: " + " </h2>" +lineSep);
			writer.write("<img src=\"" + figName + "\">");
		}
		else {
			writer.write("<p> (No variant depth information found): " + " </p>" +lineSep);
		}
		
	}


	/**
	 * Convert histogram to list of points
	 * @param histo
	 * @return
	 */
	private List<Point2D> histoToPointList(Histogram histo) {
		if (histo == null)
			return new ArrayList<Point2D>();
		
		List<Point2D> data = new ArrayList<Point2D>(histo.getBinCount());
		
		double x = histo.getMin();
		double step = histo.getBinWidth();
		
		for(int i=0; i<histo.getBinCount(); i++) {
			Point2D p = new Point2D.Double(x, histo.getFreq(i));
			data.add(p);
			x+=step;
		}
		
		return data;
	}


	private void writeBaseQualities(StringWriter writer, BAMMetrics metrics, File outputDir) throws IOException {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");

		writer.write("<h2> Raw base qualities </h2>");
		
		if (metrics == null) {
			writer.write("<h2 id=\"error\"> No .bam metrics computed! </h2>");
			return;
		}

		writer.write("<p> Bases with quality > 30 : " + metrics.basesQAbove30 + " ( " + formatter.format(100.0*metrics.basesQAbove30 / metrics.basesRead) + "% )" +" </p>" + lineSep );
		writer.write("<p> Bases with quality > 20 : " + metrics.basesQAbove20 + " ( " + formatter.format(100.0*metrics.basesQAbove20 / metrics.basesRead) + "% )" + " </p>" +lineSep );
		writer.write("<p> Bases with quality > 10 : " + metrics.basesQAbove10 + " ( " + formatter.format(100.0*metrics.basesQAbove10 / metrics.basesRead) + "% )" + " </p>" +lineSep );
		writer.write(" Mean quality :" + formatter.format(metrics.baseQualityHistogram.getMean()) + " </p>" +lineSep);		
		writer.write(" Stdev quality:" + formatter.format(metrics.baseQualityHistogram.getStdev()) + " </p>" +lineSep );
		
		String bqFigStr =  "bqfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
		String bqFigFullPath = outputDir.getAbsolutePath() + "/" + bqFigStr;
				
		Histogram[] histos = metrics.readPosQualHistos;
		
		double[][] heats = new double[histos.length][histos[0].getBinCount()];
		for(int i=0; i<histos.length; i++) {
			Histogram posHist = histos[i];
			if (posHist != null)
				System.arraycopy(posHist.getRawCounts(), 0, heats[i], 0, posHist.getRawCounts().length);
		}
		HeatMapFigure readPosFig = FigureFactory.createFigure("Read position", "Quality", heats);
		FigureFactory.saveFigure(new Dimension(500, 500), readPosFig, new File(bqFigFullPath));
		
		//CreateFigure.generateFigure(getPipelineOwner(), metrics.baseQualityHistogram, "Quality score", "Base qualities", "Frequency", bqFigFullPath);
		writer.write("<div id=\"separator\">  </div>");
		writer.write("<h2> Distribution of base qualities: " + " </h2>" +lineSep);
		writer.write("<img src=\"" + bqFigStr + "\">");
	}


	/**
	 * Write main "qc-metrics.html" page
	 * @param writer
	 * @param ppl
	 * @throws IOException
	 */
	private void writeSummary(Writer writer, Pipeline ppl) throws IOException {
		writer.write("<h2> Pipeline run summary </h2>");
		Date startTime = ppl.getStartTime();
		Date now = new Date();
		String formattedElapsedTime = ElapsedTimeFormatter.getElapsedTime(startTime.getTime(), now.getTime());
		String targetRegions = "none specified";
		if (captureBed != null) {
			targetRegions = captureBed.getAbsolutePath();
		}
		
		TableWriter sumT = new TableWriter(2);
		sumT.setWidth("800");
		
		String lineSep = System.getProperty("line.separator");
		
		sumT.addRow(Arrays.asList(new String[]{"Pipeline version :" , Pipeline.PIPELINE_VERSION } ));
		sumT.addRow(Arrays.asList(new String[]{"Pipeline path: " , MetaInfo.getJarFilePath() } ));
		sumT.addRow(Arrays.asList(new String[]{"Pipeline compile date: " , (new Date(MetaInfo.getManifestModifiedTime())).toString() } ));
		sumT.addRow(Arrays.asList(new String[]{"Pipeline execution start time :" , startTime.toString() } ));
		sumT.addRow(Arrays.asList(new String[]{"Pipeline input file : ", ppl.getSourceFile().getName()} ));
		sumT.addRow(Arrays.asList(new String[]{"Pipeline elapsed time : " , formattedElapsedTime} ));
		sumT.addRow(Arrays.asList(new String[]{"Report creation time : ",(new Date()).toString()} ));
		sumT.addRow(Arrays.asList(new String[]{"Input file modified date : ", new Date(ppl.getSourceFile().lastModified()).toString()} ));
		sumT.addRow(Arrays.asList(new String[]{"Project home directory :" , ppl.getProjectHome() } ));
		sumT.addRow(Arrays.asList(new String[]{"Target regions file: " , targetRegions } ));
		
		writer.write(sumT.getHTML());
		
		
		writeWarningsSection(writer);
		
		writer.write("<h2> Operations performed </h2>");
		List<Operator> opList = ppl.getOperatorList();
		
		//Operator summary table...
		TableWriter opT = new TableWriter(3);
		
		for(Operator op : opList) {
			String timeStr = "?";
			String stateStr = "" + op.getState();
			
			String startStr = op.getAttribute(Pipeline.START_TIME);
			Long startMS = 0L;
			if (startStr != null) {
				startMS = Long.parseLong(startStr);
			}
			String endStr = op.getAttribute(Pipeline.END_TIME);
			Long endMS = 0L;
			if (endStr != null) {
				endMS = Long.parseLong(endStr);
			}
			
			if (endMS > 0)
				timeStr = ElapsedTimeFormatter.getElapsedTime(startMS, endMS);
			
			opT.addRow(Arrays.asList(new String[]{op.getObjectLabel(), timeStr, stateStr } ));
		}
		writer.write( opT.getHTML() );
		
		writer.write("<div id=\"separator\">  </div>");
		writer.write("<h2> Pipeline properties </h2>");
		writer.write("<table border=\"0\" padding=\"5\" width=\"700\"> ");
		for(Object keyObj : ppl.getPropertyKeys()) {
			String key = keyObj.toString();
			Object value = ppl.getProperty(key);
			if (value != null) {
				writer.write(" <tr> ");
				writer.write(" <td> " + key + "</td>" + lineSep);
				writer.write(" <td> " + value.toString() + "</td>" + lineSep);
				writer.write(" </tr> ");
			}
		}
		writer.write("</table>");
	}

	private void writeWarningsSection(Writer writer) throws IOException {		
		writer.write("<div id=\"separator\">  </div>");
		writer.write("<h2> QC flags summary: </h2>");
		writer.write("<ul id=\"warningslist\"> \n");
		
		if (finalCoverageMetrics != null) {
			CoverageChecker covCheck = new CoverageChecker();
			QCCheckResult finalCov = covCheck.checkItem(finalCoverageMetrics);
		
			if (finalCov.getResult() == QCItemCheck.ResultType.WARNING) {
				writer.write("<li id=\"warning\"> QC Warning : " + finalCov.getMessage() + "</li>\n");
			}
			if (finalCov.getResult() == QCItemCheck.ResultType.SEVERE) {
				writer.write("<li id=\"error\"> QC Failure : " + finalCov.getMessage() + "</li>\n");
			}
			if (finalCov.getResult() == QCItemCheck.ResultType.OK) {
				writer.write("<li id=\"okitem\"> QC coverage metrics appear normal </li>\n");
			}
			
		}
		else {
			writer.write("<p id=\"warning\"> QC Warning : No coverage metrics found, could not assess QC metrics</p>\n");
		}
		
		if (finalBAMMetrics != null) {
			MQChecker mqCheck = new MQChecker();
			QCCheckResult mqResult = mqCheck.checkItem(finalBAMMetrics);
			if (mqResult.getResult() == QCItemCheck.ResultType.WARNING) {
				writer.write("<li id=\"warning\"> QC Warning : " + mqResult.getMessage() + "</li>\n");
			}
			if (mqResult.getResult() == QCItemCheck.ResultType.SEVERE) {
				writer.write("<li id=\"error\"> QC Failure : " + mqResult.getMessage() + "</li>\n");
			}
			if (mqResult.getResult() == QCItemCheck.ResultType.OK) {
				writer.write("<li id=\"okitem\"> Read mapping qualities appear normal </li>\n");
			}
			if (mqResult.getResult() == QCItemCheck.ResultType.UNKNOWN) {
				writer.write("<li id=\"warning\"> Unable to assess read mapping qualities </li>\n");
			}
			
			BAMMetricsChecker bamCheck = new BAMMetricsChecker();
			QCCheckResult bamResult = bamCheck.checkItem(finalBAMMetrics);
			
			if (bamResult.getResult() == QCItemCheck.ResultType.WARNING) {
				writer.write("<li id=\"warning\"> QC Warning : " + bamResult.getMessage() + "</li>\n");
			}
			if (bamResult.getResult() == QCItemCheck.ResultType.SEVERE) {
				writer.write("<li id=\"error\"> QC Failure : " + bamResult.getMessage() + "</li>\n");
			}
			if (bamResult.getResult() == QCItemCheck.ResultType.OK) {
				writer.write("<li id=\"okitem\"> QC base qualities appear normal </li>\n");
			}
		}
		else {
			writer.write("<li id=\"warning\"> QC Warning : No BAM metrics found, could not assess QC metrics</li>\n");
		}
		
		if (variantPool != null) {
			VariantPoolChecker varCheck = new VariantPoolChecker(captureBed.getExtent());
			QCCheckResult varResult = varCheck.checkItem(variantPool);
		
			if (varResult.getResult() == QCItemCheck.ResultType.WARNING) {
				writer.write("<li id=\"warning\"> QC Warning : " + varResult.getMessage() + "</li>\n");
			}
			if (varResult.getResult() == QCItemCheck.ResultType.SEVERE) {
				writer.write("<li id=\"error\"> QC Failure : " + varResult.getMessage() + "</li>\n");
			}
			if (varResult.getResult() == QCItemCheck.ResultType.OK) {
				writer.write("<li id=\"okitem\"> QC variant calls appear normal </li>\n");
			}
		}
		else {
			writer.write("<li id=\"warning\"> QC Warning : No variants found, could not assess QC metrics</li>\n");
		}
		
		if (noCallCSV != null) {
			NoCallChecker noCallChecker = new NoCallChecker(captureBed.getExtent());
			QCCheckResult result = noCallChecker.checkItem(noCallCSV);
			
			if (result.getResult() == QCItemCheck.ResultType.WARNING) {
				writer.write("<li id=\"warning\"> QC Warning : " + result.getMessage() + "</li>\n");
			}
			if (result.getResult() == QCItemCheck.ResultType.SEVERE) {
				writer.write("<li id=\"error\"> QC Failure : " + result.getMessage() + "</li>\n");
			}
			if (result.getResult() == QCItemCheck.ResultType.OK) {
				writer.write("<li id=\"okitem\"> QC no-calls appear normal </li>\n");
			}
		}
		else {
			writer.write("<li id=\"warning\"> No 'No-call' information found </li>\n");
		}
		writer.write("</ul> <!-- warningslist --> \n");
	}

	private static String formatPercent(double num, double denom) {
		if (denom == 0) 
			return "N/A";
		else
			return formatter.format(num / denom * 100);
	}


	
	private void writeBAMMetricsBlockNew(Writer writer, 
										BAMMetrics rawMetrics, 
										DOCMetrics rawDOCMetrics,
										BAMMetrics finalMetrics, 
										DOCMetrics finalDOCMetrics,
										 File outputDir) throws IOException {
		
		
		writer.write("<div class=\"bammetrics\">");
		writer.write("<h2> Alignment metrics </h2>");
		
		TableWriter bamT = new TableWriter(4);
		bamT.addRow(new String[]{"", "Raw BAM", "Final BAM", "Final / Raw %"});
		bamT.addRow(new String[]{"Total reads :", "" + rawMetrics.totalReads, "" + finalMetrics.totalReads, formatPercent(finalMetrics.totalReads , rawMetrics.totalReads)});
		bamT.addRow(new String[]{"Unmapped reads :", "" + rawMetrics.unmappedReads, "" + finalMetrics.unmappedReads, formatPercent(finalMetrics.unmappedReads , rawMetrics.unmappedReads )});
		bamT.addRow(new String[]{"Duplicate reads :", "" + rawMetrics.duplicateReads, "" + finalMetrics.duplicateReads, formatPercent(finalMetrics.duplicateReads , rawMetrics.duplicateReads )});
		bamT.addRow(new String[]{"Low vendor quality :", "" + rawMetrics.lowVendorQualityReads, "" + finalMetrics.lowVendorQualityReads, formatPercent(finalMetrics.lowVendorQualityReads , rawMetrics.lowVendorQualityReads )});
		bamT.addRow(new String[]{"Pairs w. insert > 10K :", "" + rawMetrics.hugeInsertSize, "" + finalMetrics.hugeInsertSize, formatPercent(finalMetrics.hugeInsertSize , rawMetrics.hugeInsertSize )});
		writer.write( bamT.getHTML() );
		
		
		
		writer.write("<div id=\"separator\">  </div>");
		writer.write("<h2>  Coverage statistics : " + " </h2>\n");
		if (rawDOCMetrics == null) {
			writer.write("<p id=\"error\">  No coverage information found </p> \n");
		}
		else {
			TableWriter covT = new TableWriter(4);
			covT.addRow(new String[]{"", "Raw BAM", "Final BAM", "Final / Raw %"});
			covT.addRow(new String[]{"Overall mean cov :", "" + rawDOCMetrics.getMeanCoverage(), "" + finalDOCMetrics.getMeanCoverage(), formatPercent(finalDOCMetrics.getMeanCoverage() , rawDOCMetrics.getMeanCoverage() )});
						
			if (finalDOCMetrics.getCutoffs() == null || rawDOCMetrics.getCutoffs() == null) {
				writer.write("<p id=\"error\">  Count not find cutoff values! </p>\n");
			}
			else {
				for(int i=0; i<finalDOCMetrics.getCutoffs().length; i++) {
					double rawVal  = rawDOCMetrics.getFractionAboveCutoff()[i];
					double finalVal  = finalDOCMetrics.getFractionAboveCutoff()[i];
					covT.addRow(new String[]{"% bases with coverage > " + finalDOCMetrics.getCutoffs()[i], "" + rawVal, "" + finalVal,formatPercent(finalVal , rawVal )});
					
				}
			}
			writer.write( covT.getHTML() );
			
			
			//Coverage proportions sections
			if (finalDOCMetrics.getCoverageProportions() == null || rawDOCMetrics.getCoverageProportions() == null) {
				writer.write("<p id=\"error\">  Count not find coverage proportions data </p>\n");
			}
			else {
				//dump proportions to point list..
				List<Point2D> finalCov = new ArrayList<Point2D>();
				double[] covs = finalDOCMetrics.getCoverageProportions();
				for(int i=1; i<Math.min(250, covs.length); i++) {
					Point2D p = new Point2D.Double(i, covs[i]);
					finalCov.add(p);
					//System.out.println("Final cov adding point: " + p.getX() + ", " + p.getY());
				}
				
				List<Point2D> rawCov = new ArrayList<Point2D>();
				covs = rawDOCMetrics.getCoverageProportions();
				for(int i=1; i<Math.min(250, covs.length); i++) {
					Point2D p = new Point2D.Double(i, covs[i]);
					rawCov.add(p);
					//System.out.println("Raw cov adding point: " + p.getX() + ", " + p.getY());
				}
	
				List<List<Point2D>> data = new ArrayList<List<Point2D>>();
				data.add(rawCov);
				data.add(finalCov);
				
				List<String> names = new ArrayList<String>();
				names.add("Raw coverage");
				names.add("Final coverage");
				
				List<Color> colors = new ArrayList<Color>();
				colors.add(Color.blue);
				colors.add(Color.green);
				
				String figStr =  "covfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
				String figFullPath = outputDir.getAbsolutePath() + "/" + figStr;
				File destFile = new File(figFullPath);
				XYSeriesFigure fig = FigureFactory.createFigure("Coverage", "Proportion of bases", data, names, colors); 
				FigureFactory.saveFigure(new Dimension(500, 500), fig, destFile);
			
				writer.write("<div id=\"separator\">  </div>");
				writer.write("<h2> Proportion of bases covered to given depth " + " </h2>\n");
				writer.write("<img src=\"" + figStr + "\">");
				
			}
			
		
			writer.write("<div id=\"separator\">  </div>");
			writer.write("<h2>  Distribution of mapping qualities : " + " </h2>\n");
			if (rawMetrics.mqHistogram != null) {
				XYSeriesFigure mqFig = FigureFactory.createFigure("Mapping quality", "Frequency", histoToPointList(rawMetrics.mqHistogram), "Raw reads", Color.blue); 		
				String mqFigStr =  "mqfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
				String mqFigFullPath = outputDir.getAbsolutePath() + "/" + mqFigStr;

				FigureFactory.saveFigure(new Dimension(500, 500), mqFig, new File(mqFigFullPath));

				writer.write("<img src=\"" + mqFigStr + "\">");
				
				writer.write("<p>  Mean mapping quality: " + rawMetrics.mqHistogram.getMean() + " </p> \n");
				writer.write("<p>  Percentage of reads with mq > 50 : " + formatter.format(100.0-100.0*rawMetrics.mqHistogram.getCumulativeDensity( rawMetrics.mqHistogram.getBin(50.0))) + "% </p> \n");
				writer.write("<p>  Percentage of reads with mq > 30 : " + formatter.format(100.0-100.0*rawMetrics.mqHistogram.getCumulativeDensity( rawMetrics.mqHistogram.getBin(30.0))) + "% </p> \n");
				writer.write("<p>  Percentage of reads with mq > 10 : " + formatter.format(100.0-100.0*rawMetrics.mqHistogram.getCumulativeDensity( rawMetrics.mqHistogram.getBin(10.0))) + "% </p> \n");
			}
			else {
				writer.write("<p id=\"error\">  No mapping quality information found </p> \n");
			}
			
			
			//Emit insert size distribution figure
			writer.write("<h2>  Distribution of insert sizes : " + " </h2>\n");
			
			writer.write("<p>  Mean insert size:" + formatter.format(finalMetrics.insertSizeHistogram.getMean()) + " </p>\n");		
			writer.write("<p>  Stdev insert size:" + formatter.format(finalMetrics.insertSizeHistogram.getStdev()) + " </p>\n");
			writer.write("<p>  Insert size range: " + finalMetrics.insertSizeHistogram.getMinValueAdded() + " - " + finalMetrics.insertSizeHistogram.getMaxValueAdded() + " </p>\n" );

			String figStr =  "insertsizefig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
			String figFullPath = outputDir.getAbsolutePath() + "/" + figStr;
			
			
			//System.out.println("Creating insert size histogram, orig histo is : " + finalMetrics.insertSizeHistogram.toString());
			XYSeriesFigure fig = FigureFactory.createFigure("Insert Size", "Frequency", histoToPointList(finalMetrics.insertSizeHistogram), "All reads", Color.blue); 		
			FigureFactory.saveFigure(new Dimension(500, 500), fig, new File(figFullPath));
		
			writer.write("<img src=\"" + figStr + "\">");				
			writer.write("</div> <!-- bammetrics -->\n");
			
			
			writer.write("<div id=\"separator\">  </div>");
			
			//New version, emit no-call intervals from the CSVFile, if found
			if (noCallCSV == null) {
				writer.write("<p> No call intervals : No information found </p>\n");
			}
			else {
				//Read info from CSV file
				try {
					
					//Use the feature lookup service to find which features correspond to particular low coverage intervals
					ExonLookupService featureLookup = null;
					try {
						featureLookup = new ExonLookupService();
						String featureFile = getPipelineProperty("feature.file");
						featureLookup.buildExonMap(new File(featureFile));
					}
					catch (IOException ex) {
						
						Logger.getLogger(Pipeline.primaryLoggerName).warning("Error opening feature file, can't compute features for low coverage regions. " + ex.getLocalizedMessage());
					}
					
					BufferedReader reader = new BufferedReader(new FileReader(noCallCSV.getAbsolutePath()));
					String line = reader.readLine();
					TableWriter flagT = new TableWriter(4);
					flagT.addRow(Arrays.asList(new String[]{"<b>Interval</b>", "<b>Size</b>", "<b>Cause</b>", "<b>Features affected</b>"}));
					int noCallIntervals = 0;
					int noCallPositions = 0;
					
					List<List<String>> regions = new ArrayList<List<String>>();
					
					while(line != null) {
						String[] toks = line.split(" ");
						if (toks.length == 4) {
							if (! toks[3].equals("CALLABLE")) {
								
								noCallIntervals++;
									try {
										String contig = toks[0];
										long startPos = Long.parseLong(toks[1]);
										long endPos = Long.parseLong(toks[2]);
										long length = endPos - startPos;

										String cause = toks[3];
										cause = cause.toLowerCase();
										cause  = ("" + cause.charAt(0)).toUpperCase() + cause.substring(1);

										String[] features = new String[]{};
										if (featureLookup != null) {
											features = featureLookup.getInfoForRange(contig, (int)startPos, (int)endPos);							
										}
										String featureStr = mergeStrings(features);
										if (length > 1 && (featureStr.contains("exon"))) {
											regions.add(Arrays.asList(new String[]{"chr" + toks[0] + ":" + toks[1] + " - " + toks[2], "" + length, cause, featureStr}) );
										}
										noCallPositions += length;
									} catch (NumberFormatException nfe) {
										//dont stress it
									}
								
							}
						}
						line = reader.readLine();
					}
					
					reader.close();
					
					//We now have a list of all no-call regions with their length and feature information
					//So sort it to prioritize the important ones and then add them to the "flagT" for
					//formatting and output
					Collections.sort(regions, new RegionComparator());
					for(int i=0; i<Math.min(250, regions.size()); i++) {
						flagT.addRow( regions.get(i) );
					}
					
					
					writer.write("<p> No call intervals : " + noCallIntervals + " intervals found spanning " + noCallPositions + " total bases </p>\n");
					writer.write( flagT.getHTML() );
					
					if (noCallIntervals > 250) {
						int excluded = noCallIntervals - 250;
						writer.write("<p><em> " + excluded + " additional no-call intervals not reported. </em></p>\n");
					}
					
				}
				catch(Exception ex) {
					
				}
			}
		}		
		
	}
	

	/**
	 * Merge an array of strings into a single string, removing duplicate entries
	 * and preventing list from getting too long 
	 * @param features
	 * @return
	 */
	 static String mergeStrings(String[] features) {
		if (features.length == 0) {
			return "";
		}
		
		//First remove duplicate entries, which seem to crop up...
		List<String> uniq = new ArrayList<String>();
		for(int i=0; i<features.length; i++) {
			if (! uniq.contains(features[i])) {
				uniq.add(features[i]);
			}
		}
		
		String str = uniq.get(0);
		for(int i=1; i<Math.min(2, uniq.size()); i++) {
			str = str + ", " + uniq.get(i);
		}
		if (uniq.size() > 3) {
			str = str + "...";
		}
		return str;
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					throw new IllegalArgumentException("Please supply a BamMetrics object, not a BAMFile object to the qc report (offending object:" + obj.getObjectLabel() +")");
				}
				
				if (obj instanceof BAMMetrics ) {
					if (rawBAMMetrics == null) {
						rawBAMMetrics = (BAMMetrics) obj;
					}
					else {
						if (finalBAMMetrics == null)
							finalBAMMetrics = (BAMMetrics) obj;
						else
							throw new IllegalArgumentException("Too many BAM metrics objects specified, must be exactly 2");
					}
					
				}
				
				if (obj instanceof DOCMetrics) {
					if (rawCoverageMetrics == null)
						rawCoverageMetrics = (DOCMetrics) obj;
					else {
						finalCoverageMetrics = (DOCMetrics) obj;
					}
				}
				if (obj instanceof VCFFile) {
					throw new IllegalArgumentException("Got a straight-up VCF file as input to QC metrics, this now needs to be a variant pool.");
				}
				if (obj instanceof VariantPool) {
					variantPool = (VariantPool)obj;
				}
				
				if (obj instanceof BEDFile) {
					if (captureBed == null) {
						captureBed = (BEDFile) obj;
					}
					else {
						secondaryCapture = (BEDFile) obj;
					}
				}
				if (obj instanceof CSVFile) {
					noCallCSV = (CSVFile)obj;
				}
				// ?
			}
		}
		
		if (rawBAMMetrics == null) {
			throw new IllegalArgumentException("No raw BAM metrics objects specified");
		}
		
		if (finalBAMMetrics == null) {
			throw new IllegalArgumentException("No final BAM metrics objects specified");
		}
			

	}

	/**
	 * Fancy nio-enabled file copying tool...
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	

	/**
	 * Compares strings of the type produced by the FeatureLookupService to prioritize those
	 * that are in large, exonic regions
	 * @author brendan
	 *
	 */
	class RegionComparator implements Comparator<List<String>> {

		@Override
		public int compare(List<String> s0, List<String> s1) {
		
			int val0 = 0;
			String f0 = s0.get(3);
			if (f0.contains("NR_")) {
				val0 = 1;
			}
			if (f0.contains("NM_")) {
				val0 = 2;
				if (f0.contains("exon")) {
					val0 = 3;
				}
			}
			
			int val1 = 0;
			String f1 = s1.get(3);
			if (f1.contains("NR_")) {
				val1 = 1;
			}
			if (f1.contains("NM_")) {
				val1 = 2;
				if (f1.contains("exon")) {
					val1 = 3;
				}
			}
			
			//If values are equal, compare for length
			if (val1 == val0) {
				try {
					int l1 = Integer.parseInt(s1.get(1));
					int l0 = Integer.parseInt(s0.get(1));
					return l1 -l0;
				}	
				catch(NumberFormatException nfe) {
					//forget it, this only affects the ordering of no call regions 
				}
			
			}
			return val1 - val0;
		}
		
	}
	
	static DecimalFormat formatter = new DecimalFormat("#0.00");
}

