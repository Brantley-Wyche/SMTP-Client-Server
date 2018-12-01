import java.net.*;
import java.io.*;
import java.util.*;

import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;

/**
 * SMTP Client
 * Client for ISTE-121 final project, connects to a server and will send commands to the server to process
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/29/18
 */
public class SMTPClient extends Application {
	// Attributes
	// GUI
	private Stage stage;
	private Scene scene;
	private VBox root = new VBox(8);

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
		// TODO: Set up GUI
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
