package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class Dictionary {

	
	
  public static Hashtable<String, String> ht = new Hashtable<String, String>();	 
  public  void ReadFile(String filename)
   {
	   try (BufferedReader br = new BufferedReader(new FileReader(filename))) 
		{
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
	        if(!line.trim().isEmpty())
	        {
	        	   if(line.split("  ").length!=1)
	        	   {
	    	       ht.put(line.split("  ")[0].toLowerCase(), line.split("  ")[1]);
	        	   }  
	         }
		    }		    
		 }
	   catch(IOException e)
	    {
		   System.out.println("File not found!");
	    }
    }
   
   public void WriteFile()
   {
	   Enumeration<String> Words = ht.keys();
	   try (BufferedWriter bw = new BufferedWriter(new FileWriter("Dictionary.txt")))
	   {
       while(Words.hasMoreElements())   
       {
    	    String str = Words.nextElement();
    	    bw.write(str+"  "+ht.get(str)+"\n");
       }
	   bw.close();
	   }
	   catch(IOException e)
	   {
		   System.out.println("Dictionary written error!");	   
	   }   
   }
   
  
   
	
}
