package variantPool;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import buffer.VCFFile;
import buffer.variant.VariantPool;


public class TestVariantPool {
	
	@Test
	public void TestVariantPoolCreation() {
		File emptyVCF = new File("src/test/java/testvcfs/empty.vcf");
		File freebayesVCF = new File("src/test/java/testvcfs/freebayes.single.vcf");
		File gatkVCF = new File("src/test/java/testvcfs/gatksingle.vcf");
		File solidTumorVCF = new File("src/test/java/testvcfs/solid_tumor_test1.vcf");
		File complexVCF = new File("src/test/java/testvcfs/complexVars.vcf");
		
		try {
			VariantPool pool = new VariantPool(new VCFFile(emptyVCF));
			Assert.assertTrue(pool.size() == 0); 
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		
		try {
			VariantPool pool = new VariantPool(new VCFFile(solidTumorVCF));
			Assert.assertEquals(17, pool.size());
			Assert.assertNotNull(pool.findRecord("17", 7579472));
			Assert.assertNotNull(pool.findRecord("2", 212578300, "C", "T"));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
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
			Assert.assertEquals(8, pool.size());
			Assert.assertNotNull(pool.findRecord("19", 10665691));
			Assert.assertNotNull(pool.findRecord("19", 10665691, "TTGAC", "CTGAT"));
			Assert.assertNotNull(pool.findRecord("19", 10665691, "TTGAC", "CTGAC"));
			Assert.assertNotNull(pool.findRecord("12", 57870464));
			Assert.assertNotNull(pool.findRecord("12", 57870464, "GT", "T"));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
				
		
		
	}
}
