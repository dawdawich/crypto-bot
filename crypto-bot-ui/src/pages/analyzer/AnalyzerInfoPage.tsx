import React, {useEffect, useState} from "react";
import {Analyzer} from "./model/Analyzer";
import {fetchAnalyzerData, fetchAnalyzerPosition} from "../../service/AnalyzerService";
import {Position} from "./model/Position";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {RouteComponentProps} from "wouter";
import {fetchSymbolCurrentPrice} from "../../service/ByBitService";

interface AnalyzerInfoPageProps extends RouteComponentProps<{ readonly analyzerId: string }> {
}

const AnalyzerInfoPage: React.FC<AnalyzerInfoPageProps> = (props: AnalyzerInfoPageProps) => {
    const [analyzer, setAnalyzer] = useState<Analyzer | null>(null)
    const [analyzerFetchError, setAnalyzerFetchError] = useState<Error | null>(null);

    const [positions, setPositions] = useState<Position[]>([])
    const [positionFetchError, setPositionFetchError] = useState<Error | null>(null);

    const [currentPrice, setCurrentPrice] = useState<number>()
    const [currentPriceError, setCurrentPriceError] = useState<Error>()

    useEffect(() => {
        fetchAnalyzerData(props.params.analyzerId)
            .then(data => setAnalyzer(data))
            .catch(error => setAnalyzerFetchError(error))
    }, [props.params.analyzerId]);

    useEffect(() => {
        fetchAnalyzerPosition(props.params.analyzerId)
            .then(data => setPositions(data))
            .catch(error => setPositionFetchError(error))
    }, [props.params.analyzerId]);

    useEffect(() => {
        if (analyzer) {
            fetchSymbolCurrentPrice(analyzer!!.symbol)
                .then(currentPrice => setCurrentPrice(currentPrice))
                .catch(error => setCurrentPriceError(error))
        }
    }, [analyzer]);

    if (analyzerFetchError) return <div>Error: {analyzerFetchError.message}</div>
    if (!analyzer) return (
        <div>
            <h1>Analyzer Info Page</h1>
            Loading...
        </div>
    );

    let positionsBlock;

    if (positionFetchError) {
        positionsBlock = (<div>Error: {positionFetchError.message}</div>)
    } else if (positions.length === 0) {
        positionsBlock = (
            <div>
                Fetching positions...
            </div>
        );
    } else {
        positionsBlock = (
            <div>
                <TableContainer component={Paper}>
                    <Table aria-label="simple table">
                        <TableHead>
                            <TableRow>
                                <TableCell>Entry Price</TableCell>
                                <TableCell>Close Price</TableCell>
                                <TableCell>Size</TableCell>
                                <TableCell>Profit</TableCell>
                                <TableCell>Direction</TableCell>
                                <TableCell>Close Time</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {positions
                                .sort((a, b) => {
                                    if (a.closeTime === null) return -1;
                                    if (b.closeTime === null) return 1;
                                    return b.closeTime - a.closeTime;
                                })
                                .map(position => {
                                    let profit;
                                    if (!position.closePrice && currentPrice) {
                                        let profitPerUnit = !position.long ? position.entryPrice - currentPrice : currentPrice - position.entryPrice;
                                        profit = (profitPerUnit - currentPrice * 0.00055) * position.size
                                    } else {
                                        let profitPerUnit = !position.long ? position.entryPrice - position.closePrice!! : position.closePrice!! - position.entryPrice;
                                        profit = (profitPerUnit - position.closePrice!! * 0.00055) * position.size
                                    }
                                    return (
                                        <TableRow sx={{'&:last-child td, &:last-child th': {border: 0}}}
                                                  style={{backgroundColor: !position.closeTime ? "lightgreen" : "white"}}
                                        >
                                            <TableCell component="th" scope="row">{position.entryPrice}</TableCell>
                                            <TableCell align="left">{position.closePrice}</TableCell>
                                            <TableCell align="left">{position.size}</TableCell>
                                            <TableCell align="left">{profit}</TableCell>
                                            <TableCell align="left">{position.long ? "Long" : "Short"}</TableCell>
                                            <TableCell align="left">{position.closeTime}</TableCell>
                                        </TableRow>
                                    );
                                })}
                        </TableBody>
                    </Table>
                </TableContainer>
            </div>
        );
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
            <div>
                Positions:
                {positionsBlock}
            </div>
        </div>
    );
}

export default AnalyzerInfoPage;
