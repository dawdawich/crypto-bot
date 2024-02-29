export const trimDecimalNumbers = (num: number, maxSize: number = 3): number => {
    // Convert number to a string
    const numStr = num.toString();
    // Find the position of the decimal point
    const decimalIndex = numStr.indexOf('.');

    // If there is no decimal point or the number of digits after it is within the limit, return the original number
    if (decimalIndex === -1 || numStr.length - decimalIndex - 1 <= maxSize) {
        return num;
    }

    // Otherwise, trim the number to the specified number of decimal places
    const fixedStr = numStr.substring(0, decimalIndex + maxSize + 1);
    return parseFloat(fixedStr);
}
