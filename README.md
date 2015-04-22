Pipeline is a tool to chain together and execute various modular operations described by an .xml file. Elements in the input file are converted into Java objects, and objects can refer to other objects created in the same file. While in principle Pipeline can be used to chain together operations of any sort, it is currently designed to link together frequently-performed bioinformatics tasks, such as aligning sequence data to a reference and calling variants. 

Compiling & Installing
----------------------

While Pipeline can be compiled via an IDE like Eclipse or Netbeans, it's best to use apache maven. To compile, simply:

    mvn compile
  
To create an executable jar file with dependencies included, use:

    mvn package assembly:assembly
  
The executable .jar will end up in the target/ directory


Basic usage
-----------

Typical pipeline usage involves invoking pipeline and giving a single input file as an argument. For instance:

    java -jar pipeline.jar my-input-file.xml
    
This will use the current working directory as the 'home' directory, to which all files will be written (with a few operator-specific exceptions, such as review directory creation). To specify an alternative home directory, use:

    java -jar pipeline.jar -home path/to/some/dir  my-input-file.xml
    
Many pipeline operators require some configuration - for instance, the path to a resource file or executable. These are stored in a 'properties' file. By default, Pipeline will look for the properties file in `$HOME/.pipelineprops.xml`, but you can specify any alternative by using the -props options, like this:

    java -jar pipeline -props pipeline_properties.xml my-input-file.xml
  
    

Pipeline Input Files
--------------------

Pipeline input files are xml-formatted files that specify a series of objects for Pipeline to create and, possibly, use. Here's a quick example:
    
    <Pipeline>
    
     <!-- Create a buffer.TextFile object called 'MyObject' -->
     <MyObject class='buffer.TextFile' />
     
    </Pipeline>
  
  When Pipeline executes, it will parse the input file and, when it sees the element with name 'MyObject', it will attempt to find the class 'buffer.TextFile' and create an instance of it. Every object created in the input file *must* extend pipeline.PipelineObject and have a no-arg constructor. Within Pipeline, the instance of buffer.TextFile will be associated with the name of the xml element, in this case MyObject. Future references to MyObject will reference the same object. 
  All PipelineObjects specify an initialize(NodeList xml) method. After Pipeline creates all the Pipeline objects, it calls the initialize() method on each of them. The 'NodeList' argument is the XML contained in the xml-representation of the PipelineObject. PipelineObjects can then examine the nodes and do something with the information. 
  
   *Operators* are special PipelineObjects that perform some operation. Any PipelineObject that extends from opertor.Operator is an operator and must implement a performOperation() method. Here's an input file with a simple operator:
   
    <Pipeline>
    
    <MyTextFile class='buffer.TextFile' filename='mytextfile.txt' />
    
    <WriteTheFile class='operator.example.Example1'>
     <MyTextFile />
    </WriteTheFile>
    
    </Pipeline>
  
  In this example, Pipeline creates a buffer.TextFile and an operator.example.Example1 object, then calls initialize() on both of them, and then calls performOperation() on the Example1 object. In the initialze() method for WriteTheFile, the operator examines the input XML, realizes that there's a TextFile object, and sets an internal field to reference that object. When performOperation() is called WriteTheFile then examines the TextFile and writes its contents to System.out
  
  
### A Bioinformatics Example
  
  Pipeline has LOTS of file types (internally referred to as buffers) and operators defined, so it's pretty easy to string together multiple tasks that are repeated often. For instance, let's say we wanted to align some reads with the SNAP aligner, call variants using FreeBayes, annotate the variants using SnpEff, and then write the variants to some file:
  
    <Pipeline>
    
    <!-- The reference genome -->
    <reference class="buffer.ReferenceFile" filename="/resources/human_g1k_v37.fasta" />
    
    <!-- A couple of input fastqs -->
    <readsOne class="buffer.FastQFile" filename="something_R1.fastq.gz" />
    <readsTwo class="buffer.FastQFile" filename="something_R2.fastq.gz" />

    <!-- Align the reads to the reference using SNAP -->
    <SNAPAlign class="operator.snap.SnapAlign" sample="my-sample" >
    <input>
      <reference />
      <readsOne />
      <readsTwo />
    </input>
    <output>
      <rawBAM class="buffer.BAMFile" filename="aligned.bam" />
     </output>
    </SNAPAlign>

    <!-- Call variants using FreeBayes -->
    <FreeBayes class="operator.freebayes.FreeBayes" read.mismatch.limit="4" min.map.score="30" min.base.score="20" >
    <input>
       <reference />
       <rawBAM  />
      </input>
      <output>
        <finalVariants class="buffer.VCFFile" filename="${SAMPLE}.all.vcf"/>
      </output>
    </FreeBayes>

    <!-- Push variants into a 'variant pool' that can we easily manipulate -->
    <VariantPool class="buffer.variant.VariantPool">
        <finalVariants />
    </VariantPool>

    <!-- Do some annotations using SnpEff -->
    <GeneAnnotate class="operator.snpeff.SnpEffGeneAnnotate" snpeff.genome="hg19">
        <VariantPool />
    </GeneAnnotate>
    
    <!-- Write the variants to a file using a variant pool writer -->
    <WriteVariants class="operator.writer.VarViewerWriter">
      <VariantPool />
    </WriteVariants>
    
    </Pipeline>
    
