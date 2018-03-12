package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;                  
                                        
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import util.Interval;
import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;
/**
 * This is (well, should be) the base class for all annotators that read a Tabix-ed
 * vcf file to get their annotation info. This handles several important functions such 
 * as creation of the TabixReader andnormalization of variants that are read in from the tabix.
 *
 *      support mulitple ALT alleles.
 * as creation of the TabixReader and normalization of variants that are read in from the tabix.
 * 
 * @author brendan
 *
 */
public abstract class AbstractTabixAnnotator extends Annotator {
        public static final String THREAD_KEY = "threads";	


        protected abstract String getPathToTabixedFile();

	/**
	 * Subclasses should override this method to actually perform the annotations.
	 * The VariantRec is the variant to annotate, and the 'line' argument is the information
	 * we get from the VCF. Usually, annotators will extract some information from the line 
	 * (like allele frequency, dbSNP ids, etc. etc) and turn that into an annotation or property 
	 * for the VariantToAnnotate
	 * @param
	 * Subclasses should implement logic to handle the current altIndex given, as this changes from annotator to
	 * annotator. For now most annotators ignore the altIndex.
	 * @param var
	 * @param vcfLine
	 * @param altIndex
	 */

	protected abstract boolean addAnnotationsFromString(VariantRec variantToAnnotate, String vcfLine, int altIndex) throws OperationFailedException;


	/**
	 * This overrides the 'prepare' method in the base Annotator class. It is always called 
	 * prior to the first call to annotateVariant, and gives us a chance to do a little
	 * initialization. 
	 */
        @Override
	protected void prepare() throws OperationFailedException {
	}


	/**
	 *
	 * if the reference Alt is multiallelic it will throw error.  Databases should be normilized priot to use with the
	 * AbstractTabixAnnotator class
	 *
	 * @param referenceAlt
	 * @throws OperationFailedException
	 */
	public void checkVariant(String referenceAlt) throws OperationFailedException {
		if (referenceAlt.contains(",")) {
			throw new OperationFailedException("The database contains multiple ALT alleles on a single line.  It should be normalized prior to use.", this);
		}
	}

