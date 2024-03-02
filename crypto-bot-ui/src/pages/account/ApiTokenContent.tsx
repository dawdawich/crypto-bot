import React, {useEffect, useState} from "react";
import {Button, styled, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import '../../css/pages/account/ApiTokenStyles.css'
import {useLocation} from "wouter";
import {ApiToken} from "../../model/ApiToken";
import {deleteApiToken, getApiTokens} from "../../service/AccountService";
import {useAuth} from "../../context/AuthContext";
import AddApiTokenDialog from "./dialog/AddApiTokenDialog";
import {errorToast} from "../../shared/toast/Toasts";
import {UnauthorizedError} from "../../utils/errors/UnauthorizedError";
import {useLoader} from "../../context/LoaderContext";
import loadingTableRows from "../../shared/LoadingTableRows";

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
    const [isTableLoading, setIsTableLoading] = useState(false);
    const [isApiTokenDialogOpen, setIsApiTokenDialogOpen] = useState(false);
    const [, navigate] = useLocation();
    const {authInfo, logout} = useAuth();
    const [data, setData] = useState<ApiToken[]>([]);

    document.title = 'Api Tokens';

    if (!authInfo) {
        navigate('/');
    }

    useEffect(() => {
        setIsTableLoading(true);
        getApiTokens(authInfo!)
            .then(res => {
                setIsTableLoading(false);
                setData(res);
            })
            .catch(ex => {
                setIsTableLoading(false);
                errorToast("Failed to get api tokens");
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
    }, [authInfo, logout]);

    const handleDeleteApiToken = (id: string) => {
        deleteApiToken(id, authInfo!)
            .then(() => {
                if (data) {
                    setData({...data.filter((token: ApiToken) => token.id !== id)});
                }
            })
            .catch((ex) => {
                errorToast("Failed to delete api token");
                if (ex instanceof UnauthorizedError) {
                    logout();
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
                        {isTableLoading ? loadingTableRows(({rows: 13, columns: 4, postfixSkipColumns: 1})) :
                            Array.isArray(data) && data.map((apiToken) =>
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
            <AddApiTokenDialog open={isApiTokenDialogOpen} onClose={() => setIsApiTokenDialogOpen(false)}
                               onCreate={handleNewApiToken} authInfo={authInfo!} logout={logout}/>
        </div>
    );
}

export default ApiTokenContent;
