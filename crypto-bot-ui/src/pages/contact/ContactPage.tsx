import React from "react";
import "../../css/pages/contact/ContactPageStyle.css";
import "../../css/pages/LoginBanner.css";
import { ReactComponent as FullLogo } from "../../assets/images/contact/full-logo.svg";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import {ReactComponent as MetamaskIcon} from "../../assets/images/account/metamask-icon.svg";
import {Button, styled, Typography} from "@mui/material";
import {useAuth} from "../../context/AuthContext";

const Header = styled('div')({
    font: plexFont, color: "white", fontSize: 20, fontWeight: '700', paddingTop: '24px', paddingLeft: '16px'
});

const ContactPage: React.FC = () => {
    const {authInfo, login} = useAuth();

    document.title = 'About Us';

    return (
        <div className="contact-page">
            <Header>
                About App
            </Header>
            <div className="contact-content">
                <FullLogo />
                <Typography style={{marginTop: '32'}} color="#555C68">alpha: 0.0.1</Typography>
                <Typography style={{marginTop: '80'}} color="#555C68">Docs | Support | Discord | Contact Us </Typography>
            </div>

            {
                !authInfo &&
                <div className="login-banner">
                    <div className="login-banner-content">
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            color: 'white',
                            fontSize: '14px',
                            fontWeight: 400
                        }}>
                            <MetamaskIcon style={{width: '36px', height: '34px', marginRight: '16px'}}/>
                            Connect your wallet to unlock full access
                        </div>
                        <Button variant="contained"
                                onClick={() => login()}
                                style={{
                                    backgroundColor: '#D0FF12',
                                    color: '#121417',
                                    textTransform: 'none',
                                    fontWeight: 700
                                }}>Connect Wallet</Button>
                    </div>
                </div>
            }
        </div>
    );
}

export default ContactPage;
