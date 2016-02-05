/*
 * A simple TCP client that sends messages to a server and display the message
   from the server. 
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */


import java.awt.image.DataBuffer;
import java.io.*; 
import java.net.*;
import java.nio.CharBuffer; 

class TCPClient { 

    public static void main(String args[]) throws Exception 
    { 
        if (args.length != 2)
        {
            System.out.println("Usage: TCPClient <Server IP> <Server Port>");
            System.exit(1);
        }
        
        try
        {
            // Initialize a client socket connection to the server
            Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1])); 
            
	        // Initialize input and an output stream for the connection(s)
	        DataOutputStream outBuffer = new DataOutputStream(clientSocket.getOutputStream()); 
	        BufferedReader inBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));         
	        DataInputStream inData = new DataInputStream(clientSocket.getInputStream());
	
	        // Initialize user input stream
	        String line; 
	        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 

	        // Get user input and send to the server
	        // Display the echo meesage from the server
	        System.out.print("Please enter a message to be sent to the server ('logout' to terminate): ");
	        line = inFromUser.readLine(); 
        
	        while (!line.equals("logout"))
	        {           
	            String[] split = line.split(" ");
	            split[0] = split[0].replaceAll("\\s+", "");		// trim whitespace from command
	            
	            // Send to the server
	            outBuffer.writeBytes(line + "\n"); 
	
				// List command detection
	        	if (split[0].equals("list"))
	        	{
	        		receiveFileList(inData); //receive file list
	        	}
				//Get command detection
	        	else if (split[0].equals("get"))
	        	{
	        		String filename = split.length > 1 ? split[1] : "noname";	//parse filename from user command.. use "noname" if none is given
	        		int port = clientSocket.getLocalPort(); //get client port
	        		String destfile = filename + "-" + port; //concatenate new filename
	        		receiveFile(inData, inBuffer, destfile); //getFile    			
	        	}
	        	else
	        	{
		            // Getting response from the server
		            line = inBuffer.readLine();
		            System.out.println("Server: " + line);
	        	}   
	        	
	            System.out.print("Please enter a message to be sent to the server ('logout' to terminate): ");
	            line = inFromUser.readLine(); 
	        }
	        
	        // Close the socket
	        clientSocket.close();    
        }
        catch(SocketException e)		// nicely catch error if connection is closed		
        {
        	System.out.println(e);
        }       
    } 
    
	// helper function to 
    static void receiveFile(DataInputStream dataBuffer, BufferedReader inBuffer, String destfile)
    {
		int bytesRead = 0;
		try
		{
			long filesize = dataBuffer.readLong(); // get file size
		
			if (filesize == -1)		// file error
			{
	            String line = inBuffer.readLine();
	            System.out.println("Server: " + line);
				return;
			}
			
			byte[] data = new byte[(int)filesize]; //byte conversion of filesize
			File content = new File(destfile); //destincation file generation
			
			FileOutputStream fos = new FileOutputStream(content); //new FileOutputStream
	        BufferedOutputStream bos = new BufferedOutputStream(fos); //new BufferedOutputStream
	
			bytesRead = dataBuffer.read(data, 0, data.length); //read all bytes from dataBuffer
		    bos.write(data, 0, bytesRead); //write bytes to the BufferedOutputStream
		    
		    bos.flush(); //flush BufferedOutputStream
			
		    fos.close(); //close FileOutputStream
			bos.close(); //close BufferedOutputStream
			System.out.println("File saved in " + destfile + " (" + filesize + " bytes)"); //print
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
    }
    
	//helper function to receive and display file list
    static void receiveFileList(DataInputStream inBuffer)
    {
	    int bytesRead = 0;
		try
		{
			int filesize = inBuffer.readInt(); //read file size
			
			byte[] data = new byte[filesize]; //initialize data to byte format
			bytesRead = inBuffer.read(data, 0, data.length); //get read byte size for error checking
			if (bytesRead != filesize) //read error
			{
				System.out.println("receiveFileList: expected " + filesize + " bytes, read " + bytesRead + " bytes");
				return;
			}
			String text = new String(data, "UTF-8"); //conversion of byte data to String
		 	
			System.out.print(text); //display file list
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
    }
} 