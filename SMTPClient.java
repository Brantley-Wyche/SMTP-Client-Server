import javafx.application.Application;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.control.TextInputDialog.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * SMTP Client
 * Client for ISTE-121 final project, connects to a server and will send commands to the server to process
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/29/18
 */
public class SMTPClient extends Application implements EventHandler<ActionEvent>{
   // GUI
	private Stage stage;        
   private Scene scene;   
   private MenuBar mBar = new MenuBar();     
   private VBox root = new VBox(mBar);  
   
   private Menu mOptions = new Menu("Options");
   private MenuItem miLogout = new MenuItem("Logout");
   
   //Labels
   private Label lblMessage = new Label("Message: ");
   private Label lblMailbox = new Label("Mailbox: ");
   
   //Textfields
   private TextField tfServer = new TextField();
   private TextField tfFrom = new TextField();
   private TextField tfTo = new TextField();
   
   //Textareas
   private TextArea taMessage = new TextArea();
   private TextArea taMailbox = new TextArea();
   
   //Buttons
   private Button btnSend = new Button("Send");
   private Button btnRetrieve = new Button("Receive");


	// Sockets
	private Socket socket = null;
	public static final int PORT_NUM = 30000;
   
   // Host String (IP) 
   private String host = "";

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;
   
   
   

	// Main
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Starts GUI
	 * @param _stage
	 */
	public void start(Stage _stage) {
		//Dialog called automatically after launch before main program
      TextInputDialog dialog = new TextInputDialog();
 
        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your username:");
        dialog.setContentText("Username:");
 
        Optional<String> result = dialog.showAndWait();
 
        if (result.isPresent()) {//Just opens the main program
             
        } else {//Kills the program
             System.exit(0);
        }
      
      stage = _stage;                        
      stage.setTitle("FirstGUI");       
      
      mBar.getMenus().addAll(mOptions);   
      mOptions.getItems().addAll(miLogout);  

      lblMessage.setFont(new Font("Arial", 20));
      lblMailbox.setFont(new Font("Arial", 20));
      
      //Row1 - server name/IP
      FlowPane fpRow1 = new FlowPane(8,8);
      fpRow1.setAlignment(Pos.CENTER);
      tfServer.setPrefColumnCount(45);
      fpRow1.getChildren().addAll(new Label("Server IP: "), tfServer);
      root.getChildren().add(fpRow1);
      
      //Row 2 - 'From' textfield
      FlowPane fpRow2 = new FlowPane(8,8);
      fpRow2.setAlignment(Pos.CENTER);
      tfFrom.setPrefColumnCount(47);
      fpRow2.getChildren().addAll(new Label("From: "), tfFrom);
      root.getChildren().add(fpRow2);
      
      //Row 3 - 'To' textfield
      FlowPane fpRow3 = new FlowPane(8,8);
      fpRow3.setAlignment(Pos.CENTER);
      tfTo.setPrefColumnCount(48);
      fpRow3.getChildren().addAll(new Label("To: "), tfTo);
      root.getChildren().add(fpRow3);

      //Hbox to space out the label and button
      HBox hbox1 = new HBox();
      hbox1.getChildren().addAll(lblMessage, btnSend);
      hbox1.setSpacing(508);
      
      root.getChildren().addAll(hbox1, taMessage);
      
      //Hbox to space out the label and button
      HBox hbox2 = new HBox();
      hbox2.getChildren().addAll(lblMailbox, btnRetrieve);
      hbox2.setSpacing(505);
      
      root.getChildren().addAll(hbox2, taMailbox);
      
      //Spacing everything out
      root.setSpacing(10);
      
      //Text area wrapping/columns/rows
      taMessage.setWrapText(true);           
      taMessage.setPrefColumnCount(10);
      taMessage.setPrefRowCount(30);
      taMailbox.setWrapText(true);           
      taMailbox.setPrefColumnCount(10);
      taMailbox.setPrefRowCount(30);
      
      //button handlers
      btnSend.setOnAction(this);
      btnRetrieve.setOnAction(this);
      miLogout.setOnAction(new EventHandler<ActionEvent>(){//Anonymous inner class to logout
         public void handle(ActionEvent event){
            System.exit(0);
         }
      });

      //Brings up the main program after the dialog goes away
      revealStage();
	}
   
   
   public void revealStage() {//Shows the main program
      scene = new Scene(root, 650, 650);   
                                             
      stage.setScene(scene);                
      stage.show();                          

    }
   

	/**
	 * Handles button action events
	 * @param evt Button on clicks
	 */
	public void handle(ActionEvent evt) {
		// Get button name
		Button btn = (Button)evt.getSource();

		// Case
		switch(btn.getText()) {
			case "Connect":
				doConnect(); // HELO
				break;
			case "Disconnect":
				doDisconnect(); // QUIT
				break;
			case "Retrieve":
				doRetrieve(); // RETRIEVE FROM USER+PASSWORD (needs double checking, not sure)
				break;
			case "Send":
				doSend();
				// NOTE: for doSend(), SMTP protocol requires MAIL FROM:<address> then RCPT TO: <address> then DATA before finally sending the message.
				// MAIL FROM -> RCPT TO -> DATA
				break;
		}
	}

	/**
	 * Sends command 'HELO' to server then connects 
	 */
	private void doConnect() {
		// TODO: the socket will take host, port, however the host will be different per server that we will connect to, whereas the port will always be PORT_NUM
      try{
		 // Connect & open streams
         socket = new Socket(host, PORT_NUM);
         scn = new Scanner(new InputStreamReader(socket.getInputStream()));
         pwt = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
         
         
         // Send command to server
         pwt.println("HELO");
         pwt.flush();
      }
      catch(IOException ioe){
         ioe.printStackTrace();
      }
	}

	/*
	 * Sends command 'QUIT then closes connection
	 */
	private void doDisconnect() {
      try{
         // Sends command out first
         pwt.println("QUIT");
         pwt.flush();
         
         // Then closes the connections and streams
         socket.close();
         pwt.close();
         scn.close();
      }
      catch(IOException ioe){
         ioe.printStackTrace();
      }
	}

	/**
	 * Sends command 'RETRIEVE FROM (USERNAME)' then logs all of the messages for the user
	 */
	private void doRetrieve() {
		// Send command
        pwt.println("RETRIEVE FROM "); /* Still need the username. Need the userlist */
        pwt.flush();
         
         
         
         
	}

	/**
	 * Sends command 'MAIL FROM: <address>' then 'RCPT TO: <address>' then 'DATA' before encrypting and sending the message
	 *
	 * The order of commands must all be approved by the server before sending the message
	 */
	private void doSend() {
		// Sends Server the "MAIL FROM" command
		pwt.println("MAIL FROM: " +   " " ); 

		// Reads the response from the server
		String resp = scn.nextLine();

		// Response from the server must be "OK"
		if(resp.equals("OK")){
			// Sends "RCPT" to server
			pwt.println("RCPT TO");
			
			// Read response from server again
			String resp2 = scn.nextLine();

			// Check again that the response is "OK"
			// Must do so before encrypting and sending the message
			if(resp.equals("OK")){

				// Sends "DATA" to server
				pwt.println("DATA");

				String resp3 = scn.nextLine();

				if(resp3.equals("OK")){


				}
			}
		}
	}
}
