/*
 * A simple TCP client that sends messages to a server and display the message
   from the server. 
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */


import java.io.*; 
import java.net.*; 

class TCPClient { 

    public static void main(String args[]) throws Exception 
    { 
        if (args.length != 2)
        {
            System.out.println("Usage: TCPClient <Server IP> <Server Port>");
            System.exit(1);
        }

        // Initialize a client socket connection to the server
        Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1])); 

        // Initialize input and an output stream for the connection(s)
        DataOutputStream outBuffer = 
          new DataOutputStream(clientSocket.getOutputStream()); 
        BufferedReader inBuffer = 
          new BufferedReader(new
          InputStreamReader(clientSocket.getInputStream())); 

        // Initialize user input stream
        String line; 
        BufferedReader inFromUser = 
        new BufferedReader(new InputStreamReader(System.in)); 

        // Get user input and send to the server
        // Display the echo meesage from the server
        System.out.print("Please enter a message to be sent to the server ('logout' to terminate): ");
        line = inFromUser.readLine(); 
        while (!line.equals("logout"))
        {
            // Send to the server
            outBuffer.writeBytes(line + "\n"); 
            
            // Getting response from the server
            line = inBuffer.readLine();
            
			if(!line.equals("get") || !line.equals("list") || !line.equals("terminate") ||
					!line.equals("logout")){
						
				System.out.println("Incorrect command: " + line);
			}else{
				System.out.println("Server: " + line);
			}

        	if (line.equals("list"))
        	{
        		boolean gotList = false;
				
        		// 	read line loop.  until end...
        		while (!gotList && !inBuffer.ready())
        		{        			
        		}
        		
    			while (inBuffer.ready())
    			{
        			line = inBuffer.readLine();
        			System.out.println(line);
        		}
        	}
        	else if (line.startsWith("get"))
        	{
    			int size = 0;
        		boolean gotList = false;
    			String outFile = "buh";

        		while (!gotList && !inBuffer.ready())
        		{        	
        			System.out.println("wait");		
        		}

        	//	ArrayList<byte> data = new ArrayList<byte>();
    			while (inBuffer.ready())
    			{
        			//b = inBuffer.read();
        			System.out.println(size++);
        		}
    			
    			System.out.println(String.format("File saved in %s (%d bytes)", outFile, size));
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
} 
