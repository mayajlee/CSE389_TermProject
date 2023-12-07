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
      Writer out = new OutputStreamWriter(raw);
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

        int queryIndex = fileName.indexOf('?'); // Find the index of the query parameters
        File theFile;
        if (queryIndex != -1) {
            theFile = new File(rootDirectory, fileName.substring(1, queryIndex));
        } else {
            theFile = new File(rootDirectory, fileName.substring(1, fileName.length()));
        } 
                  
        if (theFile.canRead() 
            // Don't let clients outside the document root
            && theFile.getCanonicalPath().startsWith(root)) {
          byte[] theData = Files.readAllBytes(theFile.toPath());
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", contentType, theData.length);
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
            sendHeader(out, "HTTP/1.0 200 OK", contentType, theData.length);
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
              String line = readLine(in);
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
          String emalInputValue = formData.get("emailInput");
          String dataInputValue = formData.get("dobInput");
          String phoneInputValue = formData.get("phoneInput");
      
          // log the form data
          logger.info(String.format("Received POST request. Name: %s, Email Address: %s, Date of Birth: %s, Phone Number: %s",
          nameInputValue, emalInputValue, dataInputValue, phoneInputValue));

      
          // Respond to the browser with the message
          String response = String.format(
            "<html><body><h1>POST Request Received</h1><p>Name: %s</p><p>Email Address: %s</p><p>Date of Birth: %s</p><p>Phone Number: %s</p></body></html>",
            nameInputValue, emalInputValue, dataInputValue, phoneInputValue);
        
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

  private String readLine(Reader reader) throws IOException {
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

}