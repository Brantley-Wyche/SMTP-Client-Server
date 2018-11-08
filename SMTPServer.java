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
 * SMTP Server
 * Server for ISTE 121 final project, connects to a client, then accepts commands from client to process them accordingly
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/7/18
 */
public class SMTPServer extends Application implements EventHandler<ActionEvent> {
	// Attributes
	// GUI
	private Stage stage;
	private Scene scene;
	private VBox root = new VBox(8);

	// Sockets
	private ServerSocket sSocket = null;
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
			case "Start":
				doStart();
				break;
			case "Stop":
				doStop();
				break;
		}
	}

	/**
	 * Starts sserver thread and socket to listen for connections
	 */
	private void doStart() {

	}

	/**
	 * Closes server thread and socket to prevent further connections
	 */
	private void doStop() {

	}

	/**
	 * On command "RETRIEVE FROM USER+PASSWORD", the server will look for users that have the USER+PASSWORD combination and return all their messages
	 */
	private void doRetrieve() {

	}

	/**
	 * On command "MAIL FROM: <address>", "RCPT TO: <address>", and then "DATA", the server will send the message sent from client to the indicated address.
	 *
	 * Additionally, the server will use a Ceasar Cipher of shift 13 on the message.
	 */
 	private void doSend() {

	}

	/**
	 * Encrypts message received from client with Ceasar Cipher of Shift 13
	 */
	private void doEncrypt() {

	}

	/**
	 * ServerStart
	 * Thread that will be instantiated on doStart(), this class will start a server at indicated socket and port
	 */
	class ServerStart extends Thread {
		// Attributes
		private Socket cSocket = null;

		/**
		 * Thread start
		 */
		public void run() {

		}
	}

	/**
	 * ClientConnection
	 * Thread that will be instantiated while the client connects with the server socket. This class will handle handle the interaction between client and server
	 */
	class ClientConnection extends Thread {
		// Attributes
		private Socket cSocket = null;

		/**
		 * Constructor for ClientConnection
		 * @param cSocket Accepts client connected socket
		 */
		public ClientConnection(Socket cSocket) {

		}

		/**
		 * Thread start
		 */
		public void run() {

		}
	}
}
