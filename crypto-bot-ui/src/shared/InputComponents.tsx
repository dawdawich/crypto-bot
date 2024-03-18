import {styled, TextField} from "@mui/material";

export const InputField = styled(TextField)(({theme}) => ({
        'label + &': {
            marginTop: theme.spacing(2),
        },
        '& .MuiInputBase-input': {
            height: '18px',
            padding: '10px 12px',
            backgroundColor: '#262B31',
            color: 'white',
            borderRadius: '4px',
            border: '1px solid #00000000',
            transition: '0.3s',
            '&:focus': {
                border: '1px solid ' + theme.palette.primary.main,
            },
        },
    })
)
