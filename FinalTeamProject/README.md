CSE 389 Final Project - Option 1

For compiling: 
    1. go to the folder that contains JHTTP.java and RequestProcessor.java
    2. run "javac JHTTP.java"
    3. run "javac RequestProcessor.java"

For starting and visiting server:
    1. Make sure two java files are compiled
    2. run "java JHTTP ./rootdocs"
    3. Open a browser (preferably Microsoft Edge or Safari)
    4. Type 127.0.0.1 in the address bar and hit enter
    5. The index page should come up

For testing
    1. There are six functions for testing on the index page: GET, HEAD, POST, general, secret, topsecret
    2. Click on each function to jump to the test page for each function
    3. For authentication and authorization, here are some preset accounts and the level of secret they can access (general < secret < topsecret>)

        Username     Password     Access Level
        "user"       "password"   topsecret
        "sara123"    "abc123"     secret
        "exUser"     "exPassword" general
        "maya"       "lee"        secret
        "authTest"   "authPass"   general
        "getTest"    "getPass"    topsecret

Credits: We use JHTTP.java and RequestProcessor.java from Java Network Programming, 4th Edition as the base of our code