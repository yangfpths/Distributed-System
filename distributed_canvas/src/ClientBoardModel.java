import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class ClientBoardModel implements RemoteClientBoardInterface
{

	private Boolean isNetworkOkay;
	private String user_name, board_name, mngr_name;
	
	private String ip_datacentre;
	private String port_datacentre;
	private String ip_local;
	private String port_local;
	
	private ClientBoardController myController;
	
	private Registry reg_datacentre;
	private Registry reg_board;
	private RemoteDataCenterInterface rdc_stub;
	private RemoteClientBoardInterface board_stub;
	
	public ClientBoardModel(String ip_datacentre, String port_datacentr, String ip_local, String port_local) 
	{
		this.user_name = "";
		this.board_name = "";
		
		this.ip_datacentre = ip_datacentre;
		this.port_datacentre = port_datacentr;
		this.ip_local = ip_local;
		this.port_local = port_local;
		
		try
		{	
			// get data centre's rmi stub from remote registry
			getRemoteRMIStub();
			System.out.println("remote rmi lookup finished");

//			reg_datacentre = LocateRegistry.getRegistry(ip_datacentre, Integer.parseInt(port_datacentre));
//			rdc_stub = (RemoteDataCenterInterface)reg_datacentre.lookup("RemoteDataCentre_Stub");
//			System.out.println("remote rmi lookup finished");
			
			// register local rmi onto specified port
	    	board_stub = (RemoteClientBoardInterface)UnicastRemoteObject.exportObject(this, 0);
	    	reg_board = LocateRegistry.getRegistry(Integer.parseInt(port_local));
			System.out.println("local rmi registration finished");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}

		this.isNetworkOkay = true;

	}

	private void getRemoteRMIStub() throws Exception
	{
		// get data centre's rmi stub from remote registry
		reg_datacentre = LocateRegistry.getRegistry(ip_datacentre, Integer.parseInt(port_datacentre));
		rdc_stub = (RemoteDataCenterInterface)reg_datacentre.lookup("RemoteDataCentre_Stub");
	}
	
	public void setController(ClientBoardController ctrlr)
	{
		this.myController = ctrlr;
	}
	
	public void setUserAndBoardName(String user_name, String board_name)
	{
		this.user_name = user_name;
		this.board_name = board_name;
	}
	
	public void bindClientBoardRMI()
	{
		
		if (this.user_name.isEmpty() | this.board_name.isEmpty())
			return;
		
		try 
		{
			reg_board.rebind("RemoteClientBoard_Stub" + user_name, this.board_stub);
			System.out.println("local rmi bind for user: " + user_name + " is finished");
		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}

	public String askForBoardCreation(WrapMessage message) 
	{
		String feedback = "";
		try
		{
			if (isNetworkOkay)
				feedback = rdc_stub.createBoard(message);
			else
			{
				getRemoteRMIStub();
				System.out.println("remote rmi lookup finished");
				feedback = rdc_stub.createBoard(message);
				isNetworkOkay = true;
			}
		}
		catch(Exception e)
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
		}
		
		return feedback;
	}
	
	public String askForJoinBoard(WrapMessage message)
	{
		String feedback = "";
		try
		{
			if (isNetworkOkay)
				feedback = rdc_stub.joinBoard(message);
			else
			{
				getRemoteRMIStub();
				System.out.println("remote rmi lookup finished");
				feedback = rdc_stub.joinBoard(message);
				isNetworkOkay = true;
			}
		}
		catch(Exception e)
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
		}
		
		return feedback;
	}
	
	public String askForLeaveBoard(WrapMessage message)
	{
		String feedback = "";
		try 
		{
			feedback = rdc_stub.leaveBoard(message);
		}
		catch (Exception e) 
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
		}
		
		return feedback;
	}
	
	public void askForBroadcast(WrapMessage message)
	{
		try 
		{
			rdc_stub.broadcastMessage(message);
		}
		catch (Exception e) 
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
		}
	}
	
	public ArrayList<String> askForUserList(WrapMessage message)
	{
		ArrayList<String> userlist = new ArrayList<>() ;
		try 
		{
			userlist = rdc_stub.fetchUserList(message);
		} 
		catch (RemoteException e) 
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
		}
		
		return userlist;
	}
	
	public String askForForceLeaveAUser(WrapMessage message, Boolean dueToMngrQuit)
	{
		String feedback = "";
		try 
		{
			feedback = rdc_stub.forceLeaveBoard(message, dueToMngrQuit);
		} 
		catch (RemoteException e) 
		{
			isNetworkOkay = false;
			this.myController.onServerDown("SERVER:SHUTDOWN");
			// System.out.println(e.getMessage());
		}

		return feedback;
	}


	/* Remote methods to be invoked*/

	@Override
	public void realTimeMessagePropagate(WrapMessage message) throws RemoteException 
	{
		JSONObject jmsg = message.unwrapMessage();
		
		if (jmsg.get("MSGTYPE").equals("MANAGEMENT"))
		{
			if (jmsg.get("MANAGEACTION").equals("newcanvas"))
			{
				this.myController.onNew();
			}
			else if (jmsg.get("MANAGEACTION").equals("new_picturecanvas"))
			{
				this.myController.forceOpen((byte[])jmsg.get("new_picturecanvas"));
			}
			else if (jmsg.get("MANAGEACTION").equals("closecanvas"))
			{
				this.myController.onClose();
			}
		}
		else
		{
			this.myController.Draw(message);
		}
	}


	@Override
	public void updateUserList(String formated_userlist) throws RemoteException
	{			
		this.myController.updateUserListPane(formated_userlist);
	}

	@Override
	public void forcedToLeave(Boolean dueToMngrQuit) throws RemoteException
	{
		this.myController.forcedToLeave(dueToMngrQuit);
	}


	@Override
	public String approveJoinApplication(String user_name) throws RemoteException
	{
		
		String feedback = "";
		feedback = this.myController.onConfirmLogin_ApproveJoin(user_name);
		return feedback;
	}

	@Override
	public byte[] getCanvasAsByteArray() throws RemoteException
	{
		return this.myController.TransferImage();
	}

	@Override
	public void fetchWholeCanvas(byte[] bts) throws RemoteException
	{
		this.myController.forceOpen(bts);
	}

	@Override
	public void fetchWholeChatRoom(ArrayList<String> history) throws RemoteException
	{
		this.myController.fetchChatRoomHistory(history);
	}

	@Override
	public void realTimeNotificationPropagate(String notification) throws RemoteException
	{
		if (notification.indexOf("SERVER") != -1)
		{
			isNetworkOkay = false;
			this.myController.onServerDown(notification);
		}
		else
		{
			this.myController.updateNotificationLabel(notification);
		}

	}
	
}
