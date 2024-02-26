import {CSSObjectWithLabel} from "react-select";
import {StylesConfig} from "react-select/dist/declarations/src/styles";
import {styled, Switch} from "@mui/material";

export const SelectStyle: StylesConfig = {
    control: (baseStyles, state) => ({
        ...baseStyles,
        backgroundColor: '#262B31',
        boxShadow: "none",
        color: 'white',
        border: state.isFocused || state.menuIsOpen ? '1px solid #D0FF12' : '1px solid #262B31',
        "&:hover": {
            border: '1px solid #D0FF12',
            boxShadow: "none"
        }
    }),
    option: (provided: CSSObjectWithLabel, state) => ({
        ...provided,
        height: '34px',
        display: 'flex',
        alignItems: 'center',
        backgroundColor: '#262B31',
        color: 'white',
        borderRadius: '4px',
        "&:hover": {
            border: '1px solid #D0FF12',
            boxShadow: "none",
            backgroundColor: '#262B31',
        },
    }),
    menu: (provided) => ({
        ...provided,
        backgroundColor: 'transparent',
        paddingTop: '0',
        marginTop: '0'
    }),
    valueContainer: (provided, state) => ({
        ...provided,
        padding: '0 6px',
        display: "flex",
        overflowX: "hidden"
    }),
    input: (provided, state) => ({
        ...provided,
        margin: '0px',
        color: 'white'
    }),
    indicatorSeparator: state => ({
        display: 'none',
    }),
    indicatorsContainer: (provided, state) => ({
        ...provided,
        height: '34px',
    }),
    singleValue: (provided, state) => ({
        ...provided,
        color: 'white'
    }),
};

export const MultiSelectStyle: StylesConfig = {
    ...SelectStyle,
    multiValue: (styles, {data}) => {
        return {
            ...styles,
            backgroundColor: '#121417',
        };
    },
    multiValueLabel: (styles, {data}) => ({
        ...styles,
        color: 'white',
    }),
}

export const AntSwitch = styled(Switch)({
    width: 28,
    height: 16,
    padding: 0,
    display: 'flex',
    '&:active': {
        '& .MuiSwitch-thumb': {
            width: 15,
        },
        '& .MuiSwitch-switchBase.Mui-checked': {
            transform: 'translateX(9px)',
        },
    },
    '& .MuiSwitch-switchBase': {
        padding: 2,
        '&.Mui-checked': {
            transform: 'translateX(12px)',
            color: '#1D2024',
            '& + .MuiSwitch-track': {
                opacity: 1,
                backgroundColor: '#D0FF12',
            },
            '& .MuiSwitch-thumb': {
                boxShadow: '0 2px 4px 0 rgb(0 35 11 / 20%)',
                width: 12,
                height: 12,
                borderRadius: 6,
                transition: '0.3s',
                backgroundColor: '#1D2024'
            },
        },
    },
    '& .MuiSwitch-thumb': {
        boxShadow: '0 2px 4px 0 rgb(0 35 11 / 20%)',
        width: 12,
        height: 12,
        borderRadius: 6,
        transition: '0.3s',
        backgroundColor: '#D0FF12'
    },
    '&.Mui-unchecked': {
        transform: 'translateX(12px)',
        '& + .MuiSwitch-track': {
            borderRadius: 8,
            opacity: 1,
            backgroundColor: '#1D2024',
            boxSizing: 'border-box',
        },
    },
});

export const RowDiv = styled('div')({
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
});
