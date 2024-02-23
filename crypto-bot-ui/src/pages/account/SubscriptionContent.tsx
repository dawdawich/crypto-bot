import React from "react";
import {styled} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import {useLocation} from "wouter";

const PreviousPath = styled('div')({
    font: plexFont,
    color: "#868F9C",
    fontSize: 20,
    fontWeight: '100',
    paddingTop: '24px',
    paddingLeft: '16px',
    cursor: 'pointer'
});

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '700',
    paddingTop: '24px',
    paddingLeft: '4px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const SubscriptionContent: React.FC = () => {
    const [, navigate] = useLocation();

    return (
        <div className="account-api-token-content">
            <div className="account-api-token-header">
                <div className="account-api-token-header-path">
                    <PreviousPath onClick={() => navigate("/account")}>
                        Account /
                    </PreviousPath>
                    <CurrentPath>Subscription</CurrentPath>
                </div>
                <div></div>
            </div>
        </div>
    );
}

export default SubscriptionContent;
