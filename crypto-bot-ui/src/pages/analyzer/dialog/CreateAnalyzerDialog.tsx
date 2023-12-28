import {AnalyzerModel} from "../../../model/AnalyzerModel";
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select,
    Switch,
    TextField
} from '@mui/material';
import React, {useEffect, useState} from "react";
import {fetchSymbolsNameList} from "../../../service/SymbolService";


interface CreateAnalyzerDialogProps {
    open: boolean;
    onClose: () => void;
    onCreate: (analyzerData: AnalyzerModel) => void;
}

const CreateAnalyzerDialog: React.FC<CreateAnalyzerDialogProps> = ({ open, onClose, onCreate }) => {
    const [symbols, setSymbols] = useState<string[]>([])
    const [analyzerData, setAnalyzerData] = useState<AnalyzerModel>({
        public: false,
        diapason: 0,
        gridSize: 0,
        multiplayer: 0,
        stopLoss: 0,
        takeProfit: 0,
        symbol: '',
        startCapital: 0,
        active: false,
    });

    useEffect(() => {
        fetchSymbolsNameList()
            .then(data => setSymbols(data)) // TODO: Add error handling
    }, []);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type, checked } = e.target;
        setAnalyzerData({
            ...analyzerData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = () => {
        onCreate(analyzerData);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title">Create New Analyzer</DialogTitle>
            <DialogContent>
                <FormControl fullWidth margin="dense">
                    <InputLabel id="symbol-select-label">Symbol</InputLabel>
                    <Select
                        labelId="symbol-select-label"
                        id="symbol-select"
                        value={analyzerData.symbol}
                        onChange={(event) => handleChange(event as React.ChangeEvent<HTMLInputElement>)}
                        name="symbol"
                    >
                        {symbols.map((option) => (
                            <MenuItem key={option} value={option}>{option}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <TextField
                    margin="dense"
                    name="diapason"
                    label="Diapason"
                    type="number"
                    fullWidth
                    value={analyzerData.diapason}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="gridSize"
                    label="Grid Size"
                    type="number"
                    fullWidth
                    value={analyzerData.gridSize}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="multiplayer"
                    label="Multiplayer"
                    type="number"
                    fullWidth
                    value={analyzerData.multiplayer}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="stopLoss"
                    label="Stop Loss"
                    type="number"
                    fullWidth
                    value={analyzerData.stopLoss}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="takeProfit"
                    label="Take Profit"
                    type="number"
                    fullWidth
                    value={analyzerData.takeProfit}
                    onChange={handleChange}
                />
                <TextField
                    margin="dense"
                    name="startCapital"
                    label="Start Capital"
                    type="number"
                    fullWidth
                    value={analyzerData.startCapital}
                    onChange={handleChange}
                />
                <FormControlLabel
                    control={<Switch checked={analyzerData.public} onChange={handleChange} name="public" />}
                    label="Public"
                />
                <FormControlLabel
                    control={<Switch checked={analyzerData.active} onChange={handleChange} name="active" />}
                    label="Active"
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
};

export default CreateAnalyzerDialog;
