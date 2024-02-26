import React, {useEffect, useState} from "react";
import {Button, styled} from "@mui/material";
import "../../css/pages/analyzer/SideBlock.css";
import {ReactComponent as CrossIcon} from '../../assets/images/action-icon/cross-icon.svg';
import Select from "react-select";
import {AntSwitch, MultiSelectStyle, SelectStyle} from "../../utils/styles/element-styles";
import {FolderModel} from "../../model/FolderModel";
import {errorToast} from "../toast/Toasts";
import {AnalyzerModel} from "../../model/AnalyzerModel";
import {AnalyzerModelBulk} from "../../model/AnalyzerModelBulk";
import {
    getMarketOptionFromValue,
    getStrategyOptionFromValue,
    MarketTypes,
    StrategyTypes
} from "../../model/AnalyzerConstants";

const SideBody = styled('div')({
    padding: '16px',
    display: 'flex',
    flexDirection: 'column'
});

export const RowDiv = styled('div')({
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
});

type CreationMode = 'SINGLE' | 'MULTI';

interface InitialProps {
    symbols: string[],
    predefinedAnalyzerProps: AnalyzerModel | null;
    folders: FolderModel[],
    createAnalyzerFunction: (analyzer: AnalyzerModel) => void;
    createAnalyzerBulkFunction: (analyzer: AnalyzerModelBulk) => void;
    closeAction: () => void;
}

const pattern = /\d+-\d+/;

type AnalyzerFieldsModel = {
    folders: string[];
    symbol: string | undefined;
    strategy: string | undefined;
    diapason: number | undefined;
    gridSize: number | undefined;
    multiplier: number | undefined;
    stopLoss: number | undefined;
    takeProfit: number | undefined;
    startCapital: number | undefined;
    market: string | undefined;
    demo: boolean | undefined;
    activate: boolean;
    public: boolean;
};

type MultiAnalyzerFieldsModel = {
    folders: string[];
    symbol: string[];
    strategy: string | undefined;
    diapasonMin: number | undefined;
    diapasonMax: number | undefined;
    diapasonStep: number;
    gridSizeMin: number | undefined;
    gridSizeMax: number | undefined;
    gridSizeStep: number;
    multiplierMin: number | undefined;
    multiplierMax: number | undefined;
    multiplierStep: number;
    stopLossMin: number | undefined;
    stopLossMax: number | undefined;
    stopLossStep: number;
    takeProfitMin: number | undefined;
    takeProfitMax: number | undefined;
    takeProfitStep: number;
    startCapital: number | undefined;
    market: string | undefined;
    demo: boolean | undefined;
    activate: boolean;
    public: boolean;
};

