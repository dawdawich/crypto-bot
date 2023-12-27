import React, { useState } from "react";
import '../../css/LoginPage.css';
import {fetchAuthToken} from "../../service/AccountService";
import {useLocation} from "wouter";

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
        <div className='login-container'>
            <form className='form-container'>
                <label htmlFor="email">Email:</label>
                <input type="email" id="email" value={email} onChange={event => setEmail(event.target.value)}/>
                <label htmlFor="password">Password:</label>
                <input type="password" id="password" value={password}
                       onChange={event => setPassword(event.target.value)}/>
                <button type='button' disabled={!allFieldsValidated()} onClick={login}>Login</button>
            </form>
        </div>
    );
}

export default LoginPage;
