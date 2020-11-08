
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONObject;

public class BoardDataManager 
{
	private HashMap<String, BoardDataManager> my_database;
	private String board_name; 
	private final String manager;
	private HashMap<String, UserMessageTrace> user_traces;
	private HashMap<String, UserMessageTrace> left_users_traces;

	private ArrayList<String> chat_history;
	private HashMap<String, Registry> user_registries;
	
	private Set<String> mostRecentDrawers; // to be updated every half second
	Timer timer;
	
	public BoardDataManager(String board_name, String manager_name)
	{
		this.board_name = board_name;
		this.manager = manager_name;
		user_traces = new HashMap<String, UserMessageTrace>();
		left_users_traces = new HashMap<String, UserMessageTrace>();
		user_registries = new HashMap<String, Registry>();
		chat_history = new ArrayList<>();

		mostRecentDrawers = new HashSet<String>();
		timer = new Timer();
	}

	public void setMyDatabase(HashMap<String, BoardDataManager> my_database)
	{
		this.my_database = my_database;
	}

	public String getBoardName()
	{
		return this.board_name;
	}
	
	public String getMngrName()
	{
		return this.manager;
	}

	public int getBoardSize()
	{
		return this.user_traces.size();
	}

	public Boolean isUserExist(String user_name)
	{
		if (this.user_traces.keySet().contains(user_name))
			return true;
		else
			return false;
	}
	
	public ArrayList<String> getUserList()
	{
		Set<String> nonmanging_users = user_traces.keySet();
		nonmanging_users.remove(manager);
		
		String[] array = nonmanging_users.toArray(new String[0]);
		ArrayList<String> outcome = new ArrayList<String>(Arrays.asList(array));
		
		return outcome;
	}
	
	public synchronized void addToUserTraces(String user_name, UserMessageTrace cltTrace)
	{
		this.user_traces.put(user_name, cltTrace);
		System.out.println(user_name + " just created/joined the board of " + board_name);
		
	}
	
