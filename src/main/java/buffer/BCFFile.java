package buffer;

public class BCFFile extends FileBuffer {

	public boolean isBinary() {
		return true;
	}
	
	@Override
	public String getTypeStr() {
		return "BCFFile";
	}

}
