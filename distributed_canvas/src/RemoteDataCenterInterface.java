import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteDataCenterInterface extends Remote
{
	public String createBoard(WrapMessage message) throws RemoteException;
	public String joinBoard(WrapMessage message) throws RemoteException;
	public String leaveBoard(WrapMessage message) throws RemoteException;
	public String forceLeaveBoard(WrapMessage message, Boolean dueToMngrQuit) throws RemoteException;
	
	public void broadcastMessage(WrapMessage message) throws RemoteException;
	public ArrayList<String> fetchUserList(WrapMessage message) throws RemoteException;
	
	
}

