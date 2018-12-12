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
 * @version 12/12/18
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

				sLog("Server started at Port: " + SERVER_PORT + ", Public IP: " + my_ip);
			} catch (IOException ioe) { ioe.printStackTrace(); }

			// Always listen for connections to server
			while (true) {
				// Accept connections to server
				try {
					cSocket = sSocket.accept();

					// Assign clientIp
					clientIp = String.valueOf(cSocket.getInetAddress());
					sLog("Connection established with " + clientIp);
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
			sLog("220 " + "ESMTP Postfix");
			pwt.flush();

			// Listen for commands
			while (scn.hasNextLine()) {
				// Get command
				String cmd = scn.nextLine().toLowerCase();

				// HELO
				if (cmd.startsWith("helo")) {
					System.out.println("C - " + cmd);

					// Send status code
					pwt.println("250 HELO " + clientIp + " OK");
					sLog("250 HELO " + clientIp + " OK");
					pwt.flush();
				}
				// MAIL FROM
				else if (cmd.startsWith("mail from:")) {
					System.out.println("C - " + cmd);

					// Get the sender
					sender = cmd.substring(10);

					// Remove "<" and ">" if it does not exist
					if (sender.contains("<") && sender.contains(">")) {
						String parsedSender = sender.replace("<", "");
						parsedSender = parsedSender.replace(">", "");
					}

					// Send status code
					pwt.println("250 MAIL FROM OK");
					sLog("250 MAIL FROM OK");
					pwt.flush();
				}
				// RCPT TO
				else if (cmd.startsWith("rcpt to:")) {
					System.out.println("C - " + cmd);

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
					sLog("250 RCPT TO OK");
					pwt.flush();
				}
				// DATA
				else if (cmd.equals("data")) {
					System.out.println("C - " + cmd);

					// Send status code
					pwt.println("354 End in <CR><LR>.<CR><LR>");
					sLog("354 End in <CR><LR>.<CR><LR>");
					pwt.flush();

					// Empty the message string
					message = "";

					// Listen for message
					while(scn.hasNextLine()) {		
						String line = scn.nextLine().trim();										
						if(!line.equals(".")) {
							message += line;
						}
						else {
							// Add to queue
							msgQueue.add(message);
							break;
						}
					}

					// Send status code
					pwt.println("250 Message Queued");
					sLog("250 Message Queued");
					pwt.flush();

					// Open thread to handle message in queue
					new MailThread(username, ip, msgQueue.poll()).start();
				}
				// RETRIEVE FROM username pass
				else if (cmd.startsWith("retrieve from")) {
					System.out.println("C - " + cmd);

					// Split the string by " "
					String[] parts = cmd.split(" ");

					// Get values
					String username = parts[2];
					String pass = parts[3];

					// Check for "<" and ">" in username
					if (username.contains("<") && username.contains(">")) {
						username = username.replace("<", "");
						username = username.replace(">", "");
					}

					// Check if username has "@"
					if(username.contains("@")) {
						// Split by "@"
						String[] user_parts = username.split("@");
						username = user_parts[0];
					}

					// Check for pass
					if(pass.equals(PASSWORD)) {
						// Send 250 
						pwt.println("250 Retrieval OK");
						sLog("250 Retrieval OK");
						pwt.flush();

						// Check if user has mailbox
						String mailbox = username + ".txt";
						File mailbox_file = new File(mailbox);

						// Check for mailbox
						if(!mailbox_file.exists()) {
							pwt.println("221 User has no mail on our server!");
							sLog("221 User has no mail on our server!");
							pwt.flush();
						}
						else {
							// Read from file and return all the messages
							try {
								FileInputStream fis = new FileInputStream(mailbox_file);
								Scanner mScn = new Scanner(fis);

								while(mScn.hasNextLine()) {
									String line = mScn.nextLine();
									pwt.println(line);
									sLog(line);
									pwt.flush();
								}

								// sLog(allMail);

								mScn.close();
								// pwt.print(allMail);
								// pwt.flush();
							}
							catch(IOException ioe) { ioe.printStackTrace(); }
						}
					}
					// Otherwise, send error code
					else {
						pwt.println("221 Password does not match!");
						sLog("221 Password does not match!");
						pwt.flush();
					}
				}
				// QUIT
				else if (cmd.equals("quit")) {
					System.out.println("C - " + cmd);

					// Send response
					pwt.println("221 Bye");
					sLog("221 Bye");
					pwt.flush();

					// Close streams
					try {
						scn.close();
						pwt.close();
					} catch (Exception e) { e.printStackTrace(); }
				}
				// If none of the commands are matched, send error
				else {
					System.out.println("C - " +  cmd);

					// Send error
					pwt.println("221 Unknown command recieved");
					sLog("221 Unknown command recieved");
					pwt.flush();
				}
			} // end of while
		}
	}

	/**
	 * Thread that add messages from the queue into the user's mailbox file.
	 * 
	 * This thread will check if the ip matches the host's public ip, and if it does not it will relay
	 */
	class MailThread extends Thread {
		// Attributes
		String user = "";
		String userIP = "";
		String message = "";

		// Constructor
		public MailThread(String _user, String _userIP, String _message) {
			this.user = _user;
			this.userIP = _userIP;
			this.message = _message;
		}

		// Run
		public void run() {
			// Check if user's ip matches our ip
			if(userIP.equals(my_ip) || userIP.equals(hardcoded_ip) || userIP.equals("localhost")) {
				sLog("Success - IP Verfied!");

				// Write to mailbox (user.txt)
				try {
					// Declare name of mailbox
					String mailbox = this.user + ".txt"; 

					// Open I/O
					File mailbox_file = new File(mailbox);

					// Check if file exists 
					if(!mailbox_file.exists()) {
						sLog("Creating mailbox for user...");
						mailbox_file.createNewFile();
					}

					FileOutputStream fos = new FileOutputStream(mailbox_file, true);
					PrintWriter mPwt = new PrintWriter(fos);

					// Write message
					mPwt.println(message); 
					mPwt.flush();
					sLog("Saved to mailbox!");
					mPwt.close();
				}
				catch(IOException ioe) { ioe.printStackTrace(); }	
			}
			// User's ip does not match our ip
			else {
				sLog("Failed - IP does not match.");

				// Relay to proper server
				new RelayThread(user, userIP, sender, message).start();
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
		String sender = "";
		String message = "";

		// I/O
		Scanner rScn = null;
		PrintWriter rPwt = null;

		// Constructor
		public RelayThread(String _user, String _userIP, String _sender, String _message) {
			this.user = _user;
			this.userIP = _userIP;
			this.sender = _sender;
			this.message = _message;
		}

		// Run
		public void run() {
			rLog("Relaying to server with Public IP: " + userIP);

			// Connect & open stream
			try {
				rSocket = new Socket(userIP, SERVER_PORT);
				rScn = new Scanner(new InputStreamReader(rSocket.getInputStream()));
				rPwt = new PrintWriter(new OutputStreamWriter(rSocket.getOutputStream()));

				String clientIp = String.valueOf(rSocket.getInetAddress());

				// Listen for "220"
				String resp = rScn.nextLine(); 

				if(resp.contains("220")) {
					// Send to the Server "HELO" with ip
					rPwt.println("HELO " + clientIp);
					rPwt.flush();

					// Listen for "250"
					resp = rScn.nextLine();

					if(resp.contains("250")) {
						// Show success alert
						rLog("Connected!");
					}
					// Error
					else {
						// Alert
						rLog("Error: " + resp);
					}
				}
				// Error
				else {
					// Alert
					rLog("Error: " + resp);
				}
			}
			catch(IOException ioe) {
				ioe.printStackTrace();

				// Show alert
				rLog("Could not connect to server!");
			}

			// Sends Server the "MAIL FROM" command
			rPwt.println("MAIL FROM:" + "<" + sender + ">");
			rLog("MAIL FROM:" + "<" + sender + ">");
			rPwt.flush();

			// Read resp
			String resp = rScn.nextLine();

			// Check for "250"
			if(resp.contains("250")) {
				// Sends "RCPT" to server
				rPwt.println("RCPT TO:" + "<" + this.user + "@" + this.userIP + ">");
				rLog("RCPT TO:" + "<" + this.user + "@" + this.userIP + ">");
				rPwt.flush();

				// Read resp
				resp = rScn.nextLine();

				// Check for "250"
				if(resp.contains("250")) {
					// Sends "DATA" to server
					rPwt.println("DATA");
					rLog("DATA");
					rPwt.flush();

					// Read resp
					resp = rScn.nextLine();

					// Check for "354"
					if(resp.contains("354")) {
						// Send msg
						rPwt.println(this.message);
						rLog(message); 
						rPwt.flush();

						// Read resp
						resp = rScn.nextLine();

						// Check for "250"
						if(resp.contains("250")) {
							// Show success alert
							rLog("Message sent!");

							// Disconnect
							try {
								rPwt.println("QUIT");
								rPwt.flush();
	
								// Read resp
								resp = rScn.nextLine();
	
								// Check for "221"
								if(resp.contains("221")) {
									rSocket.close();
									rPwt.close();
									rScn.close();
								}
								// Error
								else {
									rLog("Error: " + resp);
								}
							}
							catch(IOException ioe) {
								ioe.printStackTrace();
								rLog("Connection closed");
							}
						}
					}
					// Error
					else {
						rLog("Error: " + resp);
					}
				}
				// Error
				else {
					rLog("Error: " + resp);
				}
			}
			// Error
			else {
				rLog("Error: " + resp);
			}
		}

		// Print logs
		private void rLog(String m) {
			System.out.println("Relay - " + m);
		}
	}

	/**
	 * METHODS: 
	 * doStart(),
	 * sLog()
	 */

	/**
	 * Starts server thread and socket to listen for connections
	 */
	private void doStart() {
		new ServerStart().start();
	}

	/** 
	 * Logs server commands
	 */
	private void sLog(String m) {
		System.out.println("S - " + m);
	}
}
