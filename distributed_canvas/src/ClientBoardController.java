import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import javafx.embed.swing.SwingFXUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.event.*;

import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.SnapshotParameters;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;

import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ClientBoardController
{
	private File file = null;
	private Canvas canvas;
	private Canvas tempCanvas;	//drag effect
	private GraphicsContext tgc;
	private GraphicsContext gc;
	private boolean hascanvas;
	public ColorPicker cp;
	public Slider slider;
	public AnchorPane canvasPane;
	
	@FXML
	private GridPane LoginPane;
	@FXML
	private TextField BoardNameTF;
	@FXML
	private TextField UserNameTF;
	@FXML
	private Button confirmLoginBtn_Create;
	@FXML
	private Button confirmLoginBtn_Join;
	@FXML
	private Button cancelLoginBtn;
	@FXML
	private Menu mngrMenu;
	@FXML
	private MenuItem kickButton;
	@FXML
	private TextField UserToBeKicked;
	@FXML
	private TextArea usersTextArea;
	@FXML
	private Label notificationLabel;
	
	public RadioButton eraserButton;
	public RadioButton brushButton;
	public RadioButton lineButton;
	public RadioButton rectButton;
	public RadioButton fillRectButton;
	public RadioButton circleButton;
	public RadioButton fillCircleButton;
	public RadioButton ovalButton;
	public RadioButton fillOvalButton;
	public RadioButton textButton;
	
	public Button sendButton;
	public TextArea messageTextArea;
	public TextArea chatroomTextArea;
	
	@FXML
	public MenuItem newButton;
	public MenuItem openButton;
	public MenuItem saveButton;
	public MenuItem saveAsButton;
	public MenuItem closeButton;
	
	private double orgX;
	private double orgY;
	private double desX;
	private double desY;
	private double size;
	private Color color;
	
	private String user_name, board_name;
	private Boolean isManager = false;
	private Boolean isOnBoard = false;
	
	private String ip_local;
	private String port_local;
	
	private ClientBoardModel model;


	private HashMap<String, LastPoint> lastdrawing;
	
	public ClientBoardController(String ip_local, String port_local, ClientBoardModel model)
	{
		this.ip_local = ip_local;
		this.port_local = port_local;
		this.model = model;

		lastdrawing = new HashMap<String, LastPoint>();

	}

	@SuppressWarnings({"unchecked"})

	private JSONObject createMessage(String type, String detail)
	{
		JSONObject message = new JSONObject();
		
		// shared fields
		message.put("MSGTYPE", type);
		message.put("BOARDNAME", this.board_name);
		message.put("USERNAME", this.user_name);
		
		if (type == "CONNECTION")
		{
			message.put("CONNECTION", detail); 
			// "detail" can be "create", "join" or "leave"
			message.put("CLIENTRMIIP", this.ip_local);
			message.put("CLIENTRMIPORT", this.port_local);
		}
		else if (type == "CHAT")
		{
			message.put("TEXTTOPRINT", detail);
		}
		else if (type == "MANAGEMENT")
		{
			message.put("MANAGEACTION", detail);
		}
		else // type == "DRAWSHAPE" or "DRAWTEXT"
		{
			if (type == "DRAWSHAPE")
				message.put("SHAPETODRAW", detail);
			else // "DRAWTEXT"
				message.put("TEXTTODRAW", detail);
				message.put("ORIGIN", new DPoint(this.orgX,this.orgY, this.color.toString(), this.size));
				message.put("DESTINATION",new DPoint(this.desX,this.desY, this.color.toString(), this.size));	
		}
		return message;
	}
	
	/* methods for drawing*/
	public void setCurrentStatus()
	{   	
		this.color = cp.getValue();
		this.size = slider.getValue();
	}
	
	public void setOriginalPoints(double x,double y)
	{
		this.orgX = x;
		this.orgY = y;
	}
	
	public void setDestinationPoints(double x,double y)
	{
		this.desX = x;
		this.desY = y;
	}

    public void DrawText(JSONObject message)
    {
		double x0 = ((DPoint)message.get("ORIGIN")).getX();
		double y0 = ((DPoint)message.get("ORIGIN")).getY();
		double size = ((DPoint)message.get("ORIGIN")).getSize();
		String texttodraw = (String)message.get("TEXTTODRAW");
		
		Color color = Color.valueOf(((DPoint)message.get("DESTINATION")).getColor());
		
		gc.setFont(new Font(size));
		gc.setFill(color);
		gc.fillText(texttodraw,x0,y0);
    }
    	
    public void DrawBrush(JSONObject message)
    {
		String shapetodraw = (String) message.get("SHAPETODRAW");
		Color color = Color.valueOf(((DPoint)message.get("DESTINATION")).getColor());
		String drawer = (String) message.get("USERNAME");
		double size = ((DPoint)message.get("DESTINATION")).getSize();
		double x0 = ((DPoint)message.get("ORIGIN")).getX();
		double y0 = ((DPoint)message.get("ORIGIN")).getY();
		double x1 = ((DPoint)message.get("DESTINATION")).getX();
		double y1 = ((DPoint)message.get("DESTINATION")).getY();
		
		// if (this.orgX != x0 | this.orgY != y0)
		// {
		// 	setOriginalPoints(x0,y0);
		// }
		
		if (shapetodraw.equals("BrushPoint"))
		{
			gc.beginPath();
			gc.setFill(color);
			gc.fillOval(x0-size/2, y0-size/2, size, size);
			
			// gc.lineTo(x0, y0);
			// gc.setStroke(color);
		}
		else if(shapetodraw.equals("BrushLine"))
		{
			// gc.lineTo(x1, y1);
			// gc.setStroke(color);
			// gc.setFill(color);

			// first, draw a point
			gc.setFill(color);
			gc.fillOval(x1-size/2, y1-size/2, size, size);

			LastPoint lp = lastdrawing.get(drawer);
			if (lp!=null)
			{
				// check time

				long time_lapse = Math.abs(lp.getCrtMilliSec()-System.currentTimeMillis());
				if (time_lapse <= 100)
				{
					gc.setLineWidth(size);
					gc.setStroke(color);
					gc.strokeLine(lp.getX_coord(), lp.getY_coord(), x1, y1);
					gc.setLineCap(StrokeLineCap.ROUND);
					gc.setLineJoin(StrokeLineJoin.ROUND);
				}
			}

			lp = new LastPoint(drawer, x1, y1, System.currentTimeMillis());
			lastdrawing.put(drawer, lp);

		}

		// gc.setLineWidth(size);
		// gc.setLineCap(StrokeLineCap.ROUND);
		// gc.setLineJoin(StrokeLineJoin.ROUND);

		// gc.stroke();
		// gc.setEffect(null);

    }

    public void DrawEraser(JSONObject message)
    {
		String shapetoerase = (String) message.get("SHAPETODRAW");
		double size = ((DPoint)message.get("DESTINATION")).getSize();
    	double x0 = ((DPoint)message.get("ORIGIN")).getX();
    	double y0 = ((DPoint)message.get("ORIGIN")).getY();
    	double x1 = ((DPoint)message.get("DESTINATION")).getX();
    	double y1 = ((DPoint)message.get("DESTINATION")).getY();
    	
 	    if (shapetoerase.equals("EraserPoint"))
 	    	gc.clearRect(x0, y0, size*2,size*2);
 	    else if (shapetoerase.equals("EraserLine"))
 	    	gc.clearRect(x1, y1, size*2,size*2);  
    }

    public void DrawShape(JSONObject jmsg)
    {
	   double orgX = ((DPoint)jmsg.get("ORIGIN")).getX();
	   double orgY = ((DPoint)jmsg.get("ORIGIN")).getY();
	   double desX = ((DPoint)jmsg.get("DESTINATION")).getX();
	   double desY = ((DPoint)jmsg.get("DESTINATION")).getY();
	   String shapetodraw = (String) jmsg.get("SHAPETODRAW");
	   Color color = Color.valueOf(((DPoint)jmsg.get("DESTINATION")).getColor());
	   double size = ((DPoint)jmsg.get("DESTINATION")).getSize();
	   
	   gc.lineTo(desX, desY);
	   gc.setStroke(color);
	   gc.setLineWidth(size);
	   gc.setFill(color);
	
	   double x = Math.min(orgX, desX);
	   double y = Math.min(orgY, desY);
	   double width = Math.abs(orgX - desX);
	   double height = Math.abs(orgY - desY);
	   double radius = Math.min(width, height);
	
	   if (shapetodraw.equals("Line")) 
		   gc.strokeLine(orgX,orgY,desX,desY);
	   if (shapetodraw.equals("Rect")) 
		   gc.strokeRect(x,y,width,height);
	   if (shapetodraw.equals("FillRect")) 
		   gc.fillRect(x,y,width,height);
	   if (shapetodraw.equals("Circle")) 
		   gc.strokeOval(x,y,radius,radius);
	   if (shapetodraw.equals("FillCircle")) 
		   gc.fillOval(x,y,radius,radius);
	   if (shapetodraw.equals("Oval")) 
		   gc.strokeOval(x,y,width,height);
	   if (shapetodraw.equals("FillOval")) 
		   gc.fillOval(x,y,width,height);    	
    }
    
    public synchronized void Draw(WrapMessage message)
    {
/*		JSONObject jmsg = message.unwrapMessage();
		String[] shape = {"Line","Rect","FillRect","Circle","FillCircle","Oval","FillOval"};

		System.out.println("Drawing");
		if (jmsg.get("MSGTYPE").equals("DRAWSHAPE"))
		{
			String shapetodraw = (String) jmsg.get("SHAPETODRAW");
			
			System.out.println(shapetodraw);
			
			if (shapetodraw.indexOf("Brush")!=-1)
				DrawBrush(jmsg);
			if(shapetodraw.indexOf("Eraser")!=-1)
				DrawEraser(jmsg);
			if(Arrays.asList(shape).contains(shapetodraw))
				DrawShape(jmsg);
		}
		else if (jmsg.get("MSGTYPE").equals("DRAWTEXT"))
		{
			DrawText(jmsg);
		}
		else if(jmsg.get("MSGTYPE").equals("CHAT"))
		{
			SendChatMessage(jmsg);
		}*/


		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				
				JSONObject jmsg = message.unwrapMessage();
				String[] shape = {"Line","Rect","FillRect","Circle","FillCircle","Oval","FillOval"};

				System.out.println("Drawing");
				if (jmsg.get("MSGTYPE").equals("DRAWSHAPE"))
				{
					String shapetodraw = (String) jmsg.get("SHAPETODRAW");
					
					System.out.println(shapetodraw);
					
					if (shapetodraw.indexOf("Brush")!=-1)
						DrawBrush(jmsg);
					if(shapetodraw.indexOf("Eraser")!=-1)
						DrawEraser(jmsg);
					if(Arrays.asList(shape).contains(shapetodraw))
						DrawShape(jmsg);
				}
				else if (jmsg.get("MSGTYPE").equals("DRAWTEXT"))
				{
					DrawText(jmsg);
				}
				else if(jmsg.get("MSGTYPE").equals("CHAT"))
				{
					SendChatMessage(jmsg);
				}

			}
		});
    	
    }
    
    @SuppressWarnings("unchecked")
	private void setEventHandlers() 	//drag effect
    {
		canvas.setCursor(Cursor.HAND);
		gc = canvas.getGraphicsContext2D();
		tgc = tempCanvas.getGraphicsContext2D();	// drag effect
		
		tempCanvas.setOnMousePressed(e -> 	//drag effect
		{
			setCurrentStatus();
			setOriginalPoints(e.getX(),e.getY());
			
            if (brushButton.isSelected())
			{
            	JSONObject jmsg = createMessage("DRAWSHAPE","BrushPoint");
            	WrapMessage message = new WrapMessage(jmsg);
            	
            	DrawBrush(jmsg);
				
				Thread submit_to_server = new Thread(){
					@Override
					public void run() {
						// call server (data centre)'s RMI
						if (isOnBoard)
							model.askForBroadcast(message);
					}
				};
				
				submit_to_server.start();

			}

            if (eraserButton.isSelected()) 
            {
                JSONObject jmsg = createMessage("DRAWSHAPE","EraserPoint");
                WrapMessage message = new WrapMessage(jmsg);
                
                DrawEraser(jmsg);
				
				Thread submit_to_server = new Thread(){
					@Override
					public void run() {
						// call server (data centre)'s RMI
						if (isOnBoard)
							model.askForBroadcast(message);
					}
				};
				submit_to_server.start();
            }

            if (textButton.isSelected()) 
            {
    			TextField tf = new TextField();
    			tf.setLayoutX(orgX);
    			tf.setLayoutY(orgY);
    			tf.setMinWidth(400);
    			canvasPane.getChildren().add(tf);
    			tf.requestFocus();
    			tf.setOnKeyPressed(key -> 
    			{
					 if (key.getCode() == KeyCode.ENTER) 
					 {
					     Font font = new Font(size*3);
					     gc.setFont(font);
					     gc.setStroke(color);
					     String texttodraw = tf.getText();
					     canvasPane.getChildren().remove(tf);
					     
					     JSONObject jmsg = createMessage("DRAWTEXT",texttodraw);
					     WrapMessage message = new WrapMessage(jmsg);
					     DrawText(jmsg);
						
						Thread submit_to_server = new Thread(){
							@Override
							public void run() {
								// call server (data centre)'s RMI
								if (isOnBoard)
									model.askForBroadcast(message);	
							}
						};
						
						submit_to_server.start();
					 }
	   			 });
            }
        });

        tempCanvas.setOnMouseDragged(e -> 	// drag effect
        {
        	setCurrentStatus();
        	setDestinationPoints(e.getX(),e.getY());

        	if (brushButton.isSelected())
			{
				JSONObject jmsg = createMessage("DRAWSHAPE","BrushLine");
				WrapMessage message = new WrapMessage(jmsg);
				DrawBrush(jmsg);
				
				Thread submit_to_server = new Thread(){
					@Override
					public void run() {
						// call server (data centre)'s RMI
						if (isOnBoard)
							model.askForBroadcast(message);
					}
				};

				submit_to_server.start();
			}

            if (eraserButton.isSelected()) 
            {
            	JSONObject jmsg = createMessage("DRAWSHAPE","EraserLine");
            	WrapMessage message = new WrapMessage(jmsg);
            	DrawEraser(jmsg);
				
				Thread submit_to_server = new Thread(){
					@Override
					public void run() {
						// call server (data centre)'s RMI
						if (isOnBoard)
							model.askForBroadcast(message);
					}
				};
				
				submit_to_server.start();
            }

            // drag effect
            if ((lineButton.isSelected())||(rectButton.isSelected())||(fillRectButton.isSelected())||
                (circleButton.isSelected())||(fillCircleButton.isSelected())||
                (ovalButton.isSelected())||(fillOvalButton.isSelected())) {
                
                clearTemp();
                
                double x = Math.min(orgX, desX);
                double y = Math.min(orgY, desY);
                double width = Math.abs(orgX - desX);
                double height = Math.abs(orgY - desY);
                double radius = Math.min(width, height);
                
                GraphicsContext tgc = tempCanvas.getGraphicsContext2D();
                tgc.setStroke(color);
                tgc.setFill(color);
                tgc.setLineWidth(size);
                tgc.setLineCap(StrokeLineCap.ROUND);
                tgc.setLineJoin(StrokeLineJoin.ROUND);
                
                if (lineButton.isSelected()) tgc.strokeLine(orgX, orgY, desX, desY);
                
                if (rectButton.isSelected()) tgc.strokeRect(x, y, width, height);
                
                if (fillRectButton.isSelected()) tgc.fillRect(x, y, width, height);
                
                if (circleButton.isSelected()) tgc.strokeOval(x, y, radius, radius);
                
                if (fillCircleButton.isSelected()) tgc.fillOval(x, y, radius, radius);
                
                if (ovalButton.isSelected()) tgc.strokeOval(x, y, width, height);
                
                if (fillOvalButton.isSelected()) tgc.fillOval(x, y, width, height);
            }


        });

        tempCanvas.setOnMouseReleased(e -> 	// drag effect
        {
			clearTemp();  // drag effect

			setCurrentStatus();
			setDestinationPoints(e.getX(),e.getY());
			
			String detail="";
			if (lineButton.isSelected()) 
				detail = "Line";
			if (rectButton.isSelected()) 
				detail = "Rect";
			if (fillRectButton.isSelected()) 
				detail = "FillRect";
			if (circleButton.isSelected()) 
				detail = "Circle";
			if (fillCircleButton.isSelected()) 
				detail = "FillCircle";
			if (ovalButton.isSelected()) 
				detail = "Oval";
			if (fillOvalButton.isSelected()) 
				detail = "FillOval";
			if (!detail.equals(""))
			{
				JSONObject jmsg = createMessage("DRAWSHAPE", detail);
				WrapMessage message = new WrapMessage(jmsg);
				DrawShape(jmsg);
				
				Thread submit_to_server = new Thread(){
					@Override
					public void run() {
						// call server (data centre)'s RMI
						if (isOnBoard)
							model.askForBroadcast(message);
					}
				};
				submit_to_server.start();
			}
           
        });
    }

    public void initialize() 
	{
	    cp.setValue(Color.BLACK);
	    mngrMenu.setDisable(true);
	}

    public void sendMessage() 
    {
		
    	if (isOnBoard)
    	{

			JSONObject message = createMessage("CHAT", messageTextArea.getText());
			SendChatMessage(message);
			messageTextArea.setText("");

			WrapMessage msg = new WrapMessage(message);
			if(isOnBoard)
				model.askForBroadcast(msg);
		}
    }
    
    public void SendChatMessage(JSONObject message)
    {
    	String chat = (String)message.get("USERNAME")+":"+(String) message.get("TEXTTOPRINT")+"\n";
    	chatroomTextArea.appendText(chat);
    }

    public void onCreateSingleBoardMenuItem()
    {
    	if (isOnBoard)
    	{
    		Alert alert = new Alert(AlertType.INFORMATION);
	    	alert.setTitle("Login Confirmation");
	    	alert.setHeaderText("You are currently within a (connected) board.");
	    	alert.showAndWait();
    	}
    	else
    	{
    		onNew();
    		if (LoginPane.isVisible() == true)
    			LoginPane.setVisible(false);
    		mngrMenu.setDisable(false);
	    	kickButton.setDisable(true);
    	}
    }

    public void onCreateMenuItem()
    {
    	
    	if (isOnBoard)
    	{
	    	Alert alert = new Alert(AlertType.CONFIRMATION);
	    	alert.setTitle("Login Confirmation");
	    	alert.setHeaderText("You are currently within a board.");
	    	
	    	if (isManager)
				alert.setContentText("Do you want to quit the current board and join a new one? \n(You are the manager of this board. Quiting it means closing the whole board.)");
			else
				alert.setContentText("Do you want to quit the current board and join a new one?");
	
	    	Optional<ButtonType> result = alert.showAndWait();
	    	if (result.get() == ButtonType.OK)
	    	{
	    	    // ... user chose OK， then leave the current board , and end if
	    		onLogout(true);
	    	}
	    	else 
	    	{
	    	    // ... user chose CANCEL or closed the dialog, quit the whole method
	    		return;
	    	}
    	}
    	
		canvasPane.getChildren().clear();

    	LoginPane.setVisible(true);
    	confirmLoginBtn_Create.setVisible(true);
		confirmLoginBtn_Join.setVisible(false);
		BoardNameTF.setText("");
		UserNameTF.setText("");
		BoardNameTF.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!UserNameTF.getText().isEmpty())
				confirmLoginBtn_Create.setDisable(newValue.trim().isEmpty());
		});
    	
    	UserNameTF.textProperty().addListener((observable, oldValue, newValue) -> {
    		if (!BoardNameTF.getText().isEmpty())
    			confirmLoginBtn_Create.setDisable(newValue.trim().isEmpty());
    	});
    }
    
    public void onJoinMenuItem()
    {
    	if (isOnBoard)
    	{
	    	Alert alert = new Alert(AlertType.CONFIRMATION);
	    	alert.setTitle("Login Confirmation");
	    	alert.setHeaderText("You are currently within a board.");

			if (isManager)
				alert.setContentText("Do you want to quit the current board and join a new one? \n(You are the manager of this board. Quiting it means closing the whole board.)");
			else
				alert.setContentText("Do you want to quit the current board and join a new one?");
	
	    	Optional<ButtonType> result = alert.showAndWait();
	    	if (result.get() == ButtonType.OK)
	    	{
	    		// ... user chose OK， then leave the current board , and end if
	    		onLogout(true);
	    	}
	    	else 
	    	{
	    	    // ... user chose CANCEL or closed the dialog, quit the whole method
	    		return;
	    	}
    	}

    	canvasPane.getChildren().clear();
    	
		LoginPane.setVisible(true);
		confirmLoginBtn_Create.setVisible(false);
		confirmLoginBtn_Join.setVisible(true);
		BoardNameTF.setText("");
		UserNameTF.setText("");

		BoardNameTF.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!UserNameTF.getText().isEmpty())
				confirmLoginBtn_Join.setDisable(newValue.trim().isEmpty());
		});
		UserNameTF.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!BoardNameTF.getText().isEmpty())
				confirmLoginBtn_Join.setDisable(newValue.trim().isEmpty());
		});
		
	}
    
    public void onLeaveMenuItem()
    {
    	if (!isOnBoard)
    	{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Come on! You are not even on a board...");

			alert.showAndWait();
    	}
    	else
    	{
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Leave Confirmation");
			if (isManager)
			{
				alert.setHeaderText("You are the manager of this board. Quiting it means closing the whole board.");
				alert.setContentText("Do you still want to quit?");
			}
			else
			{
				alert.setHeaderText("Are you sre you wanna quit this board?");
			}

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK)
			{
				// ... user chose OK， then leave the current board , and end if
				;
			}
			else 
			{
			    // ... user chose CANCEL or closed the dialog, quit the whole method
				return;
			}
	    	
	    	onLogout(true);
    	}
    }
    
    public void onLogout(Boolean non_windowclose)
    {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				
				JSONObject jmsg = createMessage("CONNECTION", "leave");
				WrapMessage message = new WrapMessage(jmsg);

				String feedback = model.askForLeaveBoard(message);

				if (isManager)
				{
					isManager = false;
					mngrMenu.setDisable(true);
				}

				isOnBoard = false;
				usersTextArea.setText("Current Users:");
				notificationLabel.setText("Disconnected");

				canvasPane.getChildren().clear();
				
				if (!non_windowclose)
					System.exit(0);
			}
		});
		
    }
    
	public void onCancelLogin()
	{
		LoginPane.setVisible(false);
	}

    public void onConfirmLogin_Create()
    {
		this.board_name = BoardNameTF.getText();
		this.user_name = UserNameTF.getText();

		model.setUserAndBoardName(user_name, board_name);
		model.bindClientBoardRMI();
		
		
		JSONObject jmsg = createMessage("CONNECTION", "create");
		WrapMessage message = new WrapMessage(jmsg);
		String feedback = model.askForBoardCreation(message);
		System.out.println(feedback);
		if (feedback.contains("OKAY"))
		{
			LoginPane.setVisible(false);
			this.isOnBoard = true;
			this.isManager = true;
			
			this.notificationLabel.setText("Connected");
			onNew();

			mngrMenu.setDisable(false);
			kickButton.setDisable(false);
		}
		else if (feedback.equals("DENIED"))
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Sorry. This board name already exists. Try another one. ");

			alert.showAndWait();
		}
    }

	@SuppressWarnings("rawtypes")
	public String onConfirmLogin_ApproveJoin(String user_name)
	{

		String answer = "";

		@SuppressWarnings("unchecked")
		final FutureTask query = new FutureTask(new Callable() {
   			@Override
    		public Object call() throws Exception {
        		String feedback = "";
        		Alert alert = new Alert(AlertType.CONFIRMATION);
        		alert.setTitle("User Ask for joining the board.");
        		alert.setHeaderText("You are the manager of this board. \nDo you approve the joining application of the following user? ");
        		alert.setContentText(user_name);
        		
        	    Timeline idlestage = new Timeline( new KeyFrame( Duration.seconds(1), new EventHandler<ActionEvent>()
        	    {
        	    	int pass = 0;
        	        @Override
        	        public void handle( ActionEvent event )
        	        {
        	        	alert.setContentText(user_name + " (" + (4-pass) + ") seconds to decline...");
        	        	if (pass ==4)
						{
							alert.setResult(ButtonType.OK);
							alert.hide();
						}
						pass++;
        	        }
        	    }));
        	    
        	    idlestage.setCycleCount( 5 );
        	    idlestage.play();
        		
        		Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK)
				{
					// ... user chose OK， then leave the current board , and end if
					return "OKAY";
				}
				else 
				{
				    // ... user chose CANCEL or closed the dialog, quit the whole method
					return "DENIED";
				}
    		}
		});
		
		try
		{
			Platform.runLater(query);
			answer = query.get().toString();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return answer;
	}
	
    public void onConfirmLogin_Join()
    {
    	
    	this.board_name = BoardNameTF.getText();
		this.user_name = UserNameTF.getText();
		
		model.setUserAndBoardName(user_name, board_name);
		model.bindClientBoardRMI();
		
		LoginPane.setVisible(false);
		
		JSONObject jmsg = createMessage("CONNECTION", "join");
		WrapMessage message = new WrapMessage(jmsg);
		String feedback = model.askForJoinBoard(message);
		System.out.println(feedback);
		
		if (feedback.equals("OKAY"))
		{
			this.isOnBoard = true;
			this.mngrMenu.setDisable(true);
			this.notificationLabel.setText("Connected");
			
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Welcome on board. :D ");

			alert.showAndWait();
		}
		else if (feedback.equals("DENIED_BOARDNAME") | feedback.equals(""))
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Sorry. This board name does not exist. Try another one. ");

			alert.showAndWait();
		}
		else if (feedback.equals("DENIED_USERNAME"))
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Sorry. This user name already exists. ");

			alert.showAndWait();
		}
		else if (feedback.equals("DENIED_CAPACITY"))
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Sorry. This board has reached it maximum capacity. ");

			alert.showAndWait();
		}
		else if (feedback.equals("DENIED"))
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Sorry. The manager rejected your joining request. ");

			alert.showAndWait();
		}
	}
    
	public void onKick()
	{
		String theusertokick = UserToBeKicked.getText();

		if (theusertokick.isEmpty())
			return;

		System.out.println("the manager is kicking out "+theusertokick);
		JSONObject jmsg = createMessage("MANAGEMENT", theusertokick);
		WrapMessage message = new WrapMessage(jmsg);
		
		String feedback = model.askForForceLeaveAUser(message, false);

		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information Dialog");
		alert.setHeaderText(null);

		if (feedback.equals("OKAY"))
		{
			alert.setContentText("You have successfully kicked out " + theusertokick);
			
		}
		else if (feedback.equals("DENIED"))
		{
			alert.setContentText("This user does not even exist.");
		}
		else
		{
			alert.setContentText("Dear Manager, You cannot kick out yourself!");
			
		}
		alert.showAndWait();
		UserToBeKicked.setText("");
	}
	
	public void onNew() 
	{
		file = null;
		if (isManager)
		{
			// renew other member's canvas
			JSONObject jmsg = createMessage("MANAGEMENT", "newcanvas");
			WrapMessage message = new WrapMessage(jmsg);
			model.askForBroadcast(message);
		}
		newCanvas();
	}
	
	private void newCanvas() 
	{
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				
				canvasPane.getChildren().clear();
				canvas = new Canvas(canvasPane.getWidth(), canvasPane.getHeight());

				//drag effect
                tempCanvas = new Canvas(canvasPane.getWidth(), canvasPane.getHeight());
                canvasPane.getChildren().add(canvas);
                canvasPane.getChildren().add(tempCanvas);
                setEventHandlers();



				// gc = canvas.getGraphicsContext2D();
				// canvasPane.getChildren().add(canvas);
				// setEventHandlers(canvas);
			}
		});
	}

	public void onOpen() throws IOException 
	{
	   	GetOpenFile(); 
	    if(file!=null)
	    {
	    	newCanvas();
	    	Image image = new Image(new FileInputStream(file));
	    	
			JSONObject jmsg = createMessage("MANAGEMENT", "new_picturecanvas");
			System.out.println("a");
			
			byte[] img_bytes = TransferImage(image);
			System.out.println("aaa");
			jmsg.put("new_picturecanvas", img_bytes);
			System.out.println("b");
			WrapMessage message = new WrapMessage(jmsg);
			System.out.println("c");
			model.askForBroadcast(message);
	    	
			Platform.runLater(new Runnable(){
				@Override
				public void run() {
					gc.drawImage(image, 0, 0, canvasPane.getWidth(), canvasPane.getHeight());
									
				}
			});
	    }
	}
  
	public void GetOpenFile()
	{
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.bmp", "*.png", "*.jpg", "*.gif"));
		file = fc.showOpenDialog(null);
	}
	
	public void GetSaveFile()
	{
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.bmp", "*.png", "*.jpg", "*.gif"));
		file = fc.showSaveDialog(null);    
	}
	
	public void Save() throws IOException
	{
		    if(file!=null)
	    {
	    	// drag effect
	        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
	        canvas.snapshot(null, writableImage);
	        RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
	        ImageIO.write(renderedImage, "png", file);
	    }
	}
	
	public void onSave() throws IOException 
	{
	    if (file == null) 
	    { 
	    	   GetSaveFile();
	    }    
	    Save();
	}
	
	public void onSaveAs() throws IOException 
	{
	    GetSaveFile();
	    Save();
	}
	
	public void onClose() 
	{
		if (isManager)
		{
			// renew other member's canvas
			JSONObject jmsg = createMessage("MANAGEMENT", "closecanvas");
			WrapMessage message = new WrapMessage(jmsg);
			model.askForBroadcast(message);
		}
		
		Platform.runLater(new Runnable(){
			
			@Override
			public void run() {
				
				canvasPane.getChildren().clear();
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Information Dialog");
				alert.setHeaderText(null);
				alert.setContentText("The manager has (temporarily) closed the canvas.");
				
				alert.showAndWait();

			}
		});
	}
	
	public void onCloseWindow()
	{
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Leave Confirmation");
				if (isManager)
				{
					alert.setHeaderText("You are the manager of this board. Quiting it means closing the whole board.");
					alert.setContentText("Do you still want to quit?");
				}
				else
				{
					alert.setHeaderText("Are you sure you wanna quit this board?");
				}

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK)
				{
					if (isOnBoard)
						onLogout(false);
					else
						System.exit(0);
				}
			}
		});
	}
	
	public void forcedToLeave(Boolean dueToMngrQuit)
	{

		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				
				if (isManager)
					isManager = false;

				isOnBoard = false;
				usersTextArea.setText("Current Users:");
				notificationLabel.setText("Disconnected");
				
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Information Dialog");
				alert.setHeaderText(null);

				if (dueToMngrQuit)
				{
					alert.setContentText("The manager cancelled this board.");
				}
				else
				{
					alert.setContentText("You've been removed out of the board by the manager.");
				}

				alert.showAndWait();
			}
		});
	}

	public void fetchChatRoomHistory(ArrayList<String> history)
	{
		try
		{
			Platform.runLater(new Runnable(){
				@Override
				public void run()
				{
					chatroomTextArea.setText("");
					//for (int i = history.size() - 1; i >= 0; i--)
					for (int i=0; i<history.size(); i++)
						chatroomTextArea.appendText((String) history.get(i));
				}
			});
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public void forceOpen(byte[] bts)
	{
		try
		{
			Platform.runLater(new Runnable(){
				@Override
				public void run() {

					try
					{
						BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(bts));

						Image img = SwingFXUtils.toFXImage(bimg, null);
						canvasPane.getChildren().clear();
						canvas = new Canvas(canvasPane.getWidth(), canvasPane.getHeight());
						// drag effect
						tempCanvas = new Canvas(canvasPane.getWidth(), canvasPane.getHeight());
						gc = canvas.getGraphicsContext2D();
						canvasPane.getChildren().add(canvas);
						canvasPane.getChildren().add(tempCanvas);

						setEventHandlers();	// drag effect

						gc.drawImage(img, 0, 0, canvasPane.getWidth(), canvasPane.getHeight());
					}

					catch(Exception e)
					{
						System.out.println(e.getMessage());
					}

				}
			});
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public byte[] TransferImage(Image img)
	{
		byte[] result = new byte[0];
		
		try 
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			RenderedImage renderedImage = SwingFXUtils.fromFXImage(img, null);
			ImageIO.write(renderedImage, "png", baos);
			result = baos.toByteArray();
		}
		catch (Exception e) 
		{
			
			System.out.println(e.getMessage());
		}
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public byte[] TransferImage()
	{
		byte[] result = new byte[0];

		try
		{
			@SuppressWarnings("unchecked")
			final FutureTask query = new FutureTask(new Callable() {
				@Override
				public Object call() throws Exception {

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					WritableImage writableImage = new WritableImage(892, 534);

					SnapshotParameters param = new SnapshotParameters();
    				param.setDepthBuffer(true);
    				param.setFill(Color.TRANSPARENT);

					canvas.snapshot(param, writableImage);
					RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);

					ImageIO.write(renderedImage, "png", baos);

					return baos.toByteArray();
				}
			});

			Platform.runLater(query);
			result = (byte[])query.get();
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}

		return result;

	}

	public void updateNotificationLabel(String notification)
	{
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				if (notification.equals("") | notification.equals(" "))
				{
					notificationLabel.setText("Connected");
				}
				else
				{
					notificationLabel.setText("The user(s) who is(are) currently drawing/typing: " + notification);
				}
			}
		});
	}
	
	public void updateUserListPane(String formated_userlist)
	{
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				
				usersTextArea.setText("Current Users: \n" +formated_userlist);
			}
		});
	}

	//drag effect
    private void clearTemp() {
        GraphicsContext tgc = tempCanvas.getGraphicsContext2D();
        tgc.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
    }

    // information buttons
	public void onAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText(null);
		alert.setContentText("DRAW IT TOGETHER\n1.0.0\nDate:13 Oct 2017\n\nSocketThreading Studio\nFei Yang, Huo Ruixi, Wang Kangyi, Yang Yifan");

		alert.showAndWait();
	}

	public void onFaq() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("FAQ");
		alert.setHeaderText(null);
		alert.setContentText("Choose a drawing tool and drag on the canvas~");

		alert.showAndWait();
	}

	// respond to server crash or shutdown
	public void onServerDown(String notification)
	{
		Platform.runLater(new Runnable(){
			@Override
			public void run() {

				isOnBoard = false;
				if (isManager)
				{
					isManager = false;
					mngrMenu.setDisable(true);
				}
				usersTextArea.setText("Current Users:");
				notificationLabel.setText("Disconnected");

				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Network issue");
				alert.setHeaderText(null);
				alert.setContentText(notification);
				alert.showAndWait();
			}
		});
	}
}

class LastPoint
{
	String drawer_name;
	double x_coord;
	double y_coord;
	long crtMilliSec;

	public LastPoint(String drawer_name, double x_coord, double y_coord, long crtMilliSec)
	{
		this.drawer_name = drawer_name;
		this.x_coord = x_coord;
		this.y_coord = y_coord;
		this.crtMilliSec = crtMilliSec;
	}

	public LastPoint()
	{
	}

	public double getX_coord()
	{
		return x_coord;
	}

	public double getY_coord()
	{
		return y_coord;
	}

	public long getCrtMilliSec()
	{
		return crtMilliSec;
	}
}
