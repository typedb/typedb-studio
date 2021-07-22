import React from "react";
import { HashRouter, Redirect, Route, Switch } from "react-router-dom";
import { WorkspaceScreen } from "./workspace/workspace-screen";
import { LoginScreen } from "./login/login-screen";
import { databaseState } from "./state/state";

export const routes = {
    workspace: "/workspace",
}

export const StudioRouter: React.FC = () => {
    const db = databaseState.use()[0];
    return (
        <HashRouter>
            <Switch>
                {/*{<Route path="/" component={() => <h1>Hello World</h1>}/>}*/}
                <Route exact path={routes.workspace} render={() => db ? <WorkspaceScreen/> : <Redirect to="/"/>}/>
                <Route path="/" component={LoginScreen}/>
                {/*<Route component={ErrorScreen}/>*/}
            </Switch>
        </HashRouter>
    );
};
