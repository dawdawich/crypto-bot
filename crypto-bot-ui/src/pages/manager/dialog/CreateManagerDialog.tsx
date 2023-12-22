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

interface AddApiTokenDialogProps {
    open: boolean;
    onClose: () => void;
    onCreate: () => void;
}

const CreateManagerDialog: React.FC<AddApiTokenDialogProps> = ({open, onClose, onCreate}) => {
    const [tokens, setTokens] = useState<ApiToken[]>([])
    const [data, setData] = useState<{
        customAnalyzerId: string;
        active: boolean;
        apiTokenId: string | null;
    }>({
        customAnalyzerId: "",
        active: false,
        apiTokenId: null
    })
    const [, navigate] = useLocation();
    const authToken = localStorage.getItem('auth.token');

    if (!authToken) {
        navigate('/');
        window.location.reload();
    }

    let fetchTokens = async () => {
        try {
            const res = await getApiTokens(authToken as string)
            setTokens(res);
        } catch(ex) {
            console.error('Failed to fetch user\'s tokens.')
            console.error(ex);
        }
    }

    useEffect(() => {
        if (open) {
            setData({
                customAnalyzerId: "",
                active: false,
                apiTokenId: null
            });
            fetchTokens();
        }
    }, [open]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type, checked } = e.target;
        setData({
            ...data,
            [name]: type === 'checkbox' ? checked : value
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
                        name="api-token"
                    >
                        {tokens.map((option) => (
                            <MenuItem key={option.id} value={option.id}>{option.id + ' ' + option.apiKey + ' ' + option.market + ' ' + option.test ? 'Test Account' : ''}</MenuItem>
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
                <FormControlLabel
                    control={<Switch checked={data.active} onChange={handleChange} name="test" />}
                    label="Active"
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color="primary">
                    Cancel
                </Button>
                <Button onClick={handleSubmit} color="primary" disabled={data.apiTokenId === null}>
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default CreateManagerDialog;
