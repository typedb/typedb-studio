import React, { useState } from "react";
import { loginStyles } from "./login-styles";
import { StudioButton } from "../common/button/button";
import { StudioTabItem, StudioTabs } from "../common/tabs/tabs";
import { StudioAutocomplete } from "../common/autocomplete/autocomplete";
import { StudioSelect, StudioSelectOption } from "../common/select/select";
import { TypeDB } from "typedb-client/TypeDB";
import { databaseState, themeState, typeDBClientState } from "../state/typedb-client";
import { useHistory } from "react-router-dom";

export const LoginScreen: React.FC = () => {
    console.log("Rendering login screen")
    const classes = loginStyles({ theme: themeState.use()[0] });

    const tabs: StudioTabItem[] = [{
        name: "TypeDB",
        key: "TypeDB",
        content: TypeDBLoginTab,
    }, {
        name: "TypeDB Cluster",
        key: "TypeDB Cluster",
        content: TypeDBClusterLoginTab,
    }];
    
    return (
        <StudioTabs items={tabs} classes={{root: classes.tabs, tabGroup: classes.tabGroup,
            tabItem: classes.tab, selected: classes.tabSelected, tabContent: classes.tabContent}}/>
    );
}

export const TypeDBLoginTab: React.FC = () => {
    const classes = loginStyles({ theme: themeState.use()[0] });

    const [serverAddress, setServerAddress] = useState("");
    const [db, setDB] = useState("");
    const routerHistory = useHistory();

    const submit = () => {
        const client = TypeDB.coreClient();
        typeDBClientState.set(client);
        databaseState.set(db);
        routerHistory.push("/workspace");
    };

    return (
        <form className={classes.form}>
            <div className={classes.formRow}>
                <StudioAutocomplete label="Server address" value={serverAddress} setValue={setServerAddress} options={["127.0.0.1"]}/>
            </div>

            <div className={classes.formRow}>
                <StudioSelect label="Database" value={db} setValue={setDB} variant="filled">
                    <option disabled value="">Database</option>
                    <StudioSelectOption value="grabl"/>
                    <StudioSelectOption value="rowbot"/>
                    <StudioSelectOption value="typedb"/>
                </StudioSelect>
            </div>

            <div className={classes.actionList}>
                <StudioButton size="small" type="primary" onClick={submit}>Connect to TypeDB</StudioButton>
            </div>
        </form>
    );
}

export const TypeDBClusterLoginTab: React.FC = () => {
    return <h4>Coming soon!</h4>;
}
