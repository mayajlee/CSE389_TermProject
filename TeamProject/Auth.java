package TeamProject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.FileReader;
import java.io.IOException;


public class Auth {
    //read auth file
    public JSONArray users;

    public Auth(String authPath){
        Object parserObject = null;
        try {
            parserObject = new JSONParser().parse(new FileReader(authPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        //object to parse file
        JSONObject authObject =  (JSONObject) parserObject;

        // getting users
        users = (JSONArray) authObject.get("users");
    }

    public String authenticate(String userinput, String passinput) {
        for (Object user : users) {
            JSONObject jsonUser = (JSONObject) user;
            String username = (String) jsonUser.get("username");
            String password = (String) jsonUser.get("password");

            if(username.equals(userinput) && password.equals(passinput)){
                return (String) jsonUser.get("authLvl");
            }
        }
        return "done";
    }

    public void authorizationCheck (String user, String password, String docRequest) {
        String authLvl;
        String docName = docRequest;

        // match User & password
        verifyaccount(user, password);
        String authLvl = account["authLvl"];

        // Check if document exists first
        find(docName);
        String docClass = document["classification"];



        // check if authorization allows them to access it
        boolean clearance = authorizationCheck(userAuth, docClass);

        if (clearance) {
            // fulfill request, try to get file and send to user
        }
        else {
            // send rejection header for "Not Authorized for request"
        }

    }

    public boolean authorizationCheck(String userAuth, String docClass) {
        switch (userAuth) {
            case "TopSecret":
                if (docClass.equals("TopSecret")) return true;
            case "Secret":
                if (docClass.equals("Secret")) return true;
            case "General":
                return true;
            break;
            default:
                return false;
        }
    }
    //test and example of how to use -- NEED TO SOLVE JSON PROBLEM
    public static void main(String[] args) {
        Auth au = null;
        au = new Auth("src/main/java/test.json");
        System.out.println(au.authenticate("sara123", "abc123"));
    }
}

