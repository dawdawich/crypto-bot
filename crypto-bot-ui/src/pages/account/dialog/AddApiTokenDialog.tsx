import React, {useState} from "react";
import {ApiToken} from "../../../model/ApiToken";
import {
    Button, Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select, Switch,
    TextField
} from "@mui/material";
import {addApiToken} from "../../../service/AccountService";
import {useLocation} from "wouter";
import {AuthInfo} from "../../../model/AuthInfo";

interface AddApiTokenDialogProps {
    authInfo: AuthInfo;
    open: boolean;
    onClose: () => void;
    onCreate: (apiToken: ApiToken) => void;
}

const AddApiTokenDialog: React.FC<AddApiTokenDialogProps> = ({ authInfo, open, onClose, onCreate }) => {
    const markets = ["ByBit"];

    const [apiTokenData, setData] = useState<{
        market: string;
        apiKey: string | null;
        secretKey: string | null;
        test: boolean | null;
    }>({
        market: "BYBIT",
        apiKey: null,
        secretKey: null,
        test: null
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type, checked } = e.target;
        setData({
            ...apiTokenData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = () => {
        addApiToken(apiTokenData, authInfo)
            .then((id) => {
                onCreate({
                    id: id as string,
                    apiKey: apiTokenData.apiKey as string,
                    market: apiTokenData.market,
                    test: apiTokenData.test as boolean
                })
                onClose();
            })
            .catch((ex) => {
                console.error(ex);
            })
    };

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title">Add Api Token</DialogTitle>
            <DialogContent>
                <FormControl fullWidth margin="dense">
                    <InputLabel id="market-select-label">Market</InputLabel>
                    <Select
                        labelId="market-select-label"
                        id="market-select"
                        value={apiTokenData.market}
                        onChange={(event) => handleChange(event as React.ChangeEvent<HTMLInputElement>)}
                        name="market"
                    >
                        {markets.map((option) => (
                            <MenuItem key={option} value={option.toUpperCase()}>{option}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <FormControlLabel
                    control={<Switch checked={apiTokenData.test === true} onChange={handleChange} name="test" />}
                    label="Market's Test Account"
                />
                <TextField
                    margin="dense"
                    name="apiKey"
                    label="API Key"
                    type="text"
                    fullWidth
                    value={apiTokenData.apiKey}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="secretKey"
                    label="Secret Key"
                    type="password"
                    fullWidth
                    value={apiTokenData.secretKey}
                    onChange={handleChange}
                />
            </DialogContent>
            <DialogActions>
                <Button variant='contained' onClick={onClose} color="error">
                    Cancel
                </Button>
                <Button variant='contained' onClick={handleSubmit} color="primary">
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default AddApiTokenDialog;
