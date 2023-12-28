import React from "react";
import AnalyzersPage from "./pages/analyzer/AnalyzersPage";
import {Route, Switch} from "wouter";
import AnalyzerInfoPage from "./pages/analyzer/AnalyzerInfoPage";
import MainHeader from "./pages/MainHeader";
import ManagersPage from "./pages/manager/ManagersPage";
import ManagerPageEditor from "./pages/manager/ManagerPageEditor";
import LoginPage from "./pages/account/LoginPage";
import RegistrationPage from "./pages/account/RegistrationPage";
import SymbolsPage from "./pages/symbol/SymbolsPage";
import TopAnalyzersPage from "./pages/analyzer/TopAnalyzersPage";
import AccountPage from "./pages/account/AccountPage";
import {createTheme, ThemeProvider} from "@mui/material";
import {lightGreen, lime, orange, purple, red} from "@mui/material/colors";

const App: React.FC = () => {
    const theme = createTheme({
        palette: {
            primary: lime,
            secondary: purple,
            error: red,
            success: lightGreen,
            warning: orange
        },
    });
    return (
        <div className="App">
            <ThemeProvider theme={theme}>
                <MainHeader/>
                <Switch>
                    <Route path="/analyzer" component={AnalyzersPage}/>
                    <Route path="/account" component={AccountPage}/>
                    <Route path="/top-analyzers" component={TopAnalyzersPage}/>
                    <Route path="/manager" component={ManagersPage}/>
                    <Route path="/analyzer/:analyzerId" component={AnalyzerInfoPage}/>
                    <Route path="/manager/:managerId" component={ManagerPageEditor}/>
                    <Route path="/login" component={LoginPage}/>
                    <Route path="/signup" component={RegistrationPage}/>
                    <Route path="/symbols" component={SymbolsPage}/>
                </Switch>
            </ThemeProvider>
        </div>
    );
}

export default App;
