import {useLocation} from "wouter";
import {Button, Divider} from "@mui/material";
import "../css/HeaderStyles.css";
import {useState} from "react";
import RegLoginDialog from "./account/dialog/RegLoginDialog";

const MainHeader = () => {
    const [isRegistrationDialogOpen, setRegistrationDialogOpen] = useState(false)
    const [isRegistrationDialog, setRegistrationDialog] = useState(false)
    const [location, navigate] = useLocation();
    let userData = localStorage.getItem('auth.token');

    let loginPlace: any;

    if (userData == null) {
        loginPlace = (<div><Button color="inherit" onClick={() => openRegLoginDialog(false)}>Login</Button>
            <Button color="inherit" onClick={() => openRegLoginDialog(true)}>Sign Up</Button></div>);
    } else {
        const username = JSON.parse(atob(userData.split('.')[1])).username;
        loginPlace = (<strong>{username}</strong>);
    }

    const role = localStorage.getItem('auth.role')

    const openRegLoginDialog = (registration: boolean) => {
        setRegistrationDialog(registration);
        setRegistrationDialogOpen(true);
    }

    return (
        <div>
            <header className="header">
                <div className="logo-container">
                    <img src="" alt="Logo" className="logo"/>
                </div>
                <nav className="navigation">
                    {
                        userData != null &&
                        <Button color="inherit" onClick={() => navigate("/analyzer")}
                             variant={location === "/analyzer" ? 'contained' : undefined}>Analyzers</Button>
                    }
                    <Button color="inherit" onClick={() => navigate("/top-analyzers")}
                            variant={location === "/top-analyzers" ? 'contained' : undefined}>Public Top
                        Analyzers</Button>
                    {
                        userData != null &&
                        <Button color="inherit" onClick={() => navigate("/manager")}
                             variant={location === "/manager" ? 'contained' : undefined}>Managers</Button>
                    }
                    {
                        userData != null &&
                        <Button color="inherit" onClick={() => navigate("/account")}
                                variant={location === "/account" ? 'contained' : undefined}>Account</Button>
                    }
                    {
                        role === 'ADMIN' &&
                        <Button color="inherit" onClick={() => navigate("/symbols")}
                                variant={location === "/symbols" ? 'contained' : undefined}>Symbols</Button>
                    }
                    {
                        role === 'ADMIN' &&
                        <Button color="inherit" onClick={() => navigate("/monitoring")}
                                variant={location === "/monitoring" ? 'contained' : undefined}>Monitoring</Button>
                    }
                </nav>
                {loginPlace}
            </header>
            <Divider light/>
            <RegLoginDialog open={isRegistrationDialogOpen} isRegistration={isRegistrationDialog} setRegistrationDialog={setRegistrationDialog}
                            onClose={() => setRegistrationDialogOpen(false)}/>
        </div>
    );
}

export default MainHeader;
