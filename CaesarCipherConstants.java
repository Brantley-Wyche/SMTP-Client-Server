import java.util.*; 

/**
* CaesarCipherConstants 
* 
* Variables needed for Caeser Cipher
* @author Brandon Mok + Xin Liu + Brantley Wyche
*/
public interface CaesarCipherConstants{
   	// Caesar Cipher
	public static final ArrayList<Character> LETTERS = new ArrayList<>(Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'));
	public static final ArrayList<Character> NUMBERS = new ArrayList<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
    public static final ArrayList<Character> PUNCTS = new ArrayList<>(Arrays.asList(',', '.', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '_', '=', '{', '}', '[', ']', '|', '<', '>', '?', '/', ':', ';', '"'));

    // Encryption shift
    public static final int SHIFT = 13;
 }// end of interface