import React, {useEffect, useState} from "react";
import {fetchAnalyzersData} from "../../service/AnalyzerService";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {Analyzer} from "./model/Analyzer";
import {useLocation} from "wouter";

const AnalyzersPage: React.FC = () => {
    const [, setLocation] = useLocation();
    const [data, setData] = useState<Analyzer[]>([]);
    const [error, setError] = useState<Error | null>(null);

    useEffect(() => {
        fetchAnalyzersData()
            .then(data => setData(data))
            .catch(error => setError(error))
    }, []);

    if (error) return <div>Error: {error.message}</div>
    if (data.length === 0) return (
        <div>
            <h1>Analyzers Page</h1>
            Loading...
        </div>
    );

    return (
        <div>
            <h1>Analyzers Page</h1>
            <TableContainer component={Paper}>
                <Table aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Id</TableCell>
                            <TableCell>Diapason</TableCell>
                            <TableCell>Grid Size</TableCell>
                            <TableCell>Multiplayer</TableCell>
                            <TableCell>Stop Loss</TableCell>
                            <TableCell>Take Profit</TableCell>
                            <TableCell>Symbol</TableCell>
                            <TableCell>Money</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {data.map(analyzer => (
                            <TableRow
                                key={analyzer.id}
                                sx={{'&:last-child td, &:last-child th': {border: 0}}}
                                onClick={() => setLocation(`/analyzer/${analyzer.id}`)}
                            >
                                <TableCell component="th" scope="row">{analyzer.id}</TableCell>
                                <TableCell align="left">{analyzer.diapason}</TableCell>
                                <TableCell align="left">{analyzer.gridSize}</TableCell>
                                <TableCell align="left">{analyzer.multiplayer}</TableCell>
                                <TableCell align="left">{analyzer.positionStopLoss}</TableCell>
                                <TableCell align="left">{analyzer.positionTakeProfit}</TableCell>
                                <TableCell align="left">{analyzer.symbol}</TableCell>
                                <TableCell align="left">{analyzer.money}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
}

export default AnalyzersPage;
