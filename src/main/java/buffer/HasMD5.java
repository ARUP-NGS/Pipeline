package buffer;

/**
 * Buffers that can compute their own MD5 sum should implement this. 
 * @author brendan
 *
 */
public interface HasMD5 {

	public String getMD5();
	
}
