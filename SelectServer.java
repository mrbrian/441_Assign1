/*
 * A simple TCP select server that accepts multiple connections and echo message back to the clients
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class SelectServer {
    public static int BUFFERSIZE = 256;
    public static void main(String args[]) throws Exception 
    {
        if (args.length != 1)
        {
            System.out.println("Usage: UDPServer <Listening Port>");
            System.exit(1);
        }

        // Initialize buffers and coders for channel receive and send
        String line = "";
        Charset charset = Charset.forName( "us-ascii" );  
        CharsetDecoder decoder = charset.newDecoder();  
        CharsetEncoder encoder = charset.newEncoder();
        ByteBuffer inBuffer = null;
        CharBuffer cBuffer = null;
        int bytesSent, bytesRecv;     // number of bytes sent or received
        
        // Initialize the selector
        Selector selector = Selector.open();

        // Create a server channel and make it non-blocking
        ServerSocketChannel tcp_channel = ServerSocketChannel.open();
        tcp_channel.configureBlocking(false);
       
        // Get the port number and bind the socket
        InetSocketAddress isa = new InetSocketAddress(Integer.parseInt(args[0]));
        tcp_channel.socket().bind(isa);

        // Register that the server selector is interested in connection requests
        tcp_channel.register(selector, SelectionKey.OP_ACCEPT);

        // Declare a UDP server socket and a datagram packet
        DatagramChannel udp_channel = null;        
        udp_channel = DatagramChannel.open();
        udp_channel.socket().bind(isa);
        udp_channel.configureBlocking(false);
        udp_channel.register(selector, SelectionKey.OP_READ);
        
        // Wait for something happen among all registered sockets
        try {
            boolean terminated = false;
            while (!terminated) 
            {
                if (selector.select(500) < 0)
                {
                    System.out.println("select() failed");
                    System.exit(1);
                }
                
                // Get set of ready sockets
                Set readyKeys = selector.selectedKeys();
                Iterator readyItor = readyKeys.iterator();

                // Walk through the ready set
                while (readyItor.hasNext()) 
                {
                    // Get key from set
                    SelectionKey key = (SelectionKey)readyItor.next();

                    // Remove current entry
                    readyItor.remove();

                    // Accept new connections, if any
                    if (key.isAcceptable())
                    {
                        SocketChannel cchannel = ((ServerSocketChannel)key.channel()).accept();
                        cchannel.configureBlocking(false);
                        System.out.println("Accept connection from " + cchannel.socket().toString());
                        
                        // Register the new connection for read operation
                        cchannel.register(selector, SelectionKey.OP_READ);
                    } 
                    else 
                    {
                    	SelectableChannel sc = key.channel();
                    	if (sc instanceof DatagramChannel)		// is this on the UDP channel?
                    	{
                        	DatagramChannel dc = (DatagramChannel)sc;
                    		line = do_UDP(dc, key, inBuffer, cBuffer, decoder);		// then process UDP message
                    		
							if (line == null)
								continue;
							
                            if (line.equals("terminate"))		// check for terminate
                                terminated = true;
                    	}
                    	else		// it's not UDP, must be TCP then
                    	{
	                        SocketChannel cchannel = (SocketChannel)sc;
	                        if (key.isReadable())
	                        {	                        
	                            // Open input and output streams
	                            inBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
	                            cBuffer = CharBuffer.allocate(BUFFERSIZE);
	                             
	                            Thread.sleep(100);		

	                            // Read from socket
	                            bytesRecv = cchannel.read(inBuffer);
	                            if (bytesRecv <= 0)
	                            {
	                                System.out.println("read() error, or connection closed");
	                                key.cancel();  // deregister the socket
	                                continue;
	                            }
	                             
	                            inBuffer.flip();      // make buffer available  
	                            decoder.decode(inBuffer, cBuffer, false);
	                            cBuffer.flip();
	                            line = cBuffer.toString();
	                            System.out.print("TCP Client: " + line);

                            	String[] strSplit = line.split(" ");	// split up commands
                            	strSplit[0] = strSplit[0].replaceAll("\\s+", "");	// trim whitespace
                            	
	                            if (line.equals("list\n"))
	                            {   
	                            	String outputStr = getFileList(".");
	                            	int strLen = outputStr.length();
	                            	
	                            	ByteBuffer bufferSize = ByteBuffer.allocate(4);		// send the size of message
		                            bufferSize.putInt(strLen);
		                            bufferSize.rewind();
		                            cchannel.write(bufferSize);

	                            	bytesSent = sendMessage(outputStr, cchannel, encoder);	// send message to client
	                            	
		                            if (bytesSent != outputStr.length())
		                            {
		                                System.out.println("write() error, or connection closed");
		                                key.cancel();  // deregister the socket
		                                continue;
		                            }
	                            }
	                            else if (strSplit[0].equals("get"))
	                            {
		                            String filename = strSplit.length > 1 ? strSplit[1] : "<none>";		// check if a filename was passed	
		                            filename = filename.replaceAll("\\s+", "");			// trim whitespace
		                            System.out.print("Open file: " + filename + "\n");

	                            	ByteBuffer bufferSize = ByteBuffer.allocate(8);
		                            byte[] data = getFile(filename);
		                            
		                            if (data == null)		// error while reading file 
		                            {
		                                System.out.println(filename + " not found.");

			                            bufferSize.putLong(-1);			// let client know the file read failed
			                            bufferSize.rewind();
			                            cchannel.write(bufferSize);		// send fail filesize
		                            
		                                // send error message to client		                                
		                                sendMessage("Error in opening file " + filename + "\n", cchannel, encoder);
		                            }
		                            else
		                            {       
			                            bufferSize.putLong(data.length);	 
			                            bufferSize.rewind();
			                            cchannel.write(bufferSize);		// send filesize to client
		                            
			                            ByteBuffer outBuf = ByteBuffer.allocate(data.length);
			                            outBuf.put(data);
			                            outBuf.flip();
			                            cchannel.write(outBuf);		// send file data to client
		                            }
	                            }
	                            else if (line.equals("terminate\n"))
	                            {
	                                terminated = true;
	                            }
	                            else
	                            {
	                            	line = line.replaceAll("\\s+", "");						// trim whitespace
	                            	String outStr = "Unknown command: " + line + "\n";		// build error string
	                            	int outLen = outStr.length();
	                            	bytesSent = sendMessage(outStr, cchannel, encoder);		// send error to client
		                            
		                            if (bytesSent != outLen)
		                            {
		                                System.out.println("write() error, or connection closed");
		                                key.cancel();  // deregister the socket
		                                continue;
		                            }
	                            }
                        	}
                    	}
                    }
                } // end of while (readyItor.hasNext()) 
            } // end of while (!terminated)
        }
        catch (IOException e) {
            System.out.println(e);
        }
 
        // close all connections
        Set keys = selector.keys();
        Iterator itr = keys.iterator();
        while (itr.hasNext()) 
        {
            SelectionKey key = (SelectionKey)itr.next();
            //itr.remove();
            if (key.isAcceptable())
            {
            	closeChannel(key.channel());
            }
            else if (key.isValid())
            {
            	closeChannel(key.channel());                
            }
        }
    }

    // getFile - returns byte array containing the contents of some file 
    static byte[] getFile(String filename)
    {
    	byte[] result = null;
	    
    	try
	    {	
  		    File f = new File(filename);
  		    FileInputStream input = new FileInputStream(f);

	    	int size = (int)f.length();
		    result = new byte[size];

		    input.read(result);		// read bytes from file
		    input.close();
	    }
	    catch(IOException e) {
            System.out.println("open() failed");
            result = null;
        }
	    return result;
    }
    
    // getFileList - scans a path and returns string of contents
    static String getFileList(String dir)
    {
	    String result = "";
		try
		{
		    String current = new File(dir).getCanonicalPath();
		    File directory = new File(current);
		    File[] files = directory.listFiles();
			
		    for (int i = 0; i < files.length; i++) 
			{
				if (files[i].isFile()) 
					result += files[i].getName() + '\n';	// build file list 
		    }
		}
        catch (IOException e) {
            System.out.println(e);
        }
		return result;
    }    
    
    // closeChannel - closes the given SelectableChannel (either UDP or TCP)
    static void closeChannel(SelectableChannel channel)
    {
    	try
    	{
    		if (channel instanceof ServerSocketChannel)		// check is it TCP
    		{
    			ServerSocketChannel tcpChannel = (ServerSocketChannel)channel;
	        	tcpChannel.socket().close();
    		}
    		else if (channel instanceof DatagramChannel)	// check if its UDP
    		{
	        	DatagramChannel udpChannel = (DatagramChannel)channel;
            	udpChannel.socket().close();
	        }
    	}
        catch (IOException e) {
            System.out.println(e);
    	}
    }
    
    // do_UDP - processes an incoming UDP packet
    static String do_UDP(DatagramChannel dc, 
			    		SelectionKey key, 
			    		ByteBuffer inBuffer, 
			    		CharBuffer cBuffer, 
			    		CharsetDecoder decoder) throws IOException
    {
        if (key.isReadable())
        {
            // Open input and output streams
            inBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
            cBuffer = CharBuffer.allocate(BUFFERSIZE);
         
            // Read from socket
            SocketAddress addr = dc.receive(inBuffer);
            if (addr == null)
            {
                System.out.println("read() error, or connection closed");
                key.cancel();  // deregister the socket
                return null;
            }
             
            inBuffer.flip();      // make buffer available  
            decoder.decode(inBuffer, cBuffer, false);
            cBuffer.flip();
            String line = cBuffer.toString();
            int bytesRecv = line.length();
            System.out.print("UDP Client: " + line + "\n");		// print udp message
   	                          
            // Echo the message back
            inBuffer.flip();
            int bytesSent = dc.send(inBuffer, addr); 
            if (bytesSent != bytesRecv)
            {
                System.out.println("write() error, or connection closed");
                key.cancel();  // deregister the socket
                return null;
            }
            return line;
         }
        return null;
    }
     
    // sendMessage - sends string to client through TCP socketchannel
    static int sendMessage(String msg, 
				    		SocketChannel cchannel, 
				    		CharsetEncoder encoder) throws IOException
    {
    	int outLen = msg.length();
    	CharBuffer newcb = CharBuffer.allocate(outLen);		// allocate required space
    	ByteBuffer outBuf = ByteBuffer.allocate(outLen);
    	
    	newcb.put(msg);
    	newcb.rewind();
    	encoder.encode(newcb, outBuf, false);
    	outBuf.flip();
        int bytesSent = cchannel.write(outBuf);		// send to client
        
        return bytesSent;
    }
}
