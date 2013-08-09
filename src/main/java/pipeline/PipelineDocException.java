package pipeline;

/**
 * These are thrown when there are basic errors reading the pipeline document
 * (for instance, when the document root name is not "Pipeline")
 * @author brendan
 *
 */
public class PipelineDocException extends Exception {

	public PipelineDocException(String message) {
		super(message);
	}
}
