import {useLocation} from "wouter";
import {AppBar, Box, Button, styled, Toolbar} from "@mui/material";

const StyledToolbar = styled(Toolbar)({
    justifyContent: 'space-between',
    gap: '10px',
});

const MainHeader = () => {
    let userData = localStorage.getItem('auth.token');
    const [, navigate] = useLocation();

    // if (!userData) {
    //     return (
    //         <div className='links-box'>
    //             <div className='links-box-item'><Link to={"/login"} className='cute-link'>Login</Link></div>
    //             <div className='links-box-item'><Link to={"/signup"} className='cute-link'>Sign Up</Link></div>
    //         </div>
    //     )
    // }

    const role = localStorage.getItem('auth.role')

    return (
        <AppBar position="static" color="default" elevation={1}>
            <StyledToolbar>
                <Box display="flex" alignItems="center">
                    <img src="" alt="Logo" style={{ height: 50 }} />
                </Box>
                <Box display="flex" gap={2}>
                    <Button color="inherit" onClick={() => navigate("/analyzer")}>Analyzers</Button>
                    <Button color="inherit" onClick={() => navigate("/top-analyzers")}>Public Top Analyzers</Button>
                    <Button color="inherit" onClick={() => navigate("/manager")}>Managers</Button>
                    <Button color="inherit" onClick={() => navigate("/account")}>Account</Button>
                    {
                        role === 'ADMIN' &&
                        <Button color="inherit" onClick={() => navigate("/symbols")}>Symbols</Button>
                    }
                </Box>
                <Button variant="outlined" color="primary">
                    Connect Wallet
                </Button>
            </StyledToolbar>
        </AppBar>
    );
}

export default MainHeader;
