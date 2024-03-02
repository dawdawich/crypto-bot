import React, {useEffect, useRef, useState} from 'react';
import {useLoader} from '../context/LoaderContext';
import loadingSpinner from "../assets/images/loading-spinner.svga";
import {styled} from "@mui/material";

const LoadingScreen = styled('div')({
    position: 'fixed',
    width: '100vw',
    height: '100vh',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: '10'
});

const LoadingContainer = styled('div')({
    position: 'fixed',
    left: '50%',
    top: '50%',
    transform: 'translateX(-50%) translateY(-50%)',
    width: '380px',
    height: '316px',
    backgroundColor: '#121417'
});

const LoadingSpan = styled('span')({
    position: 'fixed',
    left: '50%',
    top: '50%',
    transform: 'translateX(-50%) translateY(-50%)',
    width: '60px'
});

const Loader: React.FC = () => {
    const spanRef = useRef<HTMLSpanElement>(null);
    const [animation, setAnimation] = useState('');
    const {isLoading} = useLoader();

    useEffect(() => {
        fetch(loadingSpinner)
            .then(response => response.text())
            .then(text => {
                setAnimation(text)
                if (spanRef.current) {
                    spanRef.current.innerHTML = animation
                }
            });
    }, [animation, isLoading]);

    if (!isLoading) return null;
    return (
        <LoadingScreen>
            {/*<LoadingSpan ref={spanRef}/>*/}
            <LoadingContainer>
                <h3 style={{color: 'white', top: '20%', left: '50%', position: 'fixed', transform: 'translateX(-50%) translateY(-20%)'}}>Waiting...</h3>
                <LoadingSpan ref={spanRef} />
            </LoadingContainer>
        </LoadingScreen>
    );
};

export default Loader;
