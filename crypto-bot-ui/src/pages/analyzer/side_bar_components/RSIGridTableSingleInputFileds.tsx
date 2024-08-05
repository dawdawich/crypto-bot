import React from "react";
import Select from "react-select";
import {SelectStyle} from "../../../utils/styles/element-styles";
import {kLineDurationValues} from "../../../model/AnalyzerResponse";
import {InputField} from "../../../shared/InputComponents";

type SingleInputFieldsModel = {
    gridSize: string;
    kLineDuration: string;
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onSelectChange: (name: string, value: any) => void;
}

const numberRegex = /^[0-9]+$/;

const RSIGridTableSingleInputFields: React.FC<SingleInputFieldsModel> = ({
    kLineDuration,
    gridSize,
    multiplier,
    stopLoss,
    takeProfit,
    onChange,
    onSelectChange
}) => {

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (numberRegex.test(e.target.value)) {
            onChange(e);
        } else {
            e.target.value = '';
            onChange(e);
        }
    }

    return <div>
        <div className="field-container">
            KLine Duration
            <Select
                placeholder=""
                name="kLineDuration"
                isSearchable={false}
                styles={SelectStyle}
                onChange={(newValue) => onSelectChange("kLineDuration", (newValue as any).value)}
                defaultInputValue={kLineDuration}
                options={kLineDurationValues.map((duration) => ({
                    value: duration,
                    label: duration
                }))}
            />
        </div>
        <div className={'field-container'}>
            Grid Size
            <InputField error={!!gridSize && Number(gridSize) < 1} type="text" name="gridSize" value={gridSize}
                        onChange={handleChange}/>
        </div>
        <div className={'field-container'}>
            Multiplier
            <InputField error={!!multiplier && Number(multiplier) < 1} type="text" name="multiplier"
                        value={multiplier}
                        onChange={handleChange}/>
        </div>
        <div className={'field-container'}>
            Stop Loss, %
            <InputField error={!!stopLoss && Number(stopLoss) < 1} type="text" name="stopLoss" value={stopLoss}
                        onChange={handleChange}/>
        </div>
        <div className={'field-container'}>
            Take Profit, %
            <InputField error={!!takeProfit && Number(takeProfit) < 1} type="text" name="takeProfit"
                        value={takeProfit}
                        onChange={handleChange}/>
        </div>
    </div>;
}

export default RSIGridTableSingleInputFields;
