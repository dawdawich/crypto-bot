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

interface ManagerEditorPageProps extends RouteComponentProps<{ readonly managerId: string }> {
}

const ManagerPageEditor: React.FC<ManagerEditorPageProps> = (props: ManagerEditorPageProps) => {
    const [manager, setManager] = useState<Manager | null>(null);
    const [managerFetchError, setManagerFetchError] = useState<Error | null>(null);
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        fetchManagerData(authToken as string, props.params.managerId)
            .then(data => setManager(data))
            .catch(error => setManagerFetchError(error));
    }, [props.params.managerId, authToken]);

    let handleStrategyChange = function (event: SelectChangeEvent) {
        manager!.chooseStrategy = event.target.value as string;
    }

    const changeManagerStatus = () => {
        manager!.active = !manager!.active;
        updateManagerStatus(manager!.id, manager!.active, authToken as string)
            .then(result => {
                if (result) {
                    fetchManagerData(authToken as string, props.params.managerId)
                        .then(data => setManager(data));
                }
            })
            .catch(error => setManagerFetchError(error));
    }

    const handleDeleteManager = () => {
        deleteManager(manager!.id, authToken as string)
            .then(result => {
                if (result) {
                    navigate('/');
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
                        <MenuItem value={"CUSTOM"}>Custom</MenuItem>
                    </Select>
                </FormControl>
                <p><strong>Create Time:</strong> <span id="create-time">{createDate.toLocaleDateString() + ' : ' + createDate.toLocaleTimeString()}</span></p>
                <p><strong>Update Time:</strong> <span id="update-time">{updateDate.toLocaleDateString() + ' : ' + updateDate.toLocaleTimeString()}</span></p>
            </CardContent>
            <CardActions>
                <Button size="medium" onClick={changeManagerStatus}>{manager?.active ? (<PowerOffIcon />) : (<PowerOnIcon />)}
                    {manager?.active ? "Turn Off" : "Turn On"}</Button>
                <Button size="medium" color={'error'} onClick={handleDeleteManager}><Delete /> Delete</Button>
            </CardActions>
        </Card>
    )
}

export default ManagerPageEditor;
