package annotation;

import gene.MitoAnnoLookupContainer;
import gene.MitoAnnoLookupContainer.MitoAnnoInfo;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Assert;

public class TestMitoAnnotation extends TestCase {

	public void testMitoAnno() {
		
		File testInputFile = new File("src/test/java/annotation/NC_012920.gbk");
		
		MitoAnnoLookupContainer lookup = new MitoAnnoLookupContainer();
		try {
			lookup.readIntervals(testInputFile);
			
			Object[] objs = lookup.getIntervalObjectsForRange("MT", 1610, 1610);
			Assert.assertTrue(objs.length==1);
			MitoAnnoInfo info = (MitoAnnoInfo)objs[0];
			Assert.assertTrue(info.featureType.equals("tRNA"));
			Assert.assertTrue(info.featureName.equals("TRNV"));
			
			objs = lookup.getIntervalObjectsForRange("MT", 1602, 1603);
			Assert.assertTrue(objs.length==1);
			info = (MitoAnnoInfo)objs[0];
			Assert.assertTrue(info.featureType.equals("tRNA"));
			Assert.assertTrue(info.featureName.equals("TRNV"));
			
			objs = lookup.getIntervalObjectsForRange("MT", 1601, 1602);
			Assert.assertTrue(objs.length==0);
			
			objs = lookup.getIntervalObjectsForRange("MT", 3200, 3300);
			Assert.assertTrue(objs.length==2);
			info = (MitoAnnoInfo)objs[1];
			Assert.assertTrue(info.featureType.equals("tRNA"));
			Assert.assertTrue(info.featureName.equals("TRNL1"));
			
			
			objs = lookup.getIntervalObjectsForRange("MT", 3200, 3235);
			Assert.assertTrue(objs.length==2);
			info = (MitoAnnoInfo)objs[1];
			Assert.assertTrue(info.featureType.equals("tRNA"));
			Assert.assertTrue(info.featureName.equals("TRNL1"));
			
			
			objs = lookup.getIntervalObjectsForRange("MT", 3300, 3310);
			Assert.assertTrue(objs.length==1);
			info = (MitoAnnoInfo)objs[0];
			Assert.assertTrue(info.featureType.equals("tRNA"));
			Assert.assertTrue(info.featureName.equals("TRNL1"));
			
			objs = lookup.getIntervalObjectsForRange("MT", 3305, 3310);
			Assert.assertTrue(objs.length==0);
			
			
			objs = lookup.getIntervalObjectsForRange("MT", 3200, 3225);
			Assert.assertTrue(objs.length==1);
			
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
}
