import React, {useEffect, useState} from "react";
import {useLocation} from "wouter";
import {createSymbol, fetchSymbolsList} from "../../service/SymbolService";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {Symbol} from "./model/Symbol";
import {SymbolModel} from "../../model/SymbolModel";
import CreateSymbolDialog from "./dialog/CreateSymbolDialog";

const SymbolsPage: React.FC = () => {
    const [data, setData] = useState<Symbol[]>([]);
    const [error, setError] = useState<Error | null>(null);
    const [isCreateDialogOpen, setCreateDialogOpen] = useState(false);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        fetchSymbolsList(authToken as string)
            .then(data => setData(data))
            .catch(error => setError(error))
    }, [authToken]);

    const handleCreateSymbol = (symbol: SymbolModel) => {
        createSymbol(symbol, localStorage.getItem('auth.token') as string).then(() => window.location.reload()); // TODO: Add error handling
    };

    if (error) return <div>Error: {error.message}</div>

    return (
        <div>
            <h1>Symbols Page</h1>
            <div>
                <button onClick={() => setCreateDialogOpen(true)}>Add new Symbol</button>
                <CreateSymbolDialog
                    open={isCreateDialogOpen}
                    onClose={() => setCreateDialogOpen(false)}
                    onCreate={handleCreateSymbol}
                />
            </div>
            <TableContainer component={Paper}>
                <Table aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Symbol</TableCell>
                            <TableCell>Partition</TableCell>
                            <TableCell>Is One Way Mode</TableCell>
                            <TableCell>Price Min Step</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {data.map(symbol => (
                            <TableRow
                                key={symbol.symbol}
                                sx={{'&:last-child td, &:last-child th': {border: 0}}}
                            >
                                <TableCell component="th" scope="row">{symbol.symbol}</TableCell>
                                <TableCell align="left">{symbol.partition}</TableCell>
                                <TableCell align="left">{symbol.isOneWayMode ? 'True' : 'False'}</TableCell>
                                <TableCell align="left">{symbol.priceMinStep}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
}

export default SymbolsPage;
