import '../css/LeftNavPanelStyles.css'
import React from "react";
import {ReactComponent as Logo} from "../assets/images/nav-panel/joatNavLogo.svg";
import {ReactComponent as NavAnalyzers} from "../assets/images/nav-panel/navAnalyzers.svg";
import {ReactComponent as NavManagers} from "../assets/images/nav-panel/navManagers.svg";
import {ReactComponent as NavAccount} from "../assets/images/nav-panel/navAccount.svg";
import {ReactComponent as NavContact} from "../assets/images/nav-panel/navContact.svg";
import {Divider} from "@mui/material";
import {useAuth} from "../context/AuthContext";
import {useLocation} from "wouter";

const LeftNavPanel: React.FC = () => {
    const {authInfo} = useAuth();
    const [location, navigate] = useLocation();

    const getNavElementClass = (path: string) => {
        if (!authInfo) {
            switch (path) {
                case "/account":
                case "/manager":
                    return "nav-item-disable";
                default:
                    return location.includes(path) ? "nav-item-active" : "nav-item";
            }
        } else {
            return location.includes(path) ? "nav-item-active" : "nav-item";
        }
    };

    return (<div className="left-nav-panel">
        <div>
            <Logo className="logo"/>
            <Divider variant="middle" color="#1D2024"/>
            <div className={getNavElementClass("/analyzer")} onClick={() => navigate("/analyzer")}>
                <NavAnalyzers id="icon"/>
            </div>
            <div className={getNavElementClass("/manager")} onClick={() => {
                if (!!authInfo) {
                    navigate("/manager");
                }
            }}>
                <NavManagers style={{}} id="icon"/>
            </div>
        </div>
        <div>
            <div className={getNavElementClass("/about-us")} onClick={() => navigate("/about-us")}>
                <NavContact id="icon"/>
            </div>
            <Divider variant="middle" color="#1D2024"/>
            <div className={getNavElementClass("/account")} onClick={() => {
                if (!!authInfo) {
                    navigate("/account");
                }
            }}>
                <NavAccount id="icon"/>
            </div>
        </div>
    </div>);
}

export default LeftNavPanel;
