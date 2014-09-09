package annotation;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import junit.framework.TestCase;

import org.junit.Assert;

import util.JSONVarsGenerator;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class TestJSONVarsAnnotator extends TestCase {

	public void testJSONVarsWriter() {
		
		VariantPool pool = new VariantPool();
		
		VariantRec varA = new VariantRec("1", 10, 11, "A", "G", 100.0, true);
		varA.addProperty(VariantRec.POP_FREQUENCY, 0.1);
		varA.addProperty(VariantRec.EXOMES_FREQ, 0.123);
		varA.addAnnotation(VariantRec.CDOT, "c.A450T");
		varA.addAnnotation(VariantRec.GENE_NAME, "BANG");
		pool.addRecord(varA);
		
		VariantRec varB = new VariantRec("1", 15, 17, "AAA", "G", 100.0, false);
		varB.addProperty(VariantRec.POP_FREQUENCY, 0.2);
		varB.addAnnotation(VariantRec.GENE_NAME, "BANG");
		pool.addRecord(varB);
		
		VariantRec varC = new VariantRec("1", 20, 21, "TA", "GC", 100.0, false);
		varC.addProperty(VariantRec.POP_FREQUENCY, 0.5);
		varC.addAnnotation(VariantRec.GENE_NAME, "BLAH");
		pool.addRecord(varC);
		
		VariantRec varD = new VariantRec("2", 50, 51, "-", "G", 100.0, true);
		varD.addProperty(VariantRec.POP_FREQUENCY, 0.2);
		varD.addProperty(VariantRec.EXOMES_FREQ, 0.25);
		varD.addAnnotation(VariantRec.GENE_NAME, "BLAH");
		pool.addRecord(varD);
		
		try {
			JSONObject json = JSONVarsGenerator.createJSONVariants(pool);
			
			Assert.assertTrue(json.has("variant.list"));
			JSONArray varArr = json.getJSONArray("variant.list");
			Assert.assertTrue(varArr.length()==4);
			
			//Associate json objects with positions so we can make sure the right
			//variants have the right annotations
			JSONObject objA = null;
			JSONObject objB = null;
			JSONObject objC = null;
			JSONObject objD = null;
			
			for(int i=0; i<varArr.length(); i++) {
				JSONObject obj = varArr.getJSONObject(i);
				if (! obj.has("chr")) Assert.assertTrue(false);
				if (! obj.has("pos")) Assert.assertTrue(false);
				if (! obj.has("ref")) Assert.assertTrue(false);
				if (! obj.has("alt")) Assert.assertTrue(false);
				if (! obj.has("pop.freq")) Assert.assertTrue(false);
				
				if (obj.getString("chr") == "1" && obj.getInt("pos")==10) {
					objA = obj;
				}
				if (obj.getString("chr") == "1" && obj.getInt("pos")==15) {
					objB = obj;
				}
				if (obj.getString("chr") == "1" && obj.getInt("pos")==20) {
					objC = obj;
				}
				if (obj.getString("chr") == "2" && obj.getInt("pos")==50) {
					objD = obj;
				}
			}
			
			if (objA == null) Assert.assertTrue(false);
			if (objB == null) Assert.assertTrue(false);
			if (objC == null) Assert.assertTrue(false);
			if (objD == null) Assert.assertTrue(false);
			
			Assert.assertTrue(objA.getDouble("pop.freq")==0.1);
			Assert.assertTrue(objA.has("exomes6500.frequency"));
			Assert.assertEquals(objA.getString("gene"), "BANG");
			Assert.assertTrue(objA.has("zygosity"));
			Assert.assertTrue(objB.getDouble("pop.freq")==0.2);
			Assert.assertTrue(objC.getDouble("pop.freq")==0.5);
			Assert.assertTrue(objD.getDouble("exomes6500.frequency")==0.25);
			Assert.assertEquals(objD.getString("gene"), "BLAH");
			
		} catch (JSONException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}

}

