import React, {useEffect, useState} from "react";
import {useLocation} from "wouter";
import {fetchHealthcheckReport} from "../../service/HealthcheckService";
import {Card, CardContent, Typography} from "@mui/material";

const MonitoringPage: React.FC = () => {
    const [data, setData] = useState<{
        "analyzer-service": string,
        "event-listener-service": string,
        "event-listener-topics-updates": any,
        "trade-manager-service": string
    }>();
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        fetchHealthcheckReport({authToken:authToken as string})
            .then(report => setData(report))
            .catch((error) => setError(error));
    }, [authToken]);

    const getPricesUpdateList = () => {
        if (data) {
            let tickers = JSON.parse(data["event-listener-topics-updates"]);
            let currentTime = Date.now();
            console.log(currentTime);
            return Object.keys(tickers).map((key: string) => {
                return (
                    <Typography variant="body2">
                        Event Listener Last Price Update (s) {key}: {currentTime - tickers[key]}
                    </Typography>
                );
            });
        }
    };

    const readStatusFromString = (json: string) => JSON.parse(json).status

    if (error) return <Typography variant="h6">Error: {error.message}</Typography>;

    if (!data) return <Typography variant="h6">Loading...</Typography>;

    return (
        <Card
            sx={{
                display: 'flex',
                flexDirection: 'column',
                p: 1,
                m: 1,
                bgcolor: 'background.paper',
            }}
        >
            <CardContent>
                <Typography variant="body1">
                    Analyzer Service: {readStatusFromString(data["analyzer-service"])}
                </Typography>
                <Typography variant="body1">
                    Event Listener Service: {readStatusFromString(data["event-listener-service"])}
                </Typography>
                {
                    readStatusFromString(data["event-listener-service"]) === 'UP' &&
                    getPricesUpdateList()
                }
                <Typography variant="body1">
                    Trade Manager Service: {readStatusFromString(data["trade-manager-service"])}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default MonitoringPage;
