import React from "react";
import AnalyzersPage from "./pages/analyzer/AnalyzersPage";
import {Route, Switch} from "wouter";
import AnalyzerInfoPage from "./pages/analyzer/AnalyzerInfoPage";
import MainHeader from "./pages/MainHeader";
import ManagersPage from "./pages/manager/ManagersPage";
import ManagerPageEditor from "./pages/manager/ManagerPageEditor";

const App: React.FC = () => {
    return (
        <div className="App">
            <MainHeader />
            <Switch>
                <Route path="/analyzer" component={AnalyzersPage} />
                <Route path="/manager" component={ManagersPage} />
                <Route path="/analyzer/:analyzerId" component={AnalyzerInfoPage} />
                <Route path="/manager/:managerId" component={ManagerPageEditor} />
            </Switch>
        </div>
    );
}

export default App;
