import React, {useEffect, useState, useCallback} from "react";
import {useLocation} from "wouter";
import {Manager} from "../../model/Manager";
import {fetchManagersData} from "../../service/ManagerService";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import CreateManagerDialog from "./dialog/CreateManagerDialog";

const ManagersPage: React.FC = () => {
    const [isCreateManagerDialogOpen, setIsCreateManagerDialogOpen] = useState(false);
    const [, setLocation] = useLocation();
    const [data, setData] = useState<Manager[]>([]);
    const [error, setError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    const fetchData = useCallback(() => {
        if (authToken) {
            fetchManagersData(authToken as string)
                .then(data => setData(data))
                .catch(error => setError(error));
        }
    }, [authToken]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const createManagerAndUpdate = () => {
        setIsCreateManagerDialogOpen(false);
        fetchData();
    }

    if (error) return <div>Error: {error.message}</div>;
    if (data.length === 0) return (
        <div>
            <h1>Managers Page</h1>
            Loading...
        </div>
    );

    return (
        <div>
            <h1>Managers Page</h1>
            <button className="material-button" onClick={() => setIsCreateManagerDialogOpen(true)}>Add Analyzer</button>
            <TableContainer component={Paper}>
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
                                <TableCell align="left">{manager.active ? "Active" : "Disabled"}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <CreateManagerDialog open={isCreateManagerDialogOpen} onClose={() => setIsCreateManagerDialogOpen(false)} onCreate={createManagerAndUpdate} />
        </div>
    );
}

export default ManagersPage;
