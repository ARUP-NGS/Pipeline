package operator;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.WranglerStatusWriter;

/**
 * Creates a new WranglerStatusWriter and registers it as a PipelineListener, it will
 * then log events in a JobWrangler-friendly way
 * @author brendan
 *
 */
public class StatusLogger extends Operator {

	private WranglerStatusWriter writer = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Pipeline ppl = this.getPipelineOwner();
		writer = new WranglerStatusWriter();
		ppl.addListener(writer);
	}

	@Override
	public void initialize(NodeList children) {
		//No initialization needed
	}
	
	/**
	 * A reference to the actual writer that listens to the pipeline and logs messages
	 * @return
	 */
	public WranglerStatusWriter getWriter() {
		return writer;
	}

}
