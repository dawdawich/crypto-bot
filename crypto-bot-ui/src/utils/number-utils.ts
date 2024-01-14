export const roundToNearest = (value: number, roundTo: number) => {
    return Math.round(value / roundTo) * roundTo;
};
