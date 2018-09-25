/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpfx;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import javafx.scene.control.TextArea;

/**
 *
 * @author danye
 */
public final class ClientHandler extends Thread {
    private final String directory;
    private final String portInput;
    boolean serverRunning;
    TextArea serverLogs;
    Socket clientSocket;
    ServerSocket serverSocket;
    private static final String DEFAULT_FILE = "index.html";
    
    public ClientHandler(String directory, String portInput, boolean serverRunning, 
            TextArea serverLogs, Socket clientSocket, ServerSocket serverSocket){
        this.directory = directory;
        this.portInput = portInput;
        this.serverRunning = serverRunning;
        this.serverLogs = serverLogs;
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
    }
    
    private void fileNotFound() throws IOException {
        //send file not found HTTP headers
        serverLogs.appendText("No static Content found");
        IndexController ic = new IndexController();
        ic.endServer();
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")
                || fileRequested.endsWith(".html")) {
            return "text/html";
        } else if (fileRequested.endsWith(".gif")) {
            return "image/gif";
        } else if (fileRequested.endsWith(".jpg")
                || fileRequested.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileRequested.endsWith(".class")
                || fileRequested.endsWith(".jar")) {
            return "applicaton/octet-stream";
        } else {
            return "text/plain";
        }
    }

    public void close(Object stream) {
        if (stream == null) {
            return;
        }

        try {
            if (stream instanceof Reader) {
                ((Reader) stream).close();
            } else if (stream instanceof Writer) {
                ((Writer) stream).close();
            } else if (stream instanceof InputStream) {
                ((InputStream) stream).close();
            } else if (stream instanceof OutputStream) {
                ((OutputStream) stream).close();
            } else if (stream instanceof Socket) {
                ((Socket) stream).close();
            } else {
                System.err.println("Unable to close object: " + stream);
            }
        } catch (IOException e) {
            System.err.println("Error closing stream: " + e);
        }
    }
    
    public void runMain(String directory, Socket connect, BufferedReader in, String input) throws IOException {
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            dataOut = new BufferedOutputStream(
                    connect.getOutputStream());
            out = new PrintWriter(connect.getOutputStream());
            //get first line of request from client
            //String input = in.readLine();
            //create StringTokenizer to parse request
            StringTokenizer parse = new StringTokenizer(input);
            //parse out method
            String method = parse.nextToken().toUpperCase();
            //parse out file requested
            fileRequested = parse.nextToken().toLowerCase();

            //methods other than GET and HEAD are not implemented
            if (!method.equals("GET")) {

                //send Not Implemented message to client
                serverLogs.appendText("Only GET method implemented");
                IndexController ic = new IndexController();
                ic.endServer();

                return;
            }

            //If we get to here, request method is GET or HEAD
            if (fileRequested.endsWith("/")) {
                //append default file name to request
                fileRequested += DEFAULT_FILE;
            }

            //create file object
            File file = new File(directory, fileRequested);
            //get length of file
            int fileLength = (int) file.length();

            //get the file's MIME content type
            String content = getContentType(fileRequested);

            //if request is a GET, send the file content
            if (method.equals("GET")) {
                FileInputStream fileIn = null;
                //create byte array to store file data
                byte[] fileData = new byte[fileLength];

                try {
                    //open input stream from file
                    fileIn = new FileInputStream(file);
                    //read file into byte array
                    fileIn.read(fileData);
                } finally {
                    close(fileIn); //close file input stream
                }

                //send HTTP headers
                out.println("HTTP/1.0 200 OK");
                out.println("Server: Java HTTP Server 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + content);
                out.println("Content-length: " + file.length());
                out.println(); //blank line between headers and content
                out.flush(); //flush character output stream buffer

                dataOut.write(fileData, 0, fileLength); //write file
                dataOut.flush(); //flush binary output stream buffer
            }

        } catch (FileNotFoundException fnfe) {
            //inform client file doesn't exist
            fileNotFound();
        } catch (IOException ioe) {
            System.err.println("Server Error: " + ioe);
        } finally {
            close(in); //close character input stream
            close(out); //close character output stream
            close(dataOut); //close binary output stream

        }
    }
    
    @Override
    public void run() {
        try {
                clientSocket = serverSocket.accept();
                if(clientSocket.isConnected()){
                        String addr = clientSocket.getRemoteSocketAddress().toString();
                        serverLogs.appendText(String.format("Connection Established with %s\n", addr));
                }
                //create new thread
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                //get first line of request from client
                String input = in.readLine();
                //create StringTokenizer to parse request
                StringTokenizer parse = new StringTokenizer(input);
                //parse out method
                String method = parse.nextToken().toUpperCase();
                //parse out file requested
                
                
                 //DEFAULT_FILE = parse.nextToken().toLowerCase();
                runMain(directory, clientSocket, in, input);
            //}
            //Socket s = serverSocket.accept();  // Wait for a client to connect
            //Path path = Paths.get(rootDirectory , directory);
        } catch (IOException | NumberFormatException x) {
            System.out.println(x);
            serverLogs.appendText(x.toString());
        }//To change body of generated methods, choose Tools | Templates.
    }
    
}
