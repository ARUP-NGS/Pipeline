package core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import operator.oncology.OncologyUtils;

public class TestOncologyUtils {

	@Test
	public void TestGetExonToksFromLocusStr() {
		String testStrSuccess = "E1E2";
		try {
			String[] outStr = OncologyUtils.getExonToksFromLocusStr(testStrSuccess);
			Assert.assertTrue(outStr[0].equals("1"));
			Assert.assertTrue(outStr[1].equals("2"));
		} catch (Exception e) {
			Assert.assertTrue(false);
		}

		String testStrNoMatch = "E1";
		try {
			String[] outStr = OncologyUtils.getExonToksFromLocusStr(testStrNoMatch);
			Assert.assertTrue(outStr[0].length() ==0); 
			Assert.assertTrue(outStr[1].length() ==0); 
		} catch (Exception e) {
			Assert.assertTrue(false);
		}

	}
}
