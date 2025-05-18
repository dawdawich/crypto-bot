import React, {useEffect, useState} from "react";
import {Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import '../../css/pages/backtest/BackTestPageStyle.css'
import {RequestStatusModel} from "../../model/RequestStatusModel";
import {fetchRequestStatuses} from "../../service/BackTestService";
import {useAuth} from "../../context/AuthContext";
import {useLocation} from "wouter";
import CreateBackTestDialog from "./dialog/CreateBackTestDialog";
import {fetchSymbolsNameList} from "../../service/SymbolService";
import {errorToast} from "../../shared/toast/Toasts";

const BackTestPage: React.FC = () => {
    const [requestStatuses, setRequestStatuses] = useState<RequestStatusModel[]>([]);
    const {authInfo} = useAuth();
    const [, navigate] = useLocation();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [symbols, setSymbols] = useState<string[]>([]);

    if (!authInfo) {
        navigate("/");
    }

    document.title = 'Back Test List';

    useEffect(() => {
        fetchSymbolsNameList()
            .then((symbols) => {
                setSymbols(symbols);
            })
            .catch((ex) => {
                errorToast("Failed to fetch symbols list");
                console.error(ex);
            });
        fetchRequestStatuses(authInfo!)
            .then(statuses => setRequestStatuses(statuses));
    }, [authInfo]);


    return (
        <div className="back-test-content">
            <div className="back-test-header">
                <div className="back-test-header-label">Back Tests</div>
                <Button variant="contained" className="back-test-create-button" onClick={() => setIsDialogOpen(true)}>Create
                    Backtest</Button>
            </div>
            <TableContainer>
                <Table size="small" stickyHeader>
                    <TableHead>
                        <TableRow className="back-test-table-headers">
                            <TableCell className="cell" style={{width: '75px'}}>Status</TableCell>
                            <TableCell className="cell">Request ID</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {
                            !!requestStatuses && Array.isArray(requestStatuses) && requestStatuses.map(status => {
                                return (
                                    <TableRow onClick={() => navigate('/back-tests/' + status.id)} className="back-test-table-body">
                                        <TableCell className="cell">{status.status}</TableCell>
                                        <TableCell className="cell">{status.id}</TableCell>
                                    </TableRow>
                                );
                            })
                        }
                    </TableBody>
                </Table>
            </TableContainer>

            <CreateBackTestDialog open={isDialogOpen} onClose={() => setIsDialogOpen(false)} symbols={symbols}
                                  authInfo={authInfo!} onCloseWithRequestId={(requestId) => {
                setIsDialogOpen(false);
                setRequestStatuses([...requestStatuses, {id: requestId, accountId: authInfo!.address, status: 'IN_PROGRESS'}]);
            }}/>
        </div>
    );
}

export default BackTestPage;
