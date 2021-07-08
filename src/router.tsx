import React from "react";
import { BrowserRouter, Route, Switch } from "react-router-dom";
import { WorkspaceScreen } from "./workspace/workspace-screen";

export const routes = {
    login: "/login",
    visualiser: "/visualiser",
}

export const StudioRouter: React.FC = () => {
    return (
        <BrowserRouter>
            <Switch>
                {/*<Route exact path={routes.login} component={LoginScreen}/>*/}
                {/*<Route exact path={routes.visualiser} component={VisualiserScreen}/>*/}
                <Route path="/" component={WorkspaceScreen}/>
                {/*<Route component={ErrorScreen}/>*/}
            </Switch>
        </BrowserRouter>
    );
};
