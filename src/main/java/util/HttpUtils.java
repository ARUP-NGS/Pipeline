package util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import json.JSONObject;

public class HttpUtils {
	
	static final int CONNECT_TIMEOUT_MS = 1000000; //Time to wait until done reading response, in milliseconds
	static final int READ_TIMEOUT_MS = 0; //Time to wait until done reading response, in ms 
	
	public static String HttpPostJSON(String url, JSONObject js) throws IOException{
		return HttpPostJSON(url, js, false);
	}
	
	public static String HttpPostJSON(String url, JSONObject js, boolean gzip) throws IOException{
		String content = js.toString();
		URL add = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) add.openConnection();
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
		conn.setReadTimeout(READ_TIMEOUT_MS);
		

		
		byte[] byteContent; 
		if (gzip) {
			conn.setRequestProperty("Content-Encoding", "gzip");
			byteContent = gZipString(content);
		} else {
			byteContent = content.getBytes(); 	
		}

		conn.setRequestProperty("Content-Length", "" + byteContent.length);
		conn.setRequestProperty("Content-Type", "application/json");
		
		OutputStream out = conn.getOutputStream();
		out.write(byteContent);

		
		out.close();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder response = new StringBuilder();
		
		String line = br.readLine();
		while(line != null) {
			response.append(line);
			line = br.readLine();
		}
		
		br.close();
		return response.toString();
	}
	
	public static String HttpGet(String url) throws IOException {
		URL add = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) add.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "text/text");
		conn.setDoOutput(true);
		OutputStream out = conn.getOutputStream();
		out.close();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line = br.readLine();
		while(line != null) {
			response.append(line);
			line = br.readLine();
		}
		
		br.close();
		return response.toString();
	}
	
	private static byte[] gZipString(String str) throws IOException {
	        if (str == null || str.length() == 0) {
	            return new byte[0];
	        }
	        ByteArrayOutputStream obj=new ByteArrayOutputStream();
	        GZIPOutputStream gzip = new GZIPOutputStream(obj);
	        gzip.write(str.getBytes("UTF-8"));
	        gzip.close();
	        
	        return obj.toByteArray();
	}
}
