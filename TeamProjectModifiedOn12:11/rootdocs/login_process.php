<?php
// Dummy user data for demonstration purposes
$users_json = file_get_contents('authorization.json');

// Decode the JSON file
$json_data = json_decode($users_json,true);

$users = $json_data['users'];
$dummyPassword = 'password123';

// Retrieve user input from the login form
$username = isset($_POST['username']) ? $_POST['username'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

$authLvl = 'invalid'

foreach($users as $user){
    if ($username == $user["username"] && $password == $user['password']){
        header('Location: welcome.php?authLvl='.$user['authLvl']);
        exit();
    }
}

header('Location: index.html?login_error=true');
exit();
}
?>