	public synchronized void addToUserRegistries(String user_name, String ipandport)
	{
		String[] ipandport_ = ipandport.split(":");
		String ip = ipandport_[0];
		int port = Integer.valueOf(ipandport_[1]);
		
		try
		{
			user_registries.put(user_name, LocateRegistry.getRegistry(ip, port));
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public void removeUserTrace(String user_name)
	{	
		UserMessageTrace the_trace = user_traces.get(user_name);
		this.left_users_traces.put(user_name, the_trace); // archive it to some place
		this.user_traces.remove(user_name); // remove it from the live board database

		System.out.println(user_name + " no longer exists on the board of " + board_name);
		
	}
	
	public void removeUserRegistry(String user_name)
	{

		// no need to archive the user's rmi registry
		this.user_registries.remove(user_name);
	}
	
	public String getManagerApproval(String user_name)
	{
		String feedback = "";
		try
		{
			Registry reg = user_registries.get(manager);
			RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+manager);
			feedback = board_stub.approveJoinApplication(user_name);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		return feedback;
	}

	public void forceEveryUserOut()
	{
		try
		{				
			String[] user_list = user_traces.keySet().toArray(new String[0]);
			
			for (int i=0; i<user_list.length; i++)
			{				
				forceAUserOut(user_list[i], true);
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public void forceAUserOut(String user_name, Boolean dueToMngrQuit)
	{
		try
		{
			Registry reg = user_registries.get(user_name);
			RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_name);
			
			removeUserTrace(user_name);
			removeUserRegistry(user_name);
			realTimeBroadcastUserList();

			board_stub.forcedToLeave(dueToMngrQuit);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public void pushWholeCanvasTo(String user_name)
	{
		
		try
		{
			// 1. get the whole canvas from manager
			Registry reg = user_registries.get(manager);
			RemoteClientBoardInterface board_stub1 = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+manager);
			byte[] bts = board_stub1.getCanvasAsByteArray();

			System.out.println("size="+bts.length);

			// 2. push to the new comer
			Registry reg2 = user_registries.get(user_name);
			RemoteClientBoardInterface board_stub2 = (RemoteClientBoardInterface)reg2.lookup("RemoteClientBoard_Stub"+user_name);
			board_stub2.fetchWholeCanvas(bts);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());

			removeUserTrace(user_name);
			removeUserRegistry(user_name);
			realTimeBroadcastUserList();
		}
	}

	public void pushWholeChatRoomTo(String user_name)
	{
		try
		{
			// 1 .get the whole chat room history as archived
			ArrayList<String> history = this.chat_history;

			// 2. push to the new comer
			Registry reg = user_registries.get(user_name);
			RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_name);
			board_stub.fetchWholeChatRoom(history);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());

			removeUserTrace(user_name);
			removeUserRegistry(user_name);
			realTimeBroadcastUserList();
		}
	}
	
	public void realTimeBroadcast(String user_name, WrapMessage message)
	{

		mostRecentDrawers.add(user_name);
		trySaveToChatHistory(message);
//		try
//		{
//			String[] user_list = user_traces.keySet().toArray(new String[0]);
//
//			for (int i=0; i<user_list.length; i++)
//			{
//				user_traces.get(user_list[i]).appendMessage(message);
//
//				if (user_list[i].equals(user_name))
//					continue; // no need to broadcast to the sender itself
//
//				Registry reg = user_registries.get(user_list[i]);
//				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_list[i]);
//				board_stub.realTimeMessagePropagate(message);
//			}
//		}
//		catch (Exception e)
//		{
//			System.out.println(e.getMessage());
//		}

		String[] user_list = user_traces.keySet().toArray(new String[0]);
		for (int i=0; i<user_list.length; i++)
		{
			String tmp_usrname = user_list[i];

			user_traces.get(tmp_usrname).appendMessage(message);
			if (tmp_usrname.equals(user_name))
				continue; // no need to broadcast to the sender itself

			try
			{
				Registry reg = user_registries.get(tmp_usrname);
				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+tmp_usrname);
				board_stub.realTimeMessagePropagate(message);
			}
			catch (Exception e)
			{
				// System.out.println(e.getMessage());

				if (tmp_usrname.equals(manager))
				{
					forceEveryUserOut();
					my_database.remove(board_name);
				}
				else
				{
					removeUserTrace(tmp_usrname);
					removeUserRegistry(tmp_usrname);
					realTimeBroadcastUserList();
				}
			}
		}
	}

	public void trySaveToChatHistory(WrapMessage message)
	{
		JSONObject jmsg = message.unwrapMessage();
		if(jmsg.get("MSGTYPE").equals("CHAT"))
		{
			chat_history.add((String)jmsg.get("USERNAME")+":"+(String)jmsg.get("TEXTTOPRINT")+"\n");
		}
	}
	
	public void realTimeBroadcastUserList()
	{
//		try
//		{
//			String[] user_list = user_traces.keySet().toArray(new String[0]);
//
//			String formated_userlist = "";
//			for (int i=0; i<user_list.length;i++)
//			{
//				if (user_list[i].equals(manager))
//					formated_userlist += user_list[i] + " (Manager of Board: " +board_name +")\n";
//				else
//					formated_userlist += user_list[i] + " \n";
//			}
//
//			for (int i=0; i<user_list.length; i++)
//			{
//				Registry reg = user_registries.get(user_list[i]);
//				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_list[i]);
//				board_stub.updateUserList(formated_userlist);
//			}
//		}
//		catch (Exception e)
//		{
//			System.out.println(e.getMessage());
//		}

		String[] user_list = user_traces.keySet().toArray(new String[0]);

		String formated_userlist = "";
		for (int i=0; i<user_list.length;i++)
		{
			if (user_list[i].equals(manager))
				formated_userlist += user_list[i] + " (Manager of Board: " +board_name +")\n";
			else
				formated_userlist += user_list[i] + " \n";
		}

		for (int i=0; i<user_list.length; i++)
		{
			String tmp_usrname = user_list[i];
			try
			{
				Registry reg = user_registries.get(tmp_usrname);
				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+tmp_usrname);
				board_stub.updateUserList(formated_userlist);
			}
			catch (Exception e)
			{
				// System.out.println(e.getMessage());

				if (tmp_usrname.equals(manager))
				{
					forceEveryUserOut();
					my_database.remove(board_name);
				}
				else
				{
					removeUserTrace(tmp_usrname);
					removeUserRegistry(tmp_usrname);
					realTimeBroadcastUserList();
				}
			}
		}
	}

