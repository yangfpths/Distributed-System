package Server;
import java.net.*;


import javax.net.ServerSocketFactory;

import java.io.*;
public class Server {
  private static Dictionary dt = new Dictionary();
  public static void main(String args[]) 
  {
    dt.ReadFile(args[1]);
    ServerSocketFactory factory = ServerSocketFactory.getDefault();
	try(ServerSocket server = factory.createServerSocket(Integer.parseInt(args[0])))
	{  
	while(true)
	{
		Socket client = server.accept();
		Thread t = new Thread(() -> Serveroperation(client));
		System.out.println("Client connected!");
		t.start();
	}
  
    }
	catch(IOException e)
	{
		System.out.println("Server creation failed!");
	}
	catch(Exception e)
	{
		System.out.println("Illegal input arguments!");
	}
  }
  
  
  private static void Serveroperation(Socket client)
  {
	  while(!client.isClosed())
	  {
		try {	
		  DataInputStream input = new DataInputStream(client.getInputStream());		
		    DataOutputStream output = new DataOutputStream(client.getOutputStream());
		    String key = input.readUTF();
		    String Message="";
		    if(key.split(",")[0].equals("Search"))
		    	    Message = Search(key);
		    else if(key.split(",")[0].equals("Add"))
		    	    Message = Add(key);
		    else
		    	    Message = Delete(key);
		    output.writeUTF(Message); 
		}
	   catch (EOFException e)
	         {
		    System.out.println("Client disconnected!");
		    try 
		     {
				client.close();
			 } 
		    catch (IOException e1) 
		     {
			    System.out.println("Errors occurred in disconnection!");
			 }
	         }
	   catch (IOException e) 
	         {
		       	System.out.println("Connection errors occurred at Server");
		     }
	  } 
  }
  
  
  private static String Search(String key)
  {
	  if(key.split(",").length!=1)
	  {	  
		String words = key.split(",")[1];
	    if(dt.ht.containsKey(words))
		    return dt.ht.get(words); 
	  }
	  return "No Results";
  }
  
  
  private static String Add(String key)
  {
	  if(dt.ht.containsKey(key.split(",")[1]))
	        return "Failed! The word already exists";
	  else 
		  {
		  dt.ht.put(key.split(",")[1], key.split(",")[2]);
		  dt.WriteFile();
		  }
	      
	  return "Operation Success!";
  }
  
  private static String Delete(String key)
  {
	  if(!dt.ht.containsKey(key.split(",")[1]))
	       return "Failed! The word does not exist";
	  else
	      {
		  dt.ht.remove(key.split(",")[1]);
		  dt.WriteFile();
	      }
	  return "Operation Success!";
  }
  
  
}
