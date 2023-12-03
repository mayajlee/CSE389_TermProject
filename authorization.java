
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
