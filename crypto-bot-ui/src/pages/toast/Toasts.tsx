import React from "react";
import {toast} from "react-toastify";

export const errorToast = (errorDescription: String): void => {
    toast.error(errorDescription, {
        position: toast.POSITION.TOP_RIGHT,
        autoClose: 4000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
    });
}
