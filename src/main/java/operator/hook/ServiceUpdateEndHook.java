package operator.hook;

import java.net.InetAddress;
import java.net.UnknownHostException;

import json.JSONException;
import json.JSONObject;
import operator.Operator;
import util.HttpUtils;

/**
 * This is an Operator End hook which is run after 
 * every operator executes.  This hook performs the exact
 * same thing as the ServiceUpdateStartHook for now.
 *
 * All operator hooks must extend the operator.hook.OperatorHook class and
 * implement the operator.IOperatorHook interface. 
 * @author quin
 *
 */
public class ServiceUpdateEndHook extends OperatorEndHook implements IOperatorEndHook {
	
	protected static final String service = "/Dispatcher/UpdateService";
	protected static final String success = "\"Success\"";
	
	protected String opCanName;
	protected String opName;
	protected String ipAddress;
	protected String jobID;
	
	public ServiceUpdateEndHook(){
		
	}
	
	public ServiceUpdateEndHook(String opCanName,
							String opName,
							String ipAddress,
							String jobID){
		this.opCanName = opCanName;
		this.opName = opName;
		this.ipAddress = ipAddress;
		this.jobID = jobID;
	}
	
	/**
	 * Sets the Operator canonical name
	 * @param opCanName
	 */
	public void setOpCanName(String opCanName){
		this.opCanName = opCanName;
	}
	
	/**
	 * Sets the Operator name
	 * @param opName
	 */
	public void setOpName(String opName){
		this.opName = opName;
	}

	/**
	 * Sets the Job ID
	 * @param jobID
	 */
	public void setJobID(String jobID){
		this.jobID = jobID;
	}
	

	@Override
	public void doHook() throws Exception {
		JSONObject obj = new JSONObject();
		try {
			obj.put("opCanName", opCanName);
			obj.put("opName", opName);
			obj.put("jobID", jobID);
			obj.put("ipAddress", ipAddress);
			obj.put("status", STATUS_STOPPING);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String responseText = HttpUtils.HttpPostJSON(serverURL + service, obj); 
		if(! responseText.equals(success)){
			throw new Exception("Error posting to the .NET service for End Hook: " + responseText);
		}
	}


	@Override
	public void initHook(Operator op) {
		this.opCanName = op.getClass().getCanonicalName();
		this.opName = op.getObjectLabel();
		try {
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		// Where to we get the Operator Job ID?
		String strJobID = this.getAttribute("jobID");
		if(strJobID != null){
			this.jobID = strJobID;
		}
	}

}
