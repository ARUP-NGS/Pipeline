package variantPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import pipeline.Pipeline;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;


public class TestVariantPool {
	
	
	//The adding variants algorithm is actually a little tricky and involves merging two sorted
	//lists, so some tests for it are warranted. 
	@Test
	public void TestAddVariants() {
		VariantPool poolA;
		VariantPool poolB;
		
		//Generate some random variants to add
		int varsToAdd = 10000;
		int maxChrLength = 1000000;
		List<VariantRec> vars = new ArrayList<VariantRec>();
		while(vars.size() < varsToAdd) {
			int pos = (int)(Math.random()*maxChrLength);
			VariantRec rec = new VariantRec("1", pos, pos+1, "A", "C");
			vars.add(rec);
		}
		
		poolA = new VariantPool(vars);
		poolB = new VariantPool(vars);
		
		poolA.addAll(poolB, true);
		System.out.println("Pool A size is " + poolA.size() + " and should be " + 2*varsToAdd);
		Assert.assertTrue(poolA.size() == 2*varsToAdd);
	
	
		vars.clear();
		vars.add(  new VariantRec("1", 1, 2, "A", "C") );
		vars.add(  new VariantRec("1", 2, 3, "A", "C") );
		vars.add(  new VariantRec("1", 2, 3, "A", "C") );
		vars.add(  new VariantRec("1", 2, 3, "A", "C") );
		vars.add(  new VariantRec("1", 4, 5, "A", "C") );
		vars.add(  new VariantRec("1", 5, 6, "A", "C") );
		
		poolA = new VariantPool(vars);
		poolA.addAll(poolA, false);
		System.out.println("Pool A size is " + poolA.size() + " and should be " + 4);
		Assert.assertTrue(poolA.size() == 4);
	}
	
	@Test
	public void TestVariantPoolCreation() {
		File emptyVCF = new File("src/test/java/testvcfs/empty.vcf");
		File freebayesVCF = new File("src/test/java/testvcfs/freebayes.single.vcf");
		File gatkVCF = new File("src/test/java/testvcfs/gatksingle.vcf");
		File solidTumorVCF = new File("src/test/java/testvcfs/solid_tumor_test1.vcf");
		File complexVCF = new File("src/test/java/testvcfs/complexVars.vcf");
		
		File bcrablCSV = new File("src/test/java/testcsvs/bcrabl_annotated.csv");
		File gatkCSV = new File("src/test/java/testcsvs/gatksingle_annotated.csv");
		
		File inputVCFTemplate = new File("src/test/java/annotation/testVariantPool_VCF.xml");
		File inputCSVTemplate = new File("src/test/java/annotation/testVariantPool_CSV.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");			
	
		try {
			VariantPool pool = new VariantPool(new VCFFile(emptyVCF));
			Assert.assertTrue(pool.size() == 0); 
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		
		try {
			//The solid tumor VCF contains a malformed GT
			VariantPool pool = new VariantPool(new VCFFile(solidTumorVCF));
			Assert.assertTrue(false); //should not reach this
		} catch (IOException e) {
			//expected
		} catch (IllegalStateException e) {
			//expected
		}
		
						
		try {
			VariantPool pool = new VariantPool(new VCFFile(freebayesVCF));
			Assert.assertEquals(69, pool.size());
			Assert.assertNotNull(pool.findRecord("1", 36128));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		
		
		try {
			VariantPool pool = new VariantPool(new VCFFile(complexVCF));
			Assert.assertEquals(10, pool.size());
			Assert.assertNotNull(pool.findRecord("19", 10665691));
			Assert.assertNotNull(pool.findRecord("19", 10665691, "TTGAC", "CTGAT"));
			Assert.assertNotNull(pool.findRecord("19", 10665691, "T", "C"));
			Assert.assertNotNull(pool.findRecord("12", 57870464));
			Assert.assertNotNull(pool.findRecord("12", 57870464, "G", "-"));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
				
		try {
			VariantPool pool = new VariantPool(new CSVFile(bcrablCSV));
			Assert.assertEquals(2, pool.size());
			Assert.assertNotNull(pool.findRecord("ABL1", 749, "G", "A"));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		try {
			VariantPool pool = new VariantPool(new CSVFile(gatkCSV));
			Assert.assertEquals(889, pool.size());
			Assert.assertNotNull(pool.findRecord("1", 14673, "G", "C"));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		try {
			//Run pipeline on "src/test/java/testvcfs/myeloid.vcf"
			Pipeline pplVCF = new Pipeline(inputVCFTemplate, propertiesFile.getAbsolutePath());			
			pplVCF.initializePipeline();
			pplVCF.stopAllLogging();
			pplVCF.execute();

			//Get variants input into VariantPool XML element 
			VariantPool VPVCF = (VariantPool)pplVCF.getObjectHandler().getObjectForLabel("VariantPool");
			int VPVCFsize = VPVCF.size();
			Assert.assertEquals(90, VPVCFsize);
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		try {		
			//Run pipeline on "src/test/java/annotation/gatksingle_annotated.csv"
			Pipeline pplCSV = new Pipeline(inputCSVTemplate, propertiesFile.getAbsolutePath());			
			pplCSV.initializePipeline();
			pplCSV.stopAllLogging();
			pplCSV.execute();

			//Get variants from XML element
			VariantPool VPCSV = (VariantPool)pplCSV.getObjectHandler().getObjectForLabel("VariantPool");
			int VPCSVsize = VPCSV.size();
			Assert.assertEquals(889, VPCSVsize);
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
}
