import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.ArrayList;

public interface RemoteClientBoardInterface extends Remote
{
	public void updateUserList(String formated_userlist) throws RemoteException;
	public void realTimeMessagePropagate(WrapMessage message) throws RemoteException;
	
	// public void PropagateAllExistingMsg(HashMap<String, UserMessageTrace> user_traces) throws RemoteException;
	
	public void forcedToLeave(Boolean dueToMngrQuit) throws RemoteException;
	public String approveJoinApplication(String user_name) throws RemoteException;

	public byte[] getCanvasAsByteArray() throws RemoteException;
	public void fetchWholeCanvas(byte[] bts) throws RemoteException;
	public void fetchWholeChatRoom(ArrayList<String> history) throws RemoteException;
	
	public void realTimeNotificationPropagate(String notification) throws RemoteException;
}
