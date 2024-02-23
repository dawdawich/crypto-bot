import React, {useCallback, useEffect, useState} from "react";
import {useLocation} from "wouter";
import {Button, styled} from "@mui/material";
import {ReactComponent as ActiveIcon} from "../../assets/images/analyzer/active-icon.svg";
import {ReactComponent as NotActiveIcon} from "../../assets/images/analyzer/not-active-icon.svg";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import "../../css/pages/analyzer/AnalyzerDetailStyles.css";
import {AnalyzerResponse} from "../../model/AnalyzerResponse";
import {
    changeBulkAnalyzerStatus,
    deleteAnalyzerBulk,
    fetchAnalyzerData,
    resetAnalyzerBulk
} from "../../service/AnalyzerService";
import {useAuth} from "../../context/AuthContext";
import {errorToast, successToast} from "../toast/Toasts";
import {formatDate} from "../../utils/date-utils";
import {getMarketOptionFromValue, getStrategyOptionFromValue} from "../../model/AnalyzerConstants";
import {FolderModel} from "../../model/FolderModel";
import {fetchAnalyzerFolders, removeAnalyzerFromFolder} from "../../service/FolderService";
import {getWebSocketAnalyzerService, WebSocketService} from "../../service/WebSocketService";
import {AnalyzerRuntimeModel} from "../../model/AnalyzerRuntimeModel";

interface AnalyzerDetailProps {
    analyzerId: string;
    addAnalyzerToFolder: (ids: string[]) => void;
    folderDialogStatus: boolean;
}

const PreviousPath = styled('div')({
    font: plexFont,
    color: "#868F9C",
    fontSize: 20,
    fontWeight: '100',
    paddingTop: '24px',
    paddingLeft: '16px',
    cursor: 'pointer'
});

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '300',
    paddingTop: '24px',
    paddingLeft: '4px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const LeftItemDiv = styled('div')({
    width: '60%', color: '#555C68', fontWeight: 200
});

const RightItemDiv = styled('div')({
    width: '40%', color: 'white', fontWeight: 200, alignItems: 'center', display: 'flex'
});

