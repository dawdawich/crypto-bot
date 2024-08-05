import React from "react";
import {InputField} from "../../../shared/InputComponents";

interface RangeInputFieldsProps {
    diapason: string;
    gridSize: string;
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    onChange: (value: any) => void;
    onStepChange: (value: any) => void;
}

type RangeInputFieldsModel = {
    diapason: string;
    gridSize: string;
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    diapasonStep: string;
    gridSizeStep: string;
    multiplierStep: string;
    stopLossStep: string;
    takeProfitStep: string;
}

const numberRangeRegex = /^(\d+(-\d*)?)?$/;
const numberRegex = /^\d*$/;

const GridTableRangeInputFields: React.FC<RangeInputFieldsProps> = (props) => {

    const [inputs, setInputs] = React.useState<RangeInputFieldsModel>({
        diapason: props.diapason,
        gridSize: props.gridSize,
        multiplier: props.multiplier,
        stopLoss: props.stopLoss,
        takeProfit: props.takeProfit,
        diapasonStep: '1',
        gridSizeStep: '1',
        multiplierStep: '1',
        stopLossStep: '1',
        takeProfitStep: '1'
    });

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
            <div className={'several-fields-container'}>
                <div className={'range-field-container'}>
                    Diapason, %
                    <InputField error={!!inputs.diapason && !validateDiapason(inputs.diapason)} type="text" name="diapason"
                                value={inputs.diapason} onChange={handleChange}/>
                </div>
                <div className="step-field-container">
                    Step
                    <InputField
                        error={!!inputs.diapasonStep && Number(inputs.diapasonStep) < 1} type="text"
                        value={inputs.diapasonStep} name="diapasonStep" onChange={handleStepChange}/>
                </div>
            </div>
            <div className={'several-fields-container'}>
                <div className={'range-field-container'}>
                    Grid Size
                    <InputField error={!!inputs.gridSize && !validateDiapason(inputs.gridSize)} type="text" name="gridSize"
                                value={inputs.gridSize} onChange={handleChange}/>
                </div>
                <div className="step-field-container">
                    Step
                    <InputField
                        error={!!inputs.gridSizeStep && Number(inputs.gridSizeStep) < 1} type="text"
                        value={inputs.gridSizeStep} name="gridSizeStep" onChange={handleStepChange}/>
                </div>
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
                    <InputField error={!!inputs.stopLoss && !validateDiapason(inputs.stopLoss)} type="text" name="stopLoss"
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

export default GridTableRangeInputFields;
