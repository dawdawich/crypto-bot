import React, {useEffect, useState} from "react";
import {deleteApiToken, getApiTokens} from "../../service/AccountService";
import {useLocation} from "wouter";
import "../../css/AccountInfo.css";
import AddApiTokenDialog from "./dialog/AddApiTokenDialog";
import {ApiToken} from "../../model/ApiToken";
import {Button} from "@mui/material";
import {useAuth} from "../../context/AuthContext";

const AccountPage: React.FC = () => {
    const [isApiTokenDialogOpen, setIsApiTokenDialogOpen] = useState(false);
    const [data, setData] = useState<ApiToken[]>([])
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();

    console.log(authInfo)

    if (!authInfo) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        getApiTokens(authInfo!)
            .then(res => setData(res))
            .catch(ex => setError(ex))
    }, [authInfo])

    const handleNewApiToken = (apiToken: ApiToken) => {
        if (Array.isArray(data)) {
            setData([...data, apiToken]);
        } else {
            setData([apiToken]);
        }
    }

    const handleDeleteApiToken = (id: string) => {
        deleteApiToken(id, authInfo!)
            .then(() => {
                if (data) {
                    setData({...data.filter((token: ApiToken) => token.id !== id)});
                    console.log(JSON.stringify(data))
                }
            })
    }

    if (!!error) {
        return (
            <div>
                <h1>Error occurred</h1>
                <p>{error.message}</p>
            </div>
        );
    }

    if (!data) {
        return (
            <div>
                <h1>Loading...</h1>
            </div>
        );
    }
    let apiTokensTable = data.length > 0 ?
        <table className="material-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>API Key</th>
                <th>Market</th>
                <th>Test Account</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody>
            {data.map((token: ApiToken) => (
                <tr key={token.id}>
                    <td>{token.id}</td>
                    <td>{token.apiKey}</td>
                    <td>{token.market}</td>
                    <td>{token.test.toString()}</td>
                    <td>
                        <Button variant='contained' size={'medium'} color={'error'}
                                onClick={() => handleDeleteApiToken(token.id)}>Delete</Button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
        : null;


    return (
        <div>
            <Button variant='contained' size={'medium'} color={'primary'}
                    onClick={() => setIsApiTokenDialogOpen(true)}>Add API token</Button>
            <AddApiTokenDialog open={isApiTokenDialogOpen} onClose={() => setIsApiTokenDialogOpen(false)}
                               onCreate={handleNewApiToken} authInfo={authInfo!}/>
            {apiTokensTable}
        </div>
    );
}

export default AccountPage;
