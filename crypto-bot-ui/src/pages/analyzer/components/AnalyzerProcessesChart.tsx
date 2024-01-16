import React, {createRef, JSX, useEffect, useMemo, useState} from "react";
import {Box, FormControl, InputLabel, MenuItem, Select, styled} from "@mui/material";
import Draggable from "./Draggable";
import "../../../css/PriceIndicator.css";
import {ActiveAnalyzerInfo} from "../AnalyzerInfoPage";
import {ArrowBack} from "@mui/icons-material";
import {toast} from "react-toastify";
import {roundToNearest} from "../../../utils/number-utils";

interface AnalyzerBarData {
    info: ActiveAnalyzerInfo | undefined;
}

const stepsValues = [0.01, 0.1, 1, 10, 100, 500];

const AnalyzerProcessesChart: React.FC<AnalyzerBarData> = ({info}) => {
    const [delimiters, setDelimiters] = useState<number[]>([]);
    const [stepSize, setStepSize] = useState<number>(100);
    const [stepHeight, setStepHeight] = useState<number>(30);
    const [pointers, setPointers] = useState<JSX.Element[]>([]);
    const [upperLabels, setUpperLabels] = useState<JSX.Element[]>([]);
    const [lowerLabels, setLowerLabels] = useState<JSX.Element[]>([]);
    const [centerPositionValue, setCenterPositionValue] = useState<number>(0);
    const scrollRef = createRef<HTMLDivElement>();
    const delimitersView = useMemo(() => delimiters.map((value) => (
        <Delimiter key={value} style={{height: `${stepHeight}px`}}>{roundToNearest(value, stepSize)}</Delimiter>
    )), [delimiters, stepHeight, stepSize]);

    useEffect(() => {
            if (!!info && (delimiters.length === 0 || (delimiters[0] - delimiters[1]) !== stepSize)) { // check that delimiters should be reinitialized
                let middlePrice = roundToNearest(info!.currentPrice, stepSize);
                let vDelimiters = Array.from({length: 50}, (_, index) => middlePrice - (index * stepSize));
                middlePrice += stepSize;
                vDelimiters = vDelimiters.concat(Array.from({length: 50}, (_, index) => middlePrice + (index * stepSize))).sort().reverse();
                setDelimiters(vDelimiters);
            }

            if (!!info && delimiters.length > 0 && scrollRef.current !== null) {
                const containerHeight = scrollRef.current!.scrollHeight;
                const minPrice = Math.min(...delimiters);
                const maxPrice = Math.max(...delimiters);

                if (scrollRef.current!.scrollTop === 0) { // setup scroll to the middle price position
                    scrollRef.current!.scrollTop = (info.middlePrice - minPrice) / (maxPrice - minPrice) * containerHeight - 200;
                }

                const maxPricePosition = scrollRef.current!.scrollTop;
                const topPriceValue = minPrice + ((maxPricePosition - containerHeight) / -containerHeight) * (maxPrice - minPrice);
                const minPricePosition = maxPricePosition + 400;
                const bottomPriceValue = minPrice + ((minPricePosition - containerHeight) / -containerHeight) * (maxPrice - minPrice);

                let counter = 0;
                const getPointerFunction = (key: string, price: number, color: string) => <div
                    key={key}
                    style={{
                        top: `${100 - ((price - bottomPriceValue) / (topPriceValue - bottomPriceValue)) * 100}%`,
                        position: 'absolute',
                        borderBottom: '1px solid ' + color,
                        width: '100%',
                        zIndex: 10 + counter++
                    }}
                ></div>;

                const getLabelFunction = (key: string, count?: number, color?: string) => <div
                    key={key}
                    style={{
                        margin: '5px',
                        width: '15px',
                        height: '15px',
                        backgroundColor: color
                    }}
                >{count}</div>

                const setCenterPriceFunction = () => {
                    const centerPricePosition = scrollRef.current!.scrollTop + 200;
                    const centerPricePositionValue = minPrice + ((centerPricePosition - containerHeight) / -containerHeight) * (maxPrice - minPrice);
                    setCenterPositionValue(roundToNearest(centerPricePositionValue, stepSize));
                };

                setCenterPriceFunction();

                scrollRef.current!.onscroll = () => {
                    setCenterPriceFunction();
                }; // update text for center price pointer

                scrollRef.current!.onwheel = (event: WheelEvent) => {
                    event.preventDefault();
                    let resultHeight = stepHeight + event.deltaY / 10;
                    if (resultHeight > 60) {
                        setStepHeight(60);
                        return;
                    } else if (resultHeight < 20) {
                        setStepHeight(20);
                        return;
                    }

                    setStepHeight(resultHeight);
                };

                let activeOrders = info.orders
                    .filter((pair) => {
                        const isActive = pair.split('=')[1];
                        return isActive.toLowerCase() === 'true';
                    })
                    .map((pair) => parseFloat(pair.split("=")[0]));

                // Setup pointers

                const orderPointers = activeOrders
                    .filter((price) => {
                        return price > bottomPriceValue && price < topPriceValue;
                    })
                    .map((price) => {
                        const color = price > info.middlePrice ? 'red' : 'green';
                        return getPointerFunction(`pointer-price-${price}`, price, color);
                    });


                if (info.currentPrice < topPriceValue && info.currentPrice > bottomPriceValue) {
                    orderPointers.push(getPointerFunction('current-price-pointer', info.currentPrice, 'blue'));
                }

                if (info.middlePrice < topPriceValue && info.middlePrice > bottomPriceValue) {
                    orderPointers.push(getPointerFunction('middle-price-pointer', info.middlePrice, 'yellow'));
                }

                // Setup Labels

                const upperOrderLabels = activeOrders.filter((price) => price > topPriceValue)
                    .map((price) => {
                        const color = price > info.middlePrice ? 'green' : 'red';
                        return {price: price, color: color};
                    });
                const lowerOrderLabels = activeOrders.filter((price) => price < bottomPriceValue)
                    .map((price) => {
                        const color = price > info.middlePrice ? 'green' : 'red';
                        return {price: price, color: color};
                    });

                const upperSellOrderLabelLength = upperOrderLabels.filter((pair) => pair.color === 'red').length;
                const upperBuyOrderLabelLength = upperOrderLabels.filter((pair) => pair.color === 'green').length;
                const lowerSellOrderLabelLength = lowerOrderLabels.filter((pair) => pair.color === 'red').length;
                const lowerBuyOrderLabelLength = lowerOrderLabels.filter((pair) => pair.color === 'green').length;


                const upperLabels: JSX.Element[] = [];
                const lowerLabels: JSX.Element[] = [];

                if (upperSellOrderLabelLength > 0) {
                    upperLabels.push(getLabelFunction("upperSellOrderLabelLength", upperSellOrderLabelLength, 'green'));
                }
                if (upperBuyOrderLabelLength > 0) {
                    upperLabels.push(getLabelFunction("upperBuyOrderLabelLength", upperBuyOrderLabelLength, 'red'));
                }
                if (lowerSellOrderLabelLength > 0) {
                    lowerLabels.push(getLabelFunction("lowerSellOrderLabelLength", lowerSellOrderLabelLength, 'green'));
                }
                if (lowerBuyOrderLabelLength > 0) {
                    lowerLabels.push(getLabelFunction("lowerBuyOrderLabelLength", lowerBuyOrderLabelLength, 'red'));
                }

                if (info.currentPrice > topPriceValue) {
                    upperLabels.push(getLabelFunction("upper-current-price", undefined, 'blue'));
                } else if (info.currentPrice < bottomPriceValue) {
                    lowerLabels.push(getLabelFunction("lower-current-price", undefined, 'blue'));
                }

                if (info.middlePrice > topPriceValue) {
                    upperLabels.push(getLabelFunction("upper-middle-price", undefined, 'yellow'));
                } else if (info.middlePrice < bottomPriceValue) {
                    lowerLabels.push(getLabelFunction("lower-middle-price", undefined, 'yellow'));
                }

                setPointers(orderPointers);
                setUpperLabels(upperLabels);
                setLowerLabels(lowerLabels);
            }
        }, [info, stepSize, delimiters, scrollRef, stepHeight]
    );


    return !!info ?
        <div>
            <LabelContainer>{upperLabels}</LabelContainer>
            <ScrollableBarContainer>
                <IndicatorContainer>
                    <Draggable reference={scrollRef}>
                        <div>
                            {delimitersView}
                        </div>
                    </Draggable>
                </IndicatorContainer>
                <PointersContainer>
                    {pointers}
                </PointersContainer>
                <div style={{width: '100px', height: '400px', position: 'relative', backgroundColor: 'transparent'}}>
                    <div style={{top: '50%', position: 'absolute', display: 'flex', alignItems: 'center'}}>
                        <ArrowBack/>
                        <div>{centerPositionValue}</div>
                    </div>
                </div>
            </ScrollableBarContainer>
            <LabelContainer>{lowerLabels}</LabelContainer>
            <RoundSelectArea>
                <FormControl>
                    <InputLabel id="step-label">Step</InputLabel>
                    <Select
                        labelId="step-select-label"
                        id="step-select"
                        value={stepSize}
                        onChange={(event) => setStepSize(event.target.value as number)}
                        name="step"
                    >
                        {stepsValues.map((option) => (
                            <MenuItem key={option} value={option}>{option}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </RoundSelectArea>
        </div> :
        <div></div>;
}

const ScrollableBarContainer = styled('div')({
    display: 'flex', width: '200px', height: '400px'
});

const IndicatorContainer = styled(Box)({
    height: '400px', // Set the height you want for the bar
    width: '100px',
    border: '0px',
    position: 'absolute', // Position relative to place the delimiters absolutely
    backgroundColor: '#494949',
    zIndex: 1
});

const PointersContainer = styled(Box)({
    height: '400px', // Set the height you want for the bar
    width: '100px',
    backgroundColor: 'transparent',
    position: 'relative',
    pointerEvents: 'none'
});

const LabelContainer = styled(Box)({
    backgroundColor: 'darkgray',
    width: '100px',
    height: '30px',
    border: '1px solid',
    display: 'flex'
});

const RoundSelectArea = styled(Box)({
    backgroundColor: 'transparent',
    position: 'relative',
    zIndex: 3,
    marginTop: '10px'
});

const Delimiter = styled('div')(({theme}) => ({
    position: 'static',
    display: 'grid',
    alignContent: 'end',
    width: '30%',
    borderBottom: '2px solid white',
    color: 'white',
    textAlign: 'left',
    paddingRight: theme.spacing(2), // Add some spacing before the number
    userSelect: 'none'
}));

export default AnalyzerProcessesChart;
