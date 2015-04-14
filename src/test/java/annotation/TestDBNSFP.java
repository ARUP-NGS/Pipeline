package annotation;

import junit.framework.TestCase;

import java.io.File;

import operator.variant.DBNSFPAnnotator;
import org.junit.Assert;
import pipeline.Pipeline;
import buffer.variant.VariantRec;
import util.vcfParser.VCFParser;


/**
 * This test validates the DBNSFP database, the test was created when the annotator was moved under the
 * AbstractTabixAnnotator.
 * <p/>
 * A truncated database was created from the normalized database which was downloaded on
 * April 4th 2015. The python script used to create the test database is located on scriutils git
 *
 * @author Keith Simmon
 */
public class TestDBNSFP extends TestCase {
    File inputFile = new File("src/test/java/annotation/testDBNSFP.xml");
    File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
    DBNSFPAnnotator annotator;
    //DBSNPAnnotator annotator_non_normalized;
    boolean thrown;

    public void setUp() {
        try {
            Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl.setProperty(
                    "dbnsfp.path",
                    "src/test/java/testcsvs/dbNSFP3.0b1c_2015_04_13_truncated.tab.gz");
            ppl.initializePipeline();
            ppl.stopAllLogging();
            ppl.execute();
            annotator = (DBNSFPAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

//            Pipeline ppl1 = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
//            ppl1.setProperty(
//                    "dbsnp.path",
//                    "src/test/java/testvcfs/common_all_test_dbsnp.vcf.bgz");
//            ppl1.initializePipeline();
//            ppl1.stopAllLogging();
//            ppl1.execute();
//            annotator_non_normalized = (DBSNPAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testFirstDBVariantAllAnnotations() {
        try {
            VariantRec var1 = new VariantRec("1", 69091, 69091, "A", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.13, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("1", 69091, 69091, "A", "T");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            //System.out.println(varNull.getAnnotation(VariantRec.SIFT_SCORE));
            Assert.assertNull(varNull.getAnnotation(VariantRec.SIFT_SCORE));

            Assert.assertEquals(0.0, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);
//          var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
            Assert.assertEquals(0.0, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);
//          var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
            Assert.assertEquals(0.589091, var1.getProperty(VariantRec.LRT_SCORE), 0);
//          var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
            Assert.assertNull(var1.getProperty(VariantRec.MT_SCORE));
//          var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[39]));
            Assert.assertNull(var1.getProperty(VariantRec.MA_SCORE));
//          var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[46]));
            Assert.assertEquals(2.31, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
//          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertEquals(-4.63, var1.getProperty(VariantRec.GERP_SCORE), 0);
//          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(-0.342000, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
//          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertEquals(3.5592, var1.getProperty(VariantRec.SIPHY_SCORE), 0);
//          var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[70]));
            Assert.assertNull(var1.getProperty(VariantRec.POP_FREQUENCY));
//          var.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[73]));
            Assert.assertNull(var1.getProperty(VariantRec.AFR_FREQUENCY));
//          var.addProperty(VariantRec.AFR_FREQUENCY, Double.parseDouble(toks[75]));
            Assert.assertNull(var1.getProperty(VariantRec.EUR_FREQUENCY));
//        var.addProperty(VariantRec.EUR_FREQUENCY, Double.parseDouble(toks[77]));
            Assert.assertNull(var1.getProperty(VariantRec.AMR_FREQUENCY));
//        var.addProperty(VariantRec.AMR_FREQUENCY, Double.parseDouble(toks[79]));
            Assert.assertNull(var1.getProperty(VariantRec.ASN_FREQUENCY));
//        var.addProperty(VariantRec.ASN_FREQUENCY, Double.parseDouble(toks[81]));
            Assert.assertNull(var1.getProperty(VariantRec.EXOMES_FREQ));
//        var.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[91]));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testLastDBVariantAllAnnotations() {
        try {
            VariantRec var1 = new VariantRec("M", 14673, 14673, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.0, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("M", 14673, 14673, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);

            //System.out.println(varNull.getAnnotation(VariantRec.SIFT_SCORE));
            Assert.assertNull(varNull.getAnnotation(VariantRec.SIFT_SCORE));

            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_SCORE));
//          var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
//          var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
            Assert.assertNull(var1.getProperty(VariantRec.LRT_SCORE));
//          var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
            Assert.assertNull(var1.getProperty(VariantRec.MT_SCORE));
//          var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[39]));
            Assert.assertNull(var1.getProperty(VariantRec.MA_SCORE));
//          var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[46]));
            Assert.assertEquals(4.15, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
//          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertEquals(2.11, var1.getProperty(VariantRec.GERP_SCORE), 0);
//          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(0.991, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
//          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertNull(var1.getProperty(VariantRec.SIPHY_SCORE));
//          var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[70]));
            Assert.assertNull(var1.getProperty(VariantRec.POP_FREQUENCY));
//          var.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[73]));
            Assert.assertNull(var1.getProperty(VariantRec.AFR_FREQUENCY));
//          var.addProperty(VariantRec.AFR_FREQUENCY, Double.parseDouble(toks[75]));
            Assert.assertNull(var1.getProperty(VariantRec.EUR_FREQUENCY));
//        var.addProperty(VariantRec.EUR_FREQUENCY, Double.parseDouble(toks[77]));
            Assert.assertNull(var1.getProperty(VariantRec.AMR_FREQUENCY));
//        var.addProperty(VariantRec.AMR_FREQUENCY, Double.parseDouble(toks[79]));
            Assert.assertNull(var1.getProperty(VariantRec.ASN_FREQUENCY));
//        var.addProperty(VariantRec.ASN_FREQUENCY, Double.parseDouble(toks[81]));
            Assert.assertNull(var1.getProperty(VariantRec.EXOMES_FREQ));
//        var.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[91]));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }


    public void testRandomDBVariantAllAnnotations() { //with semicolon seperated values
        try {
            VariantRec var1 = new VariantRec("19", 54594916, 54594916, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.001, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("19", 54594916, 54594916, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
//            //System.out.println(varNull.getAnnotation(VariantRec.SIFT_SCORE));
            Assert.assertNull(varNull.getAnnotation(VariantRec.SIFT_SCORE));

//          var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
            Assert.assertEquals(0.998, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);
            Assert.assertEquals(0.978, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);
            // var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
            Assert.assertEquals(0.159743, var1.getProperty(VariantRec.LRT_SCORE), 0);
////          var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.MT_SCORE), 0);
////          var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[39]));
            Assert.assertEquals(3.545, var1.getProperty(VariantRec.MA_SCORE), 0);
////          var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[46]));

            Assert.assertEquals(1.58, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

////          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertEquals(1.58, var1.getProperty(VariantRec.GERP_SCORE), 0);
////          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(0.938, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
////          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertEquals(5.3024, var1.getProperty(VariantRec.SIPHY_SCORE), 0);
////          var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[70]));
            Assert.assertNull(var1.getProperty(VariantRec.POP_FREQUENCY));
////          var.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[73]));
            Assert.assertNull(var1.getProperty(VariantRec.AFR_FREQUENCY));
////          var.addProperty(VariantRec.AFR_FREQUENCY, Double.parseDouble(toks[75]));
            Assert.assertNull(var1.getProperty(VariantRec.EUR_FREQUENCY));
////        var.addProperty(VariantRec.EUR_FREQUENCY, Double.parseDouble(toks[77]));
            Assert.assertNull(var1.getProperty(VariantRec.AMR_FREQUENCY));
////        var.addProperty(VariantRec.AMR_FREQUENCY, Double.parseDouble(toks[79]));
            Assert.assertNull(var1.getProperty(VariantRec.ASN_FREQUENCY));
////        var.addProperty(VariantRec.ASN_FREQUENCY, Double.parseDouble(toks[81]));
            Assert.assertNull(var1.getProperty(VariantRec.EXOMES_FREQ));
////        var.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[91]));

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }

    }

    public void testSiftScore() {
        //with semicolon
        try {
            VariantRec var1 = new VariantRec("X", 136207818, 136207818, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.604, var1.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIFT_SCORE));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testPOLYPHEN_SCORE() {
        //with semicolon
        //0.043;1.0;0.293;0.007;0.14;0.077
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("2", 101002796, 101002796, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testPOLYPHEN_HVAR_SCORE() {
        //with semicolon
        //1049:0.992;0.994;0.996;0.996;0.992;0.992;0.992;0.986;1.0;0.994;0.996;0.999;0.986
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("2", 25427622, 25427622, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testLRT_SCORE() {

        try {
            VariantRec var1 = new VariantRec("2", 178461225, 178461225, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.000051, var1.getProperty(VariantRec.LRT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.LRT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testMT_SCORE() {
        try {
            VariantRec var1 = new VariantRec("11", 43326586, 43326586, "T", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.MT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testMA_SCORE() {
        try {
            VariantRec var1 = new VariantRec("11", 18544655, 18544655, "A", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(-0.86, var1.getProperty(VariantRec.MA_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MA_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testGERP_NR_SCORE() {
        try {
            VariantRec var1 = new VariantRec("22", 31093820, 31093820, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(4.74, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_NR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testGERP_SCORE() {
        try {
            VariantRec var1 = new VariantRec("5", 138556782, 138556782, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(4.8, var1.getProperty(VariantRec.GERP_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    public void testPHYLOP_SCORE() {
        try {
            //5985:-0.448000
            VariantRec var1 = new VariantRec("12", 19129811, 19129811, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(-0.448, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.PHYLOP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testSIPHY_SCORE() {
        try {
            //8959:12.8949
            VariantRec var1 = new VariantRec("19", 51017119, 51017119, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(12.8949, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIPHY_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testPOP_FREQUENCY() {
        try {
            //
            VariantRec var1 = new VariantRec("8", 123140824, 123140824, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(5.990415335463259E-4, var1.getProperty(VariantRec.POP_FREQUENCY), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POP_FREQUENCY));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testAFR_FREQUENCY() {
        try {
            //1533:7.564296520423601E-4
            VariantRec var1 = new VariantRec("2", 189063248, 189063248, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(7.564296520423601E-4, var1.getProperty(VariantRec.AFR_FREQUENCY), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.AFR_FREQUENCY));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testEUR_FREQUENCY() {
        try {
            //7743:9.940357852882703E-4
            VariantRec var1 = new VariantRec("16", 90009323, 90009323, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(9.940357852882703E-4, var1.getProperty(VariantRec.EUR_FREQUENCY), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.EUR_FREQUENCY));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testAMR_FREQUENCY() {
        try {
            //4192:0.001440922190201729
            VariantRec var1 = new VariantRec("8", 17371180, 17371180, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.001440922190201729, var1.getProperty(VariantRec.AMR_FREQUENCY), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.AMR_FREQUENCY));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testASN_FREQUENCY() {
        try {
            //4596:0.006944444444444444
            VariantRec var1 = new VariantRec("9", 27060618, 27060618, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(0.006944444444444444, var1.getProperty(VariantRec.ASN_FREQUENCY), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.ASN_FREQUENCY));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testEXOMES_FREQ() {
        try {
            //9967:1.4863258026159333E-4
            VariantRec var1 = new VariantRec("X", 154467131, 154467131, "G", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator.annotateVariant(var1);
            Assert.assertEquals(1.4863258026159333E-4, var1.getProperty(VariantRec.EXOMES_FREQ), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.EXOMES_FREQ));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

}
