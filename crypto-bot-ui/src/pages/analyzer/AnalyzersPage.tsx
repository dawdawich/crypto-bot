import React, {CSSProperties, useEffect, useState} from "react";
import "../../css/pages/analyzer/AnalyzerPageStyles.css";
import "../../css/pages/LoginBanner.css";
import {Button, Divider, Menu, MenuItem, styled, Typography} from "@mui/material";
import {Route, Switch, useLocation} from "wouter";
import {ReactComponent as FlagIcon} from "../../assets/images/analyzer/flag-icon.svg";
import {ReactComponent as MetamaskIcon} from "../../assets/images/account/metamask-icon.svg";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import AnalyzerContent from "./AnalyzerContent";
import {useAuth} from "../../context/AuthContext";
import {FolderModel} from "../../model/FolderModel";
import {fetchFolderList} from "../../service/FolderService";
import {errorToast} from "../../shared/toast/Toasts";
import FolderDialog, {FolderActionType} from "./dialog/FolderDialog";
import {ReactComponent as MenuIcon} from "../../assets/images/analyzer/menu-icon.svg";
import AnalyzerDetailContent from "./AnalyzerDetailContent";
import {UnauthorizedError} from "../../utils/errors/UnauthorizedError";

const topHoverStyle = {
    background: '#1D2024',
};

const folderHoverStyle = {
    padding: '4px',
    borderRadius: '4px',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    overflowY: 'hidden',
    background: '#1D2024',
    cursor: 'pointer'
};

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '700',
    paddingTop: '24px',
    marginLeft: '16px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const StyledMenuItem = styled(MenuItem)(
    {
        '&:hover': {
            backgroundColor: "#2D323A"
        },
        marginLeft: '8px',
        marginRight: '8px',
        borderRadius: '4px',
        color: 'white'
    }
)

