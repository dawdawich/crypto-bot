import React, {useEffect, useRef, useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import {AuthInfo} from "../../../model/AuthInfo";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import Select from "react-select";
import {ApiToken} from "../../../model/ApiToken";
import {AntSwitch, SelectStyle} from "../../../utils/styles/element-styles";
import "../../../css/pages/manager/dialog/CreateManagerDialogStyles.css";
import {FolderModel} from "../../../model/FolderModel";
import {createManager} from "../../../service/ManagerService";
import {errorToast, successToast} from "../../toast/Toasts";
import loadingSpinner from "../../../assets/images/loading-spinner.svga";

interface AddApiTokenDialogProps {
    authInfo: AuthInfo;
    open: boolean;
    onClose: () => void;
    apiTokens: ApiToken[];
    folders: FolderModel[];
}

type ManagerModel = {
    apiTokenId: string | undefined;
    customName: string | undefined;
    analyzerFolderId: string | undefined;
    findAnalyzerStrategy: string | undefined;
    refreshAnalyzerTime: number | undefined;
    stopLoss: number | undefined;
    takeProfit: number | undefined;
    activate: boolean | undefined;
};

const AnalyzerStrategyOption = [
    {
        value: 'BIGGEST_BY_MONEY', label: 'Choose best by money'
    }, {
        value: 'MOST_STABLE', label: 'Choose best by stability'
    },
];

const CreateManagerDialog: React.FC<AddApiTokenDialogProps> = ({authInfo, open, onClose, apiTokens, folders}) => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [animation, setAnimation] = useState('');
    const [managerData, setManagerData] = useState<ManagerModel>({
        apiTokenId: undefined,
        customName: undefined,
        analyzerFolderId: undefined,
        findAnalyzerStrategy: undefined,
        refreshAnalyzerTime: 30,
        stopLoss: undefined,
        takeProfit: undefined,
        activate: false,
    });

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

    const convertTokensToOptions = () =>
        apiTokens.map((token) => ({
            value: token.id, label: `${token.market} - ${token.test ? 'Demo' : 'Main'}`
        }));

    const convertFoldersToOptions = () => folders.map((folder) => ({value: folder.id, label: folder.name}));

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        setManagerData({
            ...managerData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const submitData = () => {
        setIsLoading(true);
        createManager(authInfo!, {
            apiTokenId: managerData.apiTokenId as string,
            customName: !managerData.customName ? null : managerData.customName,
            status: managerData.activate ? 'ACTIVE' : 'INACTIVE',
            analyzerChooseStrategy: managerData.findAnalyzerStrategy as any,
            refreshAnalyzerTime: managerData.refreshAnalyzerTime as number,
            stopLoss: !managerData.stopLoss ? null : managerData.stopLoss,
            takeProfit: !managerData.takeProfit ? null : managerData.takeProfit,
            folder: !managerData.analyzerFolderId ? 'ALL' : managerData.analyzerFolderId
        })
            .then((id) => {
                setIsLoading(false);
                successToast('Manager created successfully');
                onClose();
            })
            .catch(() => {
                setIsLoading(false);
                errorToast('Failed to create manager');
            })
    }

    return (
        <Dialog open={open} onClose={onClose}
                PaperProps={{
                    style: {
                        backgroundColor: '#121417',
                        borderRadius: '4px',
                        boxShadow: 'none',
                        color: 'white',
                        fontWeight: '400',
                        width: '380px',
                        height: '700px',
                        position: 'relative',
                        padding: '16px'
                    }
                }}
        >
            <CrossIcon onClick={onClose} style={{
                display: 'flex',
                alignSelf: 'flex-end',
                fill: 'white',
                width: '24px',
                height: '24px',
                cursor: 'pointer'
            }}/>
            <DialogTitle>
                New manager
            </DialogTitle>
            <DialogContent>
                <div className="field-container">
                    <div className="field-container-label">
                        Api Token
                    </div>
                    <Select options={convertTokensToOptions()}
                            styles={SelectStyle}
                            onChange={(newValue) => setManagerData({...managerData, apiTokenId: (newValue as any).value})}
                    />
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Custom Name
                    </div>
                    <input type="text"
                           placeholder="Optional"
                           name="customName" className="input-field"
                           value={managerData.customName}
                           onChange={handleChange}/>
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Analyzer's Folder
                    </div>
                    <Select
                        placeholder="All"
                        options={[{value: 'ALL', label: 'All'}, ...convertFoldersToOptions()]}
                        styles={SelectStyle}
                        onChange={(newValue) => setManagerData({...managerData, analyzerFolderId: (newValue as any).value})}
                    />
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Analyzer Strategy
                    </div>
                    <Select
                        placeholder="Choose best"
                        options={AnalyzerStrategyOption}
                        styles={SelectStyle}
                        onChange={(newValue) => setManagerData({...managerData, findAnalyzerStrategy: (newValue as any).value})}
                    />
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Analyzer Refresh Time, min
                    </div>
                    <input type="number" name="refreshAnalyzerTime" className="input-field"
                           value={managerData.refreshAnalyzerTime}
                           onChange={handleChange}/>
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Stop Loss, %
                    </div>
                    <input type="number" name="stopLoss" className="input-field"
                           value={managerData.stopLoss}
                           placeholder="Optional"
                           onChange={handleChange}/>
                </div>
                <div className="field-container">
                    <div className="field-container-label">
                        Stop Loss, %
                    </div>
                    <input type="number" name="takeProfit" className="input-field"
                           value={managerData.takeProfit}
                           placeholder="Optional"
                           onChange={handleChange}/>
                </div>
            </DialogContent>
            <DialogActions className="action-buttons-container">
                <div style={{display: 'flex', flexDirection: 'row', fontWeight: 200, fontSize: '13px'}}>
                    <AntSwitch value={managerData.activate}
                               onChange={handleChange}
                               name="activate"/> Activate
                </div>
                <Button onClick={submitData} style={{
                    textTransform: 'none',
                    backgroundColor: '#D0FF12',
                    color: '#121417',
                    fontWeight :700
                }}>
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

export default CreateManagerDialog;