	public void realTimeNotificationMessage(String notification)
	{
//		try
//		{
//			String[] user_list = user_traces.keySet().toArray(new String[0]);
//			for (int i=0; i<user_list.length; i++)
//			{
//				Registry reg = user_registries.get(user_list[i]);
//				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_list[i]);
//				board_stub.realTimeNotificationPropagate(notification);
//			}
//		}
//		catch (Exception e)
//		{
//			System.out.println(e.getMessage());
//		}

		String[] user_list = user_traces.keySet().toArray(new String[0]);
		for (int i=0; i<user_list.length; i++)
		{
			String tmp_usrname = user_list[i];

			try
			{
				Registry reg = user_registries.get(tmp_usrname);
				RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+tmp_usrname);
				board_stub.realTimeNotificationPropagate(notification);
			}
			catch (Exception e)
			{
				// System.out.println(e.getMessage());

				if (tmp_usrname.equals(manager))
				{
					forceEveryUserOut();
					my_database.remove(board_name);
				}
				else
				{
					removeUserTrace(tmp_usrname);
					removeUserRegistry(tmp_usrname);
					realTimeBroadcastUserList();
				}
			}
		}
	}
	
	public void realTimeNotificationMessage()
	{
		TimerTask beeper = new TimerTask() {
			@Override
			public void run() {
				
//				try
//				{
//					String notification = "";
//					String[] recentdrawers = mostRecentDrawers.toArray(new String[0]);
//					for (int i=0; i<recentdrawers.length;i++)
//						notification += recentdrawers[i] + " ";
//
//					String[] user_list = user_traces.keySet().toArray(new String[0]);
//					for (int i=0; i<user_list.length; i++)
//					{
//						Registry reg = user_registries.get(user_list[i]);
//						RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+user_list[i]);
//						board_stub.realTimeNotificationPropagate(notification);
//					}
//					mostRecentDrawers.clear();
//				}
//				catch(Exception e)
//				{
//					System.out.println(e.getMessage());
//				}

				String notification = "";
				String[] recentdrawers = mostRecentDrawers.toArray(new String[0]);
				for (int i=0; i<recentdrawers.length;i++)
					notification += recentdrawers[i] + " ";

				String[] user_list = user_traces.keySet().toArray(new String[0]);
				for (int i=0; i<user_list.length; i++)
				{
					String tmp_usrname = user_list[i];

					try
					{
						Registry reg = user_registries.get(tmp_usrname);
						RemoteClientBoardInterface board_stub = (RemoteClientBoardInterface)reg.lookup("RemoteClientBoard_Stub"+tmp_usrname);
						board_stub.realTimeNotificationPropagate(notification);
					}
					catch (Exception e)
					{
						// System.out.println(e.getMessage());

						if (tmp_usrname.equals(manager))
						{
							forceEveryUserOut();
							my_database.remove(board_name);
						}
						else
						{
							removeUserTrace(tmp_usrname);
							removeUserRegistry(tmp_usrname);
							realTimeBroadcastUserList();
						}
					}
				}
				mostRecentDrawers.clear();
			}
		};
		timer.schedule(beeper, 10, 100); 
	}
	
	public void cancelRealTimeNotificationMessage()
	{
		timer.cancel();
	}

}



