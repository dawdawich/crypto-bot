import React, {useEffect, useState} from "react";
import {Button, styled} from "@mui/material";
import "../../../css/pages/analyzer/SideBlock.css";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import Select from "react-select";
import {AntSwitch, MultiSelectStyle, SelectStyle} from "../../../utils/styles/element-styles";
import {FolderModel} from "../../../model/FolderModel";
import {errorToast} from "../../../shared/toast/Toasts";
import {AnalyzerModel} from "../../../model/AnalyzerModel";
import {AnalyzerModelBulk} from "../../../model/AnalyzerModelBulk";
import {getMarketOptionFromValue, getStrategyOptionFromValue, StrategyTypes} from "../../../model/AnalyzerConstants";
import {InputField} from "../../../shared/InputComponents";
import SingleInputFields from "./SingleInputFields";
import RangeInputFields from "./RangeInputFields";

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

const PATTERN = /\d+(-\d+)?/;

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
            setMultiAnalyzerModel({
                    folders: [],
                    symbol: [analyzer.symbol],
                    strategy: analyzer.strategy,
                    diapasonMin: analyzer.diapason,
                    diapasonMax: undefined,
                    diapasonStep: 1,
                    gridSizeMin: analyzer.gridSize,
                    gridSizeMax: undefined,
                    gridSizeStep: 1,
                    multiplierMin: analyzer.multiplier,
                    multiplierMax: undefined,
                    multiplierStep: 1,
                    stopLossMin: analyzer.stopLoss,
                    stopLossMax: undefined,
                    stopLossStep: 1,
                    takeProfitMin: analyzer.takeProfit,
                    takeProfitMax: undefined,
                    takeProfitStep: 1,
                    startCapital: analyzer.startCapital,
                    market: analyzer.market,
                    demo: analyzer.demoAccount,
                    activate: analyzer.active,
                    public: analyzer.public
                }
            );
        }
    }, [props.predefinedAnalyzerProps]);

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
        if (value !== undefined && PATTERN.test(value)) {
            const [first, second] = value.split('-');
            return parseInt(first) < parseInt(second);
        }

        return false;
    };

    const validateField = (name: string) => {
        if (creationMode === 'SINGLE') {
            return (singleAnalyzerModel as any)[name] !== undefined && (singleAnalyzerModel as any)[name] !== '';
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
        return min !== undefined;
    }

    const validateSymbolField = () => {
        if (creationMode === 'SINGLE') {
            return singleAnalyzerModel.symbol !== undefined;
        } else {
            return multiAnalyzerModel.symbol.length > 0;
        }
    };

    const validateStrategyField = () => creationMode === 'SINGLE' ? singleAnalyzerModel.strategy !== undefined : multiAnalyzerModel.strategy !== undefined;

    const validateDiapasonField = () => validateField('diapason') && singleAnalyzerModel.diapason! > 0 && singleAnalyzerModel.diapason! < 51;
    const validateGridSizeField = () => validateField('gridSize') && singleAnalyzerModel.gridSize! > 1;
    const validateMultiplierField = () => validateField('multiplier') && singleAnalyzerModel.multiplier! > 0 && singleAnalyzerModel.multiplier! < 151;
    const validateStopLossField = () => validateField('stopLoss') && singleAnalyzerModel.stopLoss! >= 0;
    const validateTakeProfitField = () => validateField('takeProfit') && singleAnalyzerModel.takeProfit! >= 0;
    const validateStartCapitalField = () => creationMode === 'SINGLE' ?
        (singleAnalyzerModel.startCapital !== undefined && singleAnalyzerModel.startCapital.toString() !== '') :
        (multiAnalyzerModel.startCapital !== undefined && multiAnalyzerModel.startCapital.toString() !== '');
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

    const validatingDependingOnType = () => creationMode === 'SINGLE' ? validateAllFieldsSingle() : validateAllFieldsMulti();

    const calculateDiapasonFieldWithStepCount = (fieldName: string) => {
        if (validateRangeFields(fieldName)) {
            const min = (multiAnalyzerModel as any)[fieldName + 'Min'];
            const max = (multiAnalyzerModel as any)[fieldName + 'Max'];
            const step = (multiAnalyzerModel as any)[fieldName + 'Step'];
            if (!!max && !!step) {
                return ((max - min) / step) + 1;
            }
        }
        return 1;
    }

    const applyDiapasonField = (field: any) => {
        Object.entries(field).forEach(([key, value]) => {
            if (String(value).indexOf('-') > -1) {
                const values = String(value).split('-');
                setMultiAnalyzerModel({
                    ...multiAnalyzerModel,
                    [key + 'Min']: values[0],
                    [key + 'Max']: values[1],
                })
            } else {
                setMultiAnalyzerModel({
                    ...multiAnalyzerModel,
                    [key + 'Min']: value,
                })
            }
        })
    }

    const applyStepField = (e: React.ChangeEvent<HTMLInputElement>) => {
        setMultiAnalyzerModel({
            ...multiAnalyzerModel,
            [e.target.name]: e.target.value
        });
    }

    const calculateAnalyzersToCreate = () => {
        console.log(calculateDiapasonFieldWithStepCount('diapason'));
        console.log(calculateDiapasonFieldWithStepCount('gridSize'));
        console.log(calculateDiapasonFieldWithStepCount('multiplier'));
        console.log(calculateDiapasonFieldWithStepCount('stopLoss'));
        console.log(calculateDiapasonFieldWithStepCount('takeProfit'));

        return calculateDiapasonFieldWithStepCount('diapason') *
            calculateDiapasonFieldWithStepCount('gridSize') *
            calculateDiapasonFieldWithStepCount('multiplier') *
            calculateDiapasonFieldWithStepCount('stopLoss') *
            calculateDiapasonFieldWithStepCount('takeProfit') *
            multiAnalyzerModel.symbol.length;
    };


    const getDemoOptionFromValue = (value: boolean | undefined) => {
        if (value === undefined) {
            return undefined;
        }
        return value ? {value: true, label: 'True'} : {value: false, label: 'False'};
    };
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
                    <div role="button" className="switch-container-button" onClick={() => setCreationMode('SINGLE')}
                         style={getSwitcherPressedStyle('SINGLE')}>Single analyzer
                    </div>
                    <div role="button" className="switch-container-button" onClick={() => setCreationMode('MULTI')}
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
                {/*Rangeble Input Fields*/}
                {creationMode === 'SINGLE' ?
                    <SingleInputFields
                        diapason={!!singleAnalyzerModel.diapason ? singleAnalyzerModel.diapason.toString() : ''}
                        gridSize={!!singleAnalyzerModel.gridSize ? singleAnalyzerModel.gridSize.toString() : ''}
                        multiplier={!!singleAnalyzerModel.multiplier ? singleAnalyzerModel.multiplier.toString() : ''}
                        stopLoss={!!singleAnalyzerModel.stopLoss ? singleAnalyzerModel.stopLoss.toString() : ''}
                        takeProfit={!!singleAnalyzerModel.takeProfit ? singleAnalyzerModel.takeProfit.toString() : ''}
                        onChange={handleChange}/> :
                    <RangeInputFields diapason={!!multiAnalyzerModel.diapasonMin ? multiAnalyzerModel.diapasonMin.toString() : ''}
                                      gridSize={!!multiAnalyzerModel.gridSizeMin ? multiAnalyzerModel.gridSizeMin.toString() : ''}
                                      multiplier={!!multiAnalyzerModel.multiplierMin ? multiAnalyzerModel.multiplierMin.toString() : ''}
                                      stopLoss={!!multiAnalyzerModel.stopLossMin ? multiAnalyzerModel.stopLossMin.toString() : ''}
                                      takeProfit={!!multiAnalyzerModel.takeProfitMin ? multiAnalyzerModel.takeProfitMin.toString() : ''}
                                      onChange={applyDiapasonField}
                                      onStepChange={applyStepField}/>}
                {/*-----------------------------*/}
                <div className="field-container">
                    Start Capital, $
                    <InputField
                        error={(!!singleAnalyzerModel.startCapital || !!multiAnalyzerModel.startCapital) && !validateStartCapitalField()}
                        type="number"
                        name="startCapital"
                        value={creationMode === 'SINGLE' ? singleAnalyzerModel.startCapital : multiAnalyzerModel.startCapital}
                        onChange={handleChange}/>
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
                            disabled={!validatingDependingOnType()}
                            style={{
                                backgroundColor: validatingDependingOnType() ? '#D0FF12' : '#121417',
                                color: validatingDependingOnType() ? '#121417' : '#D0FF12',
                                textTransform: 'none',
                                transition: '0.3s'
                            }}>Create</Button>
                </div>
            </SideBody>
        </div>
    );
})

export default CreationSideBar;
