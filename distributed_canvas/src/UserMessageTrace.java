import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class UserMessageTrace implements Serializable
{
	private ArrayList<WrapMessage> message_trace;
	private String user_name;
	
	public UserMessageTrace(String name) 
	{
		user_name = name;
		message_trace = new ArrayList<WrapMessage>();
	}
	
	public String getUserName()
	{
		return this.user_name;
	}
	
	public void appendMessage(WrapMessage new_message)
	{
		message_trace.add(new_message);
	}
	
	public ArrayList<WrapMessage> getTrace()
	{
		return message_trace;
	}
}
