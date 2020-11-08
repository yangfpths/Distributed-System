import java.net.InetAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.event.*;
import javafx.stage.WindowEvent;


public class ClientBoardLaucher extends Application
{	
	public static void main(String[] args) 
	{
		launch(args);
	}
	
	public void start(Stage primaryStage) throws Exception 
	{
		List<String> args = (List<String>)getParameters().getRaw();
		
		String ip_datacentre = args.get(0);
		String port_datacentre = args.get(1);
		String ip_local = IPAddsStripper(InetAddress.getLocalHost().toString());
		String port_local = args.get(2);
		
		ClientBoardModel model = new ClientBoardModel(ip_datacentre, port_datacentre,
				ip_local, port_local) ;
		
		ClientBoardController controller = new ClientBoardController(ip_local, port_local, model);
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GUI.fxml"));
		fxmlLoader.setController(controller);
		model.setController(controller);
		Parent root = fxmlLoader.load();
		
		primaryStage.setTitle("Online Shared Drawing Board");
		primaryStage.setScene(new Scene(root, 1200, 680));
		primaryStage.setResizable(false);
		
		
	    primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	        @Override
	        public void handle(WindowEvent event) {
	            try 
	            {
	            	event.consume();
	            	controller.onCloseWindow();
	            } 
	            catch (Exception e) 
	            {
	                System.out.print(e.getMessage());
	            }
	        }
	    });
		
		
		
		primaryStage.show();

	}
	
	private String IPAddsStripper(String hostname)
	{
		String raw_ip = "";
		String pattern = "\\d+.\\d+.\\d+.\\d+";
		Pattern r = Pattern.compile(pattern);
		Matcher matcher = r.matcher(hostname);
		if (matcher.find())
		{
			raw_ip = hostname.substring(matcher.start());
		}
		return raw_ip;
	}
	
}
