import React, {useCallback, useEffect, useRef, useState} from "react";
import {useLocation} from "wouter";
import {
    Box,
    Button,
    Checkbox,
    Menu,
    MenuItem,
    styled,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TableSortLabel
} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import "../../css/pages/analyzer/AnalyzerContentStyles.css";
import "../../css/pages/analyzer/SideBlock.css";
import {AnalyzerResponse} from "../../model/AnalyzerResponse";
import loadingSpinner from "../../assets/images/loading-spinner.svga";
import {ReactComponent as RocketIcon} from "../../assets/images/analyzer/rocket-icon.svg";
import {ReactComponent as ActiveIcon} from "../../assets/images/analyzer/active-icon.svg";
import {ReactComponent as NotActiveIcon} from "../../assets/images/analyzer/not-active-icon.svg";
import {ReactComponent as MenuHeaderIcon} from "../../assets/images/analyzer/menu-header-icon.svg";
import {ReactComponent as MenuIcon} from "../../assets/images/analyzer/menu-icon.svg";
import {getSymbolIcon} from "../../utils/symbols-utils";
import {
    changeBulkAnalyzerStatus,
    createAnalyzer,
    createAnalyzerBulk,
    deleteAnalyzerBulk,
    fetchAnalyzersList,
    fetchTopAnalyzersData, resetAnalyzerBulk
} from "../../service/AnalyzerService";
import {errorToast, successToast} from "../toast/Toasts";
import {useAuth} from "../../context/AuthContext";
import {MultiSelectStyle, RowDiv} from "../../utils/styles/element-styles";
import Select, {ActionMeta, components, OptionProps} from "react-select";
import {fetchSymbolsNameList} from "../../service/SymbolService";
import CreationSideBar from "./CreationSideBar";
import {FolderModel} from "../../model/FolderModel";
import {AnalyzerModel} from "../../model/AnalyzerModel";
import {AnalyzerModelBulk} from "../../model/AnalyzerModelBulk";

interface AnalyzerContentProps {
    folderId: string;
    folderName: string | undefined;
    addAnalyzersToFolder: (ids: string[]) => void;
    folders: FolderModel[];
}

const PreviousPath = styled('div')({
    font: plexFont,
    color: "#868F9C",
    fontSize: 20,
    fontWeight: '100',
    paddingTop: '24px',
    cursor: 'pointer'
});

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '700',
    paddingTop: '24px',
    paddingLeft: '4px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const HideBox = styled(Box)({
    position: 'absolute',
    marginTop: '3px',
    height: '50px', // Adjust as needed
    left: '150px', // Adjust based on your table's layout
    width: '500px', // Adjust to span over the desired number of columns
    backgroundColor: 'rgba(29,32,36,0.8)',
    color: '#868F9C',
    fontSize: '14px',
    fontWeight: 400,
    backdropFilter: 'blur(3px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1, // Ensure it's above table cells,
    overflowY: 'auto'
});

