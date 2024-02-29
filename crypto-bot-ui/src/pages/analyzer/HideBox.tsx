import React, {useEffect, useState} from "react";
import {ReactComponent as RocketIcon} from "../../assets/images/analyzer/rocket-icon.svg";
import {Box, styled} from "@mui/material";

interface InnerComponentProps {
    diapasonRef: React.RefObject<HTMLDivElement>;
    stabilityRef: React.RefObject<HTMLDivElement>;
}

const HideBoxElement = styled(Box)({
    position: 'absolute',
    marginTop: '3px',
    height: '50px', // Adjust as needed
    backgroundColor: 'rgba(29,32,36,0.8)',
    color: '#868F9C',
    fontSize: '14px',
    fontWeight: 400,
    backdropFilter: 'blur(3px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 5, // Ensure it's above table cells,
    overflowY: 'auto'
});

const HideBox = React.forwardRef((props: InnerComponentProps, _: React.ForwardedRef<any>) => {
    const [hiddenBoxWidth, setHiddenBoxWidth] = useState(500);
    const [hiddenBoxMargin, setHiddenBoxMargin] = useState(0);

    useEffect(() => {
        const calculateHiddenBoxRect = () => {
            if (props.diapasonRef.current !== null && props.stabilityRef.current !== null) {
                const diapasonRect = props.diapasonRef.current.getBoundingClientRect();
                const notComponentWidth = window.innerWidth - props.diapasonRef.current.parentElement!.getBoundingClientRect().width;
                setHiddenBoxMargin(diapasonRect.x - notComponentWidth);
                setHiddenBoxWidth(props.stabilityRef.current!.getBoundingClientRect().left - diapasonRect.left);

            }
        }
        calculateHiddenBoxRect();
        window.addEventListener('resize', () => {
            calculateHiddenBoxRect();
        })
    }, [props.diapasonRef, props.stabilityRef]);

    return <HideBoxElement left={hiddenBoxMargin} width={`${hiddenBoxWidth}px`}>
        <RocketIcon style={{marginRight: '8px', fill: '#868F9C'}}/>
        Connect your wallet to unlock full access!
    </HideBoxElement>;
});

export default HideBox;
