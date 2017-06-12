package barebonehttp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URL;
import java.util.Hashtable;

/**
 * An Multithreaed Http server running a pared down version of HTTP/1.1
 *
 * @author me@syedraihan.com
 */
public class HttpServer extends Thread {
    // Program defaults
    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_DOCUMENT = "index.html";

    // Http protocol elements used in this implementation
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";    
    private static final String HTTP_STATUS_200 = "200 OK";
    private static final String HTTP_STATUS_201 = "201 Created";
    private static final String HTTP_STATUS_404 = "404 Not Found";
    private static final String HTTP_STATUS_405 = "405 Method Not Allowed";
    private static final String HTTP_STATUS_409 = "409 Conflict";

    private Socket _client;             // Socket connction to the client
    private String _rootPath;           // wwww Root directory
    private BufferedReader _request;    // Request 
    private OutputStream _response;     // Response 

    // The constructor
    public HttpServer(Socket client, String rootPath) {
        _client = client;
        _rootPath = rootPath;
    }

    // Override run() method of parent class Thread
    // Standard Java multi-threading mechanism
    @Override
    public void run() {                 
        processRequest();
    }

    // Server program entry point
    public static void main(String args[]) throws IOException  {
        int port = getPort(args);               // Get the port number from command line
        String wwwRootPath = getWWWRootPath();  // Get the root folder for the Http server

        ServerSocket server = new ServerSocket(port); 
        System.out.println("Http Server listening on port: " + port);

        // The server loop
        while (true) {
            // Wait for a client to connect
            Socket client = server.accept();

            // Create a new thread to process the client
            new HttpServer(client, wwwRootPath).start();
        }
    }

    // Processes a single http request
    private void processRequest() {
        try {
            System.out.println("Client connected IP:" + _client.getInetAddress() + " port:" + _client.getPort());

            // Get the input/output streams from the client socket.
            _request = new BufferedReader(new InputStreamReader(_client.getInputStream()));
            _response = _client.getOutputStream();

            // The first line has the request line
            String reqLine = _request.readLine();
            System.out.println(reqLine);

            // For example, this line might look like this: GET /index.html HTTP/1.1
            String[] parts = reqLine.split(" ");
            String method = parts[0];       // Http method, GET POST etc. 
            String fileName = parts[1];     // The resource path  

            // GET
            if (method.equals(HTTP_METHOD_GET)) {
                doGet(fileName);
            } 
            
            // POST
            else if (method.equals(HTTP_METHOD_POST)) {
                // Find out the content length from Http headers
                Hashtable<String, String> httpHeaders = readHttpRequestHeader();
                int contentLength = Integer.parseInt(httpHeaders.get(HTTP_HEADER_CONTENT_LENGTH));
                doPost(fileName, contentLength);
            } 

            // We don't support other http methods            
            else {
                sendStatus(HTTP_STATUS_405);    // 404 Method not allowed   
            }

            // We are done, so close the socket
            _client.close();
        } 
        catch (Exception e) {
            // Something bad happend!
            System.out.println("Error processing client request: " + e);
        }
    }

    // Handles the GET request
    private void doGet(String fileName) throws IOException {
        // If no file was specified, send the default document, which is index.html 
        String filePath = _rootPath + "/" + (fileName.equals("/") ? DEFAULT_DOCUMENT : fileName);
        System.out.println(filePath);

        if (new File(filePath).isFile()) {  // If the file exist on the wwww root folder
            sendStatus(HTTP_STATUS_200);    // 200 OK
            sendFile(filePath);             // Send the file content
        } 
        else {
            sendStatus(HTTP_STATUS_404);    // 404 File not found
        }
    }

    // Handles the POST request
    private void doPost(String fileName, int contentLength) throws IOException {
        // We will use the www root folder as the file upload folder
        // Construct the full path the target file 
        String filePath = _rootPath + "/" + fileName;    

        if (new File(filePath).exists()) {  // If the file already exist on the server's www root folder
            sendStatus(HTTP_STATUS_409);    // 409 Conflict
        } 
        else {
            // Read from request stream until Content-Lenght # of bytes has been read
            String requestBody = readRequestBody(contentLength);

            // Save the file on disk
            createFile(filePath, requestBody);

            // Send a success status code
            sendStatus(HTTP_STATUS_201);    // 201 Created
        }
    }

    // Send Http status code to client
    private void sendStatus(String code) throws IOException {
        String response = HTTP_VERSION + " " + code + "\r\n\r\n";
        _response.write(response.getBytes("UTF-8"));
    }

    // Send the content of a file in Http response body to client 
    private void sendFile(String filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        _response.write(fileContent);
    }

    // Save file send by client via POST in a disk file
    private void createFile(String filePath, String fileContent) throws IOException {
        PrintWriter writer = new PrintWriter(filePath, "UTF-8");
        writer.write(fileContent);
        writer.close();
    }

    // Read the Header section of an Http request
    private Hashtable<String, String> readHttpRequestHeader() throws  IOException {
        Hashtable<String, String> httpHeaders = new Hashtable<String, String>();

        String line = _request.readLine();  // Each header is in a single line
        while (!line.isEmpty()) {           // When there is a black line, header section ends
            String[] parts = line.split(":");
            
            String key = parts[0];
            String value = parts[1].trim();
            httpHeaders.put(key, value);
            
            line = _request.readLine();
        } 

        return httpHeaders;
    }

    // Read the Body section of an Http request
    //      Http request streams does not have an EOF, so server can not just read until EOF
    //      Http client needs to specifically mension how much data to expect in the request body
    //      That is specified in Content-Length header
    private String readRequestBody(int contentLength) throws IOException {
         
        char[] buffer = new char[contentLength];
        _request.read(buffer, 0, buffer.length);

        return new String(buffer);
    }

    // Take a port number from command line if one was specified
    private static int getPort(String args[]) {
        // The default port
        int port = DEFAULT_PORT;    

        if (args.length >= 1) {
            try {
                // If user specifed a port, use it
                port = Integer.parseInt(args[0]);
            } 
            catch (NumberFormatException e) { 
                System.out.println("Invalid port specified: " + args[0]);
                System.out.println("Falling back to default port.");
            }  
        }

        return port;
    }

    // Get the wwww root folder
    private static String getWWWRootPath() {
        // We will use the folder where the class file lives as the wwww root
        URL main = HttpServer.class.getResource("HttpServer.class");
        return new File(main.getFile()).getParentFile().getPath();
    }    
}