	/**
	 * Parses variants from the given VCF line (appropriately handling multiple alts) and compare each variant tot he
	 * 'varToAnnotate'. If a perfect match (including both chr, pos, ref, and alt) 
	 * @param varToAnnotate
	 * @param vcfLine
	 * @return
	 * @throws OperationFailedException 
	 */
	protected int findMatchingVariant(VariantRec varToAnnotate, String vcfLine) throws OperationFailedException {
		String[] toks = vcfLine.split("\t");
		String[] alts = toks[4].split(",");
		
		for(int i=0; i<alts.length; i++) {
			VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1])+toks[3].length(), toks[3], alts[i]);
			queryResultVar = VCFParser.normalizeVariant(queryResultVar);

			if (queryResultVar.getContig().equals(varToAnnotate.getContig())
					&& queryResultVar.getStart() == varToAnnotate.getStart()
					&& queryResultVar.getRef().equals(varToAnnotate.getRef())
					&& queryResultVar.getAlt().equals(varToAnnotate.getAlt())) { //change to loop through all alts

				//Everything looks good, so go ahead and annotate		
				boolean ok = addAnnotationsFromString(varToAnnotate, vcfLine, i);
				if (ok) {
					return i;
				}
			} //if perfect variant match

		}//Loop over alts	
		return -1;
	}

        /**
         * This method won't be used by TabixedAnnotators at all.
         */
        public final void annotateVariant(VariantRec var) throws OperationFailedException {}
 
        
	/**
	 * This actually annotates the variant - it performs new tabix query, then converts the
	 * result to a normalized VariantRec, then sees if the normalized VariantRec matches the
	 * variant we want to annotate. 
	 */
	public void annotateVariant(VariantRec varToAnnotate, TabixReader reader) throws OperationFailedException {

		String contig = varToAnnotate.getContig();
		Integer pos = varToAnnotate.getStart();


		String queryStr = contig + ":" + (pos-10) + "-" + (pos+10);

		//Perform the lookup
		
		TabixReader.Iterator iter = null;
		
		try {
			iter = reader.query(queryStr);
		} catch (RuntimeException ex) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Exception during tabix reading for query: " + queryStr + " : " + ex.getLocalizedMessage());
		}

		if(iter != null) {
			try {
				String val = iter.next();

				while(val != null) {

					String[] toks = val.split("\t");
					if (toks.length > 6) {

						int altIndex = findMatchingVariant(varToAnnotate, val);
						
						if (altIndex >= 0) {
							break; //break out of searching over tabix results
						}
						else {
							val = iter.next(); 
						}
					}//If there are enough  tokens in this VCF line 

				}//If iter returned non-null value
			} catch (IOException ex) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Tabix iterator exception: " + ex.getLocalizedMessage());
			}
		}
	}

        /**
         * This inner class implements Callable and its call function will be used to 
         * annotate variants in the list of VariantRec objects; This should only be 
         * used by TabixAnnotators.
         **/
        public class TabixCallable implements Callable<Integer> {
            int myVarsAnnotated = 0;
            List<VariantRec> varList;
            TabixReader reader;
  
            public TabixCallable(List<VariantRec> vList, TabixReader trr) {
                this.varList = vList;
                this.reader = trr;
            }
  
            public Integer call() throws Exception {
                for (VariantRec rec : varList) {
                    Integer recLength = Integer.valueOf(rec.getRef().length() - rec.getAlt().length());
                    if (recLength.intValue() < 0) {
                        recLength = Integer.valueOf(0);
                    } else if (recLength.intValue() == 0) {
                        if (rec.getRef().length() == 1) {
                            recLength = Integer.valueOf(1);
                        } else {
                                recLength = Integer.valueOf(rec.getRef().length());
                        }
                    }
                    Integer recEnd = Integer.valueOf(rec.getStart() - 1 + recLength.intValue());
                    Interval recInterval = new Interval(rec.getStart() - 1, recEnd.intValue());
                    if ((bedFile == null) || (bedFile.intersects(rec.getContig(), recInterval))) {
                        annotateVariant(rec, reader);
                    }
                    myVarsAnnotated += 1;
                }

                return new Integer(myVarsAnnotated);

            }
        }

        @Override	
        public void performOperation() throws OperationFailedException {
                prepare();

                int varsAnnotated = 0;

                String str_threads = this.getPipelineProperty(THREAD_KEY);
                int n_threads = 1;
                try {
                    n_threads = Integer.parseInt(str_threads);
                } catch (NumberFormatException e) {
                    n_threads = 1;
                }
                if (n_threads > 24) { n_threads = 24; }
                if (n_threads < 1) { n_threads = 1; } 

                ExecutorService execPool = Executors.newFixedThreadPool(n_threads);
                    
                List<Future<Integer>> futs = new ArrayList();
                for (String contig : variants.getContigs()) {
                        List<VariantRec> contigVars = variants.getVariantsForContig(contig);

                        if (contigVars.size() <= 0) { continue; }

                        TabixReader reader;
                        try
                        {
                               reader = new TabixReader(getPathToTabixedFile());
                        } catch (IOException e) {
                                throw new IllegalArgumentException("Error opening " + getPathToTabixedFile() + " errror : " + e.getMessage()); 
                        }
                        TabixCallable callable = new TabixCallable(contigVars, reader);
                      
                        Future<Integer> fut = execPool.submit(callable);
                        futs.add(fut);
                }
                    
                for (Future<Integer> fut : futs) {
                        try {
                                varsAnnotated += ((Integer)fut.get()).intValue();
                        } catch (InterruptedException ierr) {
                                System.out.println("Interrupted exception:");
                                ierr.printStackTrace();
                                throw new OperationFailedException(ierr.toString(), this);
                        } catch (ExecutionException err) {
                                System.out.println("Exeuction exception:");
                                err.printStackTrace();
                                throw new OperationFailedException(err.toString(), this);
                        }
                }
                    
                execPool.shutdown();
                try {
                        if (!execPool.awaitTermination(1L, TimeUnit.SECONDS)) {
                                execPool.shutdownNow();
                        }
                } catch (InterruptedException ie) {
                        execPool.shutdownNow();
                }
                    
        }

}
