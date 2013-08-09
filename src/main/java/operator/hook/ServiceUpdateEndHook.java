package operator.hook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import json.JSONException;
import json.JSONObject;

import org.w3c.dom.NodeList;

import operator.Operator;

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

	protected static final String serviceURL = "http://localhost:9172/Dispatcher/UpdateService";
	protected static final String success = "\"Success\"";
	
	protected String opCanName;
	protected String opName;
	protected String ipAddress;
	protected int jobID;
	
	public ServiceUpdateEndHook(){
		
	}
	
	public ServiceUpdateEndHook(String opCanName,
							String opName,
							String ipAddress,
							int jobID){
		this.opCanName = opCanName;
		this.opName = opName;
		this.ipAddress = ipAddress;
		this.jobID = jobID;
	}

	public void HttpPostJSON(String url, JSONObject js) throws IOException{
		String content = js.toString();
		
		URLConnection conn = new URL(url).openConnection();
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Length", "" + content.length());
		conn.setRequestProperty("Content-Type", "application/json");
		
		OutputStream out = conn.getOutputStream();
		out.write(content.getBytes());
		out.close();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String response = br.readLine();
		br.close();
		if(!response.equals(success)) throw new IOException("Error when posting update to .NET service");
	}
	
	@Override
	public void doHook() throws Exception {
		JSONObject obj = new JSONObject();
		try {
			obj.put("opCanName", opCanName);
			obj.put("opName", opName);
			obj.put("jobID", jobID);
			obj.put("ipAddress", ipAddress);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		HttpPostJSON(serviceURL, obj);
	}

	@Override
	public void initialize(NodeList children) {
		
	}

	/**
	 * From the operator we can get all the information we need
	 */
	@Override
	public void initHook(Operator op) {
		this.opCanName = op.getClass().getCanonicalName();
		this.opName = op.getObjectLabel();
		this.ipAddress = op.getAttribute("ipAddress");
		
		// Where to we get the Operator Job ID?
		String strJobID = op.getAttribute("jobID");
		if(strJobID != null){
			this.jobID = Integer.parseInt(strJobID);
		}
	}
	
	

}
