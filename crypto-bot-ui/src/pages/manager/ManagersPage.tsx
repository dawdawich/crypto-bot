import React, {useEffect, useState, useCallback} from "react";
import {useLocation} from "wouter";
import {Manager} from "../../model/Manager";
import {fetchManagersData} from "../../service/ManagerService";
import {Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import CreateManagerDialog from "./dialog/CreateManagerDialog";
import {useAuth} from "../../context/AuthContext";

const ManagersPage: React.FC = () => {
    const [isCreateManagerDialogOpen, setIsCreateManagerDialogOpen] = useState(false);
    const [, setLocation] = useLocation();
    const [data, setData] = useState<Manager[]>([]);
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();

    if (!authInfo) {
        navigate('/');
        window.location.reload();
    }

    const fetchData = useCallback(() => {
        fetchManagersData(authInfo!)
            .then(data => setData(data))
            .catch(error => setError(error));
    }, [authInfo]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const createManagerAndUpdate = () => {
        setIsCreateManagerDialogOpen(false);
        fetchData();
    }

    if (error) return <div>Error: {error.message}</div>;

    const dataTable = <TableContainer component={Paper}>
        <Table aria-label="simple table">
            <TableHead>
                <TableRow>
                    <TableCell>Id</TableCell>
                    <TableCell>Choose Strategy</TableCell>
                    <TableCell>Custom Analyzer Id</TableCell>
                    <TableCell>Status</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {data.map(manager => (
                    <TableRow
                        key={manager.id}
                        sx={{'&:last-child td, &:last-child th': {border: 0}}}
                        onClick={() => setLocation(`/manager/${manager.id}`)}
                    >
                        <TableCell component="th" scope="row">{manager.id}</TableCell>
                        <TableCell align="left">{manager.chooseStrategy}</TableCell>
                        <TableCell align="left">{manager.customAnalyzerId}</TableCell>
                        <TableCell align="left">{manager.status}</TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </TableContainer>;

    return (
        <div>
            <h1>Managers Page</h1>
            <Button variant='contained' size={'medium'} color={'primary'}
                    onClick={() => setIsCreateManagerDialogOpen(true)}>Add Manager</Button>
            <CreateManagerDialog open={isCreateManagerDialogOpen} onClose={() => setIsCreateManagerDialogOpen(false)}
                                 onCreate={createManagerAndUpdate}/>
            {data.length === 0 ? null : dataTable}
        </div>
    );
}

export default ManagersPage;
