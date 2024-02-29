const monthNames = ["January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
];

export const formatDate = (seconds: number) => {
    const date = new Date(0);
    date.setUTCMilliseconds(seconds);

    const year = date.getFullYear();
    const month = String(monthNames[date.getMonth()]).substring(0, 3);
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${month} ${day}, ${year}. ${hours}:${minutes}`;
}

export const formatDateForSignature = (seconds: number) => {
    const date = new Date(0);
    date.setUTCSeconds(seconds);

    const year = date.getUTCFullYear();
    const month = String(date.getUTCMonth() + 1).padStart(2, '0');
    const day = String(date.getUTCDate()).padStart(2, '0');
    const hours = String(date.getUTCHours()).padStart(2, '0');
    const minutes = String(date.getUTCMinutes()).padStart(2, '0');
    const secondsFormatted = String(date.getUTCSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${secondsFormatted}`;
}
