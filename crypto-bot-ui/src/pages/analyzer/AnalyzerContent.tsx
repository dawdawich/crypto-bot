import React, {useCallback, useEffect, useRef, useState} from "react";
import {useLocation} from "wouter";
import {
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
    TableSortLabel,
    Tooltip
} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import "../../css/pages/analyzer/AnalyzerContentStyles.css";
import "../../css/pages/analyzer/SideBlock.css";
import {AnalyzerResponse} from "../../model/AnalyzerResponse";
import {ReactComponent as ActiveIcon} from "../../assets/images/analyzer/active-icon.svg";
import {ReactComponent as NotActiveIcon} from "../../assets/images/analyzer/not-active-icon.svg";
import {ReactComponent as MenuIcon} from "../../assets/images/analyzer/menu-icon.svg";
import {getSymbolIcon} from "../../utils/symbols-utils";
import {
    changeBulkAnalyzerStatus,
    createAnalyzer,
    createAnalyzerBulk,
    deleteAnalyzerBulk,
    fetchAnalyzersList,
    fetchTopAnalyzersData,
    resetAnalyzerBulk
} from "../../service/AnalyzerService";
import {errorToast, successToast} from "../../shared/toast/Toasts";
import {useAuth} from "../../context/AuthContext";
import {MultiSelectStyle, RowDiv} from "../../utils/styles/element-styles";
import Select, {ActionMeta, components, OptionProps} from "react-select";
import {fetchSymbolsNameList} from "../../service/SymbolService";
import CreationSideBar from "./side_bar_components/CreationSideBar";
import {FolderModel} from "../../model/FolderModel";
import {AnalyzerModel} from "../../model/AnalyzerModel";
import {AnalyzerModelBulk} from "../../model/AnalyzerModelBulk";
import {UnauthorizedError} from "../../utils/errors/UnauthorizedError";
import {PaymentRequiredError} from "../../utils/errors/PaymentRequiredError";
import HideBox from "./HideBox";
import {trimDecimalNumbers} from "../../utils/number-utils";
import {useLoader} from "../../context/LoaderContext";
import loadingTableRows from "../../shared/LoadingTableRows";
import InfiniteScroll from "react-infinite-scroller";

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
    | 'stabilityCoef'
    | 'pNl1'
    | 'pNl12'
    | 'pNl24'; // corresponding to fields in data base

type SortData = {
    name: SortHeaders;
    direction: Order;
}


