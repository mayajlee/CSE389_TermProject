import java.io.*;
import java.net.*;
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
          // byte[] theData = Files.readAllBytes(theFile.toPath());
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", contentType, 0);
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
          String fileName = tokens[1];
          if (fileName.endsWith("/")) fileName += indexFileName;
          String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
          if (tokens.length > 2) {
              version = tokens[2];
          }
      
          int contentLength = 0;
          String contentLengthHeader = null;

          while (true) {
              String line = ((BufferedReader) in).readLine();
              if (line == null || line.trim().isEmpty()) {
                  break;
              }
              if (line.startsWith("Content-Length:")) {
                  contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
              }
          }
          
          if (contentLengthHeader != null) {
            contentLength = Integer.parseInt(contentLengthHeader.split(":")[1].trim());
          }

          // Read the POST data
            StringBuilder postData = new StringBuilder();
            for (int i = 0; i < contentLength; i++) {
                int c = in.read();
                postData.append((char) c);
            }

            // Process the POST data as needed
            System.out.println("POST Data: " + postData.toString());
      
          // Send an HTTP response indicating successful processing
          String responseBody = "<HTML>\r\n"
                  + "<HEAD><TITLE>POST Data Processed</TITLE>\r\n"
                  + "</HEAD>\r\n"
                  + "<BODY>"
                  + "<H1>HTTP POST Data Processed Successfully</H1>\r\n"
                  + "<P>Posted Data: " + postData.toString() + "</P>\r\n"
                  + "</BODY></HTML>\r\n";
      
          if (version.startsWith("HTTP/")) {
              sendHeader(out, "HTTP/1.0 200 OK", contentType, responseBody.length());
          }
      
          out.write(responseBody);
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
}