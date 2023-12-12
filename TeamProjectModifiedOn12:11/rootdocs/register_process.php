<?php
// Dummy user data for demonstration purpose

// Retrieve user input from the registration form
$name = isset($_POST['name']) ? $_POST['name'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

// Validate and store user data (insecure, for demonstration purposes)
if (!empty($name) && !empty($password)) {
    $age = array("Peter"=>35, "Ben"=>37, "Joe"=>43);

    echo json_encode($age);

    // Redirect to a success page or perform other actions
    header('Location: registration_success.php');
    exit();
} else {
    // Invalid input - redirect back to the registration page with an error message
    header('Location: register.html?registration_error=true');
    exit();
}
?>
