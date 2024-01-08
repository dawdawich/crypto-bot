import React, {useEffect, useState} from "react";
import {Analyzer} from "./model/Analyzer";
import {fetchAnalyzerData} from "../../service/AnalyzerService";
import {RouteComponentProps} from "wouter";
import {webSocketAnalyzerService} from "../../service/WebSocketService";

interface AnalyzerInfoPageProps extends RouteComponentProps<{ readonly analyzerId: string }> {
}

const AnalyzerInfoPage: React.FC<AnalyzerInfoPageProps> = (props: AnalyzerInfoPageProps) => {
    const [analyzer, setAnalyzer] = useState<Analyzer | null>(null);
    const [analyzerInfo, setAnalyzerInfo] = useState<{
        id: string;
        orders: string[];
        currentPrice: number;
        positions: {
            long: boolean;
            size: number;
            entryPrice: number;
        }[]
    } | null>(null);
    const [analyzerFetchError, setAnalyzerFetchError] = useState<Error | null>(null);

    useEffect(() => {
        fetchAnalyzerData(props.params.analyzerId)
            .then(data => setAnalyzer(data))
            .catch(error => setAnalyzerFetchError(error))
    }, [props.params.analyzerId]);

    useEffect(() => {
        if (!!analyzer && analyzer.isActive) {
            webSocketAnalyzerService.connect((analyzer) => {
                setAnalyzerInfo(JSON.parse(analyzer));
            });
            const intervalId = setInterval(() => {
                webSocketAnalyzerService.sendMessage(JSON.stringify({id: analyzer.id}));
            }, 1000);
            webSocketAnalyzerService.onDisconnect = () => {
                clearTimeout(intervalId);
            }
        }
    }, [analyzer]);

    if (analyzerFetchError) return <div>Error: {analyzerFetchError.message}</div>
    if (!analyzer) return (
        <div>
            <h1>Analyzer Info Page</h1>
            Loading...
        </div>
    );

    if (!!analyzerInfo) {

    }

    return (
        <div>
            <h1>Analyzer Info Page</h1>
            <div>ID: {analyzer.id}</div>
            <div>Diapason: {analyzer.diapason}</div>
            <div>Grid Size: {analyzer.gridSize}</div>
            <div>Multiplayer: {analyzer.multiplayer}</div>
            <div>Stop Loss: {analyzer.positionStopLoss}</div>
            <div>Take Profit: {analyzer.positionTakeProfit}</div>
            <div>Symbol: {analyzer.symbol}</div>
            <div>Money: {analyzer.money}</div>
            {
                analyzerInfo != null &&
                <div>
                    <div>Orders: {analyzerInfo.orders}</div>
                    <div>Current Price: {analyzerInfo.currentPrice}</div>
                    <div>Position: {JSON.stringify(analyzerInfo.positions)}</div>
                </div>
            }
        </div>
    );
}

export default AnalyzerInfoPage;
