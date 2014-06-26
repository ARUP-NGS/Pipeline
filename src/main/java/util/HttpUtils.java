package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import json.JSONObject;

public class HttpUtils {
	
	public static String HttpPostJSON(String url, JSONObject js) throws IOException{
		String content = js.toString();
		URL add = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) add.openConnection();
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Length", "" + content.length());
		conn.setRequestProperty("Content-Type", "application/json");
		
		OutputStream out = conn.getOutputStream();
		out.write(content.getBytes());
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
}
