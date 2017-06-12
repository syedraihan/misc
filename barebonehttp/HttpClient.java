package barebonehttp;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * An Http client running a pared down version of HTTP/1.1
 *
 * Created by me@syedraihan.com
 */
public class HttpClient {

    // Http protocol elements used in this implementation
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

    private String _host;           // The Server's IP address
    private int _port;              // The Server's port  
    private String _httpMethod;     // Requested http method (GET or POST)
    private String _fileName;       // The remote file to be downloaded (via GET) or
                                    // the local file to be uploaded (via POST)   

    private BufferedWriter _request;    // The request stream
    private BufferedReader _response;   // The response stream

    // The constructor
    public HttpClient(String host, int port, String httpMethod, String fileName) {
        _host = host;
        _port = port;
        _httpMethod = httpMethod;
        _fileName = fileName;
    }

    // The program entry point 
    public static void main(String[] args) {
        try {
            // try to get the required parameters from command line
            // instantiate the HttpClient object
            // and run it
            new HttpClient(
                args[0],                // host name
                new Integer(args[1]),   // port number
                args[2],                // http method, GET/POST
                args[3]                 // file name to be requested or sent
            ).run();
        } catch (Exception e) {
            // Something went wrong, most likely invalid commandline
            // Let's help the user on how to use the tool
            showHelp();
        }
    }

    // Effective, this is the main method
    public void run() throws IOException {
        Socket socket = null;

        try {
            // Create a sock connection to user specified destination host/port
            socket = new Socket(_host, _port);

            // Get the request and response stream
            _request = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            _response = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Based on user specified http method (GET/POST)
            // Call aproproate methods
            if      (_httpMethod.equalsIgnoreCase(HTTP_METHOD_GET))  doGet();   // make a GET request       
            else if (_httpMethod.equalsIgnoreCase(HTTP_METHOD_POST)) doPost();  // make a POST request
            else 
                // At this moment, we do not support other http methods
                throw new Exception("Http method " + _httpMethod + " not supported");

            // Clean-up
            _request.close();
            _response.close();
        }
        catch (Exception e) {
            // Something went wrong, let the user know about the error
            System.out.println("Error: " + e);
        } 
    }

    // Make a GET request
    private void doGet() throws IOException {
        sendRequestLine();          // Send the request line: i.e., GET HTTP/1.1 index.html
        _request.write("\r\n");     // then a blank line
        _request.flush();           // make sure bits are really sent and not in the buffer
        displayResponse();          // Now read what server resonds with and display in the console
    }

    // Make a POST request
    private void doPost() throws IOException {
        // Construct the absolute path from the specific relative path
        // We will assume the folder where the .class resides is the root folder 
        String filePath = getCurrentFolderPath() + "\\" + _fileName;

        // Read the content of the file in a string
        String fileContent = String.join("\n", Files.readAllLines(Paths.get(filePath)));

        sendRequestLine();          // Send the request line: i.e., POST HTTP/1.1 data.txt

        // Set the Content-Length header
        _request.write(HTTP_HEADER_CONTENT_LENGTH + ": " + (fileContent.length() - 1) + "\r\n");

        _request.write("\r\n");         // A blank line indicates end of header section
        _request.write(fileContent);    // Now send the file content in a request body
        _request.flush();               // make sure all the bit are actually sent 

        displayResponse();              // Now read what server resonds with and display in the console
    }

    // Send the request line to the server
    private void sendRequestLine() throws IOException {
        _request.write(_httpMethod + " " + _fileName + " " + HTTP_VERSION + "\r\n");
    }

    // Read what server resonds with and display in the console
    private void displayResponse() throws IOException {
        String line;
        while((line = _response.readLine()) != null)
            println(line);
    }

    // Determine which folder the current tool is running from
    private String getCurrentFolderPath() {
        URL main = HttpClient.class.getResource("HttpClient.class");
        return new File(main.getFile()).getParentFile().getPath();
    }    

    // display help on how to use the tool
    private static void showHelp() {
        println("Invalid command line argument!");
        println("");
        println("Usage: HttpClient [host] [port] [method] [filename]");
        println("");
        println("Options:");
        println("\thost\t\tIP address of the host");
        println("\tport\t\tPort number of the host");
        println("\tmethod\t\tHttp method (GET/POST)");
        println("\tfilename\tFile name to be requested or to sent.");

        System.exit(1);
    }

    // just an abstraction!
    private static void println(String msg) {
        System.out.println(msg);
    }
}