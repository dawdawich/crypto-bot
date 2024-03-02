import React, {useEffect, useState} from "react";
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
import {errorToast, successToast} from "../../shared/toast/Toasts";
import {FolderModel} from "../../model/FolderModel";
import {fetchFolderList} from "../../service/FolderService";
import {ManagerResponse} from "../../model/ManagerResponse";
import {deleteManager, fetchManagersList, updateManagerStatus} from "../../service/ManagerService";
import {ReactComponent as ActiveIcon} from "../../assets/images/analyzer/active-icon.svg";
import {ReactComponent as NotActiveIcon} from "../../assets/images/analyzer/not-active-icon.svg";
import {ReactComponent as CrossActiveIcon} from "../../assets/images/action-icon/cross-icon.svg";
import {RowDiv} from "../../utils/styles/element-styles";
import {ReactComponent as MenuHeaderIcon} from "../../assets/images/analyzer/menu-header-icon.svg";
import {ReactComponent as MenuIcon} from "../../assets/images/analyzer/menu-icon.svg";
import {UnauthorizedError} from "../../utils/errors/UnauthorizedError";
import {useLoader} from "../../context/LoaderContext";
import loadingTableRows from "../../shared/LoadingTableRows";

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
    const {authInfo, logout} = useAuth();
    const [isTableLoading, setIsTableLoading] = useState(false);
    const {showLoader, hideLoader} = useLoader();
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
            .catch((ex) => {
                errorToast('Failed to fetch API tokens');
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
        fetchFolderList(authInfo!)
            .then((foldersResponse) => setFolders(foldersResponse))
            .catch((ex) => {
                errorToast('Failed to fetch folders list');
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
        setIsTableLoading(true);
        fetchManagersList(authInfo!)
            .then((managers) => {
                setIsTableLoading(false);
                setManagers(managers);
            })
            .catch((ex) => {
                setIsTableLoading(false);
                errorToast('Failed to fetch managers');
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
    }, [authInfo, logout]);

    const getFilterStyleClass = (type: FilterType) => type === activeFilter ? 'managers-filters-cell-active' : 'managers-filters-cell';

    const changeManagerStatus = (manager: ManagerResponse, status: FilterType) => {
        showLoader();
        updateManagerStatus(authInfo!, manager.id, status)
            .then(() => {
                hideLoader();
                manager.status = status;
                setManagers([...managers]);
                successToast(status === 'ACTIVE' ? 'Manager started successfully' : 'Manager stopped successfully');
            })
            .catch((ex) => {
                hideLoader();
                errorToast(status === 'ACTIVE' ? 'Failed to start manager' : 'Failed to stop manager');
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
    }

    const removeManager = (manager: ManagerResponse) => {
        showLoader();
        deleteManager(authInfo!, manager.id)
            .then(() => {
                hideLoader();
                setManagers([...managers.filter(el => el.id !== manager.id)])
                successToast('Manager deleted successfully');
            })
            .catch((ex) => {
                hideLoader();
                errorToast('Failed to delete manager');
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
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
            <CreateManagerDialog folders={folders} apiTokens={apiTokens} authInfo={authInfo!} logout={logout}
                                 open={isDialogOpen}
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
                            {   isTableLoading ? loadingTableRows({rows: 13, columns: 6}) :
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
        </div>
    );
}

export default ManagersPage;
