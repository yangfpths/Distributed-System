package Client;

import java.net.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
public class Client  
{
	private  Socket socket;
	private  DataInputStream input;
	private  DataOutputStream output;
	public Client(String ip, int port)
	{
		try 
		{
			 socket = new Socket(ip, port);
			 input = new DataInputStream(socket.getInputStream());
		     output = new DataOutputStream(socket.getOutputStream());    
		}
		catch (UnknownHostException e) 
		{
			 System.out.println("Unknown Host!");
		} 
		
		catch(IOException e)
		{
		 System.out.println("There is a problem of connecting the Server");
		}
		
	}
	
  public String Transfer(String Message)
  {
		try 
	    {    
		output.writeUTF(Message);
		output.flush();
		return input.readUTF();
		} 
	    catch (IOException e) 
	    {
	    System.out.println("Message Transfered to Server Failed!");
		}		
		catch(NullPointerException e)
		{
		System.out.println("Can't connect to the server!");	
		}
		return "Can't get information from the server";
  }
  
  
  
}
