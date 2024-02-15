import React, {useEffect, useState} from "react";
import {useLocation} from "wouter";
import {createSymbol, fetchSymbolsList} from "../../service/SymbolService";
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {Symbol} from "../../model/Symbol";
import {SymbolModel} from "../../model/SymbolModel";
import CreateSymbolDialog from "./dialog/CreateSymbolDialog";
import {useAuth} from "../../context/AuthContext";

const SymbolsPage: React.FC = () => {
    const [data, setData] = useState<Symbol[]>([]);
    const [error, setError] = useState<Error | null>(null);
    const [isCreateDialogOpen, setCreateDialogOpen] = useState(false);
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();

    if (!authInfo) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        fetchSymbolsList(authInfo!)
            .then(data => setData(data))
            .catch(error => setError(error))
    }, [authInfo]);

    const handleCreateSymbol = (symbol: SymbolModel) => {
        createSymbol(authInfo!, symbol).then(() => window.location.reload()); // TODO: Add error handling
    };

    if (error) return <div>Error: {error.message}</div>

    return (
        <div>
            <h1>Symbols Page</h1>
            <div>
                <Button variant='contained' size={'medium'} color={'primary'} onClick={() => setCreateDialogOpen(true)}>Add new Symbol</Button>
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
                            <TableCell>Min Price</TableCell>
                            <TableCell>Max Price</TableCell>
                            <TableCell>Tick Size</TableCell>
                            <TableCell>Min Order QTY</TableCell>
                            <TableCell>Max Order QTY</TableCell>
                            <TableCell>QTY Step</TableCell>
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
                                <TableCell align="left">{symbol.minPrice}</TableCell>
                                <TableCell align="left">{symbol.maxPrice}</TableCell>
                                <TableCell align="left">{symbol.tickSize}</TableCell>
                                <TableCell align="left">{symbol.minOrderQty}</TableCell>
                                <TableCell align="left">{symbol.maxOrderQty}</TableCell>
                                <TableCell align="left">{symbol.qtyStep}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
}

export default SymbolsPage;