const AnalyzersPage: React.FC = () => {
    const [analyzerIdsToAdd, setAnalyzerIdsToAdd]
        = useState<{ ids: string[], all: boolean }>({ids: [], all: false});
    const [actionType, setActionType] = useState<FolderActionType>('create');
    const [currentFolder, setCurrentFolder] = useState<FolderModel | null>(null);
    const [selectedFolder, setSelectedFolder] = useState<FolderModel | null>(null);
    const [folders, setFolders] = useState<FolderModel[]>([]);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [location, navigate] = useLocation();
    const {authInfo, login, logout} = useAuth();
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const menuOpen = Boolean(anchorEl);

    document.title = 'Analyzers';

    if (location === '/analyzer') {
        if (!!authInfo) {
            navigate('/analyzer/folder/all');
        } else {
            navigate('/analyzer/folder/top');
        }
    }

    useEffect(() => {
        if (!!authInfo) {
            fetchFolderList(authInfo)
                .then((folders) => setFolders(folders))
                .catch(ex => {
                    errorToast("Failed to load folders list");
                    if (ex instanceof UnauthorizedError) {
                        logout();
                    }
                });
        } else {
            setFolders([])
        }
    }, [authInfo, logout]);

    const handleNewFolder = (folder: FolderModel) => {
        setFolders([...folders, folder]);
    }

    const handleDeleteFolder = (folder: FolderModel) => {
        setFolders([...folders.filter(el => el.id !== folder.id)]);
    }

    const handleRenameFolder = (folder: FolderModel) => {
        folders.find(el => el.id === folder.id)!.name = folder.name
    }

    const openCreateFolderDialog = (action: FolderActionType) => {
        setCurrentFolder(selectedFolder);
        setActionType(action);
        setIsDialogOpen(true);
        handleMenuClose();
    }

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, folder: FolderModel) => {
        setSelectedFolder(folder);
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setSelectedFolder(null);
        setAnchorEl(null);
    };

    const addAnalyzersToFolder = (ids: string[], all: boolean = false) => {
        setAnalyzerIdsToAdd({ids, all});
        openCreateFolderDialog('addToFolder');
    }

    return (
        <div className="analyzer-page">
            {!location.includes('/detail') &&
                <div className="analyzer-menu-panel">
                    <div className="analyzer-menu-panel-tabs">
                        <div className="analyzer-menu-panel-tabs-header"
                             style={location === "/analyzer/folder/top" ? {...topHoverStyle as CSSProperties} : {}}>
                            <FlagIcon/>
                            <Typography onClick={() => navigate("/analyzer/folder/top")} fontSize="14px">
                                Top Public Analyzer
                            </Typography>
                        </div>
                        <Divider color="#1D2024"/>
                        <div className="analyzer-menu-panel-folder-container"
                             style={location === "/analyzer/folder/all" ? {marginTop: '12px', ...folderHoverStyle as CSSProperties} : {marginTop: '12px'}}>
                            {!!authInfo &&
                                <div id="folder"
                                     onClick={() => navigate("/analyzer/folder/all")}>
                                    All Analyzers
                                </div>
                            }
                        </div>
                        {
                            folders.map(folder => (
                                <div className="analyzer-menu-panel-folder-container"
                                     style={location === `/analyzer/folder/${folder.id}` ? {...folderHoverStyle as CSSProperties} : {}}
                                >
                                    <div id="folder"
                                         onClick={() => navigate(`/analyzer/folder/${folder.id}`)}>
                                        {folder.name}
                                    </div>
                                    <MenuIcon className="analyzer-folder-menu-hover"
                                              onClick={(event: React.MouseEvent<HTMLElement>) => {
                                                  handleMenuClick(event, folder)
                                              }}/>
                                </div>
                            ))
                        }
                    </div>
                    <div className="analyzer-menu-panel-button">
                        {!!authInfo &&
                            <Button variant="outlined"
                                    onClick={() => openCreateFolderDialog('create')}
                                    style={{
                                        borderColor: '#D0FF12',
                                        color: '#D0FF12',
                                        textTransform: 'none',
                                        fontWeight: 700
                                    }}>New Folder</Button>}
                    </div>
                </div>
            }
            <div className="analyzer-content">
                <Switch>
                    <Route path="/analyzer/folder/:folderId">
                        {(params) => {
                            const folderName = folders.find(folder => folder.id === params.folderId)?.name;
                            return <AnalyzerContent folderId={params.folderId}
                                                    folderName={folderName}
                                                    addAnalyzersToFolder={addAnalyzersToFolder}
                                                    folders={folders}
                            />;
                        }}
                    </Route>
                    <Route path="/analyzer/detail/:analyzerId">
                        {(params) => {
                            return <AnalyzerDetailContent analyzerId={params.analyzerId}
                                                          folderDialogStatus={isDialogOpen}
                                                          addAnalyzerToFolder={addAnalyzersToFolder}
                            />;
                        }}
                    </Route>
                    <Route path="/analyzer">
                        <CurrentPath>Analyzer</CurrentPath>
                    </Route>
                </Switch>
                {
                    !authInfo &&
                    <div className="login-banner">
                        <div className="login-banner-content">
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                color: 'white',
                                fontSize: '14px',
                                fontWeight: 400
                            }}>
                                <MetamaskIcon style={{width: '36px', height: '34px', marginRight: '16px'}}/>
                                Connect your wallet to unlock full access
                            </div>
                            <Button variant="contained"
                                    onClick={() => login()}
                                    style={{
                                        backgroundColor: '#D0FF12',
                                        color: '#121417',
                                        textTransform: 'none',
                                        fontWeight: 700
                                    }}>Connect Wallet</Button>
                        </div>
                    </div>
                }
            </div>
            <FolderDialog actionType={actionType} authInfo={authInfo!} logout={logout} currentFolder={currentFolder}
                          currentFolderList={folders} open={isDialogOpen} onClose={() => setIsDialogOpen(false)}
                          onCreate={handleNewFolder} onDelete={handleDeleteFolder} onRename={handleRenameFolder}
                          analyzerIds={analyzerIdsToAdd.ids} allAnalyzers={analyzerIdsToAdd.all}/>
            <Menu
                anchorEl={anchorEl}
                open={menuOpen}
                onClose={handleMenuClose}
                sx={{
                    "& .MuiPaper-root": {
                        backgroundColor: "#262B31",
                        borderRadius: '4px',
                        borderColor: '#555C68',
                        color: 'white'
                    },
                }}
            >
                <StyledMenuItem onClick={() => openCreateFolderDialog('rename')}>Rename</StyledMenuItem>
                <StyledMenuItem onClick={() => openCreateFolderDialog('delete')}
                                style={{color: 'red'}}>Delete</StyledMenuItem>
            </Menu>
        </div>
    );
}

export default AnalyzersPage;
