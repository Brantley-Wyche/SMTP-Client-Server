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
 * Client for ISTE-121 final project, connects to a server and will send commands to the server to process
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/29/18
 */
public class SMTPClient extends Application implements EventHandler<ActionEvent>, ClientServerConstants {
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
   private Button btnRetrieve = new Button("Retrieve");

	// Sockets
	private Socket socket = null;

   // Host String (IP)
   private String host = "localhost";

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;

   // User File
   public static final String USER_FILE = "users.txt";

   // User Name from user input
   private String inputUserName = "";

   // Adds all read users into a list
   private ArrayList<String> userList = new ArrayList<String>();

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
      Dialog<String> dialog = new Dialog<>();
      dialog.setTitle("Login");
      dialog.setHeaderText("Enter your Username and Server IP");
      
      // Set the button types.
      ButtonType loginButton = new ButtonType("Login", ButtonData.OK_DONE);
      dialog.getDialogPane().getButtonTypes().addAll(loginButton, ButtonType.CANCEL);
      
      // Create the username and password labels and fields.
      GridPane gp = new GridPane();
      gp.setHgap(10);
      gp.setVgap(10);
      gp.setPadding(new Insets(20, 150, 10, 10));
      
      TextField username = new TextField();
      TextField serverIP = new TextField();
      
      gp.add(new Label("Username: "), 0, 0);
      gp.add(username, 1, 0);
      gp.add(new Label("Server IP: "), 0, 1);
      gp.add(serverIP, 1, 1);
      
      dialog.getDialogPane().setContent(gp);
      
      // Request focus on the username field by default.
      Platform.runLater(() -> username.requestFocus());
      
      
      Optional<String> result = dialog.showAndWait();
    
      
      
      if (result.isPresent()) {//Just opens the main program

            // get the input username from the 'username' textfield
            inputUserName = username.getText().trim();
            String ip = serverIP.getText().trim();
            
            System.out.println("THE IP : " + ip);
   
            // If user is allowed access
            if(readUsers(inputUserName)){
            
               // Connect
               doConnect(ip);
               
            }
            else{
            
               // Error for user not existing
               Alert alert = new Alert(AlertType.ERROR, "User does not exist!");
                  alert.setTitle("User Error");
                  alert.setHeaderText(null);
                  alert.showAndWait();
               
               //Kills the program
               System.exit(0); 
            }
   
     }
     else {//Kills the program
       System.exit(0);
     }

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

      // Logout action handler
      miLogout.setOnAction(new EventHandler<ActionEvent>(){
         public void handle(ActionEvent event){
               doDisconnect();
         }
      });

      //Brings up the main program after the dialog goes away
      revealStage();
	}

   /**
   * Shows the stage
   */
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
    * Only available when users are validated from the list of users
	 */
	private void doConnect(String ip) {
      // Connect to server socket
      try{
		   // Connect & open streams
         socket = new Socket(ip, SERVER_PORT);
         scn = new Scanner(new InputStreamReader(socket.getInputStream()));
         pwt = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

         // Set fields
         tfFrom.setText(inputUserName);
         tfServer.setText(host);

         // Send command to server
         pwt.println("HELO");
         pwt.flush();

         // Get response from the server
         String serverResp = scn.nextLine();

         // response needs to contain 250
         if(serverResp.contains("250")) {
            // Let user know they successfully connected to the server
            Alert alert = new Alert(AlertType.INFORMATION, "Connected!");
               alert.showAndWait();
         }// end of if

      }
      catch(IOException ioe) {
         ioe.printStackTrace();

         // Show alert
         Alert alert = new Alert(AlertType.ERROR, "Could not connect to server!");
            alert.showAndWait();

         // Kill client
         System.exit(0);
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

            System.exit(0);
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
            taMailbox.setText(resp+"\n");
         }
      }
      catch(NullPointerException npe){
         npe.printStackTrace();
      }
	}

	/**
	 * Sends command 'MAIL FROM: <address>' then 'RCPT TO: <address>' then 'DATA' before encrypting and sending the message
	 *
	 * The order of commands must all be approved by the server before sending the message
	 */
	private void doSend() {
      // Get values
      String fromUser = tfFrom.getText().trim();
      String toUser = tfTo.getText().trim();
      String msg = taMessage.getText().trim();

      // Check for empty
      if(fromUser.length() > 0 && toUser.length() > 0 && msg.length() > 0) {

         // Sends Server the "MAIL FROM" command
         pwt.println("MAIL FROM:" + fromUser);
         pwt.flush();

         // Reads the response from the server
         String resp = scn.nextLine();

         // Response from the server must be "OK"
         if(resp.contains("250")){
            // Sends "RCPT" to server
            pwt.println("RCPT TO:" + toUser);
            pwt.flush();

            // Read response from server again
            String resp2 = scn.nextLine();

            // Check again that the response is "OK"
            if(resp2.contains("250")){

               // Sends "DATA" to server
               pwt.println("DATA");
               pwt.flush();

               // Read resp
               String resp3 = scn.nextLine();

               // Check resp for "OK"
               if(resp3.contains("250")){

                  // Send msg
                  pwt.println(msg);
                  pwt.flush();

                  // Read resp
                  String resp4 = scn.nextLine();

                  // Check resp for "OK"
                  if(resp4.contains("250")) {
                     // Show success alert
                     Alert alert = new Alert(AlertType.INFORMATION, "Message sent!");

                     alert.showAndWait();
                  }
               }
            }
         }
      }
      // If fields are not all filled out
      else {
         // Show alert
         Alert alert = new Alert(AlertType.ERROR, "All fields must be filled out before sending!");
            alert.showAndWait();
      }
	}

   /**
   * readUsers
   *
   * @params String username
   */
   public boolean readUsers(String user) {
      // Check if user is authorized
      try{
         File file = new File(USER_FILE);
         Scanner scn = new Scanner(new FileInputStream(file));

         String name = "";

         // Read file with users
         while(scn.hasNextLine()){
            userList.add(scn.nextLine());
         }

         // Cycle through all users in the list
         for(int i = 0; i < userList.size(); i++){
            if(user.contains(userList.get(i))){
               // if user is found to exist
               return true;
            }// end of for
         }// end of for loop
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }

      // False, if not found
      return false;
   }
}
