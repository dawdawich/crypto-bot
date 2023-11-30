import React, {useEffect, useState} from "react";
import {useLocation} from "wouter";
import {Manager} from "./model/Manager";
import {fetchManagersData} from "../../service/ManagerService";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";

const ManagersPage: React.FC = () => {
    const [, setLocation] = useLocation();
    const [data, setData] = useState<Manager[]>([]);
    const [error, setError] = useState<Error | null>(null);

    useEffect(() => {
        fetchManagersData()
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
        </div>
    );
}

export default ManagersPage;
