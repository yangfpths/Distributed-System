import java.io.Serializable;

import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class WrapMessage implements Serializable
{
	  private JSONObject msg;
	  
	  public WrapMessage(JSONObject message)
	  {
		  msg = message;
	  }
	  
	  public JSONObject unwrapMessage()
	  {
		 return msg;
	  }
}
