package com.example.kontrol.tcpcommunications;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class TCPServerInfo {
	private String ipAddress, name;
	private NetworkDevice device;
	private boolean isConnecting = false;
	private String connectedMessage = null;
	private String id;
	
	public TCPServerInfo(String rawMesssage) throws Exception{
		JSONParser parser = new JSONParser();
		JSONObject parsed = (JSONObject)parser.parse(rawMesssage);
		
		this.ipAddress = parsed.get("IPAddress").toString();
		this.name = parsed.get("HostName").toString();
		this.device = NetworkDevice.getEnumValue(Integer.parseInt(parsed.get("Device").toString()));
		this.id = parsed.get("HostId").toString();
	}
	
	public String getIpAddressString() {
		return ipAddress;
	}

	public String getName() {
		return name;
	}

	public NetworkDevice getDevice(){
		return device;
	}

	public void setIsConnecting(boolean b) {
		this.isConnecting = b;
	}
	
	public boolean isConnecting(){
		return this.isConnecting;
	}

	public void setConnectedMessage(String string) {
		this.connectedMessage = string;
	}
	
	public String getConnectedMessage(){
		return this.connectedMessage;
	}
	
	public String getId(){
		return this.id;
	}
}
