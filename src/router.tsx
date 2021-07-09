import React from "react";
import { BrowserRouter, Redirect, Route, Switch } from "react-router-dom";
import { WorkspaceScreen } from "./workspace/workspace-screen";
import { LoginScreen } from "./login/login-screen";
import { typeDBClientState } from "./state/typedb-client";

export const routes = {
    workspace: "/workspace",
}

export const StudioRouter: React.FC = () => {
    const client = typeDBClientState.use()[0];
    console.log("In StudioRouter");
    return (
        <BrowserRouter>
            <Switch>
                {/*{<Route path="/" component={() => <h1>Hello World</h1>}/>}*/}
                <Route exact path={routes.workspace} render={() => client ? <WorkspaceScreen/> : <Redirect to="/"/>}/>
                <Route path="/" component={LoginScreen}/>
                {/*<Route component={ErrorScreen}/>*/}
            </Switch>
        </BrowserRouter>
    );
};