const AnalyzerContent: React.FC<AnalyzerContentProps> = ({folderId, folderName, addAnalyzersToFolder, folders}) => {
    const sideRef = useRef<HTMLDivElement>(null);
    const diapasonHeaderRef = useRef<HTMLDivElement>(null);
    const stabilityHeaderRef = useRef<HTMLDivElement>(null);
    const [isTableLoading, setIsTableLoading] = useState(false);
    const [order, setOrder] = React.useState<Order>('desc');
    const [orderBy, setOrderBy] = React.useState<SortHeaders>('none');
    const [pageName, setPageName] = useState('Unknown');
    const [pageType, setPageType] = useState<'TOP' | 'LIST' | 'FOLDER' | 'NONE'>('NONE');
    const [processedFolderId, setProcessedFolderId] = useState<string | null>(null);
    const [selectedStatusFilter, setSelectedStatusFilter] = useState<ActiveStatus>('ALL')
    const [selectedSymbolFilter, setSelectedSymbolFilter] = useState<string[]>([])
    const [selectedAnalyzers, setSelectedAnalyzers] = useState<readonly number[]>([]);
    const [selectAllCheckbox, setSelectAllCheckbox] = useState<boolean>(false);
    const [currentIndex, setCurrentIndex] = useState(-1);
    const [symbols, setSymbols] = useState<string[]>([]);
    const [data, setData] = useState<AnalyzerResponse[]>([]);
    const [dataSize, setDataSize] = useState(0);
    const [activeSize, setActiveSize] = useState(0);
    const [notActiveSize, setNotActiveSize] = useState(0);
    const [duplicatedAnalyzer, setDuplicatedAnalyzer] = useState<AnalyzerModel | null>(null);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [, navigate] = useLocation();
    const {authInfo, logout} = useAuth();
    const {showLoader, hideLoader} = useLoader();
    const open = Boolean(anchorEl);
    const [pageNumber, setPageNumber] = useState<number>(1);

    if (!authInfo) {
        navigate("/analyzer/folder/top");
    }

    if (pageType === 'FOLDER' && !folders.find(el => el.name === folderName)) {
        navigate("/analyzer/folder/all");
    }

    const updateAnalyzersList = useCallback((statusFilter: boolean | null, symbolFilter: string[], sortOption: {
        name: string,
        direction: Order
    } | null, folderId: string | null, pageNumber: number) => {
        setIsTableLoading(true);
        setData([]);
        fetchAnalyzersList(authInfo!, 0, pageNumber * 20, statusFilter, symbolFilter, sortOption, folderId)
            .then(response => {
                setData([...response.analyzers]);
                setActiveSize(response.activeSize);
                setNotActiveSize(response.notActiveSize);
                setDataSize(response.totalSize);
                setIsTableLoading(false);
            })
            .catch(ex => {
                setIsTableLoading(false);
                errorToast("Failed to fetch analyzers.");
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            });
    }, [authInfo, logout]);

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
                    if (folderName) {
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
    }, [folderId, pageType, folderName]);

    useEffect(() => {
        setIsTableLoading(true);
        switch (pageType) {
            case 'TOP':
                fetchTopAnalyzersData()
                    .then(data => {
                        setIsTableLoading(false);
                        if (Array.isArray(data)) {
                            setData(data);
                            setDataSize(data.length)
                        }
                    })
                    .catch((ex) => {
                        setIsTableLoading(false);
                        errorToast("Failed to fetch top analyzers");
                        console.error(ex);
                    });
                break;
            case 'LIST':
                updateAnalyzersList(null, [], null, null, 1);
                break;
            case 'FOLDER':
                updateAnalyzersList(null, [], null, processedFolderId, 1);
                break;
        }
    }, [authInfo, processedFolderId, pageType, updateAnalyzersList]);

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, index: number) => {
        setCurrentIndex(index);
        setAnchorEl(event.currentTarget);
    };

    const getCurrentAnalyzer = (): AnalyzerResponse => data[currentIndex];

    const handleMenuClose = () => {
        setCurrentIndex(-1);
        setAnchorEl(null);
    };

    const handleSelectAnalyzer = (id: number) => {
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
        setSelectedAnalyzers([]);
        setSelectAllCheckbox(event.target.checked);
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
            showLoader();
            deleteAnalyzerBulk(authInfo!, analyzers.map(e => e.id), selectAllCheckbox)
                .then(() => {
                    hideLoader();
                    successToast('Analyzers deleted successfully.');
                    setSelectedAnalyzers([]);
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter(), pageNumber);
                })
                .catch((ex) => {
                    hideLoader();
                    errorToast('Failed to delete analyzers.');
                    if (ex instanceof UnauthorizedError) {
                        logout();
                    }
                });
        }
    }

    const resetAnalyzers = (analyzers: AnalyzerResponse[]) => {
        if (analyzers.length > 0) {
            showLoader();
            resetAnalyzerBulk(authInfo!, analyzers.map(e => e.id), selectAllCheckbox)
                .then(() => {
                    hideLoader();
                    successToast('Analyzers updated successfully.');
                    setSelectedAnalyzers([]);
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter(), pageNumber);
                })
                .catch((ex) => {
                    hideLoader();
                    errorToast('Failed to updated analyzers.');
                    if (ex instanceof UnauthorizedError) {
                        logout();
                    }
                });
        }
    }

    const setCurrentAnalyzerStatus = (analyzers: AnalyzerResponse[], status: boolean) => {
        showLoader();
        changeBulkAnalyzerStatus(authInfo!, status,
            analyzers.filter(e => e.isActive !== status).map(e => e.id), selectAllCheckbox)
            .then(() => {
                hideLoader();
                if (!selectAllCheckbox) {
                    analyzers.forEach(e => e.isActive = status);
                    setActiveSize(activeSize + (status ? analyzers.length : -analyzers.length));
                    setNotActiveSize(notActiveSize - (status ? analyzers.length : -analyzers.length));
                } else { // case when user choose all analyzers
                    data
                        .filter(e => !analyzers.includes(e))
                        .forEach(e => e.isActive = status);
                    if (status) { // also here processing deselected analyzers
                        const notActive = analyzers.filter(e => !e.isActive).length;
                        const active = dataSize - notActive;
                        setActiveSize(active);
                        setNotActiveSize(notActive);
                    } else {
                        const active = analyzers.filter(e => e.isActive).length;
                        const notActive = dataSize - active;
                        setActiveSize(active);
                        setNotActiveSize(notActive);
                    }
                }
                setData([...data]);
                setSelectedAnalyzers([]);
                setSelectAllCheckbox(false);
                successToast(`Analyzers ${status ? 'activated' : 'deactivated'} successfully.`);
            })
            .catch((ex) => {
                hideLoader();
                setSelectedAnalyzers([]);
                setSelectAllCheckbox(false);
                errorToast(`Failed to ${status ? 'activate' : 'deactivate'} analyzers.`);
                if (ex instanceof UnauthorizedError) {
                    logout();
                } else if (ex instanceof PaymentRequiredError) {
                    errorToast('Not enough active subscriptions');
                }
            });
    }

    const addAnalyzer = (analyzer: AnalyzerModel) => {
        showLoader();
        createAnalyzer(authInfo!, analyzer)
            .then(() => {
                hideLoader();
                successToast("Analyzer created");
                if (pageType === 'LIST' || (pageType === 'FOLDER' && analyzer.folders.indexOf(folderId) > -1)) {
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter(), pageNumber);
                }
            })
            .catch((ex) => {
                hideLoader()
                errorToast('Failed to create analyzer');
                if (ex instanceof UnauthorizedError) {
                    logout();
                } else if (ex instanceof PaymentRequiredError) {
                    errorToast('Not enough active subscriptions');
                }
            });
    }

    const addAnalyzersBulk = (analyzers: AnalyzerModelBulk) => {
        showLoader();
        createAnalyzerBulk(authInfo!, analyzers)
            .then(() => {
                hideLoader();
                successToast("Analyzers created");
                if (pageType === 'LIST' || (pageType === 'FOLDER' && analyzers.folders.indexOf(folderId) > -1)) {
                    updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter(), pageNumber);
                }
            })
            .catch((ex) => {
                hideLoader()
                errorToast('Failed to create analyzers');
                if (ex instanceof UnauthorizedError) {
                    logout();
                } else if (ex instanceof PaymentRequiredError) {
                    errorToast('Not enough active subscriptions');
                }
            });
    };

    const closeRightDrawer = () => {
        setDuplicatedAnalyzer(null);
        sideRef.current!.style.width = '0px';
    };

    const openRightDrawer = () => {
        sideRef.current!.style.width = '320px';
    };

    const copyCurrentAnalyzerId = () => {
        navigator.clipboard.writeText(getCurrentAnalyzer().id).then(() => {
            successToast("Analyzer ID copied to clipboard");
        });
        handleMenuClose();
    };

    const identifyStatus = (status: ActiveStatus) => {
        switch (status) {
            case "ACTIVE":
                return true;
            case "NOT_ACTIVE":
                return false;
            case "ALL":
                return null;
        }
    };
    const getFolderFilter = () => pageType === 'FOLDER' ? folderId : null;


    const getSortObject = () => {
        if (orderBy !== 'none') {
            return {name: orderBy, direction: order}
        }
        return null;
    }

    const changeActiveFilter = (status: ActiveStatus) => {
        if (status !== selectedStatusFilter) {
            setSelectedStatusFilter(status);
            updateAnalyzersList(identifyStatus(status), selectedSymbolFilter, getSortObject(), getFolderFilter(), pageNumber);
        }
    }

    const selectedSymbolsChange = (options: unknown, _: ActionMeta<unknown>) => {
        if (Array.isArray(options) && options.length > 0) {
            const symbols = options.map(element => element.value);
            setSelectedSymbolFilter(symbols);
            updateAnalyzersList(identifyStatus(selectedStatusFilter), symbols, getSortObject(), getFolderFilter(), pageNumber);
        } else if (selectedSymbolFilter.length > 0) {
            setSelectedSymbolFilter([]);
            updateAnalyzersList(identifyStatus(selectedStatusFilter), [], getSortObject(), getFolderFilter(), pageNumber);
        }
    };

    const handleSortChange = (key: SortHeaders) => {
        if (isListPage()) {
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
            updateAnalyzersList(identifyStatus(selectedStatusFilter), selectedSymbolFilter, data, getFolderFilter(), pageNumber);
        }
    }
    const navigateToAnalyzerDetail = (analyzerId: string) => {
        if (pageType !== "TOP") {
            navigate(`/analyzer/detail/${analyzerId}`);
        }

    }
    const isListPage = () => pageType === 'LIST' || pageType === 'FOLDER';
    const isSelected = (id: number) => (!selectAllCheckbox && selectedAnalyzers.indexOf(id) !== -1) || (selectAllCheckbox && selectedAnalyzers.indexOf(id) === -1);
    const allSelectedActive = () => selectedAnalyzers.filter((index) => data[index].isActive).length;
    const allSelectedNotActive = () => selectedAnalyzers.filter((index) => !data[index].isActive).length;
    const allDeselectedActive = () => data.filter((value, index) => selectedAnalyzers.includes(index) && value.isActive).length;
    const allDeselectedNotActive = () => data.filter((value, index) => selectedAnalyzers.includes(index) && !value.isActive).length;
    const containsNotActive = () => Array.isArray(data) && ((!selectAllCheckbox && allSelectedNotActive() === selectedAnalyzers.length) || (selectAllCheckbox && notActiveSize > allDeselectedNotActive()));
    const containsActive = () => Array.isArray(data) && ((!selectAllCheckbox && allSelectedActive() === selectedAnalyzers.length) || (selectAllCheckbox && activeSize > allDeselectedActive()));
    const HeaderCellContent = styled(RowDiv)({
        "&:hover": {
            cursor: isListPage() ? 'pointer' : 'inherit'
        }
    });

    const HeaderCellIcon = styled(StyledTableSortLabel)({
        transition: '0.3s'
    });

    const getPnLColorByValue = (value: number) => {
        if (value === 0) {
            return 'white';
        }
        return value < 0 ? '#E7323B' : '#16C079';
    };
    const getSignByValue = (value: number) => value > 0 ? '+' : '';

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

    const getLoadingRows = () => {
        const columnsNumber = pageType === 'TOP' ? 10 : 13;
        const preFixCells = pageType === 'TOP' ? undefined : 1;
        return loadingTableRows({
            rows: 13,
            columns: columnsNumber,
            prefixSkipColumns: preFixCells,
            postfixSkipColumns: 1
        });
    };

    const loadMoreAnalyzers = useCallback(() => {
        fetchAnalyzersList(authInfo!, pageNumber, 20, identifyStatus(selectedStatusFilter), selectedSymbolFilter, getSortObject(), getFolderFilter())
            .then(response => {
                setData([...data, ...response.analyzers]);
                setPageNumber(pageNumber + 1)
            })
            .catch(ex => {
                errorToast("Failed to fetch analyzers.");
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
            })
    }, [authInfo, data, getFolderFilter, getSortObject, logout, pageNumber, selectedStatusFilter, selectedSymbolFilter]);


    function isExistSelectedAnalyzers() {
        return (!selectAllCheckbox && selectedAnalyzers.length > 0) || (selectAllCheckbox && selectedAnalyzers.length < dataSize);
    }

    return (
        <div className="analyzer-content-body">
            <CreationSideBar ref={sideRef} symbols={symbols} folders={folders}
                             predefinedAnalyzerProps={duplicatedAnalyzer}
                             createAnalyzerBulkFunction={addAnalyzersBulk} createAnalyzerFunction={addAnalyzer}
                             closeAction={closeRightDrawer}/>
            <div className="analyzer-header">
                <div className="analyzer-header-container">
                    <div className="analyzer-header-path">
                        <PreviousPath>
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
                                role="button"
                                onClick={() => changeActiveFilter('ALL')}
                                className={selectedStatusFilter === 'ALL' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}>
                                <div>{dataSize} Analyzers</div>
                            </div>
                            <div
                                role="button"
                                onClick={() => changeActiveFilter('ACTIVE')}
                                className={selectedStatusFilter === 'ACTIVE' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}
                                style={{marginLeft: '4px'}}>
                                <ActiveIcon style={{alignSelf: 'center'}}/>
                                <div>{activeSize} Active</div>
                            </div>
                            <div
                                role="button"
                                onClick={() => changeActiveFilter('NOT_ACTIVE')}
                                className={selectedStatusFilter === 'NOT_ACTIVE' ? "analyzer-active-header-items-count" : "analyzer-header-items-count"}
                                style={{marginLeft: '4px'}}>
                                <NotActiveIcon style={{alignSelf: 'center'}}/>
                                <div>{notActiveSize} Not Active</div>
                            </div>
                        </RowDiv>
                        <RowDiv style={{position: 'relative', zIndex: '3'}}>
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
            <TableContainer style={{overflowY: 'auto', overflowX: 'auto', maxWidth: '100%'}}>
                <InfiniteScroll
                    loadMore={loadMoreAnalyzers}
                    hasMore={data.length < dataSize && !isTableLoading}
                    useWindow={false}
                    loader={<div className='loader' key={0}></div>}
                >
                    <Table size="small" stickyHeader>
                        <TableHead>
                            <TableRow id="analyzer-table-headers">
                                {isListPage() &&
                                    <TableCell id="cell" padding="checkbox">
                                        <StyledCheckbox
                                            onChange={handleSelectAllClick}
                                            checked={selectAllCheckbox}
                                        />
                                    </TableCell>
                                }
                                <TableCell id="cell">Symbol</TableCell>
                                {isListPage() && <TableCell width="82px" align="center" id="cell">Status</TableCell>}
                                <TableCell id="cell" ref={diapasonHeaderRef}>
                                    <HeaderCellContent onClick={() => handleSortChange('diapason')}>
                                        Diapason {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'diapason'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('gridSize')}>
                                        Grid Size {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'gridSize'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('multiplier')}>
                                        Multiplier {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'multiplier'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('positionStopLoss')}>
                                        SL {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'positionStopLoss'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell align="left" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('positionTakeProfit')}>
                                        TP {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'positionTakeProfit'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                {isListPage() &&
                                    <TableCell id="cell">
                                        <HeaderCellContent onClick={() => handleSortChange('startCapital')}>
                                            Start Capital <HeaderCellIcon direction={order}
                                                                          active={orderBy === 'startCapital'}/>
                                        </HeaderCellContent>
                                    </TableCell>}
                                {isListPage() &&
                                    <TableCell id="cell">
                                        <HeaderCellContent onClick={() => handleSortChange('money')}>
                                            Total Equity <HeaderCellIcon direction={order}
                                                                         active={orderBy === 'money'}/>
                                        </HeaderCellContent>
                                    </TableCell>}
                                <TableCell align="center" id="cell" ref={stabilityHeaderRef}>
                                    <HeaderCellContent onClick={() => handleSortChange('stabilityCoef')}>
                                        Stability {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'stabilityCoef'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell align="center" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('pNl1')}>
                                        1h % {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'pNl1'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell align="center" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('pNl12')}>
                                        12h % {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'pNl12'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell align="center" id="cell">
                                    <HeaderCellContent onClick={() => handleSortChange('pNl24')}>
                                        24h % {isListPage() &&
                                        <HeaderCellIcon direction={order} active={orderBy === 'pNl24'}/>}
                                    </HeaderCellContent>
                                </TableCell>
                                <TableCell align="center" id="cell">
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody style={{position: 'relative'}}>
                            {!!data &&
                            isTableLoading ? getLoadingRows() :
                                Array.isArray(data) && data.map((analyzer, index) => {
                                    const isItemSelected = isSelected(index);
                                    return (
                                        <TableRow key={analyzer.id} id="analyzer-table-content">
                                            {isListPage() &&
                                                <TableCell id="cell" padding="checkbox">
                                                    <StyledCheckbox
                                                        checked={isItemSelected}
                                                        onClick={() => handleSelectAnalyzer(index)}
                                                    />
                                                </TableCell>
                                            }
                                            <TableCell id="cell" onClick={() => navigateToAnalyzerDetail(analyzer.id)}>
                                                <Tooltip title={analyzer.symbol} enterDelay={500} leaveDelay={200}
                                                         placement={"right"} arrow>
                                                    {getSymbolIcon(analyzer.symbol)}
                                                </Tooltip>
                                            </TableCell>
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
                                            <TableCell id="cell">{analyzer.positionStopLoss}%</TableCell>
                                            <TableCell id="cell" align="left">{analyzer.positionTakeProfit}%</TableCell>
                                            {isListPage() &&
                                                <TableCell id="cell" align="left">${analyzer.startCapital}</TableCell>}
                                            {isListPage() &&
                                                <TableCell id="cell"
                                                           align="left">{!!analyzer.money && '$' + trimDecimalNumbers(analyzer.money)}</TableCell>}
                                            <TableCell align="left"
                                                       id="cell">{trimDecimalNumbers(analyzer.stabilityCoef, 1)}</TableCell>
                                            <TableCell align="left" id="cell"
                                                       style={{color: getPnLColorByValue(analyzer.pnl1)}}>
                                                {getSignByValue(analyzer.pnl1)}{trimDecimalNumbers(analyzer.pnl1, 1)} %
                                            </TableCell>
                                            <TableCell align="left" id="cell"
                                                       style={{color: getPnLColorByValue(analyzer.pnl12)}}>
                                                {getSignByValue(analyzer.pnl12)}{trimDecimalNumbers(analyzer.pnl12, 1)} %
                                            </TableCell>
                                            <TableCell align="left" id="cell"
                                                       style={{color: getPnLColorByValue(analyzer.pnl24)}}>
                                                {getSignByValue(analyzer.pnl24)}{trimDecimalNumbers(analyzer.pnl24, 1)} %
                                            </TableCell>
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
                                                <HideBox diapasonRef={diapasonHeaderRef} stabilityRef={stabilityHeaderRef}/>
                                            }
                                        </TableRow>
                                    );
                                })}
                        </TableBody>
                    </Table>
                </InfiniteScroll>
            </TableContainer>

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
                isExistSelectedAnalyzers() &&
                <div className="analyzer-selected-banner">
                    <div className="analyzer-selected-banner-content">
                        <div style={{
                            display: 'flex',
                            color: 'white',
                            fontSize: '14px',
                            fontWeight: 400,
                            marginBottom: '16px'
                        }}>
                            Selected {selectAllCheckbox ? (dataSize - selectedAnalyzers.length) : selectedAnalyzers.length} items
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
                                (containsActive() &&
                                    <Button variant="contained"
                                            onClick={deactivateSelectedAnalyzers}
                                            style={{
                                                display: 'flex',
                                                backgroundColor: 'red',
                                                color: 'white',
                                                textTransform: 'none',
                                                fontWeight: 700,
                                                marginLeft: '16px'
                                            }}>Stop{selectAllCheckbox ? ' (' + (activeSize - allDeselectedActive()) + ')' : ''}</Button>)
                            }
                            {
                                (containsNotActive() &&
                                    <Button variant="contained"
                                            onClick={activateSelectedAnalyzers}
                                            style={{
                                                display: 'flex',
                                                backgroundColor: '#16C079',
                                                color: 'white',
                                                textTransform: 'none',
                                                fontWeight: 700,
                                                marginLeft: '16px'
                                            }}>Activate{selectAllCheckbox ? ' (' + (notActiveSize - allDeselectedNotActive()) + ')' : ''}</Button>
                                )
                            }
                        </div>
                    </div>
                </div>
            }
        </div>
    );
}

export default AnalyzerContent;
