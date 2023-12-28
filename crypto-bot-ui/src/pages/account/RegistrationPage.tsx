import React, {useState} from "react";
import '../../css/LoginPage.css';
import {createAccount} from "../../service/AccountService";
import {useLocation} from "wouter";
import {Button, TextField} from "@mui/material";

const RegistrationPage: React.FC = () => {
    const [username, setUsername] = useState("")
    const [name, setName] = useState("")
    const [surname, setSurname] = useState("")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [, navigate] = useLocation();
    const [error, setError] = useState()

    const allFieldsValidated = () => {
        return username && name && surname && email && password && confirmPassword && arePasswordsIdentical();
    };

    const arePasswordsIdentical = () => {
        return password === confirmPassword
    };

    const tryToCreateAccount = () => {
        createAccount(username, name, surname, email, password).then(() => {
            navigate('/login');
        })
            .catch((error) => setError(error));
    };

    if (!!error) {
        return (
            <div>
                <h1>Error occurred</h1>
                <p>{error}</p>
            </div>
        );
    }

    return (
        <div className='login-container'>
            <form className="form-container">
                <TextField
                    id="username"
                    label="Username"
                    type="text"
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                />

                <TextField
                    id="name"
                    label="Name"
                    type="text"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                />

                <TextField
                    id="surname"
                    label="Surname"
                    type="text"
                    value={surname}
                    onChange={(event) => setSurname(event.target.value)}
                />

                <TextField
                    id="email"
                    label="Email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                />

                <TextField
                    id="password"
                    label="Password"
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                />

                <TextField
                    id="confirmPassword"
                    label="Confirm Password"
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                />

                <Button
                    variant="contained"
                    color="primary"
                    disabled={!allFieldsValidated()}
                    onClick={() => tryToCreateAccount()}
                >
                    Let's go
                </Button>
            </form>
        </div>
    );
}

export default RegistrationPage;
