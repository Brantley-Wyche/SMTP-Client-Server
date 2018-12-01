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

public class GUI extends Application implements EventHandler<ActionEvent> {

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
   private Button btnReceive = new Button("Receive");
   
   // Main just instantiates an instance of this GUI class
   public static void main(String[] args) {
      launch(args);
      
   }
   
   public void start(Stage _stage) throws Exception {
   
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
      hbox2.getChildren().addAll(lblMailbox, btnReceive);
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
      btnReceive.setOnAction(this);
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
   
   public void handle(ActionEvent evt) {

      Button btn = (Button)evt.getSource();
      
      switch(btn.getText()) {
         case "Send":       
            System.out.println("Sent message"); //Just a test to make sure the button works
            break;
        case "Receive":
            System.out.println("Received message"); //Just a test to make sure the button works
            break;
       }
   } 
     
}	
