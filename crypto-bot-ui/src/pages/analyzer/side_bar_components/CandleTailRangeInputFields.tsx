import React from "react";
import {InputField} from "../../../shared/InputComponents";
import Select from "react-select";
import {MultiSelectStyle, SelectStyle} from "../../../utils/styles/element-styles";
import {kLineDurationValues} from "../../../model/AnalyzerResponse";

interface CandleTailRangeInputFieldsProps {
    kLineDurations: number[];
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    onChange: (value: any) => void;
    onStepChange: (value: any) => void;
    onSelectChange: (name: string, value: any) => void;
}

type CandleTailRangeInputFieldsModel = {
    kLineDurations: number[];
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    multiplierStep: string;
    stopLossStep: string;
    takeProfitStep: string;
}

const numberRangeRegex = /^(\d+(-\d*)?)?$/;
const numberRegex = /^\d*$/;

const CandleTailRangeInputFields: React.FC<CandleTailRangeInputFieldsProps> = (props) => {
    const [inputs, setInputs] = React.useState<CandleTailRangeInputFieldsModel>({
        kLineDurations: props.kLineDurations,
        multiplier: props.multiplier,
        stopLoss: props.stopLoss,
        takeProfit: props.takeProfit,
        multiplierStep: '1',
        stopLossStep: '1',
        takeProfitStep: '1'
    })

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (numberRangeRegex.test(e.target.value)) {
            setInputs({
                ...inputs,
                [e.target.name]: e.target.value
            });
            if (validateDiapason(e.target.value)) {
                props.onChange({
                    [e.target.name]: e.target.value
                });
            }
        }
    }

    const handleStepChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (numberRegex.test(e.target.value)) {
            setInputs({
                ...inputs,
                [e.target.name]: e.target.value
            });
            if (Number(e.target.value) > 0) {
                props.onStepChange(e);
            }
        }
    }

    const validateDiapason = (diapason: string): boolean => {
        if (diapason !== '') {
            if (diapason.indexOf('-') > -1) {
                const values = diapason.split('-');
                return values.length === 2 && Number(values[0]) > 0 && Number(values[0]) < Number(values[1]);
            }
            return Number(diapason) > 0;
        }
        return true;
    }

    return (
        <div>
            <div className="field-container">
                KLine Duration
                <Select
                    placeholder=""
                    name="kLineDuration"
                    isSearchable={false}
                    isMulti
                    styles={MultiSelectStyle}
                    onChange={(newValue) => props.onSelectChange("kLineDurations", newValue)}
                    defaultValue={inputs.kLineDurations.map((duration) => ({
                        value: duration,
                        label: duration
                    }))}
                    options={kLineDurationValues.map((duration) => ({
                        value: duration,
                        label: duration
                    }))}
                />
            </div>
            <div className={'several-fields-container'}>
                <div className={'range-field-container'}>
                    Multiplier
                    <InputField error={!!inputs.multiplier && !validateDiapason(inputs.multiplier)} type="text"
                                name="multiplier" value={inputs.multiplier} onChange={handleChange}/>
                </div>
                <div className="step-field-container">
                    Step
                    <InputField
                        error={!!inputs.multiplierStep && Number(inputs.multiplierStep) < 1}
                        type="text" value={inputs.multiplierStep} name="multiplierStep" onChange={handleStepChange}/>
                </div>
            </div>
            <div className={'several-fields-container'}>
                <div className={'range-field-container'}>
                    Stop Loss, %
                    <InputField error={!!inputs.stopLoss && !validateDiapason(inputs.stopLoss)} type="text"
                                name="stopLoss"
                                value={inputs.stopLoss} onChange={handleChange}/>
                </div>
                <div className="step-field-container">
                    Step
                    <InputField
                        error={!!inputs.stopLossStep && Number(inputs.stopLossStep) < 1} type="text"
                        value={inputs.stopLossStep} name="stopLossStep" onChange={handleStepChange}/>
                </div>
            </div>
            <div className={'several-fields-container'}>
                <div className={'range-field-container'}>
                    Take Profit, %
                    <InputField error={!!inputs.takeProfit && !validateDiapason(inputs.takeProfit)} type="text"
                                name="takeProfit" value={inputs.takeProfit} onChange={handleChange}/>
                </div>
                <div className="step-field-container">
                    Step
                    <InputField
                        error={!!inputs.takeProfitStep && Number(inputs.takeProfitStep) < 1} type="text"
                        value={inputs.takeProfitStep} name="takeProfitStep" onChange={handleStepChange}/>
                </div>
            </div>
        </div>
    );
}

export default CandleTailRangeInputFields;
