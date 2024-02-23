import React, {useEffect, useRef, useState} from "react";
import {AuthInfo} from "../../../model/AuthInfo";
import {ApiToken} from "../../../model/ApiToken";
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    styled,
    Switch
} from "@mui/material";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import loadingSpinner from '../../../assets/images/loading-spinner.svga';
import Select from 'react-select';
import "../../../css/pages/account/dialog/ApiTokenDialogStyles.css";
import {addApiToken} from "../../../service/AccountService";
import {errorToast} from "../../toast/Toasts";
import {AntSwitch, SelectStyle} from "../../../utils/styles/element-styles";

interface AddApiTokenDialogProps {
    authInfo: AuthInfo;
    open: boolean;
    onClose: () => void;
    onCreate: (apiToken: ApiToken) => void;
}

const FieldContainer = styled('div')({
    display: 'flex',
    flexDirection: 'column'
});

const AddApiTokenDialog: React.FC<AddApiTokenDialogProps> = ({authInfo, open, onClose, onCreate}) => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [animation, setAnimation] = useState('');
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

    useEffect(() => {
        fetch(loadingSpinner)
            .then(response => response.text())
            .then(text => {
                setAnimation(text)
                if (spanRef.current) {
                    spanRef.current.innerHTML = animation
                }
            });
    }, [animation, isLoading]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        setData({
            ...apiTokenData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = () => {
        setIsLoading(true);
        addApiToken(apiTokenData, authInfo)
            .then((id) => {
                setTimeout(() => {
                    setIsLoading(false);
                    onCreate({
                        id: id as string,
                        apiKey: apiTokenData.apiKey as string,
                        market: apiTokenData.market,
                        test: apiTokenData.demo as boolean
                    })
                    onClose();
                }, 500);
            })
            .catch((ex) => {
                setIsLoading(false);
                errorToast("Failed to add API token");
                console.error(ex);
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
            {isLoading &&
                <div style={{
                    position: 'absolute',
                    zIndex: 1,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    width: '100%',
                    height: '100%'
                }}>
                    <span style={{
                        width: '200px'
                    }} ref={spanRef}/>
                </div>
            }
        </Dialog>
    );
}

export default AddApiTokenDialog;
