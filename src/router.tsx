import React from "react";
import {BrowserRouter, Route, Switch} from "react-router-dom";

export const routes = {
    login: "/login",
    visualiser: "/visualiser",
}

export const StudioRouter: React.FC = () => {
    return (
        <BrowserRouter>
            <Switch>
                {/*<Route exact path={routes.login} component={LoginScreen}/>*/}
                <Route exact path={routes.visualiser} component={VisualiserScreen}/>
                {/*<Route component={ErrorScreen}/>*/}
            </Switch>
        </BrowserRouter>
    );
};
