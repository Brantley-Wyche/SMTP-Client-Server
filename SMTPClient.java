import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.control.ButtonBar.*;
import javafx.scene.control.TextInputDialog.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.geometry.*;
import javafx.util.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * SMTP Client
 * 
 * Client for ISTE-121 final project, connects to a server and will send commands to the server to process
 * 
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/29/18
 */
public class SMTPClient extends Application implements EventHandler<ActionEvent>, ClientServerConstants, CaesarCipherConstants {
   // GUI
	private Stage stage;
   private Scene scene;
   private MenuBar mBar = new MenuBar();
   private VBox root = new VBox(mBar);

   private Menu mOptions = new Menu("Option");
   private MenuItem miLogout = new MenuItem("Logout");

   // Labels
   private Label lblMessage = new Label("Message: ");
   private Label lblMailbox = new Label("Mailbox: ");

   // Textfields
   private TextField tfServer = new TextField();
   private TextField tfFrom = new TextField();
   private TextField tfTo = new TextField();

   // Textareas
   private TextArea taMessage = new TextArea();
   private TextArea taMailbox = new TextArea();

   // Buttons
   private Button btnSend = new Button("Send");
   private Button btnRetrieve = new Button("Retrieve");

	// Sockets
   private Socket socket = null;

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;

	// Main
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Starts GUI
	 */
	public void start(Stage _stage) {
      // GUI Set up
      stage = _stage;
      stage.setTitle("SMTP Client");

      // Set miLogout action
      miLogout.setOnAction(new EventHandler<ActionEvent>() {
         public void handle(ActionEvent evt) {
            doDisconnect();
         }
      });

      mBar.getMenus().addAll(mOptions);
      mOptions.getItems().addAll(miLogout);

      lblMessage.setFont(new Font("Arial", 20));
      lblMailbox.setFont(new Font("Arial", 20));

      tfFrom.setPromptText("example@example.com");
      tfTo.setPromptText("example@example.com");

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
      root.getChildren().addAll(hbox1, taMessage);

      //Hbox to space out the label and button
      HBox hbox2 = new HBox();
      hbox2.getChildren().addAll(lblMailbox, btnRetrieve);
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

      // Logout action handler
      miLogout.setOnAction(new EventHandler<ActionEvent>(){
         public void handle(ActionEvent event){
               doDisconnect();
         }
      });

      // Show gui
      scene = new Scene(root, 1000, 650);
      stage.setScene(scene);
      stage.show();
	}

	/**
	 * Handles button action events
	 */
	public void handle(ActionEvent evt) {
		// Get button name
		Button btn = (Button)evt.getSource();

		// Case
		switch(btn.getText()) {
			case "Retrieve":
				doRetrieve(); 
				break;
			case "Send":
				doSend();
            break;
		}
	}

	/**
	 * Sends command 'HELO' to server then connects
    */
	private void doConnect(String ip) {
      try {
		   // Connect & open streams
         socket = new Socket(ip, SERVER_PORT);
         scn = new Scanner(new InputStreamReader(socket.getInputStream()));
         pwt = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

         // Listen for "220"
         String resp = scn.nextLine(); 
         
         if(resp.contains("220")) {
            // Send to the Server "HELO" with ip
            pwt.println("HELO " + clientIp);
            pwt.flush();

            // Listen for "250"
            String resp2 = scn.nextLine();
            
            if(resp2.contains("250")) {
               // Show success alert
               Alert alert = new Alert(AlertType.INFORMATION, "Connected!");
                  alert.showAndWait();
            }
            else {
               // Alert
               Alert alert = new Alert(AlertType.INFORMATION, "Unexpected response!");
                  alert.showAndWait();
            }
         }
         else {
            // Alert
            Alert alert = new Alert(AlertType.INFORMATION, "Unexpected response!");
               alert.showAndWait();
         }
      }
      catch(IOException ioe) {
         ioe.printStackTrace();

         // Show alert
         Alert alert = new Alert(AlertType.ERROR, "Could not connect to server!");
            alert.showAndWait();
      }
   }
   
