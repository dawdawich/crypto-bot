import React, { createContext, useContext, useState } from "react";

type LoaderType = 'SIMPLE' | 'BANNER';

type LoaderContextType = {
    isLoading: boolean;
    loaderType: LoaderType;
    showLoader: () => void;
    showBannerLoader: () => void;
    hideLoader: () => void;
};

// create context with default
const LoaderContext = createContext<LoaderContextType>({
    isLoading: false,
    loaderType: 'SIMPLE',
    showLoader: () => {},
    showBannerLoader: () => {},
    hideLoader: () => {},
});

export const LoaderProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isLoading, setIsLoading] = useState(false);
    const [loaderType, setLoaderType] = useState<LoaderType>('SIMPLE');

    return (
        <LoaderContext.Provider
            value={{
                isLoading,
                loaderType,
                showLoader: () => setIsLoading(true),
                showBannerLoader: () => {
                    setLoaderType('BANNER');
                    setIsLoading(true);
                },
                hideLoader: () => {
                    setLoaderType('SIMPLE');
                    setIsLoading(false);
                },
            }}
        >
            {children}
        </LoaderContext.Provider>
    );
};

// custom hook that shorthands the context!
export const useLoader = () => useContext(LoaderContext);
