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

	// Arraylist
	ArrayList<String> userList = new ArrayList<>();

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
			// Start server at SERVER_PORT
			try {
				sSocket = new ServerSocket(SERVER_PORT);

				System.out.println("Server started at Port " + SERVER_PORT);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

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

					// RCPT TO
					String cmd2 = scn.nextLine();
					if (cmd2.startsWith("RCPT TO:")) {
						// Get the recipient
						String recipient = cmd2.substring(8);

						// Remove "<" and ">" if it does not exist
						if (recipient.contains("<") && recipient.contains(">")) {
							recipient = recipient.replace("<", "");
							recipient = recipient.replace(">", "");
						}

						// Parse the username for "@"
						String[] parts = recipient.split("@");
						try {

						}
						catch(ArrayIndexOutOfBoundsException ae) {
							ae.printStackTrace();

							pwt.println("221 Not a valid user address!");
							System.out.println("221 Not a valid user address!");
							pwt.flush();
						}
						userName = parts[0];
						ip = parts[1];

						// Send status code
						pwt.println("250 RCPT TO OK");
						System.out.println("250 RCPT TO OK");
						pwt.flush();

						// DATA
						String cmd3 = scn.nextLine();
						if (cmd3.equals("DATA")) {
							// Send status code
							pwt.println("354 End ");
							System.out.println("354 DATA OK");
							pwt.flush();

							// Listen for message
							message = scn.nextLine();

							// Add to queue
							msgQueue.add(message);

							// Send status code
							pwt.println("250 Message Queued");
							System.out.println("250 Message Queued");
							pwt.flush();

							// Open thread to handle message in queue
							new MailThread(userName, ip, msgQueue.peek()).start();
						}
					}
				}
				// RETRIEVE FROM:username pass
				else if (cmd.startsWith("RETRIEVE FROM:")) {
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

					// Flag
					boolean userAuthorized = false;

					// Check if user is authorized
					for (String u : userList) {
						if (username.equals(u)) {
							userAuthorized = true;
						}
					}

					// Check flag
					if (userAuthorized == true) {
						// Check if file exists
						if (mailboxFile.exists()) {
							// Send "250"
							pwt.println("250 RETRIEVE FROM OK");
							System.out.println("250 RETRIEVE FROM OK");
							pwt.flush();

							// Scanner
							Scanner scn = null;

							// Read file
							try {
								// Open the file if it exists
								scn = new Scanner(new FileInputStream(mailboxFile));

								String mail = "";

								while (scn.hasNextLine()) {
									mail += scn.nextLine();
								}

								pwt.println(mail);
								pwt.flush();
							}
							// Mailbox does not exist
							catch (FileNotFoundException fnfe) {
								fnfe.printStackTrace();
							}
						}
						// If mailbox does not exist
						else {
							// Create mailbox
							pwt.println("221 RETRIEVE FROM FAILED - Mailbox empty!");
							System.out.println("221 RETRIEVE FROM FAILED - Mailbox empty!!");
							pwt.flush();
						}
					} else {
						pwt.println("221 RETRIEVE FROM FAILED - User not authorized!");
						System.out.println("221 RETRIEVE FROM FAILED - User not authorized!");
						pwt.flush();
					}
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
					} catch (Exception e) {
						e.printStackTrace();
					}
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
	 * Thread that prepares the message in the queue, then add its the to the user's
	 * mailbox file.
	 * 
	 * It will check if the user is authorized on this server, and if the ip is
	 * correct. If it is not then relays to the correct server.
	 */
	class MailThread extends Thread {
		String user = "";
		String userIP = "";
		String message = "";

		public MailThread(String _user, String _userIP, String _message) {
			user = _user;
			userIP = _userIP;
			message = _message;
		}

		public void run() {
			// Check username exists
			try {
				// Prepare IO for mailboxFile
				File mailboxFile = new File(user + ".txt");

				// Flag
				boolean userAuthorized = false;

				// Iterate userList
				for (String u : userList) {
					if (user.equals(u)) {
						userAuthorized = true;
					}
				}

				// Check flag
				if (userAuthorized == true) {
					// Add "From:" into the message
					String fullMessage = "Mail From: " + sender + " " + message;

					// Encrypt message
					String encryptedMsg = doEncrypt(fullMessage);

					// Prepare schema for encrypted msg
					String finalMsg = EMAIL_START + encryptedMsg + EMAIL_END;

					// Checks if a mailbox exists for user
					if (mailboxFile.exists()) {
						System.out.println(user + "'s mailbox for " + "found!");

						// Read mailbox
						PrintWriter mailPWT = new PrintWriter(new FileOutputStream(mailboxFile, true));

						// Write message to file
						mailPWT.println(finalMsg);
						mailPWT.flush();

						// close scanner
						mailPWT.close();
					}
					// If mailbox does not exists
					else {
						System.out.println("Creating mailbox for " + user + "!");

						// Read mailbox
						PrintWriter mailPWT = new PrintWriter(new FileOutputStream(mailboxFile, true));

						// Create mailbox file
						mailboxFile.createNewFile();

						// Write message to file
						mailPWT.println(finalMsg);
						mailPWT.flush();

						// close scanner
						mailPWT.close();
					}
				}
				// user not authorized
				else {
					// Replay
					RelayThread rThread = new RelayThread(sender, user, userIP, message);
					rThread.start();

					// Send status code
					System.out.println("Message Relayed");
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}// end of run
	}// end of mailthread

	/**
	 * Thread that takes care of relaying. It will be called when relaying is
	 * required. Opens new socket and attempts to conect to the correct server ip.
	 * 
	 * It will send the SMTP protocols for sending the message
	 */
	class RelayThread extends Thread {
		String user = "";
		String userIP = "";
		String from = "";
		String message = "";

		private Scanner rScan = null;
		private PrintWriter rPwt = null;

		public RelayThread(String _from, String _user, String _userIP, String _message) {
			from = _from;
			user = _user;
			userIP = _userIP;
			message = _message;
		}

		public void run() {
			try {
				// Prepare socket
				rSocket = new Socket(userIP, SERVER_PORT);
				rScan = new Scanner(new InputStreamReader(rSocket.getInputStream()));
				rPwt = new PrintWriter(new OutputStreamWriter(rSocket.getOutputStream()));

				System.out.println("Relay - Starting Relay");

				// Get ip of this socket
				String clientIp = String.valueOf(rSocket.getInetAddress());

				// Reads response
				String resp = rScan.nextLine();

				// Listen for "220"
				if (resp.contains("220")) {
					// HELO
					rPwt.println("HELO " + clientIp);
					rPwt.flush();

					// Listen for "250"
					String resp2 = rScan.nextLine();
					if (resp2.contains("250")) {
						System.out.println("Relay - Connected to " + userIP);

						// MAIL FROM:<username@ip>
						rPwt.println("MAIL FROM:<" + user + "@" + userIP + ">");
						rPwt.flush();

						// Listen for "250"
						String resp3 = rScan.nextLine();
						if (resp3.contains("250")) {

							// RCPT TO:<username@ip>
							rPwt.println("RCPT TO:" + from);
							rPwt.flush();

							// Listen to "250"
							String resp4 = rScan.nextLine();
							if (resp4.contains("250")) {

								// DATA
								rPwt.println("DATA");
								rPwt.flush();

								// Listen to "354"
								String resp5 = rScan.nextLine();
								if (resp5.contains("354")) {
									// Send message
									rPwt.println(message);
									rPwt.flush();

									// Listen to "250"
									String resp6 = rScan.nextLine();
									if (resp6.contains("250")) {

										try {
											// QUIT
											rPwt.println("QUIT");
											rPwt.flush();

											// Listen for "221"
											String resp7 = rScan.nextLine();
											if (resp7.contains("221")) {
												// Close connections and streams
												rSocket.close();
												rScan.close();
												rPwt.close();

												System.out.println("Relay - Connection closed");
											}

										} catch (IOException ioe) {
											ioe.printStackTrace();
										}
									}
								}
							}
						}
					}
				} else {
					System.out.println("Relay Failed - " + userIP);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * METHODS: doSave(), doStart(), doStop(), doRetrieve(), doEncrypt()
	 */

	/**
	 * Starts server thread and socket to listen for connections
	 */
	private void doStart() {
		getUsers();
		new ServerStart().start();
	}

	/**
	 * Get authorized users into an ArrayList
	 */
	private void getUsers() {
		// Scanner
		Scanner mScn = null;

		// Load users into an ArrayList
		try {
			// Prepare IO for mailBoxFile
			mScn = new Scanner(new FileInputStream(USER_FILE));

			while (mScn.hasNextLine()) {
				// Get user
				String user = mScn.nextLine();

				// Add to userList
				userList.add(user);
			}

			// Close stream
			mScn.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
	}

	/**
	 * Encrypt message with cipher of 13
	 */
	private String doEncrypt(String msg) {
		// Result
		String result = "";

		// Encryption based on shift, use a for loop to check each char of the string
		for (int i = 0; i < msg.length(); i++) {
			char curr = msg.charAt(i);

			// Check for spaces
			if (curr == ' ') {
				result += " ";
			}
			// Check for Upper Cased chars using ArrayList letters
			else if (Character.isUpperCase(curr)) {
				if (LETTERS.contains(curr)) {
					// Get the index to be shifted
					int shiftedIndex = LETTERS.indexOf(curr) + SHIFT;

					// If the shiftedIndex is bigger than the size of the ArrayList, we need to
					// substract the size of the ArrayList
					if (shiftedIndex > LETTERS.size() - 1) {
						shiftedIndex -= LETTERS.size();
					}

					// Get the new shifted char
					char shiftedChar = LETTERS.get(shiftedIndex);

					// Add to result
					result += Character.toString(shiftedChar);
				}
			}
			// Check for Lower Cased chars for using ArrayList letters
			else if (Character.isLowerCase(curr)) {
				// Because letters is all capitalized letters, we need to upper case the current
				// char
				char upperChar = Character.toUpperCase(curr);

				if (LETTERS.contains(upperChar)) {
					// Get the index to be shifted
					int shiftedIndex = LETTERS.indexOf(upperChar) + SHIFT;

					// If the shiftedIndex is bigger than the size of the ArrayList, we need to
					// substract the size of the ArrayList
					if (shiftedIndex > LETTERS.size() - 1) {
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
			else if (PUNCTS.contains(curr)) {
				result += Character.toString(curr);
			}
			// Check for numbers using ArrayList numbers
			else if (NUMBERS.contains(curr)) {
				result += Character.toString(curr);
			}
		}

		// Return result
		return result;
	}
}
