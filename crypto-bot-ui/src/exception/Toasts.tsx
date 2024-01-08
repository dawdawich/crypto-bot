import React from "react";
import {toast} from "react-toastify";
import {CommonErrorModel} from "./model/CommonErrorModel";

export const showToast = (errorResponse: CommonErrorModel): void => {
        toast.error(errorResponse.errorDescription, {
            position: toast.POSITION.TOP_RIGHT,
            autoClose: 4000,
            hideProgressBar: false,
            closeOnClick: true,
            pauseOnHover: true,
            draggable: true,
        });
};
