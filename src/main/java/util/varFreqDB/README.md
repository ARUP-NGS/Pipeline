
## This package contains code related to the ARUP Frequency calculations

Typically, the `ComputeVarFreqs.java` file is compiled into a .jar file called `computeVarFreqs.jar`, which is executed periodically
to compute a new ARUP frequency table. The table is often tabix-compressed and used for annotations, specifically the [ARUPDBAnnotator](https://github.com/ARUP-NGS/Pipeline/blob/master/src/main/java/operator/variant/ARUPDBAnnotate.java). More recently, the information is also uploaded into MongoDB where it can be used by NGS.Web.

The input to `ComputeVarFreqs` is a list of results directories, or a file containing a list of results directories. ComputeVarFreqs examines each directory and pulls the .vcf file containing the list of variants, and also the .bed file containing the list of regions examined. For every variant present across all samples, it then computes how many times the variant was seen (as either a het or hom), and how many samples 'looked' for the variant (as defined by the bed file). It then writes this information in tab-delimited columns, for instance

     1	10004	A	G	overall	3	1	0
     1	10004	A	G	Exome	2	1	0
     1	10004	A	G	Aorto	1	0	0

  
The first four columns are the typical genomic coordinates of the variant. The remaining columns are:
  5. Test type, as parsed from sampleManifest for each sample. `Overall` is a special category that is the sum of all test types.
  6. Total samples that could have possibly found the variant (as defined by their BED files)
  7. Total samples in which the variant was found in heterozygous state
  8. Total samples in which variant was found in homozygous state

In the above example, just one variant was found. (chr1, 10004 A>G). The variant was found in a single exome sample, but was overlapped by one more exome sample and one Aorto sample. 
Note that the output can get pretty ridiculously big - there are often multiple entries for every single variant found. 

 

### variantFrequencyCalculator script

In NGS.Web, this tools is run nightly by a script called `/mnt/ngseqstore1/tools/scripts/variantFrequencyCalculator.py` (cron runs the job nightly - try `sudo crontab -e` to see the actual call details).
The script takes the output of this tool and does two things.
 1. Puts the data into a tabix-compressed file in `/mnt/ngseqstore1/ngsdata/resources/arupFreqDB...csv.gz`, and updates a link called `/mnt/ngseqstore1/ngsdata/resources/variantFrequencies.csv.gz` to point to this new version
 2. Uploads the data into a Mongo collection using a small separate app called [MongoFreqUploader](https://github.com/ARUP-NGS/Pipeline/blob/dev/src/main/java/util/varFreqDB/MongoFreqUploader.java). 


### Algorithm details


The meat of the operation takes place in `ComputeVarFreqs emitTabulatedByContig(...)`. As the name implies, we do this on a per-contig (chromosome) basis, since we don't want to end up reading too much info into memory. There are two basic phases:

  1. Read all variants from the requested contig for all samples into a single, giant VariantPool. Don't tolerate repeats, so the VariantPool should contain just one unique entry for each variant.
  2. Iterate over all variants in the variant pool and, for each variant, over all samples. For each sample, ask if its BED file contains the current variant. If so, ask if the sample contains the variant, and if so, get the variants zygosity. Increment the variant's annotations accordingly (there are separate annotations for every test type, het, and hom zygosity). 
  3. Emit everything to output in the format specified above.


