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

	// Object streams
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	public static final String MSG_FILE = "mailbox.obj";
   
   // Users
   public static final String USER_FILE = "users.txt";
   private ArrayList<String> userList = new ArrayList<String>();
   
   private String sender = "";
   private String userName = "";
   private String ip = "";
   private String message = "";

   

	// Queue for queueing messages
	Queue<String> msgQueue = new LinkedList<>();

	// HashMap for storing messages with user
// 	Map<String, ArrayList<String>> msgStore = null;

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
				System.out.println("Server started at " + SERVER_PORT);
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
         
			// 220 SENT FIRST
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

					// Send status code
					pwt.println("250 MAIL FROM OK");
					System.out.println("250 MAIL FROM OK");
					pwt.flush();

					// RCPT TO
					String cmd2 = scn.nextLine();
					if(cmd2.startsWith("RCPT TO:")) {
						// Get the recipient
						String recipient = cmd2.substring(8);
                  
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
                     
                     msgQueue.add(message);
                     new MailThread(userName,ip,msgQueue.peek()).start();
                     

							// Send status code
							pwt.println("250 Message Queued");
							System.out.println("250 Message Queued");
							pwt.flush();
						}
					}
				}
				else if(cmd.startsWith("RETRIEVE FROM:")){
					// Get username
					String user = cmd.substring(14);

					// Retrieve messages for this user
				//	doRetrieve(user);
               
               
               
				}
				else if(cmd.equals("QUIT")) {
					// Send response;
					pwt.println("221 Bye"); 
					System.out.println("Connection closed with " + clientIp); 
					pwt.flush();
				}
			}// end of while
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
	 * Starts sserver thread and socket to listen for connections
	 * 
	 * Will check for a save file to preload HashMap with user's messages
	 */
	@SuppressWarnings("unchecked") 
	private void doStart() {
		// Look for previous saved messages on server
	// 	try{
// 			File msgFile = new File(MSG_FILE);
// 			ois = new ObjectInputStream(new FileInputStream(msgFile));
// 
// 			Object o = ois.readObject();
// 
// 			if(o instanceof HashMap) {
// 				msgStore = (Map)o; 
// 				System.out.println("Mailbox found!");
// 			}
// 		}
// 		catch(FileNotFoundException fnfe){
// 			fnfe.printStackTrace();
// 
// 			System.out.println("Previous Mailbox not found...");
// 			System.out.println("Creating new Mailbox...");
// 			msgStore = new HashMap<>();
// 		}
// 		catch(ClassNotFoundException cnfe) { cnfe.printStackTrace();}
// 		catch(IOException ioe){ ioe.printStackTrace(); }

		// Starts ServerStart thread
		new ServerStart().start();
	}

	/** 
	 * Saves HashMap into obj file to preload for next use
	 */
	private void doSave() {
		// Save HashMap
// 		try {
// 			File file = new File(MSG_FILE);
// 			oos = new ObjectOutputStream( 
// 					new FileOutputStream(file)); 
// 
// 			// Writes HashMap into file
// 			oos.writeObject(msgStore);
// 			oos.flush();
// 			System.out.println("Saving Mailbox ...");
// 		}
// 		catch(FileNotFoundException fnfe) { fnfe.printStackTrace(); System.exit(0); }
// 		catch(IOException ioe) { ioe.printStackTrace(); }
 	}

	/**
	 * Closes server thread and socket to prevent further connections
	 */
	private void doStop() {
		// Save HashMap
		doSave();
		
		// Closes sSocket and streams
		try {
			// Close stream
			sSocket.close();
			scn.close();
			pwt.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
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
   
   
   /**
   * mail thread
   */
   class MailThread extends Thread{
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
                     if(userName.equals(scn.nextLine())){
                        if(ip.equals(String.valueOf(sSocket.getInetAddress()))){
                              // Add "From:" into the message
                           	String fullMessage = "Mail From: " + sender + "\n" + message;
                           
                           	// Encrypt message
                           	String encryptedMsg = doEncrypt(fullMessage);
                           
                           	// Prepare schema for encrypted msg
                           	String finalMsg = EMAIL_START + encryptedMsg + EMAIL_END;
                                       
                                       
                              // TODO: create mailbox file for user
                              File mailBoxFile = new File(userName+".txt");
                              mailPWT = new PrintWriter(new FileOutputStream(mailBoxFile,true)); // printwriter to read mailbox
                                    
                                       
                              // Checks for the user mailbox         
                              if(mailBoxFile.exists()){
                                    // Write message to file
                                    mailPWT.println(message);
                                    mailPWT.flush();
                              }
                              else{
                                   // Create mailbox file
                                   mailBoxFile.createNewFile();
                                             
                                    // Write message to file
                                    mailPWT.println(message);
                                    mailPWT.flush();
                               }     
                        }else{
                              RelayThread rThread = new RelayThread(sender, user, userIP, message);
                                    rThread.start();
                        }
                     }else{
                           RelayThread rThread = new RelayThread(sender, user, userIP, message);
                                 rThread.start();
                                
                              // Send message
                              pwt.println("221");
                              System.out.println("221");
                              pwt.flush();
                           }
                  }// end of while
                        
                        
                  // close scanner
                  scn.close();
                  mailPWT.close();
            }
            catch(IOException ioe){
            
            }              
      }// end of run
   }// end of mailthread
   
   
   
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
                
                String clientIp = String.valueOf(rSocket.getInetAddress());
                
                // Reads response
                String resp = rScan.nextLine();
                
                if(resp.contains("220")){
                  rPwt.println("HELO " + clientIp);
                  rPwt.flush();
                  
                  String resp2 = rScan.nextLine();
                  if(resp2.contains("250")){
                     System.out.println("Connected to " + userIP); 
                     
                     
                     rPwt.println("MAIL FROM:" + user+"@"+userIP);
                     System.out.println("MAIL FROM:" + user+"@"+userIP);
                     rPwt.flush();
                     
                     
                     String resp3 = rScan.nextLine();
                     if(resp3.contains("250")){
                        rPwt.println("RCPT TO:" + from);
                        rPwt.flush();
                        
                        String resp4 = rScan.nextLine();
                        if(resp4.contains("250")){
                           rPwt.println("DATA");
                           rPwt.flush();
                           
                           
                           String resp5 = rScan.nextLine();
                           if(resp5.contains("354")){
                              rPwt.println(message);
                              rPwt.flush();
                              
                              String resp6 = rScan.nextLine();
                              if(resp6.contains("250")){
                                 System.out.println("Message sent");
                                 try{
                                    rPwt.println("QUIT");
                                    rPwt.flush();
                                    
                                    
                                    String resp7 = rScan.nextLine();
                                    if(resp7.contains("221")){
                                       rSocket.close();
                                       rScan.close();
                                       rPwt.close();
                                    }

                                 }catch(IOException ioe){}
                              }
                           }
                        }
                     }
                  }else{   
                     System.out.println("221 Failed to relay to " + userIP);
                  }
                } 
            }
            catch(IOException ioe){
               
            }
    
      }
   }
}
