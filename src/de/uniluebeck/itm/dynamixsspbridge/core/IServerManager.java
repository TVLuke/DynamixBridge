package de.uniluebeck.itm.dynamixsspbridge.core;

public interface IServerManager 
{

	public void startServer(String serverName);
	
	public void startServer(String servername, int port);
	
	public String getBaseURL();
	
}