const AnalyzerDetailContent: React.FC<AnalyzerDetailProps> = ({
                                                                  analyzerId,
                                                                  addAnalyzerToFolder,
                                                                  folderDialogStatus
                                                              }) => {
    const [analyzer, setAnalyzer] = useState<AnalyzerResponse>();
    const [analyzerRuntimeInfo, setAnalyzerRuntimeInfo] = useState<AnalyzerRuntimeModel | null>(null);
    const [analyzerFolders, setAnalyzerFolders] = useState<FolderModel[]>([]);
    const [webSocket] = useState<WebSocketService>(getWebSocketAnalyzerService());
    const {authInfo} = useAuth();
    const [, navigate] = useLocation();

    if (!authInfo) {
        navigate('/analyzer');
    }

    const updateFoldersList = useCallback((analyzerId: string) => {
        fetchAnalyzerFolders(authInfo!, analyzerId)
            .then((folders) => setAnalyzerFolders(folders))
            .catch(() => errorToast('Failed to fetch folder list'));
    }, [authInfo]);

    useEffect(() => {
        if (!folderDialogStatus) {
            updateFoldersList(analyzerId);
        }
    }, [analyzerId, folderDialogStatus, updateFoldersList]);

    useEffect(() => {
        fetchAnalyzerData(authInfo!, analyzerId)
            .then((response) => {
                setAnalyzer(response);
            })
            .catch(() => {
                errorToast('Failed to fetch analyzer info');
            });
    }, [authInfo, analyzerId]);

    useEffect(() => {
        if (!!analyzer && analyzer.isActive && !webSocket.isOpen()) {
            let intervalId: NodeJS.Timeout;
            webSocket.connect((message) => {
                setAnalyzerRuntimeInfo(JSON.parse(message));
            });
            intervalId = setInterval(() => {
                webSocket.sendMessage(JSON.stringify({id: analyzer.id}));
            }, 5000);
            webSocket.onDisconnect = () => {
                setAnalyzerRuntimeInfo(null);
                clearTimeout(intervalId);
            };
        }

        if (!!analyzer && !analyzer.isActive && webSocket.isOpen()) {
            webSocket.disconnect();
        }

        return () => {
            if (webSocket.isOpen()) {
                webSocket.disconnect();
            }
        }
    }, [analyzer, webSocket]);

    const deleteAnalyzerFromFolder = (analyzerId: string, folderId: string) => {
        removeAnalyzerFromFolder(authInfo!, folderId, analyzerId)
            .then(() => {
                successToast('Analyzer removed from folder');
                updateFoldersList(analyzerId);
            })
            .catch(() => errorToast('Failed to remove analyzer from folder'));
    }

    const copyCurrentAnalyzerId = () => {
        navigator.clipboard.writeText(analyzerId).then(() => {
            successToast("Analyzer ID copied to clipboard");
        });
    };

    const addToFolder = () => {
        addAnalyzerToFolder([analyzerId]);
    };

    const resetAnalyzer = () => {
        resetAnalyzerBulk(authInfo!, [analyzerId])
            .then(() => successToast('Analyzer reset successfully.'))
            .catch(() => errorToast('Failed to reset analyzer'));
    };

    const deleteAnalyzer = () => {
        deleteAnalyzerBulk(authInfo!, [analyzerId])
            .then(() => {
                navigate('/analyzer');
                successToast('Analyzer deleted');
            })
            .catch(() => errorToast('Failed to delete analyzer'));
    };

    const stopAnalyzer = () => {
        changeBulkAnalyzerStatus(authInfo!, [analyzerId], false)
            .then(() => {
                successToast('Analyzer stopped');
                setAnalyzer({...analyzer as AnalyzerResponse, isActive: false});
            })
            .catch(() => errorToast('Failed to stop analyzer'))
    }

    const activateAnalyzer = () => {
        changeBulkAnalyzerStatus(authInfo!, [analyzerId], true)
            .then(() => {
                successToast('Analyzer stopped');
                setAnalyzer({...analyzer as AnalyzerResponse, isActive: true});
            })
            .catch(() => errorToast('Failed to stop analyzer'))
    }

    return (
        <div className="analyzer-detail-content">
            <div className="analyzer-detail-header">
                <div className="analyzer-detail-header-path">
                    <PreviousPath onClick={() => navigate("/analyzer")}>
                        Analyzer /
                    </PreviousPath>
                    <CurrentPath>{analyzerId}</CurrentPath>
                    <Button variant="outlined"
                            onClick={copyCurrentAnalyzerId}
                            style={{
                                textTransform: 'none',
                                border: '1px solid #868F9C',
                                color: '#868F9C',
                                marginLeft: '16px',
                                height: '34px',
                                alignSelf: 'center'
                            }}>Copy ID</Button>
                </div>
                <div className="analyzer-detail-header-actions">
                    <Button variant="outlined"
                            onClick={addToFolder}
                            style={{
                                textTransform: 'none',
                                border: '1px solid #D0FF12',
                                color: '#D0FF12',
                                height: '34px',
                                alignSelf: 'center'
                            }}>Add to the Folder</Button>
                    <Button variant="outlined"
                            onClick={resetAnalyzer}
                            style={{
                                textTransform: 'none',
                                border: '1px solid #D0FF12',
                                color: '#D0FF12',
                                height: '34px',
                                alignSelf: 'center'
                            }}>Restart</Button>
                    <Button variant="outlined"
                            onClick={deleteAnalyzer}
                            style={{
                                textTransform: 'none',
                                border: '1px solid #E7323B',
                                color: '#E7323B',
                                height: '34px',
                                alignSelf: 'center'
                            }}>Delete</Button>
                    {   analyzer?.isActive ?
                        <Button variant="contained"
                             onClick={stopAnalyzer}
                             style={{
                                 textTransform: 'none',
                                 backgroundColor: '#E7323B',
                                 color: 'white',
                                 height: '34px',
                                 alignSelf: 'center'
                             }}>Stop</Button> :
                        <Button variant="contained"
                                onClick={activateAnalyzer}
                                style={{
                                    textTransform: 'none',
                                    backgroundColor: '#16C079',
                                    color: 'white',
                                    height: '34px',
                                    alignSelf: 'center'
                                }}>Activate</Button>
                    }
                </div>
            </div>
            <div className="analyzer-detail-cards-content">
                <div className="analyzer-detail-card">
                    <div className="analyzer-detail-card-header">
                        Analyzer setup
                    </div>
                    {
                        !!analyzer &&
                        <div className="analyzer-detail-card-content">
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Start capital
                                </LeftItemDiv>
                                <RightItemDiv>
                                    $ {analyzer.startCapital}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Diapason
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.diapason}%
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Grid Size
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.gridSize}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Symbol
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.symbol}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Leverage
                                </LeftItemDiv>
                                <RightItemDiv>
                                    x {analyzer.multiplier}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Stop Loss
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.positionStopLoss}%
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Take Profit
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.positionTakeProfit}%
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Strategy
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {getStrategyOptionFromValue(analyzer.strategy)?.label}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Crypto market
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {getMarketOptionFromValue(analyzer.market)?.label}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Demo Account
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {analyzer.demoAccount ? 'Yes' : 'No'}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Created Date
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {formatDate(analyzer.createTime)}
                                </RightItemDiv>
                            </div>
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv>
                                    Updated Date
                                </LeftItemDiv>
                                <RightItemDiv>
                                    {formatDate(analyzer.updateTime)}
                                </RightItemDiv>
                            </div>
                        </div>}
                </div>
                <div className="analyzer-detail-card">
                    <div className="analyzer-detail-card-header">
                        Working progress
                    </div>
                    <div className="analyzer-detail-card-content">
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Status
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive ? <ActiveIcon style={{marginRight: '4px'}}/> : <NotActiveIcon
                                    style={{marginRight: '4px'}}/>} {analyzer?.isActive ? '' : 'Not'} Active
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Current equity
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? `$ ${analyzerRuntimeInfo.money}` : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                1h %
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? `20%` : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                6h %
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? `20%` : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                24h %
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? `20%` : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Stability
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo && analyzerRuntimeInfo.stability ? `${analyzerRuntimeInfo.stability}` : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Position Directions
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? !!analyzerRuntimeInfo.positionDirection ? analyzerRuntimeInfo.positionDirection : 'Not open' : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Position Entry Price
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? !!analyzerRuntimeInfo.positionEntryPrice ? analyzerRuntimeInfo.positionEntryPrice : 'Not open' : '-'}
                            </RightItemDiv>
                        </div>
                        <div className="analyzer-detail-card-content-item">
                            <LeftItemDiv>
                                Position Size
                            </LeftItemDiv>
                            <RightItemDiv>
                                {analyzer?.isActive && !!analyzerRuntimeInfo ? !!analyzerRuntimeInfo.positionSize ? analyzerRuntimeInfo.positionSize : 'Not open' : '-'}
                            </RightItemDiv>
                        </div>
                    </div>
                </div>
                <div className="analyzer-detail-card">
                    <div className="analyzer-detail-card-header">
                        Active folders
                    </div>
                    <div className="analyzer-detail-card-content">
                        {analyzerFolders.map((folder) => (
                            <div className="analyzer-detail-card-content-item">
                                <LeftItemDiv style={{color: 'white'}}>
                                    {folder.name}
                                </LeftItemDiv>
                                <RightItemDiv style={{color: 'red', cursor: 'pointer'}}
                                              onClick={() => deleteAnalyzerFromFolder(analyzerId, folder.id)}>
                                    Remove
                                </RightItemDiv>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default AnalyzerDetailContent;
