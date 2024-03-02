import React from "react";
import {Button, Dialog, DialogContent, DialogTitle, Slider, Stack, styled} from "@mui/material";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import {ReactComponent as Logo} from '../../../assets/images/nav-panel/joatNavLogo.svg';
import {sendToken} from "../../../utils/token-data";
import {errorToast} from "../../../shared/toast/Toasts";
import {useLoader} from "../../../context/LoaderContext";

interface BuyTokensDialogProps {
    open: boolean;
    onClose: () => void;
}

const StyledSlider = styled(Slider)({
    color: '#D0FF12',
    '.MuiSlider-thumb': {
        border: '2px solid #121417'
    }
})

const BuyTokensDialog: React.FC<BuyTokensDialogProps> = ({open, onClose}) => {
    const {showBannerLoader, hideLoader} = useLoader();
    const [value, setValue] = React.useState<number>(0);

    const handleChange = (_: Event, newValue: number | number[]) => {
        setValue(newValue as number);
    };

    const makePurchase = () => {
        onClose();
        showBannerLoader();
        sendToken(value / 100)
            .then(() => {
                setTimeout(() => {
                    hideLoader();
                    window.location.reload();
                }, 6000);
            })
            .catch(() => {
                hideLoader();
                errorToast('Failed to make a transaction');
            });
    };

    return (
        <Dialog open={open} onClose={onClose}
                PaperProps={{
                    style: {
                        backgroundColor: '#121417',
                        borderRadius: '4px',
                        boxShadow: 'none',
                        color: 'white',
                        fontWeight: '400',
                        width: '380px',
                        height: '312px',
                        position: 'relative',
                        padding: '16px'
                    }
                }}>
            <CrossIcon style={{
                display: 'flex',
                alignSelf: 'flex-end',
                fill: 'white',
                width: '24px',
                height: '24px',
                cursor: 'pointer'
            }} onClick={onClose}/>
            <DialogTitle>
                Add Api Token
            </DialogTitle>
            <DialogContent>
                Amount (Analyzers)
                <StyledSlider min={0} max={100000} step={100} onChange={handleChange}/>
                <div>
                    <Stack direction="row" sx={{alignItems: 'center'}} spacing={2}>
                        <h1>{value}</h1>
                        <div style={{color: '#868F9C'}}>Analyzers for one month</div>
                    </Stack>
                </div>
            </DialogContent>
            <DialogContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" width="100%">
                    <Stack direction="row" spacing={2} alignItems="center">
                        <h2>{(value / 100)}</h2>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Logo/>
                            <div style={{color: '#868F9C'}}>JOAT</div>
                        </Stack>
                    </Stack>
                    <Button
                        onClick={() => makePurchase()}
                        variant='contained'
                        style={{
                            textTransform: 'none',
                            backgroundColor: '#D0FF12',
                            color: '#121417',
                            fontWeight: 700
                        }}>
                        Buy
                    </Button>
                </Stack>
            </DialogContent>
        </Dialog>
    );
}

export default BuyTokensDialog;