   /**
    * Connects with user inputted ip address first 
    *
	 * Sends command 'MAIL FROM: <address>' then 'RCPT TO: <address>' then 'DATA' to server to process
    * 
    * All addresses should follow the format: username@ip
	 *
	 * The order of commands must all be approved by the server before sending the message
	 */
	private void doSend() {
      // Get values
      String fromUser = tfFrom.getText().trim();
      String toUser = tfTo.getText().trim();
      String msg = taMessage.getText().trim();
      String serverIp = tfServer.getText().trim();

      // Check for empty
      if(fromUser.length() > 0 && toUser.length() > 0 && msg.length() > 0 && serverIp.length() > 0) {

         // Connect to server
         doConnect(serverIp);

         // Sends Server the "MAIL FROM" command
         pwt.println("MAIL FROM:" + fromUser);
         System.out.println("MAIL FROM:" + "<" + fromUser + ">");
         pwt.flush();

         // Reads the response from the server
         String resp = scn.nextLine();

         // Response from the server must be "250"
         if(resp.contains("250")) {
            // Sends "RCPT" to server
            pwt.println("RCPT TO:" + toUser);
            System.out.println("RCPT TO:" + "<" + toUser + ">");
            pwt.flush();

            // Read response from server again
            String resp2 = scn.nextLine();

            // Check again that the response is "250"
            if(resp2.contains("250")){

               // Sends "DATA" to server
               pwt.println("DATA");
               pwt.flush();

               // Read resp
               String resp3 = scn.nextLine();

               // Check resp for "354"
               if(resp3.contains("354")){

                  // Send msg
                  pwt.println(msg);
                  pwt.flush();

                  // Read resp
                  String resp4 = scn.nextLine();

                  // Check resp for "250"
                  if(resp4.contains("250")) {
                     // Show success alert
                     Alert alert = new Alert(AlertType.INFORMATION, "Message sent!");
                        alert.showAndWait();
                     
                     // Disconnect
                     doDisconnect();
                  }
               }
            }
         }
         // On unknown resps
         else {
            Alert alert = new Alert(AlertType.INFORMATION, "Unknown response! Please try again.");

            alert.showAndWait();
         }
      }
      // If fields are not all filled out
      else {
         // Show alert
         Alert alert = new Alert(AlertType.ERROR, "ServerIp, From, To, and Message must be filled out before sending!");
            alert.showAndWait();
      }
	}

	/*5
	 * Sends command 'QUIT then closes connection
	 */
	private void doDisconnect() {
      try{
         // Sends command out first
         pwt.println("QUIT");
         pwt.flush();

         // Get status code
         String resp = scn.nextLine();

         // Check for "OK"
         if(resp.contains("221")) {
            // Then closes the connections and streams
            socket.close();
            pwt.close();
            scn.close();
         }
      }
      catch(IOException ioe){
         ioe.printStackTrace();
      }
	}

	/**
	 * Sends command 'RETRIEVE FROM (USERNAME)' then logs all of the messages for the user
	 */
	private void doRetrieve() {
      try{
         // Send command
         pwt.println("RETRIEVE FROM:"+ tfFrom.getText());
         pwt.flush();

         // Read from server 
         String resp = scn.nextLine();
         
         // Check resp 
         if(resp.contains("221")) {
            // Show Alert
            Alert alert = new Alert(AlertType.ERROR, "221 - RETRIEVE FROM - FAIL");
               alert.showAndWait();
         }
         // The resp is the user's mailbox
         else {   
            String editResp1 = resp.replace(EMAIL_START, "");
            String editResp2 = editResp1.replace(EMAIL_END, "");
            taMailbox.appendText( doDecrypt(editResp2) + "\n\n");
         }
      }
      catch(NullPointerException npe){
         npe.printStackTrace();
      }
	}

   /**
    * Decrypts ceasar cipher by 13
    */
   private String doDecrypt(String msg) {
		// Result
		String result = "";

		// Decryption based on shift, use a for loop to check each char of the String
		for(int i=0; i<msg.length(); i++) {
			char curr = msg.charAt(i);

			// Check for spaces
			if(curr == ' ') {
				result += " ";
			}
			// Check for Upper Cased chars using ArrayList letters
			else if(Character.isUpperCase(curr)) {
				if(LETTERS.contains(curr)) {
					// Get the index to be shifted
					int shiftedIndex = LETTERS.indexOf(curr) - SHIFT;

					// If the shiftedIndex is smaller than 0, we need to add on the size of the ArrayList
					if(shiftedIndex < 0) {
						shiftedIndex += LETTERS.size();
					}
					// Get the new shifted char
					char shiftedChar = LETTERS.get(shiftedIndex);

					// Add to result
					result += Character.toString(shiftedChar);
				}
			}
			// Check for Lower Cased chars for using ArrayList letters
			else if(Character.isLowerCase(curr)) {
				// Because letters is all capitalized letters, we need to upper case the current char
				char upperChar = Character.toUpperCase(curr);

				if(LETTERS.contains(upperChar)) {
					// Get the index to be shifted
					int shiftedIndex = LETTERS.indexOf(upperChar) - SHIFT;

					// If the shiftedIndex is smaller than 0, we need to add on the size of the ArrayList
					if(shiftedIndex < 0) {
						shiftedIndex += LETTERS.size();
					}

					// Get the new shifted char
					char shiftedChar = LETTERS.get(shiftedIndex);

					// Lower case the shifted char
					char lowerShifted = Character.toLowerCase(shiftedChar);

					// Add to result
					result += Character.toString(lowerShifted);
				}
			}
			// Check for punctuations using ArrayList puncts
			else if(PUNCTS.contains(curr)) {
				result += Character.toString(curr);
			}
			// Check for numbers using ArrayList numbers
			else if(NUMBERS.contains(curr)) {
				result += Character.toString(curr);
			}
      }
   
		return result;
   }
}
