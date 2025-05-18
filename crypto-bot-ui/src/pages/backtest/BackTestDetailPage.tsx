import React, {useEffect, useState} from "react";
import {useLocation, useParams} from "wouter";
import '../../css/pages/backtest/BackTestPageStyle.css';
import {Divider, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {fetchRequestResult} from "../../service/BackTestService";
import {useAuth} from "../../context/AuthContext";
import {errorToast} from "../../shared/toast/Toasts";

export interface BackTestResults {
    startCapital: number;
    diapason: number;
    gridSize: number;
    takeProfit: number;
    stopLoss: number;
    results: {
        symbol: string;
        leverage: number;
        resultCapital: number;
        startPriceTime: number;
        endPriceTime: number;
    }[];
}

const BackTestDetailPage: React.FC = () => {
    const {requestId} = useParams();
    const [requestResult, setRequestResult] = useState<BackTestResults>();
    const {authInfo} = useAuth();
    const [, navigate] = useLocation();

    if (!authInfo) {
        navigate("/");
    }

    document.title = 'Back Test Result';

    useEffect(() => {
        fetchRequestResult(authInfo!, requestId!)
            .then(res => setRequestResult(res))
            .catch((ex) => {
                errorToast("Failed to fetch request result");
                console.error(ex);
            })
    }, [authInfo, requestId]);

    return !!requestResult ? (
        <div className="back-test-content">
            <div className="back-test-header">
                <div className="back-test-header-label">Back Tests Request - {requestId}</div>
            </div>
            <div className="detail-container">
                <div>Start Capital: {requestResult!.startCapital}</div>
                <div>Diapason: {requestResult!.diapason}</div>
                <div>Grid Size: {requestResult!.gridSize}</div>
                <div>Take Profit: {requestResult!.takeProfit}</div>
                <div>Stop Loss: {requestResult!.stopLoss}</div>
            </div>
            <Divider />
            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow className="back-test-table-headers">
                            <TableCell className="cell">Pair</TableCell>
                            <TableCell className="cell">Leverage</TableCell>
                            <TableCell className="cell">Start Calc Time</TableCell>
                            <TableCell className="cell">End Calc Time</TableCell>
                            <TableCell className="cell">Result Capital</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {
                            Array.isArray(requestResult!.results) && requestResult!.results.map(pairInfo => {
                                return (
                                    <TableRow className="back-test-table-body">
                                        <TableCell className="cell">{pairInfo.symbol}</TableCell>
                                        <TableCell className="cell">{pairInfo.leverage}</TableCell>
                                        <TableCell className="cell">{pairInfo.startPriceTime}</TableCell>
                                        <TableCell className="cell">{pairInfo.endPriceTime}</TableCell>
                                        <TableCell className="cell">{pairInfo.resultCapital}</TableCell>
                                    </TableRow>
                                );
                            })
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    ) : (<div>
        Loading ...
    </div>);
}

export default BackTestDetailPage;
