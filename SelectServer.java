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
    public static int BUFFERSIZE = 32;
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
        InetSocketAddress udp_isa = new InetSocketAddress(Integer.parseInt(args[0]));
        udp_channel.socket().bind(udp_isa);
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
                        System.out.println("Accept conncection from " + cchannel.socket().toString());
                        
                        // Register the new connection for read operation
                        cchannel.register(selector, SelectionKey.OP_READ);
                    } 
                    else 
                    {
                    	SelectableChannel sc = key.channel();
                    	if (sc instanceof DatagramChannel)
                    	{
                    		DatagramChannel dc = (DatagramChannel)sc;
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
	                                continue;
	                            }
	                             
	                            inBuffer.flip();      // make buffer available  
	                            decoder.decode(inBuffer, cBuffer, false);
	                            cBuffer.flip();
	                            line = cBuffer.toString();
	                            bytesRecv = line.length();
	                            System.out.print("UDP Client: " + line);
	                   	                          
	                            // Echo the message back
	                            inBuffer.flip();
	                            bytesSent = dc.send(inBuffer, addr); 
	                            if (bytesSent != bytesRecv)
	                            {
	                                System.out.println("write() error, or connection closed");
	                                key.cancel();  // deregister the socket
	                                continue;
	                            }
	                            
	                            if (line.equals("terminate"))
	                                terminated = true;
	                         }
                    	}
                    	else
                    	{
	                        SocketChannel cchannel = (SocketChannel)sc;
	                        if (key.isReadable())
	                        {
	                            Socket socket = cchannel.socket();
	                        
	                            // Open input and output streams
	                            inBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
	                            cBuffer = CharBuffer.allocate(BUFFERSIZE);
	                             
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

	                            if (line.equals("list"))
	                            {
	                            	System.out.println("");
		                            String current = new File( "." ).getCanonicalPath();
		                            File directory = new File(current);
		                            File[] files = directory.listFiles();

	                            	for (int i = 0; i < files.length; i++) 
	                            	{
                            			if (files[i].isFile()) 
                            			{
                            				System.out.println(files[i].getName());
                            			} 
	                                }
	                            }

	                            if (line.startsWith("get"))
	                            {
	                            	
	                            }
	                            
	                            // Echo the message back
	                            inBuffer.flip();
	                            bytesSent = cchannel.write(inBuffer); 
	                            if (bytesSent != bytesRecv)
	                            {
	                                System.out.println("write() error, or connection closed");
	                                key.cancel();  // deregister the socket
	                                continue;
	                            }
	                            if (line.equals("terminate\n"))
	                                terminated = true;
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
    
	void execute(String c) 
	{
		Process p;
		try 
		{
			p = Runtime.getRuntime().exec(c);
			p.waitFor();
		} 
		catch (Exception e) 
		{
            System.out.println(e);
		}
	}
	
    static void closeChannel(SelectableChannel channel)
    {
    	try
    	{
    		if (channel instanceof ServerSocketChannel)
    		{
    			ServerSocketChannel tcpChannel = (ServerSocketChannel)channel;
	        	tcpChannel.socket().close();
    		}
    		else if (channel instanceof DatagramChannel)
    		{
	        	DatagramChannel udpChannel = (DatagramChannel)channel;
            	udpChannel.socket().close();
	        }
    	}
        catch (IOException e) {
            System.out.println(e);
    	}
    }
}
