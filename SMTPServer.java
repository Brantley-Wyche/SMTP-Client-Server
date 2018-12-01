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

   // Object streams
   private ObjectInputStream ois = null;
   private ObjectOutputStream oos = null;
   public static final String MSG_FILE = "userMSGS.obj";

	// Queue for queueing messages
	Queue<String> msgQueue = new LinkedList<>();

	// HashMap for storing messages with user
	Map<String, ArrayList<String>> msgStore = null;

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
					System.out.println("250 - HELO - OK");
					pwt.flush();
				}
				// MAIL FROM
				else if(cmd.startsWith("MAIL FROM:")) {
					// Get the username
					String username = cmd.substring(10);

					// Send status code
					pwt.println("250 - MAIL FROM - OK");
					System.out.println("250 - MAIL FROM - OK");
					pwt.flush();

					// RCPT TO
					String cmd2 = scn.nextLine();
					if(cmd2.startsWith("RCPT TO:")) {
						// Get the username
						String username2 = cmd2.substring(8);

						// Send status code
						pwt.println("250 - RCPT TO - OK");
						System.out.println("250 - RCPT TO - OK");
						pwt.flush();

						// DATA
						String cmd3 = scn.nextLine();
						if(cmd3.equals("DATA")) {
							// Send status code
							pwt.println("250 - DATA - OK");
							System.out.println("250 - DATA - OK");
							pwt.flush();

							// The client will send the message next
							String message = scn.nextLine();

							// Add From: into the message
							String fullMessage = "Mail From: " + username + "\n" + message + "\n";

							// Encrypt message
							String encryptedMsg = doEncrypt(fullMessage);

							// Prepare schema for encrypted msg
							String finalMsg = EMAIL_START + encryptedMsg + EMAIL_END;

							// Add to queue
							msgQueue.add(finalMsg);

							// Start MailThread
							MailThread mt = new MailThread(username2, msgQueue.peek());
							mt.start();

							// Send status code
							pwt.println("250 - Message Queued - OK");
							System.out.println("250 - Message Queued - OK");
							pwt.flush();
						}
					}
				}
				else if(cmd.startsWith("RETRIEVE FROM:")){
				// Get username
				String user = cmd.substring(14);

				// retrieve messages for this user
				doRetrieve(user);
				}
				else if(cmd.equals("QUIT")) {
					// Send response;
					pwt.flush();

					// Stop streams, write HashMap
					pwt.println("221 - QUIT - OK") into obj file
					doStop();

					// Kill program
					System.exit(0);
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
		// Attributes
		String username = "";
		String message = "";

		// Constructor
		public MailThread(String _username, String _message) {
			this.username = _username;
			this.message = _message;
		}

		// Run
		public void run() {
			// Map will take the username as key, ArrayList as value

			// Check if username exists in map first
			if(msgStore.containsKey(username)) {
				// Get user's current messages
				ArrayList<String> userMessages =  msgStore.get(username);

				// Add message to userMessages
				userMessages.add(message);

				// Update map
				msgStore.put(username, userMessages);
			}
			// If username does not exist
			else {
				// Create Arraylist
				ArrayList<String> allMessages = new ArrayList<>();

				// Add to allMesssages
				allMessages.add(message);

				// Add to map
				msgStore.put(username, allMessages);
			}
		}
	}


	/**
	 * Starts sserver thread and socket to listen for connections
	 */
	private void doStart() {
      msgStore = new HashMap<>();
      ArrayList<String> list = new ArrayList<>();
      list.add("Hello there");
      msgStore.put("bxm5989",list);

      // Look for map object file first before starting
      // try{
//          File msgFile = new File(MSG_FILE);
//          ois = new ObjectInputStream(new FileInputStream(msgFile));
//
//          // Checking if the fiel exists
//          if(!msgFile.exists()){
//             msgStore = new HashMap<>();
//          }else{
//
//             // General object to readObject
//             Object o = ois.readObject();
//
//             if(o instanceof HashMap){
//                 msgStore = (Map)o; // cast to hashmap
//             }
//
//          }// end of else
//       }
//       catch(FileNotFoundException fnfe){
//          fnfe.printStackTrace();
//       }
//       catch(ClassNotFoundException cnfe){
//          cnfe.printStackTrace();
//       }
//       catch(IOException ioe){
//          ioe.printStackTrace();
//       }

		// Starts ServerStart thread
		new ServerStart().start();
	}

	/**
	 * Closes server thread and socket to prevent further connections
	 */
	private void doStop() {
		// Closes sSocket and streams
		try {
         // Make file object
         File file = new File(MSG_FILE);
         oos = new ObjectOutputStream(new FileOutputStream(file)); // objectoutputstream

         // writes the hashmap into separate msg file
         oos.writeObject(msgStore);
         oos.flush();


         // Close stream
			sSocket.close();
			scn.close();
			pwt.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}


	/**
	 * On command "RETRIEVE FROM USER", the server will look for users that have the USER combination and return all their messages
	 */
	private void doRetrieve(String _user) {
         String user = _user;

         // Send messages back to user
         ArrayList<String> allMail = msgStore.get(user);
         String mail = "";

         // interate through list
         for(String m : allMail) {
             mail += m + "\n";
          }

          // send Mail to user
          pwt.println(mail);
          pwt.flush();
	}// end of doRetrieve



	/**
	 * Encrypts message received from client with Ceasar Cipher of Shift 13
	 */
	private String doEncrypt(String message) {
      String result = "";

      for(int i = 0; i < message.length(); i++){
         int characterPos = LETTERS.indexOf(message.charAt(i));
         int keyValue = (SHIFT + characterPos) % 26;
         char replaceVal = LETTERS.charAt(keyValue);
         result += replaceVal;
	  }

	  return result;
	}// end of doEncrypt
}