const CreationSideBar = React.forwardRef<HTMLDivElement, InitialProps>((props, ref) => {
    const [creationMode, setCreationMode] = useState<CreationMode>('SINGLE');
    const [singleAnalyzerModel, setSingleAnalyzerModel] = useState<AnalyzerFieldsModel>({
        folders: [],
        symbol: undefined,
        strategy: undefined,
        diapason: undefined,
        gridSize: undefined,
        multiplier: undefined,
        stopLoss: undefined,
        takeProfit: undefined,
        startCapital: undefined,
        market: undefined,
        demo: undefined,
        activate: false,
        public: false
    });
    const [multiAnalyzerModel, setMultiAnalyzerModel] = useState<MultiAnalyzerFieldsModel>({
        folders: [],
        symbol: [],
        strategy: undefined,
        diapasonMin: undefined,
        diapasonMax: undefined,
        diapasonStep: 1,
        gridSizeMin: undefined,
        gridSizeMax: undefined,
        gridSizeStep: 1,
        multiplierMin: undefined,
        multiplierMax: undefined,
        multiplierStep: 1,
        stopLossMin: undefined,
        stopLossMax: undefined,
        stopLossStep: 1,
        takeProfitMin: undefined,
        takeProfitMax: undefined,
        takeProfitStep: 1,
        startCapital: undefined,
        market: undefined,
        demo: undefined,
        activate: false,
        public: false
    });

    useEffect(() => {
        if (props.predefinedAnalyzerProps !== null) {
            const analyzer = props.predefinedAnalyzerProps;
            console.log(JSON.stringify(analyzer));
            setSingleAnalyzerModel({
                folders: [],
                symbol: analyzer.symbol,
                strategy: analyzer.strategy,
                diapason: analyzer.diapason,
                gridSize: analyzer.gridSize,
                multiplier: analyzer.multiplier,
                stopLoss: analyzer.stopLoss,
                takeProfit: analyzer.takeProfit,
                startCapital: analyzer.startCapital,
                market: analyzer.market,
                demo: analyzer.demoAccount,
                activate: analyzer.active,
                public: analyzer.public
            });
        }
    }, [props.predefinedAnalyzerProps]);

    const checkAndHandleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {value, name} = e.target;
        if (pattern.test(value)) {
            const [min, max] = value.split("-");
            if (parseInt(min) < parseInt(max)) {
                setMultiAnalyzerModel({
                    ...multiAnalyzerModel,
                    [name + 'Min']: min,
                    [name + 'Max']: max
                });
            }
        }
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value, type, checked} = e.target;
        if (creationMode === 'SINGLE') {
            setSingleAnalyzerModel({
                ...singleAnalyzerModel,
                [name]: type === 'checkbox' ? checked : value
            });
        } else {
            setMultiAnalyzerModel({
                ...multiAnalyzerModel,
                [name]: type === 'checkbox' ? checked : value
            });
        }
    };

    const handleSelectChange = (name: string, value: any) => {
        if (creationMode === 'SINGLE') {
            setSingleAnalyzerModel({
                ...singleAnalyzerModel,
                [name]: Array.isArray(value) ? value.map(el => el.value) : value
            });
        } else {
            setMultiAnalyzerModel({
                ...multiAnalyzerModel,
                [name]: Array.isArray(value) ? value.map(el => el.value) : value
            });
        }
    };

    const getSwitcherPressedStyle = (mode: CreationMode) => creationMode === mode ? {
        backgroundColor: '#D0FF12', color: '#1D2024'
    } : {};

    const validateDiapasonableField = (value: string | undefined) => {
        if (value !== undefined && pattern.test(value)) {
            const [first, second] = value.split('-');
            return parseInt(first) < parseInt(second);
        }

        return false;
    };

    const validateField = (name: string) => {
        if (creationMode === 'SINGLE') {
            return (singleAnalyzerModel as any)[name] !== undefined;
        } else {
            return validateDiapasonableField((multiAnalyzerModel as any)[name]);
        }
    }

    const validateRangeFields = (fieldName: string) => {
        const min = (multiAnalyzerModel as any)[fieldName + 'Min'];
        const max = (multiAnalyzerModel as any)[fieldName + 'Max'];
        const step = (multiAnalyzerModel as any)[fieldName + 'Step'];
        if (min !== undefined && max !== undefined && step !== undefined) {
            const difference = Math.abs(min - max);
            return step >= 1 && step <= difference;
        }

        return false;
    }

    const validateSymbolField = () => {
        if (creationMode === 'SINGLE') {
            return singleAnalyzerModel.symbol !== undefined;
        } else {
            return multiAnalyzerModel.symbol.length > 0;
        }
    };

    const validateStrategyField = () => creationMode === 'SINGLE' ? singleAnalyzerModel.strategy !== undefined : multiAnalyzerModel.strategy !== undefined;

    const validateDiapasonField = () => validateField('diapason');
    const validateGridSizeField = () => validateField('gridSize');
    const validateMultiplierField = () => validateField('multiplier');
    const validateStopLossField = () => validateField('stopLoss');
    const validateTakeProfitField = () => validateField('takeProfit');
    const validateStartCapitalField = () => creationMode === 'SINGLE' ? singleAnalyzerModel.startCapital !== undefined : multiAnalyzerModel.startCapital !== undefined;
    const validateMarketField = () => creationMode === 'SINGLE' ? singleAnalyzerModel.market !== undefined : multiAnalyzerModel.market !== undefined;
    const validateDemoField = () => creationMode === 'SINGLE' ? singleAnalyzerModel.demo !== undefined : multiAnalyzerModel.market !== undefined;
    const validateDiapasonStepField = () => validateRangeFields('diapason');
    const validateGridSizeStepField = () => validateRangeFields('gridSize');
    const validateMultiplierStepField = () => validateRangeFields('multiplier');
    const validateStopLossStepField = () => validateRangeFields('stopLoss');
    const validateTakeProfitStepField = () => validateRangeFields('takeProfit');

    const validateAllFieldsSingle = () => validateSymbolField() && validateStrategyField() && validateDiapasonField() &&
        validateGridSizeField() && validateMultiplierField() && validateStopLossField() && validateTakeProfitField() &&
        validateStartCapitalField() && validateMarketField() && validateDemoField();

    const validateAllFieldsMulti = () => validateSymbolField() && validateStrategyField() && validateDiapasonStepField() &&
        validateGridSizeStepField() && validateMultiplierStepField() && validateStopLossStepField() && validateTakeProfitStepField() &&
        validateStartCapitalField() && validateMarketField() && validateDemoField();

    const calculateDiapasonFieldWithStepCount = (fieldName: string) => {
        if (validateRangeFields(fieldName)) {
            const min = (multiAnalyzerModel as any)[fieldName + 'Min'];
            const max = (multiAnalyzerModel as any)[fieldName + 'Max'];
            const step = (multiAnalyzerModel as any)[fieldName + 'Step'];
            return ((max - min) / step) + 1;
        }
        return 1;
    }

    const calculateAnalyzersToCreate = () => calculateDiapasonFieldWithStepCount('diapason') *
        calculateDiapasonFieldWithStepCount('gridSize') *
        calculateDiapasonFieldWithStepCount('multiplier') *
        calculateDiapasonFieldWithStepCount('stopLoss') *
        calculateDiapasonFieldWithStepCount('takeProfit') *
        multiAnalyzerModel.symbol.length;


    const getDemoOptionFromValue = (value: boolean | undefined) => value !== undefined ? value ? {value: true, label: 'True'} : {value: false, label: 'False'} : undefined;

    const createAnalyzer = () => {
        if ((creationMode === 'SINGLE' && validateAllFieldsSingle()) || (creationMode === 'MULTI' && validateAllFieldsMulti())) {
            if (creationMode === 'SINGLE') {
                props.createAnalyzerFunction(
                    {
                        diapason: singleAnalyzerModel.diapason as number,
                        gridSize: singleAnalyzerModel.gridSize as number,
                        multiplier: singleAnalyzerModel.multiplier as number,
                        stopLoss: singleAnalyzerModel.stopLoss as number,
                        takeProfit: singleAnalyzerModel.takeProfit as number,
                        symbol: singleAnalyzerModel.symbol as string,
                        startCapital: singleAnalyzerModel.startCapital as number,
                        active: singleAnalyzerModel.activate,
                        public: singleAnalyzerModel.public,
                        folders: singleAnalyzerModel.folders,
                        strategy: singleAnalyzerModel.strategy as string,
                        market: singleAnalyzerModel.market as string,
                        demoAccount: singleAnalyzerModel.demo as boolean
                    }
                );
            } else {
                props.createAnalyzerBulkFunction({
                    symbols: multiAnalyzerModel.symbol,
                    stopLossMin: multiAnalyzerModel.stopLossMin as number,
                    stopLossMax: multiAnalyzerModel.stopLossMax as number,
                    stopLossStep: multiAnalyzerModel.stopLossStep,
                    takeProfitMin: multiAnalyzerModel.takeProfitMin as number,
                    takeProfitMax: multiAnalyzerModel.takeProfitMax as number,
                    takeProfitStep: multiAnalyzerModel.takeProfitStep,
                    diapasonMin: multiAnalyzerModel.diapasonMin as number,
                    diapasonMax: multiAnalyzerModel.diapasonMax as number,
                    diapasonStep: multiAnalyzerModel.diapasonStep,
                    gridSizeMin: multiAnalyzerModel.gridSizeMin as number,
                    gridSizeMax: multiAnalyzerModel.gridSizeMax as number,
                    gridSizeStep: multiAnalyzerModel.gridSizeStep,
                    multiplierMin: multiAnalyzerModel.multiplierMin as number,
                    multiplierMax: multiAnalyzerModel.multiplierMax as number,
                    multiplierStep: multiAnalyzerModel.multiplierStep,
                    startCapital: multiAnalyzerModel.startCapital as number,
                    demoAccount: multiAnalyzerModel.demo as boolean,
                    market: multiAnalyzerModel.market as string,
                    active: multiAnalyzerModel.activate,
                    public: multiAnalyzerModel.public,
                    strategy: multiAnalyzerModel.strategy as string,
                    folders: multiAnalyzerModel.folders
                });
            }
            return;
        }
        errorToast("All fields should be filled");
    }

    return (
        <div ref={ref} className="side-nav">
            <SideBody>
                <RowDiv>
                    <div className="header-label">New analyzer</div>
                    <CrossIcon style={{fill: 'white', cursor: 'pointer'}} onClick={() => {
                        return props.closeAction();
                    }}/>
                </RowDiv>
                <div className="switch-container">
                    <div className="switch-container-button" onClick={() => setCreationMode('SINGLE')}
                         style={getSwitcherPressedStyle('SINGLE')}>Single analyzer
                    </div>
                    <div className="switch-container-button" onClick={() => setCreationMode('MULTI')}
                         style={getSwitcherPressedStyle('MULTI')}>Diapason
                    </div>
                </div>
                <div className="field-container">
                    Folders
                    <Select
                        placeholder=""
                        name="folders"
                        isMulti
                        isSearchable={false}
                        styles={MultiSelectStyle}
                        onChange={(newValue) => handleSelectChange("folders", newValue)}
                        options={props.folders.map((folder) => ({
                            value: folder.id,
                            label: folder.name
                        }))}
                    />
                </div>
                <div className="field-container">
                    Symbol
                    <Select
                        placeholder=""
                        name="symbol"
                        isMulti={creationMode === 'MULTI'}
                        isSearchable={false}
                        styles={MultiSelectStyle}
                        value={creationMode === 'SINGLE' ? {
                            value: singleAnalyzerModel.symbol,
                            label: singleAnalyzerModel.symbol
                        } : multiAnalyzerModel.symbol.map(el => ({value: el, label: el}))}
                        onChange={(newValue) => handleSelectChange("symbol", Array.isArray(newValue) ? newValue : (newValue as any).value)}
                        options={props.symbols.map(el => ({value: el, label: el}))}
                    />
                </div>
                <div className="field-container">
                    Strategy
                    <Select
                        placeholder=""
                        name="strategy"
                        isSearchable={false}
                        value={creationMode === 'SINGLE' ? getStrategyOptionFromValue(singleAnalyzerModel.strategy) : getStrategyOptionFromValue(multiAnalyzerModel.strategy)}
                        onChange={(newValue) => handleSelectChange("strategy", (newValue as any).value)}
                        styles={SelectStyle}
                        options={StrategyTypes}
                    />
                </div>
                {creationMode === 'SINGLE' ?
                    <div className="field-container">
                        Diapason, %
                        <input type="number" name="diapason" value={singleAnalyzerModel.diapason}
                               className="text-field-style" onChange={handleChange}/>
                    </div> :
                    <div className="several-fields-container">
                        <div className="field-container" style={{marginTop: '0', marginRight: '8px', width: '100%'}}>
                            Diapason, %
                            <input type="text" name="diapason" placeholder="1-2" className="text-field-style"
                                   onChange={checkAndHandleChange}/>
                        </div>
                        <div className="field-container" style={{marginTop: '0', width: '80px'}}>
                            Step
                            <input type="number" min="1" value={multiAnalyzerModel.diapasonStep} name="diapasonStep"
                                   className="text-field-style"
                                   onChange={handleChange}/>
                        </div>
                    </div>
                }
                {creationMode === 'SINGLE' ?
                    <div className="field-container">
                        Grid Size
                        <input type="number" name="gridSize" value={singleAnalyzerModel.gridSize}
                               className="text-field-style" onChange={handleChange}/>
                    </div> :
                    <div className="several-fields-container">
                        <div className="field-container" style={{marginTop: '0', marginRight: '8px', width: '100%'}}>
                            Grid Size
                            <input type="text" name="gridSize" placeholder="1-2" className="text-field-style"
                                   onChange={checkAndHandleChange}/>
                        </div>
                        <div className="field-container" style={{marginTop: '0', width: '80px'}}>
                            Step
                            <input type="number" min="1" value={multiAnalyzerModel.gridSizeStep} name="gridSizeStep"
                                   className="text-field-style"
                                   onChange={handleChange}/>
                        </div>
                    </div>
                }
                {creationMode === 'SINGLE' ?
                    <div className="field-container">
                        Multiplier
                        <input type="number" name="multiplier" value={singleAnalyzerModel.multiplier}
                               className="text-field-style" onChange={handleChange}/>
                    </div> :
                    <div className="several-fields-container">
                        <div className="field-container" style={{marginTop: '0', marginRight: '8px', width: '100%'}}>
                            Multiplier
                            <input type="text" name="multiplier" placeholder="1-2" className="text-field-style"
                                   onChange={checkAndHandleChange}/>
                        </div>
                        <div className="field-container" style={{marginTop: '0', width: '80px'}}>
                            Step
                            <input type="number" min="1" value={multiAnalyzerModel.multiplierStep} name="multiplierStep"
                                   className="text-field-style"
                                   onChange={handleChange}/>
                        </div>
                    </div>
                }
                {creationMode === 'SINGLE' ?
                    <div className="field-container">
                        Stop Loss, %
                        <input type="number" name="stopLoss" value={singleAnalyzerModel.stopLoss}
                               className="text-field-style" onChange={handleChange}/>
                    </div> :
                    <div className="several-fields-container">
                        <div className="field-container" style={{marginTop: '0', marginRight: '8px', width: '100%'}}>
                            Stop Loss, %
                            <input type="text" name="stopLoss" placeholder="1-2" className="text-field-style"
                                   onChange={checkAndHandleChange}/>
                        </div>
                        <div className="field-container" style={{marginTop: '0', width: '80px'}}>
                            Step
                            <input type="number" min="1" name="stopLossStep" value={multiAnalyzerModel.stopLossStep}
                                   className="text-field-style"
                                   onChange={handleChange}/>
                        </div>
                    </div>
                }
                {creationMode === 'SINGLE' ?
                    <div className="field-container">
                        Take Profit, %
                        <input type="number" name="takeProfit" value={singleAnalyzerModel.takeProfit}
                               className="text-field-style" onChange={handleChange}/>
                    </div> :
                    <div className="several-fields-container">
                        <div className="field-container" style={{marginTop: '0', marginRight: '8px', width: '100%'}}>
                            Take Profit, %
                            <input type="text" name="takeProfit" placeholder="1-2" className="text-field-style"
                                   onChange={checkAndHandleChange}/>
                        </div>
                        <div className="field-container" style={{marginTop: '0', width: '80px'}}>
                            Step
                            <input type="number" min="1" name="takeProfitStep" value={multiAnalyzerModel.takeProfitStep}
                                   className="text-field-style"
                                   onChange={handleChange}/>
                        </div>
                    </div>
                }
                <div className="field-container">
                    Start Capital, $
                    <input type="number" name="startCapital"
                           value={creationMode === 'SINGLE' ? singleAnalyzerModel.startCapital : multiAnalyzerModel.startCapital}
                           className="text-field-style" onChange={handleChange}/>
                </div>
                <div className="field-container">
                    Market
                    <Select
                        placeholder=""
                        name="market"
                        isSearchable={false}
                        styles={SelectStyle}
                        value={creationMode === 'SINGLE' ? getMarketOptionFromValue(singleAnalyzerModel.market) : getMarketOptionFromValue(multiAnalyzerModel.market)}
                        onChange={(newValue) => handleSelectChange("market", (newValue as any).value)}
                        options={[{value: 'BYBIT', label: 'ByBit'}]}
                    />
                </div>
                <div className="field-container">
                    Demo Account
                    <Select
                        placeholder=""
                        name="demo"
                        isSearchable={false}
                        onChange={(newValue) => handleSelectChange("demo", (newValue as any).value)}
                        value={creationMode === 'SINGLE' ? getDemoOptionFromValue(singleAnalyzerModel.demo) : getDemoOptionFromValue(multiAnalyzerModel.demo)}
                        styles={SelectStyle}
                        options={[{value: true, label: 'True'}, {value: false, label: 'False'}]}
                    />
                </div>
                {creationMode === 'MULTI' &&
                    <div className="count-field-container">
                        ! Will be created {calculateAnalyzersToCreate()} Analyzers !
                    </div>
                }
                <div style={{
                    marginTop: '16px',
                    display: 'flex',
                    flexDirection: 'row',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                }}>
                    <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
                        <AntSwitch name="activate" onChange={handleChange}
                                   checked={creationMode === 'SINGLE' ? singleAnalyzerModel.activate : multiAnalyzerModel.activate}/>
                        <div style={{marginLeft: '8px', fontWeight: 200, fontSize: '14px', color: 'white'}}>Activate
                        </div>
                    </div>
                    <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
                        <AntSwitch name="public" onChange={handleChange}
                                   checked={creationMode === 'SINGLE' ? singleAnalyzerModel.public : multiAnalyzerModel.public}/>
                        <div style={{marginLeft: '8px', fontWeight: 200, fontSize: '14px', color: 'white'}}>Public
                        </div>
                    </div>
                    <Button variant='contained'
                            onClick={createAnalyzer}
                            disabled={creationMode === 'SINGLE' ? !validateAllFieldsSingle() : !validateAllFieldsMulti()}
                            style={{
                                backgroundColor: '#D0FF12',
                                color: '#121417',
                                textTransform: 'none'
                            }}>Create</Button>
                </div>
            </SideBody>
        </div>
    );
})

export default CreationSideBar;
