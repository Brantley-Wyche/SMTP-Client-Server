import java.net.*;
import java.io.*;
import java.util.*;

/**
 * SMTP Server
 * 
 * Server for ISTE 121 final project, connects to a client, then accepts
 * commands from client to process them accordingly
 * 
 * @author Brandon Mok + Xin Liu + Brantley Wyche
 * @version 11/7/18
 */
public class SMTPServer implements ClientServerConstants, CaesarCipherConstants {
	// Sockets
	private ServerSocket sSocket = null;
	private Socket rSocket = null;

	// I/O
	private Scanner scn = null;
	private PrintWriter pwt = null;

	// Global vars
	private String username = ""; // RCPT TO
	private String ip = ""; // RCPT TO
	
	private String sender = "";
	private String message = "";

	private String clientIp = ""; 
	private String my_ip = "";
	private String hardcoded_ip = "129.21.125.104";

	// Queue for queueing messages
	private Queue<String> msgQueue = new LinkedList<>();

	/**
	 * Main
	 */
	public static void main(String[] args) {
		// Start socket
		new SMTPServer().doStart();
	}

	/**
	 * Classes:
	 * 
	 * ServerStart, ClientConnection, MailThread, RelayThread
	 */

	/**
	 * ServerStart
	 *
	 * Thread that will be instantiated on doStart(), this class will start a server
	 * at indicated port
	 */
	class ServerStart extends Thread {
		// Attributes
		private Socket cSocket = null;

