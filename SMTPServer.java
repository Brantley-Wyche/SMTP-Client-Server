import java.net.*;
import java.io.*;
import java.util.*;

/**
 * SMTP Server
 * 
 * Server for ISTE 121 final project, connects to a client, then accepts commands from client to process them accordingly
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
   private PrintWriter mailPWT = null; // only to write messages to user file

	// Users
	public static final String USER_FILE = "users.txt";

	// Global vars
	private String sender = "";
	private String userName = "";
	private String ip = "";
	private String message = "";

	// Queue for queueing messages
	Queue<String> msgQueue = new LinkedList<>();

	// Ips
	String clientIp = "";
	String serverIp = "";

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
	 * ServerStart,
	 * ClientConnection,
	 * MailThread, 
	 * RelayThread
	 */

	/**
	 * ServerStart
	 *
	 * Thread that will be instantiated on doStart(), this class will start a server at indicated port
	 */
	class ServerStart extends Thread {
		// Attributes
		private Socket cSocket = null;

		// Run
		public void run() {
			// Start server at SERVER_PORT
			try {
				sSocket = new ServerSocket(SERVER_PORT);

				// Assign serverIp
				serverIp = String.valueOf(sSocket.getInetAddress());
				System.out.println("Server started at Port " + SERVER_PORT);
			}
			catch(IOException ioe) { ioe.printStackTrace(); }

			// Always listen for connections to server
			while(true) {
				// Accept connections to server
				try {
					cSocket = sSocket.accept();

					// Assign clientIp
					clientIp = String.valueOf(cSocket.getInetAddress());
					System.out.println("Connection established with " + clientIp);
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
	 * Thread that will be instantiated while the client connects with the server socket. This class will handle the interaction between client and server
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
         
			// Send "220"
			pwt.println("220 " + serverIp + " ESMTP Postfix");
			System.out.println("220 " + serverIp + " ESMTP Postfix");
			pwt.flush();
         
			// Listen for commands
			while(scn.hasNextLine()) {
				// Get command
				String cmd = scn.nextLine();

				// HELO
				if(cmd.contains("HELO")) {
					// Send status code
					pwt.println("250 HELO - " + clientIp + " OK");
					System.out.println("250 HELO - " + clientIp + " OK");
					pwt.flush();
				}
				// MAIL FROM
				else if(cmd.startsWith("MAIL FROM:")) {
					// Get the sender
					sender = cmd.substring(10);

					// Remove "<" and ">" if it does not exist
					if(sender.contains("<") && sender.contains(">")) {
						String parsedSender = sender.replace("<", "");
						parsedSender = parsedSender.replace(">", "");
					}

					// Send status code
					pwt.println("250 MAIL FROM OK");
					System.out.println("250 MAIL FROM OK");
					pwt.flush();

					// RCPT TO
					String cmd2 = scn.nextLine();
					if(cmd2.startsWith("RCPT TO:")) {
						// Get the recipient
						String recipient = cmd2.substring(8);

						// Remove "<" and ">" if it does not exist
						if(recipient.contains("<") && recipient.contains(">")) {
							recipient = recipient.replace("<", "");
							recipient = recipient.replace(">", "");
						}
						
						// Parse the username for "@"
						String[] parts = recipient.split("@");
						userName = parts[0];
						ip = parts[1];

						// Send status code
						pwt.println("250 RCPT TO OK");
						System.out.println("250 RCPT TO OK");
						pwt.flush();

						// DATA
						String cmd3 = scn.nextLine();
						if(cmd3.equals("DATA")) {
							// Send status code
							pwt.println("354 DATA OK");
							pwt.flush();

							// Listen for message 
							message = scn.nextLine();
							
							// Add to queue
							msgQueue.add(message);

							// Open thread to handle message in queue
							new MailThread(userName,ip,msgQueue.peek()).start();
						}
					}
				}
				else if(cmd.startsWith("RETRIEVE FROM:")){
					// Get username
					String user = cmd.substring(14);
				}
				else if(cmd.equals("QUIT")) {
					// Send response
					pwt.println("221 Bye"); 
					System.out.println("221 Bye");
					System.out.println("Connection closed with " + clientIp); 
					pwt.flush();
				}
				// If none of the commands are matched, send error
				else {
					// Send error
					pwt.println("221 - Unknown command recieved");
					System.out.println("221 - Unknown command recieved");
					pwt.flush();
				}
			}// end of while
		}
	}

	/**
	 * Thread that prepares the message in the queue, then add its the to the user's mailbox file. 
	 * 
	 * It will check if the user is authorized on this server, and if the ip is correct. If it is not then relays to the correct server.
	 */
	class MailThread extends Thread {
		String user = "";
		String userIP = "";
		String message = "";
		
		public MailThread(String _user, String _userIP, String _message){
		   user = _user;
		   userIP = _userIP;
		   message = _message;
		}
		public void run(){
			  // Check username exists
			  try{
					Scanner scn = new Scanner(new FileInputStream(USER_FILE));  
					
					while(scn.hasNextLine()){

					   // If user is authorized on this server
					   if(userName.equals(scn.nextLine())){
						  
						  // If ip matches our server ip
						  if(ip.equals(String.valueOf(sSocket.getInetAddress()))) {
								// Add "From:" into the message
								String fullMessage = "Mail From: " + sender + "\n" + message;

								// Encrypt message
								String encryptedMsg = doEncrypt(fullMessage);

								// Prepare schema for encrypted msg
								String finalMsg = EMAIL_START + encryptedMsg + EMAIL_END;

								// Prepare IO for mailBoxFile
								File mailBoxFile = new File(userName+".txt");
								mailPWT = new PrintWriter(new FileOutputStream(mailBoxFile,true)); // printwriter to read mailbox
									  
								// Checks if a mailbox exists for user
								if(mailBoxFile.exists()){
									// Write message to file
									mailPWT.println(finalMsg);
									mailPWT.flush();

									// Send status code
									pwt.println("250 Message Queued");
									System.out.println("250 Message Queued");
									pwt.flush();

									// close scanner
									scn.close();
									mailPWT.close();
								}
								else{
									// Create mailbox file
									mailBoxFile.createNewFile();

									// Write message to file
									mailPWT.println(finalMsg);
									mailPWT.flush();

									// Send status code
									pwt.println("250 Message Queued");
									System.out.println("250 Message Queued");
									pwt.flush();
									
									// close scanner
									scn.close();
									mailPWT.close();
								 }     
						  }
						  // If user is not on this server
						  else {
							  	// Relay
								RelayThread rThread = new RelayThread(sender, user, userIP, message);
									  rThread.start();

								// close scanner
								scn.close();
								mailPWT.close();
						  }
					   }
					   // If the user is not authorized on this server
					   else{
							// Replay
							RelayThread rThread = new RelayThread(sender, user, userIP, message);
								rThread.start();

							// close scanner
							scn.close();
							mailPWT.close();
						}
					}// end of while
			  }
			  catch(IOException ioe){ ioe.printStackTrace(); }   
		}// end of run
	 }// end of mailthread
	 
	 
	 /**
	  * Thread that takes care of relaying. It will be called when relaying is required. Opens new socket and attempts to conect to the correct server ip.
  
	  It will send the SMTP protocols for sending the message
	  */
	 class RelayThread extends Thread{
		String user = "";
		String userIP = "";
		String from = "";
		String message = "";
		
		private Scanner rScan = null;
		private PrintWriter rPwt = null;
		
		public RelayThread(String _from, String _user, String _userIP, String _message){
		   from = _from;
		   user = _user;
		   userIP = _userIP;
		   message = _message;
		}
		public void run(){
			  try{
				  rSocket = new Socket(userIP, SERVER_PORT);
				  rScan = new Scanner(new InputStreamReader(rSocket.getInputStream()));
				  rPwt = new PrintWriter(new OutputStreamWriter(rSocket.getOutputStream()));
				  
				  // Get ip of this socket
				  String clientIp = String.valueOf(rSocket.getInetAddress());
				  
				  // Reads response
				  String resp = rScan.nextLine();
				  
				  // Listen for "220"
				  if(resp.contains("220")){
					// HELO
					rPwt.println("HELO " + clientIp);
					rPwt.flush();
					
					// Listen for "250"
					String resp2 = rScan.nextLine();
					if(resp2.contains("250")){
					   System.out.println("Relay - Connected to " + userIP); 
					   
					   // MAIL FROM:<username@ip>
					   rPwt.println("MAIL FROM:<" + user+"@"+userIP + ">");
					   System.out.println("MAIL FROM:<" + user+"@"+userIP + ">");
					   rPwt.flush();
					   
					   // Listen for "250"
					   String resp3 = rScan.nextLine();
					   if(resp3.contains("250")){
						  
						  // RCPT TO:<username@ip>
						  rPwt.println("RCPT TO:" + from);
						  rPwt.flush();
						  
						  // Listen to "250"
						  String resp4 = rScan.nextLine();
						  if(resp4.contains("250")){
						     // DATA
							 rPwt.println("DATA");
							 rPwt.flush();
							 
							 // Listen to "354"
							 String resp5 = rScan.nextLine();
							 if(resp5.contains("354")){
								// Send message
								rPwt.println(message);
								rPwt.flush();
								
								// Listen to "250"
								String resp6 = rScan.nextLine();
								if(resp6.contains("250")){
								   	
								   System.out.println("Relay - Message sent");
								   try{
									  // QUIT
									  rPwt.println("QUIT");
									  rPwt.flush();
									  
									  // Listen for "221"
									  String resp7 = rScan.nextLine();
									  if(resp7.contains("221")){
										 // Close connections and streams
										 rSocket.close();
										 rScan.close();
										 rPwt.close();

										 System.out.println("Relay - Connection closed");
									  }
  
								   }catch(IOException ioe){ ioe.printStackTrace(); }
								}
							 }
						  }
					   }
					}
				  }
				  else {   
					System.out.println("Relay Failed - " + userIP);
				} 
			  }
			  catch(IOException ioe){ ioe.printStackTrace(); }
		}
	 }

	/**
	 * METHODS: 
	 * doSave(),
	 * doStart(),
	 * doStop(),
	 * doRetrieve(),
	 * doEncrypt()
	 */

	/**
	 * Starts server thread and socket to listen for connections
	 */
	private void doStart() {
		new ServerStart().start();
	}

	/** 
	 * Encrypt message with cipher of 13
	 */
	private String doEncrypt(String msg) {
		// Result 
		String result = "";

		// Encryption based on shift, use a for loop to check each char of the string
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
					int shiftedIndex = LETTERS.indexOf(curr) + SHIFT;

					// If the shiftedIndex is bigger than the size of the ArrayList, we need to substract the size of the ArrayList
					if(shiftedIndex > LETTERS.size()-1) {
						shiftedIndex -= LETTERS.size();
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
					int shiftedIndex = LETTERS.indexOf(upperChar) + SHIFT;

					// If the shiftedIndex is bigger than the size of the ArrayList, we need to substract the size of the ArrayList
					if(shiftedIndex > LETTERS.size()-1) {
						shiftedIndex -= LETTERS.size();
					}

					// Get the new shifted char
					char shiftedChar = LETTERS.get(shiftedIndex);

					// Lower case the shifted char
					char lowerShifted = Character.toLowerCase(shiftedChar);

					// Add to result
					result += Character.toString(lowerShifted);
				}
			}
			// Check for punctuations using Arraylist puncts
			else if(PUNCTS.contains(curr)) {
				result += Character.toString(curr);
			}
			// Check for numbers using ArrayList numbers
			else if(NUMBERS.contains(curr)) {
				result += Character.toString(curr);
			}
		}

		// Return result
		return result;
	}
}
