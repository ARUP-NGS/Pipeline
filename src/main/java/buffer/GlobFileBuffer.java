package buffer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;

/**
 * This MultiFileBuffer contains all files matching the regexp given in the filename attribute
 * For instance, if filename="*.bam" is an attribute, this thing will contain all files in PROJECT_HOME
 * that end in .bam
 * @author brendan
 *
 */
public class GlobFileBuffer extends MultiFileBuffer {

	private boolean guessContig = true; //Attempt to associate contig numbers with input files
	private FileMatcher fileMatcher = null; //Object that will look for files matching the pattern provided
	
	
	/**
	 * During initialization we determine the file pattern to be matched, but we only
	 * actually look for files when this method gets called..
	 */
	public void findFiles() {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		File[] listing = fileMatcher.parent.listFiles(fileMatcher);	
		
		String warnAttr = this.getAttribute("warn");
		boolean warn = true;
		if (warnAttr != null)
			warn = Boolean.parseBoolean(warnAttr);
		
		if (warn && (listing == null || listing.length == 0)) {
			logger.warning("GlobFileBuffer with pattern " + fileMatcher.pattern + " matched zero files!");
		}
		
		for(int i=0; i<listing.length; i++) {
			FileBuffer buffer = FileTypeGuesser.GuessFileType( listing[i]);
			if (guessContig) {
				String contig = guessContig(buffer);
				buffer.setContig(contig);
			}
			addFile( buffer );
		}
	}
	
	@Override
	public void initialize(NodeList children) {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		String guessAttr = properties.get(GUESS_CONTIG);
		if (guessAttr != null) {
			guessContig = Boolean.parseBoolean(guessAttr);
		}
		
		final String pattern = properties.get(FILENAME_ATTR);
		File parent;
		if (pattern.startsWith("/")) {
			parent = (new File(pattern)).getParentFile();
		}
		else
			parent = new File( this.getProjectHome() );
		
		
		String patternStr = pattern.substring( pattern.lastIndexOf("/")+1 );
		
		logger.info("Initializing GlobFileBuffer with parent file: " + parent.getAbsolutePath() + " and pattern : " + patternStr);
		
		fileMatcher = new FileMatcher(parent, Pattern.compile(patternStr));
		findFiles();
	}

	
	public class FileMatcher implements FilenameFilter {
		
		Pattern pattern;
		File parent;
		
		public FileMatcher(File parent, Pattern pattern) {
			this.pattern = pattern;
			this.parent = parent;
		}
		
		@Override
		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
			return matcher.matches();
		}
		
	}

}
