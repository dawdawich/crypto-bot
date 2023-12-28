import React, { useState } from "react";
import '../../css/LoginPage.css';
import {fetchAuthToken} from "../../service/AccountService";
import {useLocation} from "wouter";
import {Button, TextField} from "@mui/material";

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [, navigate] = useLocation();

    const allFieldsValidated = () => {
        return email && password;
    };

    const login = () => {
        fetchAuthToken(email, password).then((token) => {
            const role = JSON.parse(atob(token.split('.')[1])).role
            localStorage.setItem('auth.token', token);
            localStorage.setItem('auth.role', role)
            navigate('/');
            window.location.reload();
        })
    };

    return (
        <div className="login-container">
            <form className="form-container">
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

                <Button
                    variant="contained"
                    color="primary"
                    disabled={!allFieldsValidated()}
                    onClick={login}
                >
                    Login
                </Button>
            </form>
        </div>
    );
}

export default LoginPage;
