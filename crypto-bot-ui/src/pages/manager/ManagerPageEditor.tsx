import React, {useEffect, useState} from "react";
import {RouteComponentProps, useLocation} from "wouter";
import {Manager} from "../../model/Manager";
import {deleteManager, fetchManagerData, updateManagerStatus} from "../../service/ManagerService";
import "../../css/ManagerEditPage.css";
import {
    Button,
    Card,
    CardActions,
    CardContent,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    SelectChangeEvent
} from "@mui/material";
import PowerOffIcon from "@mui/icons-material/PowerOff"
import PowerOnIcon from "@mui/icons-material/Power"
import {Delete} from "@mui/icons-material";
import {webSocketManagerService} from "../../service/WebSocketService";
import {useAuth} from "../../context/AuthContext";

interface ManagerEditorPageProps extends RouteComponentProps<{ readonly managerId: string }> {
}

const ManagerPageEditor: React.FC<ManagerEditorPageProps> = (props: ManagerEditorPageProps) => {
    const [manager, setManager] = useState<Manager>();
    const [managerFetchError, setManagerFetchError] = useState<Error>();
    const [managerInfo, setManagerInfo] = useState<{
        id: string;
        currentPrice: number;
        analyzerId: number;
        startCapital: number;
        currentCapital: number;
        orders: string[];
        positions: {
            long: boolean;
            size: number;
            entryPrice: number;
        }[]
    }>();
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();

    if (!authInfo) {
        navigate('/');
    }

    useEffect(() => {
        fetchManagerData(authInfo!, props.params.managerId)
            .then(data => setManager(data))
            .catch(error => setManagerFetchError(error));
    }, [props.params.managerId, authInfo]);

    useEffect(() => {
        if (!!manager && manager.status === 'ACTIVE') {
            webSocketManagerService.connect((managerInfo) => {
                setManagerInfo(JSON.parse(managerInfo));
            });
            const intervalId = setInterval(() => {
                webSocketManagerService.sendMessage(JSON.stringify({id: manager.id}));
            }, 1000);
            webSocketManagerService.onDisconnect = () => {
                clearTimeout(intervalId);
            }
        }
    }, [manager]);

    let handleStrategyChange = function (event: SelectChangeEvent) {
        manager!.chooseStrategy = event.target.value as string;
    }

    const changeManagerStatus = () => {
        manager!.status = manager!.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        updateManagerStatus(authInfo!, manager!.id, manager!.status)
            .then(result => {
                if (result) {
                    fetchManagerData(authInfo!, props.params.managerId)
                        .then(data => setManager(data));
                }
            })
            .catch(error => setManagerFetchError(error));
    }

    const handleDeleteManager = () => {
        deleteManager(authInfo!, manager!.id)
            .then(result => {
                if (result) {
                    navigate('/manager');
                    window.location.reload();
                }
            })
            .catch(error => setManagerFetchError(error));
    }

    if (managerFetchError) return <div>Error: {managerFetchError.message}</div>
    if (!manager) return (
        <div>
            <h1>Analyzer Info Page</h1>
            Loading...
        </div>
    );
    let createDate = new Date(manager.createTime);
    let updateDate = new Date(manager.updateTime);

    return (
        <Card id="account-info">
            <CardContent>
                <p><strong>ID:</strong> <span id="username">{manager.id}</span></p>

                <FormControl fullWidth>
                    <InputLabel id="choose-strategy-select-label">Analyzer Find Strategy</InputLabel>
                    <Select
                        labelId="choose-strategy-select-label"
                        id="choose-strategy-select"
                        value={manager?.chooseStrategy}
                        label="Strategy"
                        onChange={handleStrategyChange}
                    >
                        <MenuItem value={"BIGGEST_BY_MONEY"}>Biggest by money</MenuItem>
                        <MenuItem value={"MOST_STABLE"}>Most stable analyzer</MenuItem>
                        <MenuItem value={"CUSTOM"}>Custom</MenuItem>
                    </Select>
                </FormControl>
                <p><strong>Status:</strong> <span id="status">{manager.status}</span></p>
                {manager.stopLoss ? <p><strong>Stop Loss:</strong> <span id="error-description">{manager.stopLoss}</span></p> : null}
                {manager.takeProfit ? <p><strong>Take Profit:</strong> <span id="error-description">{manager.takeProfit}</span></p> : null}
                {manager.stopDescription ? <p><strong>Stop description:</strong> <span id="error-description">{manager.stopDescription}</span></p> : null}
                {manager.errorDescription ? <p><strong>Error description:</strong> <span id="error-description">{manager.errorDescription}</span></p> : null}
                <p><strong>Create Time:</strong> <span id="create-time">{createDate.toLocaleDateString() + ' : ' + createDate.toLocaleTimeString()}</span></p>
                <p><strong>Update Time:</strong> <span id="update-time">{updateDate.toLocaleDateString() + ' : ' + updateDate.toLocaleTimeString()}</span></p>

                {!!managerInfo &&
                    <div>
                        <div>Current Price: {managerInfo.currentPrice}</div>
                        <div>Analyzer Id: {managerInfo.analyzerId}</div>
                        <div>Start Capital: {managerInfo.startCapital}</div>
                        <div>Current Capital: {managerInfo.currentCapital}</div>
                        <div>Orders: {managerInfo.orders}</div>
                        <div>positions: {JSON.stringify(managerInfo.positions)}</div>
                    </div>
                }
            </CardContent>
            <CardActions>
                <Button variant='contained' size="medium" onClick={changeManagerStatus}>{manager.status === 'ACTIVE' ? (<PowerOffIcon />) : (<PowerOnIcon />)}
                    {manager.status === 'ACTIVE' ? "Turn Off" : "Turn On"}</Button>
                <Button variant='contained' size="medium" color={'error'} onClick={handleDeleteManager}><Delete />Delete</Button>
            </CardActions>
        </Card>
    )
}

export default ManagerPageEditor;
