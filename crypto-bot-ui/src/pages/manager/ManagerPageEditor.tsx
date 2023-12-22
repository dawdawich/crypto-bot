import React, {useEffect, useState} from "react";
import {RouteComponentProps} from "wouter";
import {Manager} from "../../model/Manager";
import {fetchManagerData, updateManagerData} from "../../service/ManagerService";
import "../../css/TextClasses.css";
import {
    Button,
    Card, CardActions,
    CardContent,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    SelectChangeEvent,
    Typography
} from "@mui/material";
import PowerOffIcon from "@mui/icons-material/PowerOff"
import PowerOnIcon from "@mui/icons-material/Power"

interface ManagerEditorPageProps extends RouteComponentProps<{ readonly managerId: string }> {
}

const ManagerPageEditor: React.FC<ManagerEditorPageProps> = (props: ManagerEditorPageProps) => {
    const [manager, setManager] = useState<Manager | null>(null)
    const [managerFetchError, setManagerFetchError] = useState<Error | null>(null);

    useEffect(() => {
        fetchManagerData(props.params.managerId)
            .then(data => setManager(data))
            .catch(error => setManagerFetchError(error));
    }, [props.params.managerId]);

    let handleStrategyChange = function (event: SelectChangeEvent) {
        manager!.chooseStrategy = event.target.value as string;
        updateManagerData(manager!)
            .then(result => {
                if (result) {
                    fetchManagerData(props.params.managerId)
                        .then(data => setManager(data));
                }
            })
            .catch(error => setManagerFetchError(error));
    }

    let changeManagerStatus = () => {
        manager!.active = !manager!.active;
        updateManagerData(manager!)
            .then(result => {
                if (result) {
                    fetchManagerData(props.params.managerId)
                        .then(data => setManager(data));
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

    return (
        <Card>
            <CardContent>
                <Typography gutterBottom variant="h5" component="div" className="center-text-align">
                    ID: {manager?.id}
                </Typography>

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
                <Typography variant="body2" className="right-text-align">
                    Create Time: {manager?.createTime} || Update Time: {manager?.updateTime}
                </Typography>
            </CardContent>
            <CardActions>
                <Button size="medium" onClick={changeManagerStatus}>{manager?.active ? (<PowerOffIcon />) : (<PowerOnIcon />)}
                    {manager?.active ? "Turn Off" : "Turn On"}</Button>
            </CardActions>
        </Card>
    )
}

export default ManagerPageEditor;
