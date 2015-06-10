package plugin;

/**
 * These get thrown when something happens while attempting to load a Plugin
 * @author brendan
 *
 */
public class PluginLoaderException extends Exception {
	
	public PluginLoaderException(String msg) {
		super(msg);
	}
}
