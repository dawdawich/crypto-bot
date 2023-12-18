import React, {useState} from "react";
import '../../css/LoginPage.css';
import {createAccount} from "../../service/AccountService";
import {useLocation} from "wouter";
import {toast, ToastContainer} from "react-toastify";

const RegistrationPage: React.FC = () => {
    const [username, setUsername] = useState("")
    const [name, setName] = useState("")
    const [surname, setSurname] = useState("")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [, navigate] = useLocation();

    const allFieldsValidated = () => {
        return username && name && surname && email && password && confirmPassword && arePasswordsIdentical();
    };

    const arePasswordsIdentical = () => {
        return password === confirmPassword
    };

    const tryToCreateAccount = () => {
        createAccount(username, name, surname, email, password).then(() => navigate('/login')).catch(() => toast('Failed to create account'));
    };

    return (
        <div className='login-container'>
            <form className='form-container'>
                <label htmlFor="username">Username:</label>
                <input type="text" id="username" value={username} onChange={event => setUsername(event.target.value)}/>
                <label htmlFor="name">Name:</label>
                <input type="text" id="name" value={name} onChange={event => setName(event.target.value)}/>
                <label htmlFor="surname">Surname:</label>
                <input type="text" id="surname" value={surname} onChange={event => setSurname(event.target.value)}/>
                <label htmlFor="email">Email:</label>
                <input type="email" id="email" value={email} onChange={event => setEmail(event.target.value)}/>
                <label htmlFor="password">Password:</label>
                <input type="password" id="password" value={password}
                       onChange={event => setPassword(event.target.value)}/>
                <label htmlFor="confirmPassword">Confirm Password:</label>
                <input type="password" id="confirmPassword" value={confirmPassword}
                       onChange={event => setConfirmPassword(event.target.value)}/>
                <button type='submit' disabled={!allFieldsValidated()} onClick={tryToCreateAccount}>Let's go</button>
            </form>
            <ToastContainer/>
        </div>
    );
}

export default RegistrationPage;
