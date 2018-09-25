/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpfx;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author danye
 */
public class IndexController implements Initializable {

    @FXML
    private Button startServer;
    @FXML
    private Button stopServer;
    @FXML
    private TextField port;
    @FXML
    private TextArea serverLogs;
    @FXML
    private ListView serverHome;

    private static final String ROOT_DIRECTORY = "C:\\";
    
    Socket clientSocket;
    ServerSocket serverSocket;
    boolean serverRunning = true;

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        File f = new File(ROOT_DIRECTORY);
        ArrayList<File> files = new ArrayList<>(Arrays.asList(f.listFiles()));
        files.stream().filter((fl) -> (fl.isDirectory())).forEachOrdered((fl) -> {
            serverHome.getItems().add(fl.getName());
        });
    }

    @FXML
    private void StartServer(ActionEvent event) throws IOException, InterruptedException {
        String portInput = port.getText().trim();
        //String serverDir = serverHome.getSelectionModel().getSelectedItem().toString();
        boolean isNum = isNumber(portInput);
        boolean isInRange = isWithinRange(portInput);
        if (portInput.isEmpty()) {
            serverLogs.appendText("Please specify the Server Port\n\n");
        } else if (serverHome.getSelectionModel().isEmpty()) {
            serverLogs.appendText("Please select a directory with static content from Server Home\n\n");
        } else if (isNum || isInRange) {
            serverLogs.appendText("Port must be a number and must range from 1023 to 65535\n\n");
        } else {
            serverLogs.appendText(String.format("Starting Web Server on port %s\n", portInput));
            Path filePath = Paths.get(ROOT_DIRECTORY, serverHome.getSelectionModel().getSelectedItem().toString());
            try{
                serverSocket = new ServerSocket(Integer.parseInt(portInput));
                serverLogs.appendText("Server Started...\n");
                serverLogs.appendText("Awaiting Client Connection...\n");
                stopServer.setDisable(false);
                startServer.setDisable(true);
                TimeUnit.SECONDS.sleep(2);
                while(serverRunning){
                    serverRunning = false;
                    //Thread.sleep(2000);
                    //clientSocket = serverSocket.accept(); //instantiate HttpServer
//                    if(clientSocket.isConnected()){
//                        String addr = clientSocket.getRemoteSocketAddress().toString();
//                        serverLogs.appendText(String.format("Connection Established with %s\n", addr));
                    ClientHandler clientHandler = new ClientHandler(filePath.toString(), portInput, serverRunning, serverLogs, clientSocket, serverSocket);
                    clientHandler.start();
                    //}
                }
                
            }
            catch(IOException | NumberFormatException x){
                System.err.println(x);
            }
            
        }
    }

    @FXML
    private void StopServer(ActionEvent event) throws IOException {
        serverRunning = false;
        serverLogs.appendText("Stopping Server...\n");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(IndexController.class.getName()).log(Level.SEVERE, null, ex);
        }
        serverLogs.appendText("Server stopped...\n");
        stopServer.setDisable(true);
        startServer.setDisable(false);
        serverSocket.close();
    }

    private boolean isNumber(String portInput) {
        return !portInput.matches("[0-9]*");
    }

    private boolean isWithinRange(String portInput) {
        if (!portInput.matches("[0-9]*")) {
            return false;
        } else return !(Integer.parseInt(portInput) >= 1023 && Integer.parseInt(portInput) <= 65535);
    }
    
    
    public void endServer() throws IOException{
        stopServer.setDisable(true);
        startServer.setDisable(false);
        serverSocket.close();
    }

}
