package util.tribble;

import java.io.IOException;

import org.broad.tribble.readers.TabixReader;

/**
 * Some example code for how to use a TabixReader. 
 * @author brendan
 *
 */
public class TabixReaderExample {

    public static void main(String[] args) {
    	args = new String[]{"/home/brendan/oldhome/1000genomes/1000G.phase1.v3.20101123.sites.vcf.gz",
    						"1:2000007"};
    	
    	
    	
        if (args.length < 1) {
            System.out.println("Usage: java -cp .:sam.jar TabixReader <in.gz> [region]");
            System.exit(1);
        }
        try {
            TabixReader tr = new TabixReader(args[0]);
            String s;
            if (args.length == 1) { // no region is specified; print the whole file
                while ((s = tr.readLine()) != null)
                    System.out.println(s);
            } else { // a region is specified; random access
                TabixReader.Iterator iter = tr.query(args[1]); // get the iterator
                while ((s = iter.next()) != null)
                    System.out.println(s);
            }
        } catch (IOException e) {
        }
    }
    
}
