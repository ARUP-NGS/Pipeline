package util.reviewDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * A manifest that supports writing / saving operations
 * @author brendan
 *
 */
public class WritableManifest extends SampleManifest {
	
	WritableManifest(File source, Map<String, String> manifest,
			Map<String, File> files) {
		super(source, manifest, files);
		// TODO Auto-generated constructor stub
	}
	
	public WritableManifest(SampleManifest manifest) {
		super(manifest.getSourceFile(), manifest.manifest, manifest.files);
	}

	boolean manifestModified = false;
	
	/**
	 * Put the given key / value pair into the manifest. This does not automatically save
	 * @param key
	 * @param value
	 */
	public void put(String key, String value) {
		manifestModified = true;
		manifest.put(key, value);
	}
	
	/**
	 * Remove the item with the given key from the manifest
	 * @param key
	 * @return
	 */
	public String remove(String key) {
		return manifest.remove(key);
	}
	
	/**
	 * Write all keys/values in manifest field to the manifest file. This overwrites the entire file
	 * and does not preserve keys in the file that are not in the manifest object. 
	 * @throws IOException
	 */
	public void save() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter( getManifestFile() ));
		for(String key : manifest.keySet()) {
			writer.write(key + "=" + manifest.get(key) + "\n");
		}
		writer.close();
	}
	

}