		// Run
		public void run() {
			// Get public ip address
			try {
				// Get ip from URL 
				URL checkip = new URL("http://checkip.amazonaws.com/");
				URLConnection connect = checkip.openConnection();
		
				// Read text from URL
				BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
		
				// Get up
				my_ip = in.readLine();

				// Fail-safe
				if(my_ip.length() < 1) {
					// In the event my_ip cannot be idenitified from the url, we will hardcode the ip
					my_ip = hardcoded_ip;
				}
			}
			catch(IOException ioe) { ioe.printStackTrace(); }

			// Start server at SERVER_PORT
			try {
				sSocket = new ServerSocket(SERVER_PORT);

				System.out.println("Server started at Port: " + SERVER_PORT + ", Public IP: " + my_ip);
			} catch (IOException ioe) { ioe.printStackTrace(); }

			// Always listen for connections to server
			while (true) {
				// Accept connections to server
				try {
					cSocket = sSocket.accept();

					// Assign clientIp
					clientIp = String.valueOf(cSocket.getInetAddress());
					System.out.println("Connection established with " + clientIp);
				} catch (IOException ioe) {
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
	 * Thread that will be instantiated while the client connects with the server
	 * socket. This class will handle the interaction between client and server
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
				scn = new Scanner(new InputStreamReader(cSocket.getInputStream()));
				pwt = new PrintWriter(new OutputStreamWriter(cSocket.getOutputStream()));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			// Send "220"
			pwt.println("220 " + "ESMTP Postfix");
			System.out.println("220 " + "ESMTP Postfix");
			pwt.flush();

			// Listen for commands
			while (scn.hasNextLine()) {
				// Get command
				String cmd = scn.nextLine();

				// HELO
				if (cmd.startsWith("HELO")) {
					// Send status code
					pwt.println("250 HELO " + clientIp + " OK");
					System.out.println("250 HELO " + clientIp + " OK");
					pwt.flush();
				}
				// MAIL FROM
				else if (cmd.startsWith("MAIL FROM:")) {
					// Get the sender
					sender = cmd.substring(10);

					// Remove "<" and ">" if it does not exist
					if (sender.contains("<") && sender.contains(">")) {
						String parsedSender = sender.replace("<", "");
						parsedSender = parsedSender.replace(">", "");
					}

					// Send status code
					pwt.println("250 MAIL FROM OK");
					System.out.println("250 MAIL FROM OK");
					pwt.flush();
				}
				// RCPT TO
				else if (cmd.startsWith("RCPT TO:")) {
					// Get the recipient
					String recipient = cmd.substring(8);

					// Remove "<" and ">" if it does not exist
					if (recipient.contains("<") && recipient.contains(">")) {
						recipient = recipient.replace("<", "");
						recipient = recipient.replace(">", "");
					}

					// Parse the username for "@"
					String[] parts = recipient.split("@");

					username = parts[0];
					ip = parts[1];

					// Send status code
					pwt.println("250 RCPT TO OK");
					System.out.println("250 RCPT TO OK");
					pwt.flush();
				}
				// DATA
				else if (cmd.equals("DATA")) {
					// Send status code
					pwt.println("354 End in <CR><LR>.<CR><LR>");
					System.out.println("354 End in <CR><LR>.<CR><LR>");
					pwt.flush();

					// Listen for message
					while(scn.hasNextLine()) {
						if(!scn.nextLine().equals(".")) {
							message += scn.nextLine();
						}
						else {
							// Split the message by "\n"
							String[] messages = message.split("\n");
							String queuedMessage = "";

							for(int i=0; i<messages.length; i++) {
								queuedMessage += messages[i];  
							}

							// Add to queue
							msgQueue.add(queuedMessage);
							break;
						}
					}

					// Send status code
					pwt.println("250 Message Queued");
					System.out.println("250 Message Queued");
					pwt.flush();

					// Open thread to handle message in queue
					new MailThread(username, ip, msgQueue.peek()).start();
				}
				// RETRIEVE FROM username pass
				else if (cmd.startsWith("RETRIEVE FROM")) {
					// Get everything after :
					String username_pass = cmd.substring(14);

					// Split the string by " "
					String[] parts = username_pass.split(" ");

					// Get values
					String username = parts[0];
					String pass = parts[1];

					// Check for "<" and ">" in username
					if (username.contains("<") && username.contains(">")) {
						username = username.replace("<", "");
						username = username.replace(">", "");
					}

					// Get mailbox
					File mailboxFile = new File(username + ".txt");	

					// TODO: Check if user has mailbox
				}
				// QUIT
				else if (cmd.equals("QUIT")) {
					// Send response
					pwt.println("221 Bye");
					System.out.println("221 Bye");
					pwt.flush();

					// Close streams
					try {
						scn.close();
						pwt.close();
					} catch (Exception e) { e.printStackTrace(); }
				}
				// If none of the commands are matched, send error
				else {
					// Send error
					pwt.println("221 Unknown command recieved");
					System.out.println("221 Unknown command recieved");
					pwt.flush();
				}
			} // end of while
		}
	}

	/**
	 * Thread that prepares the message in the queue, then add its the to the user's mailbox file.
	 * 
	 * This thread will check if the ip matches the host's public ip, and if it does not it will relay
	 */
	class MailThread extends Thread {
		// Attributes
		String user = "";
		String userIP = "";
		String message = "";
		String mailbox = user + ".txt"; 

		// Constructor
		public MailThread(String _user, String _userIP, String _message) {
			this.user = _user;
			this.userIP = _userIP;
			this.message = _message;
		}

		// Run
		public void run() {
			// Check if user's ip matches our ip
			if(userIP.equals(my_ip) || userIP.equals(hardcoded_ip)) {
				// User belongs to our ip, check if they have a mailbox file
				File mailbox_file = new File(mailbox);

				if(mailbox_file.exists()) {
					// Write to mailbox_file
					try {
						// Open I/O
						PrintWriter mPwt = new PrintWriter(mailbox_file); 

						mPwt.println(message); 

						mPwt.close();
					}
					catch(IOException ioe) { ioe.printStackTrace(); }
				}
				// User does not have mailbox on our server, but matches our ip
				else {
					try {
						// Open I/O
						PrintWriter mPwt = new PrintWriter(user+".txt");
						
						mPwt.println(message);

						mPwt.close();
					}
					catch(IOException ioe) { ioe.printStackTrace(); }
				}
			}
			// User's ip does not match our ip
			else {
				// Relay to proper server
				new RelayThread(user, userIP, message).start();
			}
		}
	}

	/**
	 * Thread will open a socket connection with the user's ip and send the mail to that server
	 */
	class RelayThread extends Thread {
		// Attributes
		String user = "";
		String userIP = "";
		String message = "";

		// Constructor
		public RelayThread(String _user, String _userIP, String _message) {
			this.user = _user;
			this.userIP = _userIP;
			this.message = _message;
		}

		// Run
		public void run() {

		}
	}

	/**
	 * METHODS: doStart()
	 */

	/**
	 * Starts server thread and socket to listen for connections
	 */
	private void doStart() {
		new ServerStart().start();
	}
}
