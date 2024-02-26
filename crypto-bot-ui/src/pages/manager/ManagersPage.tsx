import React, {useEffect, useRef, useState} from "react";
import "../../css/pages/manager/ManagersPageStyles.css";
import {
    Button,
    Menu,
    MenuItem,
    styled,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow
} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import {useAuth} from "../../context/AuthContext";
import CreateManagerDialog from "./dialog/CreateManagerDialog";
import {ApiToken} from "../../model/ApiToken";
import {getApiTokens} from "../../service/AccountService";
import {useLocation} from "wouter";
import {errorToast, successToast} from "../toast/Toasts";
import {FolderModel} from "../../model/FolderModel";
import {fetchFolderList} from "../../service/FolderService";
import {ManagerResponse} from "../../model/ManagerResponse";
import {deleteManager, fetchManagersList, updateManagerStatus} from "../../service/ManagerService";
import {ReactComponent as ActiveIcon} from "../../assets/images/analyzer/active-icon.svg";
import {ReactComponent as NotActiveIcon} from "../../assets/images/analyzer/not-active-icon.svg";
import {ReactComponent as CrossActiveIcon} from "../../assets/images/action-icon/cross-icon.svg";
import {RowDiv} from "../../utils/styles/element-styles";
import loadingSpinner from "../../assets/images/loading-spinner.svga";
import {ReactComponent as MenuHeaderIcon} from "../../assets/images/analyzer/menu-header-icon.svg";
import {ReactComponent as MenuIcon} from "../../assets/images/analyzer/menu-icon.svg";

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '300',
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

type FilterType = 'ALL' | 'ACTIVE' | 'INACTIVE' | 'CRASHED';

