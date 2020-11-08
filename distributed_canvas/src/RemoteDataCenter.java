import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class RemoteDataCenter implements RemoteDataCenterInterface
{
	private static HashMap<String, BoardDataManager> database;
	
	public static void main(String[] args)
	{
		database = new HashMap<String, BoardDataManager>();
		try 
		{
			String IPADDS = InetAddress.getLocalHost().toString();
			int PORT = Integer.valueOf(args[0]);
			
			System.out.println("This is the Remote Data Centre!\n To create/join a board, "
					+ "connect to RMI registry at:\nHost Name: "+ IPADDS + "\nPort: "+PORT);
			
			RemoteDataCenter rdc = new RemoteDataCenter();
			RemoteDataCenterInterface rdc_stub = (RemoteDataCenterInterface)UnicastRemoteObject.exportObject(rdc, 0);
			Registry reg = LocateRegistry.getRegistry(PORT);
			reg.bind("RemoteDataCentre_Stub", rdc_stub);		
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            // broadcast to each BOARD that the server is about to shutdown
            String[] board_list = database.keySet().toArray(new String[0]);
            for (int i=0; i< board_list.length; i++)
            {
                System.out.println(i);
                database.get(board_list[i]).realTimeNotificationMessage("SERVER:QUIT");
            }
        }));
	}

	/*********************************** RMI methods **********************************************************/
	
	@Override
	public String createBoard(WrapMessage message) throws RemoteException 
	{
		String feedback = "";
		JSONObject jmsg = message.unwrapMessage();
		
		 if (((String)jmsg.get("CONNECTION")).equals("create"))
		 {
			String intended_boardname = (String)jmsg.get("BOARDNAME");
			String intended_username = (String)jmsg.get("USERNAME");
			String user_ip = (String)jmsg.get("CLIENTRMIIP");
			String user_port = (String)jmsg.get("CLIENTRMIPORT");
			
			if (database.keySet().contains(intended_boardname))
			{	// the intended board name already exists
				feedback = "DENIED";
			}
			else
			{
				BoardDataManager mngr = new BoardDataManager(intended_boardname, intended_username);

				mngr.addToUserTraces(intended_username, new UserMessageTrace(intended_username));
				mngr.addToUserRegistries(intended_username, user_ip+":"+user_port);

				mngr.realTimeNotificationMessage();
				mngr.realTimeBroadcastUserList();

				database.put(intended_boardname, mngr);
				mngr.setMyDatabase(database);
				
				feedback = "OKAY";
			}
		 }
		return feedback;
	}

	@Override
	public String joinBoard(WrapMessage message) throws RemoteException 
	{
		String feedback = "";
		JSONObject jmsg = message.unwrapMessage();
		
		if (((String)jmsg.get("CONNECTION")).equals("join"))
		{
			String intended_boardname = (String)jmsg.get("BOARDNAME");
			String intended_username = (String)jmsg.get("USERNAME");
			String user_ip = (String)jmsg.get("CLIENTRMIIP");
			String user_port = (String)jmsg.get("CLIENTRMIPORT");
			
			if (!database.keySet().contains(intended_boardname))
				// the intended board name doesn't exist
				feedback = "DENIED_BOARDNAME";
			else
			{
				BoardDataManager mngr = database.get(intended_boardname);
				
				if (mngr.isUserExist(intended_username))
					// the intended user name already exists
					feedback = "DENIED_USERNAME";
				else if (mngr.getBoardSize() >= 3)
				{
					feedback = "DENIED_CAPACITY";
				}
				else
				{	
					feedback = mngr.getManagerApproval(intended_username);
					if (feedback.equals("OKAY"))
					{
						mngr.addToUserTraces(intended_username, new UserMessageTrace(intended_username));
						mngr.addToUserRegistries(intended_username, user_ip+":"+user_port);
						mngr.realTimeBroadcastUserList();

						mngr.pushWholeCanvasTo(intended_username);
						mngr.pushWholeChatRoomTo(intended_username);
					}
				}			
			}
		}
		return feedback;
	}

	@Override
	public String leaveBoard(WrapMessage message) throws RemoteException 
	{
		
		String feedback = "";
		JSONObject jmsg = message.unwrapMessage();
		
		if (((String)jmsg.get("CONNECTION")).equals("leave"))
		{
			String boardname = (String)jmsg.get("BOARDNAME");
			String username = (String)jmsg.get("USERNAME");
			
			if (!database.keySet().contains(boardname))
				// the intended board name doesn't exist
				feedback = "DENIED";
			else
			{
				BoardDataManager mngr = database.get(boardname);
				
				if (!mngr.isUserExist(username))
					// the intended user name already exists
					feedback = "DENIED";
				else
				{	
					if (username.equals(mngr.getMngrName()))
					{
						mngr.cancelRealTimeNotificationMessage();
						mngr.forceEveryUserOut();
						mngr = null;
						database.remove(boardname);
					}
					else
					{
						mngr.removeUserTrace(username);
						mngr.removeUserRegistry(username);
						mngr.realTimeBroadcastUserList();
					}
					feedback = "OKAY";
				}			
			}
		}
		return feedback;
	}

	@Override
	public String forceLeaveBoard(WrapMessage message, Boolean dueToMngrQuit) throws RemoteException
	{
		String feedback = "";

		JSONObject jmsg = message.unwrapMessage();
		String boardname = (String)jmsg.get("BOARDNAME");
		String username = (String)jmsg.get("MANAGEACTION");
		
		if (database.keySet().contains(boardname))
		{
			BoardDataManager mngr = database.get(boardname);
			if (mngr.isUserExist(username))
			{
				if (username.equals(mngr.getMngrName()))
					feedback = "DENIED_MANAGERITSELF";
				else
				{
					mngr.forceAUserOut(username, dueToMngrQuit);
					feedback = "OKAY";
				}
			}
			else
			{
				feedback = "DENIED";
			}
		}
		return feedback;
	}
	
	@Override
	public void broadcastMessage(WrapMessage message) throws RemoteException 
	{
		JSONObject jmsg = message.unwrapMessage();
		String boardname = (String)jmsg.get("BOARDNAME");
		String username = (String)jmsg.get("USERNAME");
		
		if (database.keySet().contains(boardname))
		{
			BoardDataManager mngr = database.get(boardname);
			if (mngr.isUserExist(username))
			{
				// System.out.println("broadcasting...");
				mngr.realTimeBroadcast(username, message);
			}
		}
	}
	
	@Override
	public ArrayList<String> fetchUserList(WrapMessage message) throws RemoteException 
	{
		ArrayList<String> outcome = new ArrayList<>();
		
		JSONObject jmsg = message.unwrapMessage();
		String boardname = (String)jmsg.get("BOARDNAME");
		String username = (String)jmsg.get("USERNAME");
		
		if (database.keySet().contains(boardname))
		{
			BoardDataManager mngr = database.get(boardname);
			if (mngr.isUserExist(username))
			{
				outcome = mngr.getUserList();
			}
		}
		return outcome;
	}

}
