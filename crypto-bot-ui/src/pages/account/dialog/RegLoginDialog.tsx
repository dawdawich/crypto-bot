import React, {useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField} from "@mui/material";
import {createAccount, fetchAuthToken} from "../../../service/AccountService";
import {ToastContainer, toast} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {errorToast} from "../../toast/Toasts";

interface RegLoginDialogProps {
    open: boolean;
    isRegistration: boolean;
    onClose: () => void;
    setRegistrationDialog: (value: boolean) => void;
}

const RegLoginDialog: React.FC<RegLoginDialogProps> = ({open, isRegistration, onClose, setRegistrationDialog}) => {
    const [username, setUsername] = useState("");
    const [name, setName] = useState("");
    const [surname, setSurname] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState<Error>();

    const allFieldsValidated = () => {
        return isRegistration ?
            (username.length > 0 && name.length > 0 && surname.length > 0 && email.length > 0 && password.length > 0 && confirmPassword.length > 0 && arePasswordsIdentical())
            : email.length > 0 && password.length > 0;
    };

    const arePasswordsIdentical = () => {
        return password === confirmPassword
    };

    const tryToCreateAccount = () => {
        createAccount(username, name, surname, email, password)
            .then(() => setRegistrationDialog(false))
            .catch(errorMsg => errorToast(errorMsg))
    };

    const login = () => {
        fetchAuthToken(email, password)
            .then((token) => {
                const role = JSON.parse(atob(token.split('.')[1])).role
                localStorage.setItem('auth.token', token);
                localStorage.setItem('auth.role', role);
                onClose();
            })
            .catch(errorMsg => errorToast(errorMsg))
    };

    if (!!error) {
        return (
            <div>
                <h1>Error occurred</h1>
                <p>{error.message}</p>
            </div>
        );
    }

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title">{isRegistration ? 'Registration' : 'Login'}</DialogTitle>
            <DialogContent style={{display: 'flex', flexDirection: 'column', gap: '10px', padding: '10px'}}>
                {
                    isRegistration &&
                    <TextField
                        id="username"
                        label="Username"
                        type="text"
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                    />
                }

                {
                    isRegistration &&
                    <TextField
                        id="name"
                        label="Name"
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                    />
                }

                {
                    isRegistration &&
                    <TextField
                        id="surname"
                        label="Surname"
                        type="text"
                        value={surname}
                        onChange={(event) => setSurname(event.target.value)}
                    />
                }

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

                {
                    isRegistration &&
                    <TextField
                        id="confirmPassword"
                        label="Confirm Password"
                        type="password"
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                    />
                }
            </DialogContent>
            <DialogActions>
                <Button variant='contained' onClick={onClose} color="error">
                    Cancel
                </Button>
                <Button variant='contained' disabled={!allFieldsValidated()}
                        onClick={isRegistration ? tryToCreateAccount : login} color="primary">
                    {isRegistration ? 'Sign Up' : 'Login'}
                    <ToastContainer/>
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default RegLoginDialog;
