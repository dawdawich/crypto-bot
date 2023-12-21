import React, {useEffect, useState} from "react";
import {Account} from "../../model/Account";
import {fetchAccountInfo} from "../../service/AccountService";
import {useLocation} from "wouter";
import "../../css/AccountInfo.css";

const AccountPage: React.FC = () => {
    const [data, setData] = useState<Account | null>()
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
    return (
    <div id="account-info">
        <h2>Your Account Info</h2>
        <div id="user-info">
            <p><strong>Username:</strong> <span id="username">{data.username}</span></p>
            <p><strong>Name:</strong> <span id="name">{data.name}</span></p>
            <p><strong>Surname:</strong> <span id="surname">{data.surname}</span></p>
            <p><strong>Email:</strong> <span id="email">{data.email}</span></p>
            <p><strong>Account Creation Date:</strong> <span id="creation-date">{new Date(data.createTime).toLocaleDateString()}</span></p>
        </div>
    </div>
    );
}

export default AccountPage;
