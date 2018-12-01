import java.net.*;
import java.io.*;
import java.util.*;

/**
 * SMTP Server
 * Server for ISTE 121 final project, connects to a client, then accepts commands from client to process them accordingly
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/7/18
 */
public class SMTPServer implements ClientServerConstants {
	// Sockets
	private ServerSocket sSocket = null;

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;

	// Queue for storing messages
	Queue<String> msgQueue = new LinkedList<>();
   
	/**
	 * Main
	 */
	public static void main(String[] args) {
		// Start socket
		new SMTPServer().doStart();
	}

	/**
	 * ServerStart
	 * 
	 * Thread that will be instantiated on doStart(), this class will start a server at indicated socket and port
	 */
	class ServerStart extends Thread {
		// Attributes
		private Socket cSocket = null;

		// Run
		public void run() {
			// Start server at SERVER_PORT
			try {
				sSocket = new ServerSocket(SERVER_PORT);
				System.out.println("Server started at " + SERVER_PORT);
			}
			catch(IOException ioe) { ioe.printStackTrace(); }

			// Always listen for connections to server
			while(true) {
				// Accept connections to server
				try {
					cSocket = sSocket.accept();
					System.out.println("Connection established!");
				}
				catch(IOException ioe) { 
					ioe.printStackTrace(); 
					return; 
				}

				// Start thread for ClientConnection 
				ClientConnection ct = new ClientConnection(cSocket);
				ct.start(); 
			}
		}
	}

	/**
	 * ClientConnection
	 * 
	 * Thread that will be instantiated while the client connects with the server socket. This class will handle handle the interaction between client and server
	 */
	class ClientConnection extends Thread {
		// Attributes
		private Socket cSocket = null;

		// Constructor
		public ClientConnection(Socket _cSocket) {
			this.cSocket = _cSocket;
		}
		
		// Run
		public void run() {
			// Prepare I/O
			try {
				scn = new Scanner(new InputStreamReader( cSocket.getInputStream() ));
				pwt = new PrintWriter(new OutputStreamWriter( cSocket.getOutputStream() ));
			}
			catch(IOException ioe) { ioe.printStackTrace(); }

			// Listen for commands
			while(scn.hasNextLine()) { 
				// Get command
				String cmd = scn.nextLine();

				// HELO
				if(cmd.equals("HELO")) {
					// Send status code 
					pwt.println("250 - HELO - OK");
					pwt.flush();
				}
			}// end of while
		}
	}

	/**
	 * MailThread
	 * 
	 * Thread that will read from the queue and store the message with its corresponding user in a file
	 * 
	 * There will a list of users authorized to recieve messages on this server.   
	 */
	class MailThread extends Thread {
		public void run() {}
	}
	
	/**
	 * Starts sserver thread and socket to listen for connections
	 */
	private void doStart() {
		// Starts ServerStart thread
		new ServerStart().start();
	}

	/**
	 * Closes server thread and socket to prevent further connections
	 */
	private void doStop() {
		// Closes sSocket and streams
		try {
			sSocket.close();
			scn.close();
			pwt.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
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
	private void doEncrypt(String message) {
      String result = "";
      
      for(int i = 0; i < message.length(); i++){
         int characterPos = LETTERS.indexOf(message.charAt(i));
         int keyValue = (SHIFT + characterPos) % 26;
         char replaceVal = LETTERS.charAt(keyValue);
         result += replaceVal;
	  }      
	}// end of doEncrypt
}