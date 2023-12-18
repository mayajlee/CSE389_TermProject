import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.*;
    
public class RequestProcessor implements Runnable {
  
  private final static Logger logger = Logger.getLogger(
      RequestProcessor.class.getCanonicalName());

  private File rootDirectory;
  private String indexFileName = "index.html";
  private Socket connection;
  private boolean unauthenticatedRequest = false;
  private Writer out = null;

  public RequestProcessor(File rootDirectory, 
      String indexFileName, Socket connection) {
        
    if (rootDirectory.isFile()) {
      throw new IllegalArgumentException(
          "rootDirectory must be a directory, not a file");   
    }
    try {
      rootDirectory = rootDirectory.getCanonicalFile();
    } catch (IOException ex) {
    }
    this.rootDirectory = rootDirectory;

    if (indexFileName != null) this.indexFileName = indexFileName;
    this.connection = connection;
  }
  
  @Override
  public void run() {
    // for security checks
    String root = rootDirectory.getPath();
    try {              
      OutputStream raw = new BufferedOutputStream(
                          connection.getOutputStream()
                         );
      out = new OutputStreamWriter(raw);
      Reader in = new InputStreamReader(
                   new BufferedInputStream(
                    connection.getInputStream()
                   ),"US-ASCII"
                  );
      StringBuilder requestLine = new StringBuilder();


      while (true) {
        int c = in.read();
        if (c == '\r' || c == '\n') break;
        requestLine.append((char) c);
      }

      String get = requestLine.toString();
      System.out.println("request line: "+ requestLine);


      logger.info(connection.getRemoteSocketAddress() + " " + get);
      
      String[] tokens = get.split("\\s+");
      String method = tokens[0];
      String version = "";
      if (method.equals("GET")) {
    	  String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType = 
            URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        logger.info("Checking if authetication/authorization is needed");

        if(authRequired(fileName)){
          String authHeader = getCredentials(fileName);
          if(authHeader == null || !authenticate(authHeader, fileName)){
            logger.info("Prompt user to log in and send header");
            sendAuthRejectResponse(out);
            return;
          }
        }
          int queryIndex = fileName.indexOf('?'); // Find the index of the query parameters
          File theFile;
          if (queryIndex != -1) {
            theFile = new File(rootDirectory, fileName.substring(1, queryIndex));
          } else {
            theFile = new File(rootDirectory, fileName.substring(1, fileName.length()));
          }

          if (theFile.canRead()
                  && theFile.getCanonicalPath().startsWith(root)) {
            byte[] theData = Files.readAllBytes(theFile.toPath());
            if (version.startsWith("HTTP/")) { // send a MIME header
              sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", theData.length);
            }

            // send the file; it may be an image or other binary data
            // so use the underlying output stream
            // instead of the writer
            raw.write(theData);
            raw.flush();
          } else { // can't find the file
            String body = new StringBuilder("<HTML>\r\n")
                    .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
                    .append("</HEAD>\r\n")
                    .append("<BODY>")
                    .append("<H1>HTTP Error 404: File Not Found</H1>\r\n")
                    .append("</BODY></HTML>\r\n").toString();
            if (version.startsWith("HTTP/")) { // send a MIME header
              sendHeader(out, "HTTP/1.0 404 File Not Found",
                      "text/html; charset=utf-8", body.length());
            }
            out.write(body);
            out.flush();
        }
      } else if (method.equals("HEAD")) {
    	  String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
            URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        int queryIndex = fileName.indexOf('?'); // Find the index of the query parameters
        File theFile;
        if (queryIndex != -1) {
            theFile = new File(rootDirectory, fileName.substring(1, queryIndex));
        } else {
            theFile = new File(rootDirectory, fileName.substring(1, fileName.length()));
        }

        if (theFile.canRead()
            && theFile.getCanonicalPath().startsWith(root)) {
           byte[] theData = Files.readAllBytes(theFile.toPath());
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", theData.length);
          }
          raw.flush();
        } else { // can't find the file
        	String body = new StringBuilder("<HTML>\r\n")
              .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
              .append("</HEAD>\r\n")
              .append("<BODY>")
              .append("<H1>HTTP Error 404: File Not Found</H1>\r\n")
              .append("</BODY></HTML>\r\n").toString();
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found",
                "text/html; charset=utf-8", body.length());
          }
            out.write(body);
            out.flush();
          }
        } else if (method.equals("POST")) {
          StringBuilder requestBody = new StringBuilder();
          int contentLength = 0;
      
          // Read the headers to find the content length
          while (true) {
              String line = ReadLine(in);
              if (line == null || line.isEmpty()) {
                break;
              }
      
              if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
              }
          }
      
          // Read the POST data from the request body
          char[] postData = new char[contentLength];
          in.read(postData, 0, contentLength);
          requestBody.append(postData);
      
          // Parse the POST data to extract form parameters
          Map<String, String> formData = parseFormData(requestBody.toString());
      
          // Store the submitted data through the for in formData 
          String nameInputValue = formData.get("nameInput");
          String emailInputValue = formData.get("emailInput");
          String dataInputValue = formData.get("dobInput");
          String phoneInputValue = formData.get("phoneInput");
      
          // log the form data
          logger.info(String.format("Received POST request. Name: %s, Email Address: %s, Date of Birth: %s, Phone Number: %s",
          nameInputValue, emailInputValue, dataInputValue, phoneInputValue));

      
          // Respond to the browser with the message
          String response = String.format(
            "<html><body><h1>POST Request Received</h1><p>Name: %s</p><p>Email Address: %s</p><p>Date of Birth: %s</p><p>Phone Number: %s</p></body></html>",
            nameInputValue, emailInputValue, dataInputValue, phoneInputValue);
        
          sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", response.length());
          out.write(response);
          out.flush();

        } else { // method does not equal "GET", "HEAD" or "POST"
          String body = new StringBuilder("<HTML>\r\n")
            .append("<HEAD><TITLE>Not Implemented</TITLE>\r\n")
            .append("</HEAD>\r\n")
            .append("<BODY>")
            .append("<H1>HTTP Error 501: Not Implemented</H1>\r\n")
            .append("</BODY></HTML>\r\n").toString();
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 501 Not Implemented",
                    "text/html; charset=utf-8", body.length());
          }
          out.write(body);
          out.flush();
        }
      } catch (IOException ex) {
        logger.log(Level.WARNING, 
            "Error talking to " + connection.getRemoteSocketAddress(), ex);
      } finally {
        try {
          if(out != null){
            if (unauthenticatedRequest) {
              sendUnauthorizedResponse(out);
              return;
            }
          }
          connection.close();        
        }
        catch (IOException ex) {} 
      }
    }

  private void sendHeader(Writer out, String responseCode,
      String contentType, int length)
      throws IOException {
    out.write(responseCode + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + length + "\r\n");
    out.write("Content-type: " + contentType + "\r\n\r\n");
    out.flush();
  }

  private String ReadLine(Reader reader) throws IOException {
    StringBuilder line = new StringBuilder();
    int c;
    while ((c = reader.read()) != -1) {
        if (c == '\r') {
            reader.read(); // Consume the '\n' character
            break;
        }
        line.append((char) c);
    }
    return line.toString();
  }

  private Map<String, String> parseFormData(String formData) {
    Map<String, String> result = new HashMap<>();
    String[] pairs = formData.split("&");
    for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
            String key = keyValue[0];
            String value = keyValue[1];
            try {
                key = URLDecoder.decode(key.replace('+', ' '), "UTF-8");
                value = URLDecoder.decode(value.replace('+', ' '), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Handle the exception as needed
            }
            result.put(key, value);
        }
    }
    return result;
  }

  //Auth Code
  //checks whether get request can be filled without authorization given a filename
  private boolean authRequired(String fileName){
    return fileName.startsWith("/secret") || fileName.startsWith("/topsecret") || fileName.startsWith("/general");
  }

  //gets non encoded credentials from the get requested
  private String getCredentials(String request){
    int queryIndex = request.indexOf("?");
    if (queryIndex != -1 || queryIndex == request.length()-1){
      String query = request.substring(queryIndex);
      String[] cred = query.split("&");
      String creds = cred[0].split("=")[1] + ":" + cred[1].split("=")[1];
      System.out.println("creds:" + creds);
      return creds;
    }
    return null;
  }

  //retrieve auth header which contains encoded username and password
  private String getAuthHeader(String request){
    String[] lines = request.split("\r\n");
    for (String line : lines) {
      if (line.startsWith("Authorization:")) {
        return line.substring("Authorization:".length()).trim();
      }
    }
    return null;
  }
  public boolean authenticate(String authHeader, String fileName){
    logger.info("Authenticating user using encoded user credentials");
    System.out.println("auth: " + authHeader);
    System.out.println("file: " + fileName);
    //list of accepted user names in passwords
    HashMap<String, String> users = new HashMap<String, String>();
    users.put("user", "password");
    users.put("sara123", "abc123");
    users.put("exUser", "exPassword");
    users.put("maya", "lee");
    users.put("authTest", "authPass");
    users.put("getTest", "getPass");

    //list of ranks and users
    List<String> topSecretUsers = Arrays.asList("exUser", "getTest");
    //symbolizes the auth relationship in presentation: User can see all auth levels below
    List<String> secretUsers = Arrays.asList("sara123", "maya", "users", "getTest", "exUser");

    if (authHeader != null) {
      try {
        //Decode auth header to retrieve username and password
        //String encodedCredentials = authHeader.substring("Basic ".length()).trim();
        //String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);
        String[] credentials = authHeader.split(":");
        //credentials[0] = username credentials[1]=password
        System.out.println(credentials[0]);
        System.out.println(credentials[1]);
        if(users.containsKey(credentials[0]) && users.get(credentials[0]).equals(credentials[1])){
          logger.info("Authentication passed now checking authorization");
          if (fileName.startsWith("/general")){
            logger.info("General Success");
            return true;
          } else if (fileName.startsWith("/secret") && secretUsers.contains(credentials[0])){
            logger.info("Secret Success");
            return true;
          } else if (fileName.startsWith("/topsecret") && topSecretUsers.contains(credentials[0])) {
            logger.info("Top Secret Success");
            return true;
          }
          return false;
        }

      } catch (Exception e) {
        // Handle decoding or other exceptions as needed
        logger.info("Error with decoding");
        return false;
      }
    }
    logger.info("Authentication failed");
    return false;
  }

  //prompts user to enter their username and password.
  private void sendUnauthorizedResponse(Writer out) throws IOException{
    out.write("HTTP/1.1 401 Unauthorized" + "\r\n");
    out.write("WWW-Authenticate: Basic realm=\"Password Realm\"\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + 100 + "\r\n");
    out.write("Content-type: text/html\r\n\r\n");
    out.flush();
  }

  private void sendAuthRejectResponse(Writer out) throws IOException{
    out.write("HTTP/1.1 403 Forbidden" + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + 100 + "\r\n");
    out.write("Content-type: text/html\r\n\r\n");
    out.flush();
  }
}