export const sanitizeMiddle = (str: string, visibleCount: number): string => {
    // Calculate the length of the visible part at the start and end
    const visibleLength = Math.floor(visibleCount / 2);

    // Check if the string needs sanitizing
    if (str.length <= visibleCount) {
        // If the string is short enough, return it as is
        return str;
    } else {
        // Extract the start and end parts of the string
        const start = str.substring(0, visibleLength);
        const end = str.substring(str.length - visibleLength);
        // Replace the middle part with "..."
        return start + '...' + end;
    }
}