const StyledCheckbox = styled(Checkbox)({
    color: '#8A8A8A',
    '&.Mui-checked': {
        color: "#D0FF12",
    },
    transition: '0.1s'
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

const pageTypes = {
    top: 'TOP',
    all: 'LIST'
} as any;

type ActiveStatus = 'ALL' | 'ACTIVE' | 'NOT_ACTIVE';

const SelectOption = (props: OptionProps<any>) => {
    return (
        <components.Option {...props}>
            {getSymbolIcon(props.label)}
            <div style={{marginLeft: '8px'}}>{props.children}</div>
        </components.Option>
    );
};

const StyledTableSortLabel = styled(TableSortLabel)({
    '& .MuiTableSortLabel-icon': {
        color: 'white !important',
        width: '14px !important'
    }
});

type Order = 'asc' | 'desc';
type SortHeaders =
    'none'
    | 'diapason'
    | 'gridSize'
    | 'multiplier'
    | 'positionStopLoss'
    | 'positionTakeProfit'
    | 'startCapital'
    | 'money'
    | 'stabilityCoef'; // corresponding to fields in data base

type SortData = {
    name: SortHeaders;
    direction: Order;
}


const AnalyzerContent: React.FC<AnalyzerContentProps> = ({folderId, folderName, addAnalyzersToFolder, folders}) => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const sideRef = useRef<HTMLDivElement>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isBigLoading, setIsBigLoading] = useState(false);
    const [order, setOrder] = React.useState<Order>('desc');
    const [orderBy, setOrderBy] = React.useState<SortHeaders>('none');
    const [animation, setAnimation] = useState('');
    const [pageName, setPageName] = useState('Unknown');
    const [pageType, setPageType] = useState<'TOP' | 'LIST' | 'FOLDER' | 'NONE'>('NONE');
    const [processedFolderId, setProcessedFolderId] = useState<string | null>(null);
    const [selectedStatusFilter, setStatusFilter] = useState<ActiveStatus>('ALL')
    const [selectedSymbolFilter, setSymbolFilter] = useState<string[]>([])
    const [selectedAnalyzers, setSelectedAnalyzers] = useState<readonly number[]>([]);
    const [currentIndex, setCurrentIndex] = useState(-1);
    const [symbols, setSymbols] = useState<string[]>([]);
    const [data, setData] = useState<AnalyzerResponse[]>([]);
    const [dataSize, setDataSize] = useState(0);
    const [activeSize, setActiveSize] = useState(0);
    const [notActiveSize, setNotActiveSize] = useState(0);
    const [rowsCount, setRowsCount] = useState<number>(0);
    const [duplicatedAnalyzer, setDuplicatedAnalyzer] = useState<AnalyzerModel | null>(null);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [, navigate] = useLocation();
    const {authInfo} = useAuth();
    const open = Boolean(anchorEl);

    const updateAnalyzersList = useCallback((statusFilter: boolean | null, symbolFilter: string[], sortOption: {
        name: string,
        direction: Order
    } | null, folderId: string | null) => {
        setIsLoading(true);
        fetchAnalyzersList(authInfo!, 0, 20, statusFilter, symbolFilter, sortOption, folderId)
            .then(response => {
                setData([...response.analyzers]);
                setRowsCount(response.analyzers.length);
                setActiveSize(response.activeSize);
                setNotActiveSize(response.notActiveSize);
                setDataSize(response.totalSize);
                setIsLoading(false);
            })
            .catch(error => {
                setIsLoading(false);
                errorToast("Failed to fetch analyzers.");
                console.error(error);
            });
    }, [authInfo]);

    useEffect(() => {
        if (pageTypes[folderId] as string !== pageType) {
            switch (folderId) {
                case "top":
                    sideRef.current!.style.width = '0px';
                    setPageType(pageTypes[folderId]);
                    setPageName("Top Public Analyzers");
                    break;
                case "all":
                    setPageType(pageTypes[folderId]);
                    setPageName("All Analyzers");
                    break;
                default:
                    if (!!folderName) {
                        setPageType('FOLDER');
                        setPageName(folderName);
                        setProcessedFolderId(folderId);
                    }
            }
            setData([]);
            fetchSymbolsNameList()
                .then((symbols) => {
                    setSymbols(symbols);
                })
                .catch((ex) => {
                    errorToast("Failed to fetch symbols list");
                    console.error(ex);
                });
        }
    }, [folderId, pageType, pageName, folderName]);

    useEffect(() => {
        setIsLoading(true);
        switch (pageType) {
            case 'TOP':
                fetchTopAnalyzersData()
                    .then(data => {
                        setIsLoading(false);
                        if (Array.isArray(data)) {
                            setData(oldData => {
                                let newVar = [...oldData, ...data];
                                setDataSize(newVar.length);
                                setRowsCount(data.length);
                                return newVar;
                            });
                        }
                    })
                    .catch((ex) => {
                        setIsLoading(false);
                        errorToast("Failed to fetch top analyzers");
                        console.error(ex);
                    });
                break;
            case 'LIST':
                updateAnalyzersList(null, [], null, null);
                break;
            case 'FOLDER':
                updateAnalyzersList(null, [], null, processedFolderId);
                break;
        }
    }, [authInfo, processedFolderId, pageType, updateAnalyzersList]);

    useEffect(() => {
        fetch(loadingSpinner)
            .then(response => response.text())
            .then(text => {
                setAnimation(text)
                if (spanRef.current) {
                    spanRef.current.innerHTML = animation
                }
            });
    }, [animation, isLoading, isBigLoading]);

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, index: number) => {
        setCurrentIndex(index);
        setAnchorEl(event.currentTarget);
    };

    const getCurrentAnalyzer = (): AnalyzerResponse => data[currentIndex];

    const handleMenuClose = () => {
        setCurrentIndex(-1);
        setAnchorEl(null);
    };

    const handleClick = (id: number) => {
        const selectedIndex = selectedAnalyzers.indexOf(id);
        let newSelected: readonly number[] = [];

        if (selectedIndex === -1) {
            newSelected = newSelected.concat(selectedAnalyzers, id);
        } else if (selectedIndex === 0) {
            newSelected = newSelected.concat(selectedAnalyzers.slice(1));
        } else if (selectedIndex === selectedAnalyzers.length - 1) {
            newSelected = newSelected.concat(selectedAnalyzers.slice(0, -1));
        } else if (selectedIndex > 0) {
            newSelected = newSelected.concat(
                selectedAnalyzers.slice(0, selectedIndex),
                selectedAnalyzers.slice(selectedIndex + 1),
            );
        }
        setSelectedAnalyzers(newSelected);
    };

    const handleSelectAllClick = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.checked) {
            const newSelected = Array.from({length: rowsCount}, (_, index) => index);
            setSelectedAnalyzers(newSelected);
            return;
        }
        setSelectedAnalyzers([]);
    };

    const activateSelectedAnalyzers = () => {
        setCurrentAnalyzerStatus(selectedAnalyzers.map(index => data[index]), true);

    }

    const deactivateSelectedAnalyzers = () => {
        setCurrentAnalyzerStatus(selectedAnalyzers.map(index => data[index]), false);
    }

    const resetSelectedAnalyzers = () => {
        resetAnalyzers(selectedAnalyzers.map(index => data[index]));
    }

    const deleteSelectedAnalyzers = () => {
        deleteAnalyzers(selectedAnalyzers.map(index => data[index]));
    }

    const deleteAnalyzers = (analyzers: AnalyzerResponse[]) => {
        if (analyzers.length > 0) {
            setIsBigLoading(true);
            deleteAnalyzerBulk(authInfo!, analyzers.map(e => e.id))
                .then(() => {
                    setIsBigLoading(false);
                    successToast('Analyzers deleted successfully.');
                    setSelectedAnalyzers([]);
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter());
                })
                .catch(() => {
                    setIsBigLoading(false);
                    errorToast('Failed to delete analyzers.');
                });
        }
    }

    const resetAnalyzers = (analyzers: AnalyzerResponse[]) => {
        if (analyzers.length > 0) {
            setIsBigLoading(true);
            resetAnalyzerBulk(authInfo!, analyzers.map(e => e.id))
                .then(() => {
                    setIsBigLoading(false);
                    successToast('Analyzers updated successfully.');
                    setSelectedAnalyzers([]);
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter());
                })
                .catch(() => {
                    setIsBigLoading(false);
                    errorToast('Failed to updated analyzers.');
                });
        }
    }

    const setCurrentAnalyzerStatus = (analyzers: AnalyzerResponse[], status: boolean) => {
        if (analyzers.length > 0) {
            setIsBigLoading(true);
            changeBulkAnalyzerStatus(authInfo!, analyzers.map(e => e.id), status)
                .then(() => {
                    setIsBigLoading(false);
                    analyzers.forEach(e => e.isActive = true);
                    successToast(`Analyzers ${status ? 'activated' : 'deactivated'} successfully.`);
                    setSelectedAnalyzers([]);
                    setActiveSize(activeSize + analyzers.length)
                    setNotActiveSize(notActiveSize - analyzers.length)
                })
                .catch((ex) => {
                    setIsBigLoading(false);
                    setSelectedAnalyzers([]);
                    errorToast(`Failed to ${status ? 'activate' : 'deactivate'} analyzers.`);
                    console.error(ex);
                });
        }
    }

    const addAnalyzer = (analyzer: AnalyzerModel) => {
        setIsBigLoading(true);
        createAnalyzer(authInfo!, analyzer)
            .then(() => {
                setIsBigLoading(false);
                successToast("Analyzer created");
                if (pageType === 'LIST' || (pageType === 'FOLDER' && analyzer.folders.indexOf(folderId) > -1)) {
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter());
                }
            })
            .catch(() => {
                setIsBigLoading(false)
                errorToast('Failed to create analyzer');
            });
    }

    const addAnalyzersBulk = (analyzers: AnalyzerModelBulk) => {
        setIsBigLoading(true);
        createAnalyzerBulk(authInfo!, analyzers)
            .then(() => {
                setIsBigLoading(false);
                successToast("Analyzers created");
                if (pageType === 'LIST' || (pageType === 'FOLDER' && analyzers.folders.indexOf(folderId) > -1)) {
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter());
                }
            })
            .catch(() => {
                setIsBigLoading(false)
                errorToast('Failed to create analyzers');
            });
    };

    const closeRightDrawer = () => {
        setDuplicatedAnalyzer(null);
        return sideRef.current!.style.width = '0px';
    };

    const openRightDrawer = () => {
        return sideRef.current!.style.width = '320px';
    };

    const copyCurrentAnalyzerId = () => {
        navigator.clipboard.writeText(getCurrentAnalyzer().id).then(() => {
            successToast("Analyzer ID copied to clipboard");
        });
        handleMenuClose();
    };

    const identifyStatus = (status: string) => status === 'ACTIVE' ? true : status === 'NOT_ACTIVE' ? false : null;
    const getFolderFilter = () => pageType === 'FOLDER' ? folderId : null;


    const getSortObject = () => {
        if (orderBy !== 'none') {
            return {name: orderBy, direction: order}
        }
        return null;
    }

    const changeActiveFilter = (status: ActiveStatus) => {
        if (status !== selectedStatusFilter) {
            setStatusFilter(status);
            updateAnalyzersList(identifyStatus(status), selectedSymbolFilter, getSortObject(), getFolderFilter());
        }
    }

    const selectedSymbolsChange = (options: unknown, actionMeta: ActionMeta<unknown>) => {
        if (Array.isArray(options) && options.length > 0) {
            const symbols = options.map(element => (element as any).value);
            setSymbolFilter(symbols);
            updateAnalyzersList(identifyStatus(selectedStatusFilter), symbols, getSortObject(), getFolderFilter());
        } else if (selectedSymbolFilter.length > 0) {
            setSymbolFilter([]);
            updateAnalyzersList(identifyStatus(selectedStatusFilter), [], getSortObject(), getFolderFilter());
        }
    };

    const handleSortChange = (key: SortHeaders) => {
        let data: SortData | null = {name: key, direction: 'asc'};
        if (key === orderBy && order === 'desc') {
            data.direction = 'asc';
            setOrder('asc');
        } else if (key === orderBy && order === 'asc') {
            data = null;
            setOrder('desc');
            setOrderBy('none');
        } else if (key !== orderBy) {
            setOrder('desc');
            data.direction = 'desc';
            setOrderBy(key);
        }
        updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, data, getFolderFilter());
    }

    const isListPage = () => pageType === 'LIST' || pageType === 'FOLDER';
    const isSelected = (id: number) => selectedAnalyzers.indexOf(id) !== -1;
    const allSelectedActive = () => selectedAnalyzers.filter((index) => data[index].isActive).length;
    const allSelectedNotActive = () => selectedAnalyzers.filter((index) => !data[index].isActive).length;
    const isAllSelectedActive = () => Array.isArray(data) && allSelectedActive() === selectedAnalyzers.length;
    const isAllSelectedNotActive = () => Array.isArray(data) && allSelectedNotActive() === selectedAnalyzers.length;
    const HeaderCellContent = styled(RowDiv)({
        "&:hover": {
            cursor: isListPage() ? 'pointer' : 'inherit'
        }
    });
    const HeaderCellIcon = styled(StyledTableSortLabel)({
        transition: '0.3s'
    });

    const mapAnalyzerToModel = (analyzer: AnalyzerResponse) => ({
        diapason: analyzer.diapason,
        gridSize: analyzer.gridSize,
        multiplier: analyzer.multiplier,
        stopLoss: analyzer.positionStopLoss,
        takeProfit: analyzer.positionTakeProfit,
        symbol: analyzer.symbol,
        startCapital: analyzer.startCapital,
        active: analyzer.isActive,
        public: analyzer.public,
        folders: [],
        strategy: analyzer.strategy,
        market: analyzer.market,
        demoAccount: analyzer.demoAccount
    });

    return (
        <div className="analyzer-content-body">
            <CreationSideBar ref={sideRef} symbols={symbols} folders={folders}
                             predefinedAnalyzerProps={duplicatedAnalyzer}
                             createAnalyzerBulkFunction={addAnalyzersBulk} createAnalyzerFunction={addAnalyzer}
                             closeAction={closeRightDrawer}/>
            <div className="analyzer-header">
                <div className="analyzer-header-container">
                    <div className="analyzer-header-path">
                        <PreviousPath onClick={() => navigate("/analyzer")}>
                            Analyzer /
                        </PreviousPath>
                        <CurrentPath>{pageName}</CurrentPath>
                    </div>
                    {!!authInfo && isListPage() &&
                        <Button variant="contained"
                                onClick={openRightDrawer}
                                style={{
                                    textTransform: 'none',
                                    backgroundColor: '#D0FF12',
                                    color: '#121417',
                                    fontSize: '14px',
                                    fontWeight: 700,
                                    borderRadius: '4px',
                                    height: '34px',
                                    width: '130px',
                                    alignSelf: 'center',
                                }}>New analyzer</Button>
                    }
                </div>
                {isListPage() &&
                    <div className="analyzer-header-container" style={{minHeight: '50px', marginBottom: '8px'}}>
                        <RowDiv>
                            <div
                                onClick={() => changeActiveFilter('ALL')}
                                className={selectedStatusFilter === 'ALL' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}>
                                <div>{dataSize} Analyzers</div>
                            </div>
                            <div
                                onClick={() => changeActiveFilter('ACTIVE')}
                                className={selectedStatusFilter === 'ACTIVE' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}
                                style={{marginLeft: '4px'}}>
                                <ActiveIcon style={{alignSelf: 'center'}}/>
                                <div>{activeSize} Active</div>
                            </div>
                            <div
                                onClick={() => changeActiveFilter('NOT_ACTIVE')}
                                className={selectedStatusFilter === 'NOT_ACTIVE' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}
                                style={{marginLeft: '4px'}}>
                                <NotActiveIcon style={{alignSelf: 'center'}}/>
                                <div>{notActiveSize} Not Active</div>
                            </div>
                        </RowDiv>
                        <RowDiv>
                            <div style={{color: 'white', marginRight: '8px'}}>Symbol</div>
                            <div style={{width: '500px'}}>
                                <Select
                                    options={symbols.map((symbol) => {
                                        return {value: symbol, label: symbol}
                                    })}
                                    isMulti
                                    components={{IndicatorSeparator: () => null, Option: SelectOption}}
                                    placeholder=""
                                    name="market"
                                    onChange={selectedSymbolsChange}
                                    styles={MultiSelectStyle}
                                />
                            </div>
                        </RowDiv>
                    </div>
                }
            </div>
            {isLoading ?
                <div style={{
                    flexGrow: 1,
                    width: '50%',
                    height: '50%',
                    alignSelf: "center"
                }}>
                    <span ref={spanRef}/>
                </div> :
                dataSize > 0 &&
                <TableContainer style={{overflowY: 'auto'}}>
                    <Table size="small">
                        <TableHead>
                            <TableRow id="analyzer-table-headers">
                                {isListPage() &&
                                    <TableCell id="cell" padding="checkbox">
                                        <StyledCheckbox
                                            onChange={handleSelectAllClick}
                                            checked={rowsCount > 0 && selectedAnalyzers.length === rowsCount}
                                        />
                                    </TableCell>
                                }
                                <TableCell width="82px" id="cell">Symbol</TableCell>
                                {isListPage() && <TableCell width="82px" align="center" id="cell">Status</TableCell>}
                                <TableCell width="82px" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('diapason')}>
                                        Diapason {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'diapason'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell width="82px" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('gridSize')}>
                                        Grid Size {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'gridSize'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell width="82px" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('multiplier')}>
                                        Multiplier {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'multiplier'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell width="25px" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('positionStopLoss')}>
                                        SL {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'positionStopLoss'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell width="25px" align="left" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('positionTakeProfit')}>
                                        TP {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'positionTakeProfit'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                {isListPage() &&
                                    <TableCell width="100px" id="cell">
                                        <HeaderCellContent onClick={() => handleSortChange('startCapital')}>
                                            Start Capital <HeaderCellIcon direction={order}
                                                                          active={orderBy === 'startCapital'}/>
                                        </HeaderCellContent>
                                    </TableCell>}
                                {isListPage() &&
                                    <TableCell width="80px" id="cell">
                                        <HeaderCellContent onClick={() => handleSortChange('money')}>
                                            Total Equity <HeaderCellIcon direction={order}
                                                                         active={orderBy === 'money'}/>
                                        </HeaderCellContent>
                                    </TableCell>}
                                <TableCell width="50px" align="center" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('stabilityCoef')}>
                                        Stability {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'stabilityCoef'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell width="50px" align="center" id="cell">1h %</TableCell>
                                <TableCell width="50px" align="center" id="cell">12h %</TableCell>
                                <TableCell width="50px" align="center" id="cell">24h %</TableCell>
                                <TableCell width="60px" align="center" id="cell">
                                    {isListPage() && <MenuHeaderIcon/>}
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody style={{position: 'relative'}}>
                            {!!data && Array.isArray(data) && data.map((analyzer, index) => {
                                const isItemSelected = isSelected(index);
                                return (
                                    <TableRow key={analyzer.id} id="analyzer-table-content">
                                        {isListPage() &&
                                            <TableCell id="cell" padding="checkbox">
                                                <StyledCheckbox
                                                    checked={isItemSelected}
                                                    onClick={() => handleClick(index)}
                                                />
                                            </TableCell>
                                        }
                                        <TableCell id="cell"
                                                   onClick={() => navigate(`/analyzer/detail/${analyzer.id}`)}>{getSymbolIcon(analyzer.symbol)}</TableCell>
                                        {isListPage() && <TableCell align="left" id="cell">
                                            {analyzer.isActive ?
                                                <div className="analyzer-table-item-status">
                                                    <ActiveIcon style={{alignSelf: 'center'}}/>
                                                    <div style={{marginLeft: '4px'}}>Active</div>
                                                </div>
                                                :
                                                <div className="analyzer-table-item-status"
                                                     style={{paddingLeft: 0, paddingRight: 0, minWidth: '85px'}}>
                                                    <NotActiveIcon style={{alignSelf: 'center'}}/>
                                                    <div style={{marginLeft: '4px'}}>Not Active</div>
                                                </div>
                                            }
                                        </TableCell>}
                                        <TableCell id="cell">{analyzer.diapason}%</TableCell>
                                        <TableCell id="cell">{analyzer.gridSize}</TableCell>
                                        <TableCell id="cell">x{analyzer.multiplier}</TableCell>
                                        <TableCell id="cell"
                                                   style={{color: 'red'}}>{analyzer.positionStopLoss}%</TableCell>
                                        <TableCell id="cell" align="left"
                                                   style={{color: '#D0FF12'}}>{analyzer.positionTakeProfit}%</TableCell>
                                        {isListPage() &&
                                            <TableCell id="cell" align="left">{analyzer.startCapital}</TableCell>}
                                        {isListPage() &&
                                            <TableCell id="cell"
                                                       align="left">{!!analyzer.money && analyzer.money.toFixed(3)}</TableCell>}
                                        <TableCell align="center" id="cell">1</TableCell>
                                        <TableCell align="center" id="cell">20 %</TableCell>
                                        <TableCell align="center" id="cell">20 %</TableCell>
                                        <TableCell align="center" id="cell">20 %</TableCell>
                                        <TableCell align={isListPage() ? "center" : "right"} id="cell">
                                            {
                                                isListPage() ?
                                                    <MenuIcon className="analyzer-folder-menu-hover"
                                                              onClick={(event: React.MouseEvent<HTMLElement>) => handleMenuClick(event, index)}/>
                                                    :
                                                    <Button variant="outlined"
                                                            onClick={() => {
                                                                setDuplicatedAnalyzer(mapAnalyzerToModel(analyzer));
                                                                navigate("/analyzer/folder/all")
                                                                openRightDrawer();
                                                            }}
                                                            disabled={!authInfo} style={{
                                                        textTransform: 'none',
                                                        borderColor: !authInfo ? '#2D323A' : '#D0FF12',
                                                        color: !authInfo ? '#2D323A' : '#D0FF12'
                                                    }}>Duplicate</Button>}
                                        </TableCell>
                                        {
                                            !authInfo &&
                                            <HideBox>
                                                <RocketIcon style={{marginRight: '8px', fill: '#868F9C'}}/>
                                                Connect your wallet to unlock full access!
                                            </HideBox>
                                        }
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            }
            {isListPage() && currentIndex > -1 &&
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
                    <StyledMenuItem onClick={
                        () => {
                            setCurrentAnalyzerStatus([getCurrentAnalyzer()], !getCurrentAnalyzer().isActive);
                            handleMenuClose();
                        }
                    }>{
                        getCurrentAnalyzer().isActive ? "Stop" : "Activate"
                    }</StyledMenuItem>
                    <StyledMenuItem onClick={() => {
                        setDuplicatedAnalyzer(mapAnalyzerToModel(getCurrentAnalyzer()));
                        openRightDrawer();
                        handleMenuClose();
                    }}>Duplicate</StyledMenuItem>
                    <StyledMenuItem onClick={
                        () => {
                            addAnalyzersToFolder([getCurrentAnalyzer().id]);
                            handleMenuClose();
                        }
                    }>Add to the Folder</StyledMenuItem>
                    <StyledMenuItem onClick={copyCurrentAnalyzerId}>Copy ID</StyledMenuItem>
                    <StyledMenuItem onClick={() => {
                        resetAnalyzers([getCurrentAnalyzer()]);
                        handleMenuClose();
                    }}>Reset</StyledMenuItem>
                    <StyledMenuItem sx={{color: 'red'}} onClick={() => {
                        deleteAnalyzers([getCurrentAnalyzer()]);
                        handleMenuClose();
                    }}>Delete</StyledMenuItem>
                </Menu>
            }
            {
                selectedAnalyzers.length > 0 &&
                <div className="analyzer-selected-banner">
                    <div className="analyzer-selected-banner-content">
                        <div style={{
                            display: 'flex',
                            color: 'white',
                            fontSize: '14px',
                            fontWeight: 400,
                            marginBottom: '16px'
                        }}>
                            Selected {selectedAnalyzers.length} items
                        </div>
                        <div style={{
                            display: 'flex',
                            flexDirection: 'row',
                            alignItems: 'space-between'
                        }}>
                            <Button variant="outlined"
                                    onClick={() => {
                                        addAnalyzersToFolder(selectedAnalyzers.map(index => data[index].id));
                                    }}
                                    style={{
                                        display: 'flex',
                                        borderColor: '#D0FF12',
                                        color: '#D0FF12',
                                        textTransform: 'none',
                                        fontWeight: 700
                                    }}>Add to the Folder</Button>
                            <Button variant="outlined"
                                    onClick={resetSelectedAnalyzers}
                                    style={{
                                        display: 'flex',
                                        borderColor: '#D0FF12',
                                        color: '#D0FF12',
                                        textTransform: 'none',
                                        fontWeight: 700,
                                        marginLeft: '16px'
                                    }}>Reset</Button>
                            <Button variant="outlined"
                                    onClick={deleteSelectedAnalyzers}
                                    style={{
                                        display: 'flex',
                                        borderColor: 'red',
                                        color: 'red',
                                        textTransform: 'none',
                                        fontWeight: 700,
                                        marginLeft: '16px'
                                    }}>Delete</Button>
                            {
                                (isAllSelectedActive() &&
                                    <Button variant="contained"
                                            onClick={deactivateSelectedAnalyzers}
                                            style={{
                                                display: 'flex',
                                                backgroundColor: 'red',
                                                color: 'white',
                                                textTransform: 'none',
                                                fontWeight: 700,
                                                marginLeft: '16px'
                                            }}>Stop</Button>)
                            }
                            {
                                (isAllSelectedNotActive() &&
                                    <Button variant="contained"
                                            onClick={activateSelectedAnalyzers}
                                            style={{
                                                display: 'flex',
                                                backgroundColor: '#16C079',
                                                color: 'white',
                                                textTransform: 'none',
                                                fontWeight: 700,
                                                marginLeft: '16px'
                                            }}>Activate</Button>
                                )
                            }
                        </div>
                    </div>
                </div>
            }
            {isBigLoading &&
                <div style={{
                    position: 'absolute',
                    zIndex: 3,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    height: '100%',
                    width: '100%',
                }}>
                    <span className="analyzer-big-loading-banner" ref={spanRef}/>
                </div>
            }
        </div>
    );
}

export default AnalyzerContent;
