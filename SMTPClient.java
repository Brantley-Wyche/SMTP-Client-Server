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
 * SMTPClient
 * Client for ISTE-121 final project, will send commands to the server to process
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/7/18
 */
public class SMTPClient extends Application implements EventHandler<ActionEvent> {
	// Attributes
	// GUI
	private Stage stage;
	private Scene scene;
	private VBox root = new VBox(8);

	// Sockets
	private Socket socket = null;
	public static final int PORT_NUM = 30000;

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;

	// Main
	public static void main(String[] args) {
		launch(args)
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

	}

	/*
	 * Sends command 'QUIT then closes connection
	 */
	private void doDisconnect() {

	}

	/**
	 * Sends command 'RETRIEVE FROM (USERNAME + PASSWORD)' then logs all of the messages for the user
	 */
	private void doRetrieve() {

	}

	/**
	 * Sends command 'MAIL FROM: <address>' then 'RCPT TO: <address>' then 'DATA' before encrypting and sending the message
	 *
	 * The order of commands must all be approved by the server before encrypting and sending the message
	 */
	private void doSend() {
		// TODO: Ceasar cipher should go here
	}
}
