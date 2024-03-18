import React from "react";
import AnalyzersPage from "./pages/analyzer/AnalyzersPage";
import {Redirect, Route, Switch} from "wouter";
import SymbolsPage from "./pages/symbol/SymbolsPage";
import AccountPage from "./pages/account/AccountPage";
import MonitoringPage from "./pages/monitor/MonitoringPage";
import {AuthProvider} from "./context/AuthContext";
import LeftNavPanel from "./pages/LeftNavPanel";
import ContactPage from "./pages/contact/ContactPage";
import ManagersPage from "./pages/manager/ManagersPage";
import {createTheme, Stack, ThemeProvider} from "@mui/material";
import {LoaderProvider} from "./context/LoaderContext";
import Loader from "./shared/Loader";

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
                                <Route path="/about-us" component={ContactPage}/>
                                <Route path="/analyzer/:path*" component={AnalyzersPage}/>
                                <Route path="/account/:path*" component={AccountPage}/>
                                <Route path="/manager" component={ManagersPage}/>
                                <Route path="/symbols" component={SymbolsPage}/>
                                <Route path="/monitoring" component={MonitoringPage}/>
                                <Route>
                                    <Redirect to="/analyzer/folder/top" />
                                </Route>
                            </Switch>
                        </div>
                    </Stack>
                </LoaderProvider>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;
