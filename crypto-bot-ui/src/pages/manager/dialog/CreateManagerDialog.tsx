import React, {useEffect, useState} from "react";
import {ApiToken} from "../../../model/ApiToken";
import {useLocation} from "wouter";
import {getApiTokens} from "../../../service/AccountService";
import {
    Button, Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select,
    Switch, TextField
} from "@mui/material";
import {createManager} from "../../../service/ManagerService";
import {AuthInfo} from "../../../model/AuthInfo";

interface AddApiTokenDialogProps {
    open: boolean;
    onClose: () => void;
    onCreate: () => void;
}

const CreateManagerDialog: React.FC<AddApiTokenDialogProps> = ({open, onClose, onCreate}) => {
    const [tokens, setTokens] = useState<ApiToken[]>([])
    const [data, setData] = useState<{
        customAnalyzerId: string;
        status: string;
        apiTokenId: string | null;
        stopLoss: number | null;
        takeProfit: number | null;
    }>({
        customAnalyzerId: "",
        status: 'INACTIVE',
        apiTokenId: null,
        stopLoss: null,
        takeProfit: null
    })
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');
    let address = localStorage.getItem('auth.address');
    let signature = localStorage.getItem('auth.signature');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    useEffect(() => {
        if (open) {
            getApiTokens({accountId: address as string, signature: signature as string})
                .then(res => setTokens(res))
                .catch(ex => {
                    console.error('Failed to fetch user\'s tokens.');
                    console.error(ex);
                })
        }
    }, [open, authToken]);

    const convertOptionalValues = (value: string) => {
        if (value === "" || parseInt(value) < 1) {
            return null;
        }
        return parseInt(value);
    }


    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        setData({
            ...data,
            [name]: type === 'checkbox' ? (checked ? 'ACTIVE' : 'INACTIVE') : (name === 'stopLoss' || name === 'takeProfit') ? convertOptionalValues(value) : value
        });
    };

    const handleSubmit = () => {
        createManager(data, authToken as string)
            .then((id) => {
                onCreate()
            })
            .catch((ex) => {
                console.log('Failed to create Manager');
                console.log(ex);
            });
    }

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title">Add Manager</DialogTitle>
            <DialogContent>
                <FormControl fullWidth margin="dense">
                    <InputLabel id="api-token-select-label">Api Token</InputLabel>
                    <Select
                        labelId="api-token-select-label"
                        id="api-token-select"
                        value={data.apiTokenId}
                        onChange={(event) => handleChange(event as React.ChangeEvent<HTMLInputElement>)}
                        name="apiTokenId"
                    >
                        {tokens.map((option) => (
                            <MenuItem key={option.id}
                                      value={option.id}>{option.id.toString() + ' | ' + option.apiKey.toString() + ' | ' + option.market.toString() + ' | ' + (option.test ? 'Test Account' : '')}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <TextField
                    margin="dense"
                    name="customAnalyzerId"
                    label="Custom Name (Optional)"
                    type="text"
                    fullWidth
                    value={data.customAnalyzerId}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="stopLoss"
                    label="Take Profit (Optional)"
                    type="number"
                    fullWidth
                    value={data.stopLoss}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="takeProfit"
                    label="Take Profit (Optional)"
                    type="number"
                    fullWidth
                    value={data.takeProfit}
                    onChange={handleChange}
                />
                <FormControlLabel
                    control={<Switch checked={data.status === 'ACTIVE'} onChange={handleChange} name="status"/>}
                    label="Active"
                />
            </DialogContent>
            <DialogActions>
                <Button variant='contained' onClick={onClose} color="error">
                    Cancel
                </Button>
                <Button variant='contained' onClick={handleSubmit} color="primary" disabled={data.apiTokenId === null}>
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default CreateManagerDialog;
