import React, {useEffect, useState} from "react";
import {Account} from "../../model/Account";
import {deleteApiToken, fetchAccountInfo} from "../../service/AccountService";
import {useLocation} from "wouter";
import "../../css/AccountInfo.css";
import AddApiTokenDialog from "./dialog/AddApiTokenDialog";
import {ApiToken} from "../../model/ApiToken";
import {Button} from "@mui/material";

const AccountPage: React.FC = () => {
    const [isApiTokenDialogOpen, setIsApiTokenDialogOpen] = useState(false);
    const [data, setData] = useState<Account | null>(null)
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        fetchAccountInfo(authToken as string)
            .then(res => setData(res))
            .catch(ex => setError(ex))
    }, [])

    const handleNewApiToken = (apiToken: ApiToken) => {
        if (data) {
            data.tokens.push(apiToken);
            setData(data);
        }
    }

    const handleDeleteApiToken = (id: string) => {
        deleteApiToken(id, authToken as string)
            .then(() => {
                if (data) {
                    setData({...data, tokens: data.tokens.filter((token: ApiToken) => token.id !== id)});
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
    let date = new Date(data.createTime);
    let apiTokensTable = data.tokens.length > 0 ?
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
            {data.tokens.map((token: ApiToken) => (
                <tr key={token.id}>
                    <td>{token.id}</td>
                    <td>{token.apiKey}</td>
                    <td>{token.market}</td>
                    <td>{token.test.toString()}</td>
                    <td>
                        <Button variant='contained' size={'medium'} color={'error'} onClick={() => handleDeleteApiToken(token.id)}>Delete</Button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
        : null;


    return (
        <div>
            <div id="account-info">
                <h2>Your Account Info</h2>
                <div id="user-info">
                    <p><strong>Username:</strong> <span id="username">{data.username}</span></p>
                    <p><strong>Name:</strong> <span id="name">{data.name}</span></p>
                    <p><strong>Surname:</strong> <span id="surname">{data.surname}</span></p>
                    <p><strong>Email:</strong> <span id="email">{data.email}</span></p>
                    <p><strong>Account Creation Date:</strong> <span
                        id="creation-date">{date.toLocaleDateString() + ' : ' + date.toLocaleTimeString()}</span></p>
                </div>
                <Button variant='contained' size={'medium'} color={'primary'} onClick={() => setIsApiTokenDialogOpen(true)}>Add API token</Button>
                <AddApiTokenDialog open={isApiTokenDialogOpen} onClose={() => setIsApiTokenDialogOpen(false)}
                                   onCreate={handleNewApiToken}/>
            </div>
            {apiTokensTable}
        </div>
    );
}

export default AccountPage;