const ManagersPage: React.FC = () => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const [animation, setAnimation] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const {authInfo} = useAuth();
    const [managers, setManagers] = useState<ManagerResponse[]>([]);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [apiTokens, setApiTokens] = useState<ApiToken[]>([]);
    const [folders, setFolders] = useState<FolderModel[]>([]);
    const [activeFilter, setActiveFilter] = useState<FilterType>('ALL');
    const [currentIndex, setCurrentIndex] = useState(-1);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);
    const [, navigate] = useLocation();

    if (!authInfo) {
        navigate('/analyzer');
    }

    document.title = 'Managers';

    useEffect(() => {
        getApiTokens(authInfo!)
            .then((tokens) => setApiTokens(tokens))
            .catch(() => errorToast('Failed to fetch API tokens'));
        fetchFolderList(authInfo!)
            .then((foldersResponse) => setFolders(foldersResponse))
            .catch(() => errorToast('Failed to fetch folders list'));
        fetchManagersList(authInfo!)
            .then((managers) => setManagers(managers))
            .catch(() => errorToast('Failed to fetch managers'));
    }, [authInfo]);

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

    const getFilterStyleClass = (type: FilterType) => type === activeFilter ? 'managers-filters-cell-active' : 'managers-filters-cell';

    const changeManagerStatus = (manager: ManagerResponse, status: FilterType) => {
        setIsLoading(true);
        updateManagerStatus(authInfo!, manager.id, status)
            .then(() => {
                setIsLoading(false);
                manager.status = status;
                setManagers([...managers]);
                successToast(status === 'ACTIVE' ? 'Manager started successfully' : 'Manager stopped successfully');
            })
            .catch(() => {
                setIsLoading(false);
                errorToast(status === 'ACTIVE' ? 'Failed to start manager' : 'Failed to stop manager');
            });
    }

    const removeManager = (manager: ManagerResponse) => {
        setIsLoading(true);
        deleteManager(authInfo!, manager.id)
            .then(() => {
                setIsLoading(false);
                setManagers([...managers.filter(el => el.id !== manager.id)])
                successToast('Manager deleted successfully');
            })
            .catch(() => errorToast('Failed to delete manager'));
    }

    const changeActiveFilter = (type: FilterType) => {
        if (type !== activeFilter) {
            setActiveFilter(type);
        }
    };

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, index: number) => {
        setCurrentIndex(index);
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setCurrentIndex(-1);
        setAnchorEl(null);
    };

    const getCurrentManager = () => managers[currentIndex];

    const getManagers = () => {
        switch (activeFilter) {
            case "ALL":
                return managers;
            case "ACTIVE":
                return managers.filter((manager) => manager.status === 'ACTIVE');
            case "INACTIVE":
                return managers.filter((manager) => manager.status === 'INACTIVE');
            case "CRASHED":
                return managers.filter((manager) => manager.status === 'CRASHED');
        }
    }

    const getStatusCell = (status: string) => status === 'ACTIVE' ?
        <div className="managers-table-item-status">
            <ActiveIcon/>
            <div style={{marginLeft: '4px'}}>Active</div>
        </div>
        : status === 'INACTIVE' ?
            <div className="managers-table-item-status"
                 style={{
                     paddingLeft: 0,
                     paddingRight: 0,
                     minWidth: '85px'
                 }}>
                <NotActiveIcon/>
                <div style={{marginLeft: '4px'}}>Not Active</div>
            </div>
            :
            <div className="managers-table-item-status"
                 style={{
                     paddingLeft: 0,
                     paddingRight: 0,
                     minWidth: '85px'
                 }}>
                <CrossActiveIcon style={{width: '16px', height: '16px'}}/>
                <div style={{marginLeft: '4px'}}>Crashed</div>
            </div>;

    return (
        <div className="managers-content">
            <CreateManagerDialog folders={folders} apiTokens={apiTokens} authInfo={authInfo!} open={isDialogOpen}
                                 onClose={() => setIsDialogOpen(false)}/>
            <div className="managers-header">
                <div className="managers-header-path">
                    <CurrentPath>Managers</CurrentPath>
                </div>
                <Button onClick={() => setIsDialogOpen(true)} variant='contained'
                        style={{
                            textTransform: 'none',
                            backgroundColor: '#D0FF12',
                            color: '#121417',
                            fontWeight: 700
                        }}
                >
                    New manager
                </Button>
            </div>
            <div className="managers-filters-content">
                <RowDiv>
                    <div
                        className={getFilterStyleClass("ALL")}
                        onClick={() => changeActiveFilter('ALL')}
                    >
                        <div>{managers.length} Managers</div>
                    </div>
                    <div
                        className={getFilterStyleClass('ACTIVE')}
                        onClick={() => changeActiveFilter('ACTIVE')}
                        style={{marginLeft: '4px'}}>
                        <ActiveIcon style={{alignSelf: 'center'}}/>
                        <div>{managers.filter((el) => el.status === 'ACTIVE').length} Active</div>
                    </div>
                    <div
                        className={getFilterStyleClass('INACTIVE')}
                        onClick={() => changeActiveFilter('INACTIVE')}
                        style={{marginLeft: '4px'}}>
                        <NotActiveIcon style={{alignSelf: 'center'}}/>
                        <div>{managers.filter((el) => el.status === 'INACTIVE').length} Not Active</div>
                    </div>
                    <div
                        className={getFilterStyleClass('CRASHED')}
                        onClick={() => changeActiveFilter('CRASHED')}
                        style={{marginLeft: '4px'}}>
                        <CrossActiveIcon style={{alignSelf: 'center', width: '16px', height: '16px'}}/>
                        <div>{managers.filter((el) => el.status === 'CRASHED').length} Crashed</div>
                    </div>
                </RowDiv>
            </div>
            <div className="managers-content-body">
                <TableContainer style={{overflowY: 'auto'}}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell id="cell">Name</TableCell>
                                <TableCell id="cell">Status</TableCell>
                                <TableCell id="cell">Market</TableCell>
                                <TableCell id="cell">Stop Loss</TableCell>
                                <TableCell id="cell">Take Profit</TableCell>
                                <TableCell id="cell">Analyzers</TableCell>
                                <TableCell align="center" id="cell"><MenuHeaderIcon/></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {
                                getManagers().map((manager, index) =>
                                    (
                                        <TableRow>
                                            <TableCell
                                                id="cell">{!manager.customName ? manager.id : manager.customName}</TableCell>
                                            <TableCell id="cell">{getStatusCell(manager.status)}</TableCell>
                                            <TableCell id="cell">{manager.market}</TableCell>
                                            <TableCell id="cell">{manager.stopLoss}</TableCell>
                                            <TableCell id="cell">{manager.takeProfit}</TableCell>
                                            <TableCell id="cell">{manager.analyzersCount}</TableCell>
                                            <TableCell align="center" id="cell">
                                                <MenuIcon className="managers-menu-hover"
                                                          onClick={(event: React.MouseEvent<HTMLElement>) => handleMenuClick(event, index)}/>
                                                {
                                                    // manager.status !== 'ACTIVE' ?
                                                    //     <Button onClick={() => changeManagerStatus(manager, 'ACTIVE')}
                                                    //             variant='contained'
                                                    //             style={{
                                                    //                 textTransform: 'none',
                                                    //                 backgroundColor: '#16C079',
                                                    //                 color: 'white',
                                                    //                 fontWeight: 700
                                                    //             }}
                                                    //     >
                                                    //         Start
                                                    //     </Button> :
                                                    //     <Button onClick={() => changeManagerStatus(manager, 'INACTIVE')}
                                                    //             variant='contained'
                                                    //             style={{
                                                    //                 textTransform: 'none',
                                                    //                 backgroundColor: '#E7323B',
                                                    //                 color: 'white',
                                                    //                 fontWeight: 700
                                                    //             }}
                                                    //     >
                                                    //         Stop
                                                    //     </Button>
                                                }
                                            </TableCell>
                                        </TableRow>
                                    ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            </div>
            {currentIndex > -1 &&
                <Menu
                    anchorEl={anchorEl}
                    open={open}
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
                    <StyledMenuItem onClick={() => {
                        const manager = getCurrentManager();
                        changeManagerStatus(manager, manager.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
                        handleMenuClose();
                    }}>
                        {getCurrentManager().status === 'ACTIVE' ? 'Stop' : 'Activate'}
                    </StyledMenuItem>
                    <StyledMenuItem onClick={() => {
                        removeManager(getCurrentManager());
                        handleMenuClose();
                    }}
                                    style={{color: 'red'}}>
                        Delete
                    </StyledMenuItem>
                </Menu>
            }
            {isLoading &&
                <div style={{
                    position: 'absolute',
                    zIndex: 3,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    height: '100%',
                    width: '100%',
                }}>
                    <span className="managers-big-loading-banner" ref={spanRef}/>
                </div>
            }
        </div>
    );
}

export default ManagersPage;
