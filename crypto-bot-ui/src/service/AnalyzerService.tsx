const API_URL = 'http://localhost:8080/analyzer';

export const fetchAnalyzersData = async () => {
    try {
        const response = await fetch(`${API_URL}/top20`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzerData = async (analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${analyzerId}`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzerPosition = async (analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/positions/${analyzerId}`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}
