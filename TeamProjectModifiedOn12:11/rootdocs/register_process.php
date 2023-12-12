<?php
// Dummy user data for demonstration purposes
$dummyUserData = array();

// Retrieve user input from the registration form
$name = isset($_POST['name']) ? $_POST['name'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

// Validate and store user data (insecure, for demonstration purposes)
if (!empty($name) && !empty($password)) {
    // In a real scenario, you would hash the password before storing it in a database
    $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

    // Store user data (for demonstration purposes, data is stored in an array)
    $dummyUserData[] = array('name' => $name, 'password' => $hashedPassword);

    // Redirect to a success page or perform other actions
    header('Location: registration_success.php');
    exit();
} else {
    // Invalid input - redirect back to the registration page with an error message
    header('Location: register.html?registration_error=true');
    exit();
}
?>
