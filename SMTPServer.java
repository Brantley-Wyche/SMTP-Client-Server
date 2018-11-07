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
}
