package annotation;


import java.io.File;

import junit.framework.TestCase;
import operator.variant.DBSNPAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

/**
 * Created by Keith simmon on 4/24/15.
 *
 * This test validates the DBSNP database, the test was created when the annotator was moved under the
 * AbstractTabixAnnotator.
 *
 * A truncated database was created from the normalized database which was downloaded on
 * April 4th 2015. The python script used to create the test database is located on scriutils git
 *
 *
 *
 * @author Keith Simmon
 *
 */
public class TestDBSNP extends TestCase {
    File inputFile = new File("src/test/java/annotation/testDBSNP.xml");
    File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
    DBSNPAnnotator annotator;
    DBSNPAnnotator annotator_non_normalized;

    boolean thrown = false;

    public void setUp() {
        try {
            Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl.setProperty(
                    "dbsnp.path",
                    "src/test/java/testvcfs/dbsnp_00_All_15-04-12_testing.vcf.gz");
            ppl.initializePipeline();
            ppl.stopAllLogging();
            ppl.execute();
            annotator = (DBSNPAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

            Pipeline ppl1 = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl1.setProperty(
                    "dbsnp.path",
                    "src/test/java/testvcfs/common_all_test_dbsnp.vcf.bgz");
            ppl1.initializePipeline();
            ppl1.stopAllLogging();
            ppl1.execute();
            annotator_non_normalized = (DBSNPAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

        }catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testNormilizingVariant() { //make sure variant normilization behaves correctly
        VariantRec v = new VariantRec("14", 23727902, 23727902, "CTT", "CT");
        VariantRec v1 = new VariantRec("14", 23727902, 23727902, "CT", "C");
        v = VCFParser.normalizeVariant(v);
        v1 = VCFParser.normalizeVariant(v1);
        Assert.assertTrue(v1.toString().equals(v.toString()));
    }

    public void testFirstDBVariant(){
        //1       10108   rs62651026      C       T       .       .       RS=62651026
        try {
            VariantRec var1 = new VariantRec("1", 10108, 10108, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            System.out.println(var1.getAnnotation(VariantRec.RSNUM));
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs62651026"));

            VariantRec varNull = new VariantRec("1", 10108, 10108, "C", "G");
            var1 = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            System.out.println(varNull.getAnnotation(VariantRec.RSNUM));
            Assert.assertNull(varNull.getAnnotation(VariantRec.RSNUM));

        }
        catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }

    }

    public void testKnownVariants() {
        try {
            //2       83512429        rs190851925     G       C       .       .       RS=190851925;
            VariantRec var1 = new VariantRec("2", 83512429, 83512429, "G", "C");
            annotator.annotateVariant(var1);
            var1 = VCFParser.normalizeVariant(var1);
            System.out.println(var1.getAnnotation(VariantRec.RSNUM));
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs190851925"));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }


    }

    public void testKnownVariant3rdline() {
        try {
            //1       958601  rs183632277     C       T       .       .       RS=183632277;
            //This variant would be normailized to -- 1	10056	10057	-	A	1000.0	-	unknown	-	-
            VariantRec var1 = new VariantRec("1", 958601, 958601, "C", "T");
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs183632277"));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void test_MultiAllelicVariant_pretest() {
        try {
            //4       10105588        rs373722200     T       C       .       .
            VariantRec  var1 = new VariantRec("4", 10105588, 10105588, "T", "C");
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs373722200"));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

//Commented out for now - we should never see an multiallelic variant as input
//    public void testMultiAllelicVariant() {
//        try {
//            //1	17114420	rs202217127	G	A
//
//            VariantRec  var1 = new VariantRec("4", 10105588, 10105588, "T", "G,C,A");
//            annotator.annotateVariant(var1);
//            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs373722200"));
//
//        } catch (Exception ex) {
//            thrown = true;
//            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
//            ex.printStackTrace();
//            Assert.assertTrue(false);
//        }
//    }

    public void testVariantNotInDBsnp() {
        try {
            VariantRec  var1 = new VariantRec("1", 10020, 10021, "A", "T");
            annotator.annotateVariant(var1);
            Assert.assertNull(var1.getAnnotation(VariantRec.RSNUM));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testMultiAllelicVariantNotInDBsnp() {
        try {
            VariantRec  var1 = new VariantRec("1", 10020, 10021, "A", "T,C");
            annotator.annotateVariant(var1);
            Assert.assertNull(var1.getAnnotation(VariantRec.RSNUM));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testLargeDeletion() {
        try {
            //1       1466207 rs564016727     TCAAAAAAAAAAAAAAAAAAAAA T       .       .       RS=564016727
            VariantRec var1 = new VariantRec("1", 1466207, 1466207, "TCAAAAAAAAAAAAAAAAAAAAA", "T");
            var1 = VCFParser.normalizeVariant(var1);
            //System.out.print(var1.toString());
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs564016727"));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testLargeInsertion() {
        try {
            //19      54711591        rs142338855     A       AACTTCAGGGTGAT  .       .
            VariantRec var1 = new VariantRec("4", 26736520, 26736520, "G",
                    "ATATATACACACATGTGTACATACACATATATGTGTGTATATATGTATACACATATGTATATGTATATATGTATACACATATGTATA");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs68002084"));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testMitochondriaVariant() {
        try {
            // MT      16529   rs370705831     T       C
            VariantRec var1 = new VariantRec("MT", 16529, 16529, "T", "C");
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs370705831"));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testYVariant() {
        try {
            //Y       1427711 rs35662505      G       GC      .       .
            VariantRec var1 = new VariantRec("Y", 1427711, 1427711, "G", "GC");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            System.out.println(var1.toString());
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs35662505"));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testXVariant() {
        try {
            //X       147991599       rs201998867     CAG     C       .       .
            VariantRec var1 = new VariantRec("X", 147991599 , 147991599 , "CAG", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs201998867"));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testExceptionCase_non_normilized_Database() {
        try {
            //1	4066114	rs201371334	T	TG,TT,TTG
            VariantRec var1 = new VariantRec("1", 4066114, 4066114, "T", "TG");
            VCFParser.normalizeVariant(var1);
            annotator_non_normalized.annotateVariant(var1);
            //Assert.assertTrue(var1.getAnnotation(VariantRec.RSNUM).equals("rs192710666"));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();

        }
        Assert.assertTrue(true);

    }
}









