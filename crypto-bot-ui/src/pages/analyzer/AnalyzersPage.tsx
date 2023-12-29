import React, {useCallback, useEffect, useState} from "react";
import {changeAnalyzerStatus, deleteAnalyzer, fetchAnalyzersList} from "../../service/AnalyzerService";
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import {Analyzer} from "./model/Analyzer";
import {useLocation} from "wouter";
import CreateAnalyzerDialog from "./dialog/CreateAnalyzerDialog";

const AnalyzersPage: React.FC = () => {
    const [isCreateDialogOpen, setCreateDialogOpen] = useState(false);
    const [data, setData] = useState<Analyzer[]>([]);
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    console.log('test')

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    const updateAnalyzersList = useCallback(() => {
        fetchAnalyzersList(authToken as string)
            .then(data => setData(data))
            .catch(error => setError(error))
    }, [authToken])

    useEffect(() => {
        updateAnalyzersList();
    }, [updateAnalyzersList]);


    const changeAnalyzerActiveStatus = (analyzer: Analyzer) => {
        changeAnalyzerStatus(analyzer.id, !analyzer.isActive, authToken as string)
            .then(() => updateAnalyzersList())
            .catch((error) => setError(error));
    }

    const deleteItem = (id: string) => {
        deleteAnalyzer(id, authToken as string)
            .then(() => updateAnalyzersList())
            .catch((error) => setError(error));
    }

    if (error) return <div>Error: {error.message}</div>

    return (
        <div>
            <h1>Analyzers Page</h1>
            <div>
                <Button variant='contained' size={'medium'} color={'primary'} onClick={() => setCreateDialogOpen(true)}>Create
                    New Analyzer</Button>
                <CreateAnalyzerDialog
                    open={isCreateDialogOpen}
                    authToken={authToken as string}
                    onClose={(result: boolean) => {
                        if (result) {
                            updateAnalyzersList();
                        }
                        setCreateDialogOpen(false);
                    }}
                />
            </div>
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
                            <TableCell>Start Capital</TableCell>
                            <TableCell>Money</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Delete</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {data.map(analyzer => (
                            <TableRow
                                key={analyzer.id}
                                sx={{'&:last-child td, &:last-child th': {border: 0}}}
                            >
                                <TableCell component="th" scope="row">{analyzer.id}</TableCell>
                                <TableCell align="left">{analyzer.diapason}</TableCell>
                                <TableCell align="left">{analyzer.gridSize}</TableCell>
                                <TableCell align="left">{analyzer.multiplayer}</TableCell>
                                <TableCell align="left">{analyzer.positionStopLoss}</TableCell>
                                <TableCell align="left">{analyzer.positionTakeProfit}</TableCell>
                                <TableCell align="left">{analyzer.symbol}</TableCell>
                                <TableCell align="left">{analyzer.startCapital}</TableCell>
                                <TableCell align="left">{analyzer.money}</TableCell>
                                <TableCell align="left">
                                    <Button variant='contained' size={'medium'}
                                            color={analyzer.isActive ? 'warning' : 'success'}
                                            onClick={() => changeAnalyzerActiveStatus(analyzer)}>{analyzer.isActive ? 'Deactivate' : 'Activate'}</Button>
                                </TableCell>
                                <TableCell align="left">
                                    <Button variant='contained' size={'medium'} color={'error'}
                                            onClick={() => deleteItem(analyzer.id)}>{'Delete'}</Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
}

export default AnalyzersPage;
