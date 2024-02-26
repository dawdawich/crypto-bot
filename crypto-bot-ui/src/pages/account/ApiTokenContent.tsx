import React, {useEffect, useRef, useState} from "react";
import {Button, styled, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import '../../css/pages/account/ApiTokenStyles.css'
import {useLocation} from "wouter";
import {ApiToken} from "../../model/ApiToken";
import {deleteApiToken, getApiTokens} from "../../service/AccountService";
import {useAuth} from "../../context/AuthContext";
import AddApiTokenDialog from "./dialog/AddApiTokenDialog";
import loadingSpinner from "../../assets/images/loading-spinner.svga";
import {errorToast} from "../toast/Toasts";

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

const marketNames = {
    BYBIT: "ByBit"
};

const ApiTokenContent: React.FC = () => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [animation, setAnimation] = useState('');
    const [isApiTokenDialogOpen, setIsApiTokenDialogOpen] = useState(false);
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();
    const [data, setData] = useState<ApiToken[]>([]);

    document.title = 'Api Tokens';

    useEffect(() => {
        setIsLoading(true);
        getApiTokens(authInfo!)
            .then(res => {
                setIsLoading(false);
                setData(res);
            })
            .catch(ex => {
                setIsLoading(false);
                errorToast("Failed to get api tokens");
                console.error(ex);
            });
    }, [authInfo]);

    const handleDeleteApiToken = (id: string) => {
        deleteApiToken(id, authInfo!)
            .then(() => {
                if (data) {
                    setData({...data.filter((token: ApiToken) => token.id !== id)});
                }
            });
    }

    const handleNewApiToken = (apiToken: ApiToken) => {
        if (Array.isArray(data)) {
            setData([...data, apiToken]);
        } else {
            setData([apiToken]);
        }
    }

    useEffect(() => {
        fetch(loadingSpinner)
            .then(response => response.text())
            .then(text => {
                setAnimation(text)
                if (spanRef.current) {
                    spanRef.current.innerHTML = animation
                }
            });
    }, [animation, isLoading]);

    return (
        <div className="account-api-token-content">
            <div className="account-api-token-header">
                <div className="account-api-token-header-path">
                    <PreviousPath onClick={() => navigate("/account")}>
                        Account /
                    </PreviousPath>
                    <CurrentPath>API Token</CurrentPath>
                </div>
                <Button variant="contained" style={{
                    borderRadius: '4px',
                    marginRight: '16px',
                    alignSelf: 'center',
                    height: '34px',
                    backgroundColor: '#D0FF12',
                    color: '#121417',
                    fontWeight: '700',
                    textTransform: 'none'
                }} onClick={() => setIsApiTokenDialogOpen(true)}>Add API Token</Button>
            </div>
            {isLoading ?
                <div style={{
                    flexGrow: 1,
                    width: '50%',
                    height: '50%',
                    alignSelf: "center"
                }}>
                    <span ref={spanRef}/>
                </div> :
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow id="account-api-token-table-headers">
                                <TableCell id="cell">ID</TableCell>
                                <TableCell id="cell">API Key</TableCell>
                                <TableCell id="cell">Market</TableCell>
                                <TableCell id="cell">Demo Account</TableCell>
                                <TableCell id="cell"></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {!!data && Array.isArray(data) && data.map((apiToken) =>
                                <TableRow key={apiToken.id}
                                          sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                    <TableCell id="account-api-token-table-body-cell"
                                               align="left">{apiToken.id}</TableCell>
                                    <TableCell id="account-api-token-table-body-cell"
                                               align="left">{apiToken.apiKey}</TableCell>
                                    <TableCell id="account-api-token-table-body-cell"
                                               align="left">{(marketNames as any)[apiToken.market]}</TableCell>
                                    <TableCell id="account-api-token-table-body-cell"
                                               align="left">{apiToken.test ? 'Yes' : 'No'}</TableCell>
                                    <TableCell id="account-api-token-table-body-cell" align="center">
                                        <Button variant="contained" style={{
                                            borderRadius: '4px',
                                            alignSelf: 'center',
                                            height: '34px',
                                            backgroundColor: '#E7323B',
                                            color: 'white',
                                            fontWeight: '400',
                                            textTransform: 'none'
                                        }} onClick={() => handleDeleteApiToken(apiToken.id)}>
                                            Delete
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            }
            <AddApiTokenDialog open={isApiTokenDialogOpen} onClose={() => setIsApiTokenDialogOpen(false)}
                               onCreate={handleNewApiToken} authInfo={authInfo!}/>
        </div>
    );
}

export default ApiTokenContent;
