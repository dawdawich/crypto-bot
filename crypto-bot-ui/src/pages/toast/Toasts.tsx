import {Bounce, toast} from "react-toastify";
import { ReactComponent as FailedIcon } from "../../assets/images/toast/failed-icon.svg"
import { ReactComponent as SuccessIcon } from "../../assets/images/toast/success-icon.svg"

export const errorToast = (errorDescription: string): void => {
    toast.error(errorDescription, {
        position: "bottom-center",
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
        progress: undefined,
        theme: "dark",
        transition: Bounce,
        icon: () => <FailedIcon style={{fill: 'white'}} />,
        style: {fontSize: '13px'}
    });
}

export const successToast = (text: string): void => {
    toast.info(text, {
        position: "bottom-center",
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
        progress: undefined,
        theme: "dark",
        transition: Bounce,
        icon: () => <SuccessIcon style={{fill: 'white'}} />,
        style: {fontSize: '13px'}
    });
}
