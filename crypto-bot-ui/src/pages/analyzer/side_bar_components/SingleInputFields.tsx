import React from "react";
import {InputField} from "../../../shared/InputComponents";

type SingleInputFieldsModel = {
    diapason: string;
    gridSize: string;
    multiplier: string;
    stopLoss: string;
    takeProfit: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const numberRegex = /^[0-9]+$/;

const SingleInputFields: React.FC<SingleInputFieldsModel> = ({
                                                                 diapason,
                                                                 gridSize,
                                                                 multiplier,
                                                                 stopLoss,
                                                                 takeProfit,
                                                                 onChange
                                                             }) => {

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (numberRegex.test(e.target.value)) {
            onChange(e);
        }
    }

    return (
        <div>
            <div className={'field-container'}>
                Diapason, %
                <InputField error={!!diapason && Number(diapason) < 1} type="text" name="diapason" value={diapason}
                            onChange={handleChange}/>
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
        </div>
    );
}

export default SingleInputFields;
