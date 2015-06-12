package annotation;


import junit.framework.TestCase;
import java.io.File;

import operator.variant.ClinVarAnnotator;
import org.junit.Assert;
import pipeline.Pipeline;
import buffer.variant.VariantRec;
import util.vcfParser.VCFParser;

/**
 * Created by Keith simmon on 4/24/15.
 *
 * This test validates the DBSNP database, the test was created when the annotator was moved under the
 * AbstractTabixAnnotator.
 *
 * A truncated database was created from the nomrmilized database which was downloaded on
 * April 4th 2015. The python script used to create the test database is located on scriutils git
 *
 *
 *
 * @author Keith Simmon
 *
 */

public class TestClinvarTabixAnnotator extends TestCase {
    File inputFile = new File("src/test/java/annotation/testClinvarTabixAnnotator.xml");
    File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
    ClinVarAnnotator annotator;
    ClinVarAnnotator annotator_non_normalized;

    boolean thrown = false;

    public void setUp() {
        try {
            Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl.setProperty(
                    "clinvar.path",
                    "src/test/java/testvcfs/clinvar_2015-06-11_1000_testing.vcf.gz");
            ppl.initializePipeline();
            ppl.stopAllLogging();
            ppl.execute();
            annotator = (ClinVarAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

            Pipeline ppl1 = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl1.setProperty(
                    "clinvar.path",
                    "src/test/java/testvcfs/clinvar_2015-06-11_1000_testing.vcf.gz");
            ppl1.initializePipeline();
            ppl1.stopAllLogging();
            ppl1.execute();
            annotator_non_normalized = (ClinVarAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
        }
        catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }




//    public void testNormilizingVariant() { //make sure variant normilization behaves correctly
//        VariantRec v = new VariantRec("14", 23727902, 23727902, "CTT", "CT");
//        VariantRec v1 = new VariantRec("14", 23727902, 23727902, "CT", "C");
//        v = VCFParser.normalizeVariant(v);
//        v1 = VCFParser.normalizeVariant(v1);
//        Assert.assertTrue(v1.toString().equals(v.toString()));
//    }
//
    public void testFirstDBVariant(){
//        1       883516  rs267598747     G       A
//         CLNSIG=1;CLNDBN=Malignant_melanoma;CLNDSDBID=C0025202:2092003;CLNDSDB=MedGen:SNOMED_CT;CLNREVSTAT=not

        try {
            VariantRec var1 = new VariantRec("1", 883516, 883516, "G", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNSIG).equals("1"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDBN).equals("Malignant_melanoma"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDSDBID).equals("C0025202:2092003"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDSDB).equals("MedGen:SNOMED_CT"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNREVSTAT).equals("not"));
        }
        catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }

    }


    public void testlastDBVariant(){
        //MT	15990	rs199474699	C	T	.	.
        // CLNSIG=5;CLNDBN=Myopathy;CLNDSDBID=C0026848;CLNDSDB=MedGen;CLNREVSTAT=single;CLNHGVS=NC_012920.1:m.15990C>T

        try {
            VariantRec var1 = new VariantRec("MT", 15990, 15990, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNSIG).equals("5"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDBN).equals("Myopathy"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDSDBID).equals("C0026848"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNDSDB).equals("MedGen"));
            Assert.assertTrue(var1.getAnnotation(VariantRec.CLNREVSTAT).equals("single"));
        }
        catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }

    }


    public void testVariantNotInDB() {
        try {
            //Y	28484157	rs207481635	T	C	.	.
            //CLNSIG=0;CLNDBN=Lung_cancer;CLNDSDBID=C0684249:211980:187875007;CLNDSDB=MedGen:OMIM:SNOMED_CT;CLNREVSTAT=single;CLNHGVS=NC_000024.9:g.28484157T>C
            VariantRec  var1 = new VariantRec("Y", 28484157, 28484157, "T", "A");
            annotator.annotateVariant(var1);
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNSIG));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDBN));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDSDBID));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDSDB));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNREVSTAT));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }



    public void test_null_variant() {
        try {

            VariantRec  var1 = new VariantRec("17", 7124899, 7124899, "G", "T");
            //17	7124899	rs369560930	G	T	.	.	CLNSIG;CLNDBN;CLNDSDBID;CLNDSDB;CLNREVSTAT;CLNHGVS

            annotator.annotateVariant(var1);
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNSIG));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDBN));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDSDBID));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNDSDB));
            Assert.assertNull(var1.getAnnotation(VariantRec.CLNREVSTAT));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

}



