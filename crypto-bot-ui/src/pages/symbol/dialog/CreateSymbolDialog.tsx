import React, {useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField} from "@mui/material";
import {SymbolModel} from "../../../model/SymbolModel";

interface CreateSymbolDialogProps {
    open: boolean;
    onClose: () => void;
    onCreate: (analyzerData: SymbolModel) => void;
}

const CreateSymbolDialog: React.FC<CreateSymbolDialogProps> = ({ open, onClose, onCreate }) => {
    const [symbolData, setData] = useState<SymbolModel>({
        symbol: ''
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type, checked } = e.target;
        setData({
            ...symbolData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = () => {
        onCreate(symbolData);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title">Create New Analyzer</DialogTitle>
            <DialogContent>
                <TextField
                    margin="dense"
                    name="symbol"
                    label="Symbol"
                    type="text"
                    fullWidth
                    value={symbolData.symbol}
                    onChange={handleChange}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color="primary">
                    Cancel
                </Button>
                <Button onClick={handleSubmit} color="primary">
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default CreateSymbolDialog;
