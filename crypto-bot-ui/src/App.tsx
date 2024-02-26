import React from "react";
import AnalyzersPage from "./pages/analyzer/AnalyzersPage";
import {Route, Switch} from "wouter";
import SymbolsPage from "./pages/symbol/SymbolsPage";
import AccountPage from "./pages/account/AccountPage";
import MonitoringPage from "./pages/monitor/MonitoringPage";
import {AuthProvider} from "./context/AuthContext";
import LeftNavPanel from "./pages/LeftNavPanel";
import ContactPage from "./pages/contact/ContactPage";
import ManagersPage from "./pages/manager/ManagersPage";

const App: React.FC = () => {
    return (
        <AuthProvider>
            <div style={{
                display: 'flex',
                flexDirection: 'row'
            }}>
                <LeftNavPanel/>
                <div style={{flexGrow: '1', backgroundColor: '#1D2024'}}>
                    <Switch>
                        <Route path="/about-us" component={ContactPage}/>
                        <Route path="/analyzer/:path*" component={AnalyzersPage} />
                        <Route path="/account/:path*" component={AccountPage}/>
                        <Route path="/manager" component={ManagersPage}/>
                        <Route path="/symbols" component={SymbolsPage}/>
                        <Route path="/monitoring" component={MonitoringPage}/>
                    </Switch>
                </div>
            </div>
        </AuthProvider>
    );
}

export default App;
