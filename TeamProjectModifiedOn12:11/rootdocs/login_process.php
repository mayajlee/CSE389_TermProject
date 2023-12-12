<?php
// Dummy user data for demonstration purposes
$dummyUsername = 'user123';
$dummyPassword = 'password123';

// Retrieve user input from the login form
$username = isset($_POST['username']) ? $_POST['username'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

// Check if the provided credentials match the dummy data
if ($username === $dummyUsername && $password === $dummyPassword) {
    // Successful login - redirect to a welcome page or perform other actions
    header('Location: welcome.php');
    exit();
} else {
    // Failed login - redirect back to the login page with an error message
    header('Location: index.html?login_error=true');
    exit();
}
?>
