import React from "react";
import {Route, Switch} from "wouter";
import BackTestPage from "./pages/backtest/BackTestPage";
import SymbolsPage from "./pages/symbol/SymbolsPage";
import AccountPage from "./pages/account/AccountPage";
import MonitoringPage from "./pages/monitor/MonitoringPage";
import {AuthProvider} from "./context/AuthContext";
import LeftNavPanel from "./pages/LeftNavPanel";
import ContactPage from "./pages/contact/ContactPage";
import {createTheme, Stack, ThemeProvider} from "@mui/material";
import {LoaderProvider} from "./context/LoaderContext";
import Loader from "./shared/Loader";
import BackTestDetailPage from "./pages/backtest/BackTestDetailPage";

const App: React.FC = () => {
    const theme = createTheme({
        typography: {
            fontFamily: 'PlexSans'
        },
        palette: {
            primary: {
                main: '#D0FF12',
                contrastText: '#121417'
            },
            secondary: {
                main: '#868F9C',
                contrastText: '#121417'
            },
            success: {
                main: '#16C079',
                contrastText: '#FFF'
            },
            error: {
                main: '#E7323B',
                contrastText: '#FFF'
            }
        }
    });

    return (
        <ThemeProvider theme={theme}>
            <AuthProvider>
                <LoaderProvider>
                    <Loader />
                    <Stack direction="row">
                        <LeftNavPanel/>
                        <div style={{flexGrow: '1', backgroundColor: '#1D2024'}}>
                            <Switch location="">
                                <Route path="/back-tests" component={BackTestPage}/>
                                <Route path="/back-tests/:requestId" component={BackTestDetailPage}/>
                                <Route path="/about-us" component={ContactPage}/>
                                <Route path="/account/:path*" component={AccountPage}/>
                                <Route path="/symbols" component={SymbolsPage}/>
                                <Route path="/monitoring" component={MonitoringPage}/>
                            </Switch>
                        </div>
                    </Stack>
                </LoaderProvider>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;
