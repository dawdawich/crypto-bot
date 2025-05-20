import React, {useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import '../../../css/pages/backtest/dialog/CreateBackTestDialogStyle.css';
import Select from "react-select";
import {MultiSelectStyle} from "../../../utils/styles/element-styles";
import {InputField} from "../../../shared/InputComponents";
import {createBackTest} from "../../../service/BackTestService";
import {AuthInfo} from "../../../model/AuthInfo";
import {errorToast} from "../../../shared/toast/Toasts";

interface CreateBackTestDialogProps {
    open: boolean;
    onClose: () => void;
    onCloseWithRequestId: (requestId: string) => void;
    symbols: string[];
    authInfo: AuthInfo;
}

export type BacktestRequestModel = {
    symbols: string[];
    startCapital: number | undefined;
}

const submitData = (requestModel: BacktestRequestModel, authInfo: AuthInfo, onClose: (requestId: string) => void) => {
    createBackTest(requestModel, authInfo)
        .then(result => {
            onClose(result.requestId);
        })
        .catch((ex) => {
            errorToast("Failed to create backtest");
            console.error(ex);
        });
}

const CreateBackTestDialog: React.FC<CreateBackTestDialogProps> = ({open, onClose, symbols, onCloseWithRequestId, authInfo}) => {
    const [backtestRequestModel, setBacktestRequestModel] = useState<BacktestRequestModel>({
        symbols: [],
        startCapital: undefined
    });

    const handleSelectChange = (name: string, value: any) => {
        setBacktestRequestModel({
            ...backtestRequestModel,
            [name]: Array.isArray(value) ? value.map(el => el.value) : value
        });
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        setBacktestRequestModel({
            ...backtestRequestModel,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    return (
        <Dialog open={open} onClose={onClose} PaperProps={{
            style: {
                backgroundColor: '#121417',
                borderRadius: '4px',
                boxShadow: 'none',
                color: 'white',
                fontWeight: '400',
                width: '690px',
                height: '780px',
                position: 'relative',
                padding: '16px'
            }
        }}>
            <CrossIcon className="cross-icon-button" onClick={onClose}/>
            <DialogTitle>New Backtest</DialogTitle>
            <DialogContent>
                <div className="field-container">
                    Symbols
                    <Select
                        styles={MultiSelectStyle}
                        placeholder=""
                        name="symbol"
                        isMulti={true}
                        isSearchable={true}
                        value={backtestRequestModel.symbols.map(el => ({value: el, label: el}))}
                        onChange={(newValue) => handleSelectChange("symbols", newValue)}
                        options={symbols.map(el => ({value: el, label: el}))}
                    />
                </div>
                <div className="field-container">
                    Start Capital, $
                    <InputField
                        error={!backtestRequestModel.startCapital}
                        type="number"
                        name="startCapital"
                        value={backtestRequestModel.startCapital}
                        onChange={handleChange}/>
                </div>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} style={{
                    textTransform: 'none',
                    backgroundColor: '#121417',
                    color: '#D0FF12',
                    fontWeight :700
                }}>
                    Cancel
                </Button>
                <Button onClick={() => {submitData(backtestRequestModel, authInfo, onCloseWithRequestId)}} style={{
                    textTransform: 'none',
                    backgroundColor: '#D0FF12',
                    color: '#121417',
                    fontWeight :700
                }}>
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default CreateBackTestDialog;
