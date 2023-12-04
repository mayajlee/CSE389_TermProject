import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.util.Dictionary;
import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class Auth {
    //read auth file
    public JSONArray users;
    public JSONArray authLvls;
    public Auth(String authPath) {
        try {
            JSONParser authInfo = new JSONParser(new FileReader(authPath));

            //store list of usernames and passwords
            JSONArray users = (JSONArray)jsonObject.get("users");
            JSONArray authLvls = (JSONArray)jsonObject.get("documents");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String authenticate(String userinput, String passinput){
        //plan:
        for(int i = 0; i < users.length; i++) {
            if (users[i].get("username").equals(userinput)) {
                if (users[i].get("password").equals(passinput)) {
                    return users[i].get("authLvl");
                }
                else{
                    return "Fail"; //call for false header here
                }
            }
        }
        return "Fail";
    }

    //authorization.java stuff would go here

   //test and example of how to use -- NEED TO SOLVE JSON PROBLEM
    public static void main(String[] args){
        Auth au = new Auth("/authorization.json");
        System.out.println(au.authenticate("sara123", "abc123"));
    }
