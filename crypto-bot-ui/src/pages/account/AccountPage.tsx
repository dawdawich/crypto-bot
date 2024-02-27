import React from "react";
import '../../css/pages/account/AccountPageStyles.css';
import {ReactComponent as MetamaskIcon} from '../../assets/images/account/metamask-icon.svg';
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import {Button, Divider, styled, Typography} from "@mui/material";
import {useAuth} from "../../context/AuthContext";
import {sanitizeMiddle} from "../../utils/string-utils";
import {Route, Switch, useLocation} from "wouter";
import ApiTokenContent from "./ApiTokenContent";
import SubscriptionContent from "./SubscriptionContent";
import {successToast} from "../toast/Toasts";

const hoverStyle = {
    borderRadius: '4px',
    backgroundColor: '#1D2024'
};

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '700',
    paddingTop: '24px',
    marginLeft: '16px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const AccountPage: React.FC = () => {
    const {authInfo, logout} = useAuth();
    const [location, navigate] = useLocation();

    document.title = 'Account';

    if (!authInfo) {
        navigate('/');
    }

    const copyWalletId = () => {
        if (!!authInfo) {
            navigator.clipboard.writeText(authInfo!.address).then(() => {
                successToast("Wallet ID copied to clipboard");
            });
        }
    };

    return (
        <div className="account-page">
            <div className="account-menu-panel">
                <div className="account-menu-panel-tabs">
                    <Typography onClick={() => navigate("/account/subscription")} id="tab"
                                style={location === "/account/subscription" ? hoverStyle : {}}>
                        Subscription
                    </Typography>
                    <Typography onClick={() => navigate("/account/api-token")} id="tab"
                                style={location === "/account/api-token" ? hoverStyle : {}}>
                        Api Token
                    </Typography>
                </div>
                <div className="account-metamask-content">
                    <Divider color="#1D2024"/>
                    <MetamaskIcon style={{marginTop: '12px'}}/>
                    {!!authInfo &&
                        <Typography style={{marginTop: '8px', fontFamily: plexFont}}
                                    color="white">{sanitizeMiddle(authInfo!.address, 22)}
                        </Typography>}
                    <div className="account-metamask-content-buttons">
                        <Button variant="outlined" onClick={copyWalletId}
                                style={{color: '#868F9C', borderColor: '#868F9C', textTransform: 'none'}}>Copy
                            ID</Button>
                        <Button variant="contained" onClick={logout}
                                style={{color: 'white', backgroundColor: '#E7323B', textTransform: 'none'}}>Log
                            Out</Button>
                    </div>
                </div>
            </div>
            <div className="account-content">
                <Switch>
                    <Route path="/account/api-token" component={ApiTokenContent}/>
                    <Route path="/account/subscription" component={SubscriptionContent}/>
                    <Route path="/account">
                        <CurrentPath>Account</CurrentPath>
                    </Route>
                </Switch>
            </div>
        </div>
    );
};

export default AccountPage;
