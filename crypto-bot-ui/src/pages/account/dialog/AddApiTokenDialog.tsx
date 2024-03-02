import React, {useEffect, useState} from "react";
import {AuthInfo} from "../../../model/AuthInfo";
import {ApiToken} from "../../../model/ApiToken";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, styled} from "@mui/material";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import Select from 'react-select';
import "../../../css/pages/account/dialog/ApiTokenDialogStyles.css";
import {addApiToken} from "../../../service/AccountService";
import {errorToast} from "../../../shared/toast/Toasts";
import {AntSwitch, SelectStyle} from "../../../utils/styles/element-styles";
import {UnauthorizedError} from "../../../utils/errors/UnauthorizedError";
import {useLoader} from "../../../context/LoaderContext";

interface AddApiTokenDialogProps {
    authInfo: AuthInfo;
    logout: () => void;
    open: boolean;
    onClose: () => void;
    onCreate: (apiToken: ApiToken) => void;
}

const FieldContainer = styled('div')({
    display: 'flex',
    flexDirection: 'column'
});

const AddApiTokenDialog: React.FC<AddApiTokenDialogProps> = ({authInfo, logout, open, onClose, onCreate}) => {
    const {showBannerLoader, hideLoader} = useLoader();
    const [apiTokenData, setData] = useState<{
        market: string;
        apiKey: string;
        secretKey: string;
        demo: boolean;
    }>({
        market: "",
        apiKey: "",
        secretKey: "",
        demo: false
    });

    useEffect(() => {
        return () => {
            setData({
                market: "",
                apiKey: "",
                secretKey: "",
                demo: false
            });
        }
    }, [onClose]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        setData({
            ...apiTokenData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = () => {
        onClose();
        showBannerLoader();
        addApiToken(apiTokenData, authInfo)
            .then((id) => {
                hideLoader();
                onCreate({
                    id: id as string,
                    apiKey: apiTokenData.apiKey as string,
                    market: apiTokenData.market,
                    test: apiTokenData.demo as boolean
                })
            })
            .catch((ex) => {
                hideLoader();
                errorToast("Failed to add API token");
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
    };

    const validateFields = apiTokenData.market !== "" && apiTokenData.apiKey !== "" && apiTokenData.secretKey !== "";

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title"
                PaperProps={{
                    style: {
                        backgroundColor: '#121417',
                        borderRadius: '4px',
                        boxShadow: 'none',
                        color: 'white',
                        fontWeight: '400',
                        width: '380px',
                        height: '440px',
                        position: 'relative'
                    }
                }}
        >
            <CrossIcon style={{
                display: 'flex',
                alignSelf: 'flex-end',
                fill: 'white',
                width: '24px',
                height: '24px',
                marginRight: '16px',
                marginTop: '16px',
                cursor: 'pointer'
            }} onClick={onClose}/>
            <DialogTitle style={{marginLeft: '16px'}}>
                Add Api Token
            </DialogTitle>
            <DialogContent style={{marginLeft: '16px'}}>
                <FieldContainer>
                    <div style={{fontSize: '14px', fontWeight: '200'}}>
                        Market
                    </div>
                    <Select options={[{value: "BYBIT", label: "ByBit"}]}
                            components={{IndicatorSeparator: () => null}}
                            placeholder=""
                            name="market"
                            styles={SelectStyle}
                            onChange={(changedValue) => setData({
                                ...apiTokenData,
                                market: !!changedValue ? (changedValue as any).value : ''
                            })}
                    />
                </FieldContainer>
                <FieldContainer style={{marginTop: '16px'}}>
                    <div style={{fontSize: '14px', fontWeight: '200'}}>
                        API Key
                    </div>
                    <input type="text" name="apiKey" style={{
                        borderRadius: '4px',
                        height: '34px',
                        boxShadow: 'none',
                        border: 0,
                        backgroundColor: '#262B31',
                        color: 'white',
                        padding: '8px',
                        fontSize: '14px',
                        fontWeight: '200',
                    }}
                           value={apiTokenData.apiKey}
                           onChange={handleChange}/>
                </FieldContainer>
                <FieldContainer style={{marginTop: '16px'}}>
                    <div style={{fontSize: '14px', fontWeight: '200'}}>
                        Secret Key
                    </div>
                    <input type="password" name="secretKey" style={{
                        borderRadius: '4px',
                        height: '34px',
                        boxShadow: 'none',
                        border: 0,
                        backgroundColor: '#262B31',
                        color: 'white',
                        padding: '8px',
                        fontSize: '14px',
                        fontWeight: '200'
                    }}
                           value={apiTokenData.secretKey}
                           onChange={handleChange}/>
                </FieldContainer>
                <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start', marginTop: '16px'}}>
                    <AntSwitch onChange={handleChange} value={apiTokenData.demo} name="demo"/>
                    <div style={{marginLeft: '8px', fontWeight: 200, fontSize: '14px'}}>Market's Demo Account</div>
                </div>
            </DialogContent>
            <DialogActions style={{paddingBottom: '32px', paddingRight: '32px'}}>
                <Button variant='outlined' style={{textTransform: 'none'}} onClick={onClose} color="error">
                    Cancel
                </Button>
                <Button variant={validateFields ? 'contained' : 'outlined'} onClick={handleSubmit}
                        disabled={!validateFields}
                        style={{
                            textTransform: 'none',
                            borderColor: validateFields ? 'inherit' : '#D0FF12',
                            backgroundColor: validateFields ? '#D0FF12' : 'inherit',
                            color: validateFields ? '#121417' : '#D0FF12'
                        }} color="primary">
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export default AddApiTokenDialog;
