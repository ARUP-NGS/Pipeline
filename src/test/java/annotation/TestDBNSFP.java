package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.DBNSFPAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;


/**
 * This test validates the DBNSFP database, the test was created when the annotator_30 was moved under the
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
    DBNSFPAnnotator annotator_30;//currently commented out
    DBNSFPAnnotator annotator_29;
    DBNSFPAnnotator annotator_292;
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
           annotator_30 = (DBNSFPAnnotator) ppl.getObjectHandler().getObjectForLabel("GeneAnnotate30");


            Pipeline ppl2 = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl2.setProperty(
                    "dbnsfp.path",
                    "src/test/java/testcsvs/dbNSFP2.9_2015_09_11_test.tab.gz");
            ppl2.initializePipeline();
            ppl2.stopAllLogging();
            ppl2.execute();
            annotator_29 = (DBNSFPAnnotator) ppl2.getObjectHandler().getObjectForLabel("GeneAnnotate29");
            
            
            Pipeline ppl3 = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
            ppl3.setProperty(
                    "dbnsfp.path",
                    "src/test/java/testcsvs/dbNSFP2.9.2.b1c_2017_01_23-truncated.tab.gz"); 
            ppl3.initializePipeline();
            ppl3.stopAllLogging();
            ppl3.execute();
            annotator_292 = (DBNSFPAnnotator) ppl3.getObjectHandler().getObjectForLabel("GeneAnnotate292");
            


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    public void testFirstDBVariantAllAnnotations292() {
        try {
            VariantRec var1 = new VariantRec("1", 35138,  35138, "T", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertNull(var1.getAnnotation(VariantRec.SIFT_SCORE));

            VariantRec varNull = new VariantRec("1", 883869, 883869, "C", "T");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getAnnotation(VariantRec.SIFT_SCORE));
            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_SCORE));
            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
            Assert.assertNull(var1.getProperty(VariantRec.LRT_SCORE));
            Assert.assertEquals(1, var1.getProperty(VariantRec.MT_SCORE), 0);
            Assert.assertNull(var1.getProperty(VariantRec.MA_SCORE)); //value is "."
            Assert.assertEquals(0.742, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
            Assert.assertEquals(0.742, var1.getProperty(VariantRec.GERP_SCORE), 0); 
            Assert.assertEquals(3.8237, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testLastDBVariantAllAnnotations292() {
        try {
            VariantRec var1 = new VariantRec("X", 78216484, 78216484, "T", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);

            VariantRec varNull = new VariantRec("X", 78216484, 78216484, "T", "C");//double check this
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);

            Assert.assertEquals(0.011, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            Assert.assertEquals(0.908, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);
            Assert.assertEquals(0.070684, var1.getProperty(VariantRec.LRT_SCORE), 0);
            Assert.assertEquals(0.790707, var1.getProperty(VariantRec.MT_SCORE), 0);
            Assert.assertEquals(0.89745, var1.getProperty(VariantRec.MA_SCORE), 0);
            Assert.assertEquals(4.93, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
            Assert.assertEquals(4.93, var1.getProperty(VariantRec.GERP_SCORE), 0);
            Assert.assertEquals(12.4896, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testRandomDBVariantAllAnnotations292() { //with semicolon separated values
        try {
            VariantRec var1 = new VariantRec("19", 48946461, 48946461, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.001, var1.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec varNull = new VariantRec("19", 48946461, 48946461, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);

            Assert.assertEquals(0.045, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);
            Assert.assertEquals(0.139850, var1.getProperty(VariantRec.LRT_SCORE), 0);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.MT_SCORE), 0);
            Assert.assertEquals(0.06538, var1.getProperty(VariantRec.MA_SCORE), 0);
            Assert.assertEquals(2.05, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
            Assert.assertEquals(0.918, var1.getProperty(VariantRec.GERP_SCORE), 0);
            Assert.assertEquals(4.361, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testSiftScore292() {
        //with semicolon
        try {
            VariantRec var1 = new VariantRec("19", 15636185, 15636185, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.023, var1.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec var2 = new VariantRec("12", 25249910, 25249910, "A", "G");
            var2 = VCFParser.normalizeVariant(var2);
            annotator_292.annotateVariant(var2);
            Assert.assertEquals(0.106, var2.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec varNull = new VariantRec("19", 15636185, 15636185, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIFT_SCORE));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }



    public void testPOLYPHEN_HVAR_SCORE292() {
        //with semicolon
        try {
            VariantRec var1 = new VariantRec("12", 25699414, 25699414, "A", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.998, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);

            VariantRec varNull = new VariantRec("12", 25699414, 25699414, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testLRT_SCORE292() {
        try {
            VariantRec var1 = new VariantRec("10", 22830948, 22830948, "T", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.000001, var1.getProperty(VariantRec.LRT_SCORE), 0);

            VariantRec varNull = new VariantRec("10", 22607060, 22607060, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.LRT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    
    public void testMT_SCORE292() {
        try {
            VariantRec var1 = new VariantRec("1", 1139566, 1139566, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.999998, var1.getProperty(VariantRec.MT_SCORE), 0);

            VariantRec varNull = new VariantRec("1", 1139566, 1139566, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    

    public void testMA_SCORE292() {    	
        try {
            VariantRec var1 = new VariantRec("11", 71146654, 71146654, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(0.87347, var1.getProperty(VariantRec.MA_SCORE), 0);

            VariantRec varNull = new VariantRec("11", 71146654, 71146654, "A", "G");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MA_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testGERP_NR_SCORE292() {
        try {
            VariantRec var1 = new VariantRec("6", 90402250, 90402250, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(5.67, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

            VariantRec varNull = new VariantRec("6", 90402250, 90402250, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_NR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }



    public void testGERP_SCORE292() {
        try {
            VariantRec var1 = new VariantRec("5", 72800195, 72800195, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(5.61, var1.getProperty(VariantRec.GERP_SCORE), 0);

            VariantRec varNull = new VariantRec("5", 72800195, 72800195, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testSIPHY_SCORE292() {
        try {
            //8959:12.8949
            VariantRec var1 = new VariantRec("4", 140309231, 140309231, "A", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_292.annotateVariant(var1);
            Assert.assertEquals(16.3636, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_292.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIPHY_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
   
 
    public void testFirstDBVariantAllAnnotations29() {
        try {
            VariantRec var1 = new VariantRec("1", 35138,  35138, "T", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            //System.out.println(var1.toString());
            //Assert.assertEquals(0.0, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            Assert.assertNull(var1.getAnnotation(VariantRec.SIFT_SCORE));
            //System.out.println(var1.getAnnotation(VariantRec.SIFT_SCORE));
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("1", 883869, 883869, "C", "T");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            //System.out.println(varNull.getAnnotation(VariantRec.SIFT_SCORE));
            Assert.assertNull(varNull.getAnnotation(VariantRec.SIFT_SCORE));

            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_SCORE));
//          var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
            Assert.assertNull(var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
//          var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
            Assert.assertNull(var1.getProperty(VariantRec.LRT_SCORE));
//          var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
            Assert.assertEquals(1, var1.getProperty(VariantRec.MT_SCORE), 0);
//          var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[39]));
            Assert.assertNull(var1.getProperty(VariantRec.MA_SCORE));
//          var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[46]));
            Assert.assertEquals(0.742, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);
//          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertEquals(0.742, var1.getProperty(VariantRec.GERP_SCORE), 0);
//          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(0.339, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
//          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertEquals(3.8237, var1.getProperty(VariantRec.SIPHY_SCORE), 0);
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

        }
        Assert.assertFalse(thrown);
    }



    public void testLastDBVariantAllAnnotations29() {
        try {
            VariantRec var1 = new VariantRec("Y", 28133957, 28133957, "G", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertNull(var1.getProperty(VariantRec.SIFT_SCORE));
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("Y", 28133957, 28133957, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);

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
            Assert.assertNull(var1.getProperty(VariantRec.GERP_NR_SCORE));
//          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertNull(var1.getProperty(VariantRec.GERP_SCORE));
//          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(0.170, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
//          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertNull(var1.getProperty(VariantRec.SIPHY_SCORE));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testRandomDBVariantAllAnnotations29() { //with semicolon seperated values
        try {
            VariantRec var1 = new VariantRec("19", 48946461, 48946461, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.001, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("19", 48946461, 48946461, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
//            //System.out.println(varNull.getAnnotation(VariantRec.SIFT_SCORE));

//          var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
            Assert.assertEquals(0.421, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);
            Assert.assertEquals(0.045, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);
            // var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
            Assert.assertEquals(0.139850, var1.getProperty(VariantRec.LRT_SCORE), 0);
////          var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.MT_SCORE), 0);
////          var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[39]));
            Assert.assertEquals(0.08118, var1.getProperty(VariantRec.MA_SCORE), 0);
////          var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[46]));

            Assert.assertEquals(2.05, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

////          var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
            Assert.assertEquals(0.918, var1.getProperty(VariantRec.GERP_SCORE), 0);
////          var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
            Assert.assertEquals(0.404, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);
////          var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
            Assert.assertEquals(4.361, var1.getProperty(VariantRec.SIPHY_SCORE), 0);
////          var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[70]));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testSiftScore29() {
        //with semicolon
        try {
            VariantRec var1 = new VariantRec("19", 15636185, 15636185, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.023, var1.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec var2 = new VariantRec("12", 25249910, 25249910, "A", "G");
            var2 = VCFParser.normalizeVariant(var2);
            annotator_29.annotateVariant(var2);
            Assert.assertEquals(0.106, var2.getProperty(VariantRec.SIFT_SCORE), 0);



            VariantRec varNull = new VariantRec("19", 15636185, 15636185, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIFT_SCORE));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testPOLYPHEN_SCORE29() {
        //with semicolon
        //0.043;1.0;0.293;0.007;0.14;0.077
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("16", 685297, 685297, "A", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.622, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);

            VariantRec varNull = new VariantRec("16", 685297, 685297, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testPOLYPHEN_HVAR_SCORE29() {
        //with semicolon
        //1049:0.992;0.994;0.996;0.996;0.992;0.992;0.992;0.986;1.0;0.994;0.996;0.999;0.986
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("12", 25699414, 25699414, "A", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.998, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);

            VariantRec varNull = new VariantRec("12", 25699414, 25699414, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testLRT_SCORE29() {
        try {
            VariantRec var1 = new VariantRec("10", 22830948, 22830948, "T", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.000001, var1.getProperty(VariantRec.LRT_SCORE), 0);

            VariantRec varNull = new VariantRec("10", 22607060, 22607060, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.LRT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    
    public void testMT_SCORE29() {
        try {
            VariantRec var1 = new VariantRec("X", 78216484, 78216484, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.768, var1.getProperty(VariantRec.MT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 78216484, 78216484, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
    public void testMT_PRED29(){
        try {
            VariantRec var1 = new VariantRec("X", 78216484, 78216484, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.MT_PRED).equals("polymorphism"));
            VariantRec varNull = new VariantRec("X", 78216484, 78216484, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MT_PRED));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        //Assert.assertFalse(thrown);
    }
    

    public void testMA_SCORE29() {    	
        try {
            VariantRec var1 = new VariantRec("11", 71146654, 71146654, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(0.79805, var1.getProperty(VariantRec.MA_SCORE), 0);

            VariantRec varNull = new VariantRec("11", 71146654, 71146654, "A", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MA_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
    public void testMA_PRED29(){
        try {
            VariantRec var1 = new VariantRec("11", 71146654, 71146654, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertTrue(var1.getAnnotation(VariantRec.MA_PRED).equals("M"));

            VariantRec varNull = new VariantRec("11", 71146654, 71146654, "A", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MA_PRED));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testGERP_NR_SCORE29() {
        try {
            VariantRec var1 = new VariantRec("6", 90402250, 90402250, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(5.64, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

            VariantRec varNull = new VariantRec("6", 90402250, 90402250, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_NR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }



    public void testGERP_SCORE29() {
        try {
            VariantRec var1 = new VariantRec("5", 72800195, 72800195, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(5.61, var1.getProperty(VariantRec.GERP_SCORE), 0);

            VariantRec varNull = new VariantRec("5", 72800195, 72800195, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testPHYLOP_SCORE29() {
        try {
            //5985:-0.448000
            VariantRec var1 = new VariantRec("4", 140394089, 140394089, "G", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(8.062, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);

            VariantRec varNull = new VariantRec("4", 140394089, 140394089, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.PHYLOP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }


    public void testSIPHY_SCORE29() {
        try {
            //8959:12.8949
            VariantRec var1 = new VariantRec("4", 140309231, 140309231, "A", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_29.annotateVariant(var1);
            Assert.assertEquals(16.3636, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_29.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIPHY_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
   
/** Commenting out v3.0 which is not used in production (December 2016) for better organization and debugging - CHRISK
    public void testFirstDBVariantAllAnnotations30() {
        try {
            VariantRec var1 = new VariantRec("1", 69091, 69091, "A", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(0.13, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("1", 69091, 69091, "A", "T");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
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

        }
        Assert.assertFalse(thrown);
    }
    public void testLastDBVariantAllAnnotations30() {
        try {
            VariantRec var1 = new VariantRec("M", 14673, 14673, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(0.0, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("M", 14673, 14673, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);

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
        }
        Assert.assertFalse(thrown);
    }
    public void testRandomDBVariantAllAnnotations30() { //with semicolon seperated values
        try {
            VariantRec var1 = new VariantRec("19", 54594916, 54594916, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(0.001, var1.getProperty(VariantRec.SIFT_SCORE), 0);
            //Assert.assertTrue(var1.getProperty(VariantRec.SIPHY_SCORE).equals("0.13"));

            VariantRec varNull = new VariantRec("19", 54594916, 54594916, "T", "C");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
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

        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    public void testSiftScore30() {
        //with semicolon
        try {
            VariantRec var1 = new VariantRec("X", 136207818, 136207818, "A", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(0.604, var1.getProperty(VariantRec.SIFT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIFT_SCORE));


        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }

    public void testPOLYPHEN_SCORE30() {
        //with semicolon
        //0.043;1.0;0.293;0.007;0.14;0.077
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("2", 101002796, 101002796, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.POLYPHEN_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
    public void testPOLYPHEN_HVAR_SCORE30() {
        //with semicolon
        //1049:0.992;0.994;0.996;0.996;0.992;0.992;0.992;0.986;1.0;0.994;0.996;0.999;0.986
        //2       101002796       G       C
        try {
            VariantRec var1 = new VariantRec("2", 25427622, 25427622, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.POLYPHEN_HVAR_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262982, 154262982, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.POLYPHEN_HVAR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
        public void testLRT_SCORE30() {
        try {
            VariantRec var1 = new VariantRec("2", 178461225, 178461225, "G", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(0.000051, var1.getProperty(VariantRec.LRT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.LRT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
        public void testMT_SCORE30() {
        try {
            VariantRec var1 = new VariantRec("11", 43326586, 43326586, "T", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(1.0, var1.getProperty(VariantRec.MT_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MT_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
        public void testMA_SCORE30() {
        try {
            VariantRec var1 = new VariantRec("11", 18544655, 18544655, "A", "C");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(-0.86, var1.getProperty(VariantRec.MA_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.MA_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
        public void testGERP_NR_SCORE30() {
        try {
            VariantRec var1 = new VariantRec("22", 31093820, 31093820, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(4.74, var1.getProperty(VariantRec.GERP_NR_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_NR_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
        public void testGERP_SCORE30() {
        try {
            VariantRec var1 = new VariantRec("5", 138556782, 138556782, "C", "T");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(4.8, var1.getProperty(VariantRec.GERP_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.GERP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
        public void testPHYLOP_SCORE30() {
        try {
            //5985:-0.448000
            VariantRec var1 = new VariantRec("12", 19129811, 19129811, "T", "G");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(-0.448, var1.getProperty(VariantRec.PHYLOP_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.PHYLOP_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
        public void testSIPHY_SCORE30() {
        try {
            //8959:12.8949
            VariantRec var1 = new VariantRec("19", 51017119, 51017119, "C", "A");
            var1 = VCFParser.normalizeVariant(var1);
            annotator_30.annotateVariant(var1);
            Assert.assertEquals(12.8949, var1.getProperty(VariantRec.SIPHY_SCORE), 0);

            VariantRec varNull = new VariantRec("X", 154262983, 154262983, "T", "A");
            varNull = VCFParser.normalizeVariant(varNull);
            annotator_30.annotateVariant(varNull);
            Assert.assertNull(varNull.getProperty(VariantRec.SIPHY_SCORE));
        } catch (Exception ex) {
            thrown = true;
            System.err.println("Exception during testing: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        Assert.assertFalse(thrown);
    }
    
        public void testMT_PRED30(){
    	//TODO
    }
    
        public void testMA_PRED30(){
    	//TODO
    }
    **/
}
