import React, {useEffect, useState} from "react";
import {Account} from "../../model/Account";
import {fetchAccountInfo} from "../../service/AccountService";
import {useLocation} from "wouter";

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
    })

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
        <div>
            <label htmlFor="username">Username:</label>
            <input disabled type="text" id="username" value={data.username}/>
            <label htmlFor="name">Name:</label>
            <input disabled type="text" id="name" value={data.name}/>
            <label htmlFor="surname">Surname:</label>
            <input disabled type="text" id="surname" value={data.surname}/>
            <label htmlFor="email">Email:</label>
            <input disabled type="email" id="email" value={data.email}/>
        </div>
    );
}

export default AccountPage;
