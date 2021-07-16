import clsx from "clsx";
import React, { useContext, useState } from "react";
import { Database, TypeDBClientError } from "typedb-client";
import { SnackbarContext } from "../app";
import { loginStyles } from "./login-styles";
import { StudioButton } from "../common/button/button";
import { StudioTabItem, StudioTabs } from "../common/tabs/tabs";
import { StudioAutocomplete } from "../common/autocomplete/autocomplete";
import { StudioSelect } from "../common/select/select";
import { TypeDB } from "typedb-client/TypeDB";
import { databaseState, themeState, typeDBClientState } from "../state/typedb-client";
import { useHistory } from "react-router-dom";

export const LoginScreen: React.FC = () => {
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
        <div className={classes.backdrop}>
            <StudioTabs items={tabs} classes={{root: clsx(classes.tabs, classes.loginTabs), tabGroup: classes.tabGroup,
                tabItem: classes.tab, selected: classes.tabSelected, tabContent: classes.tabContent}}/>
        </div>
    );
}

const DATABASE = "Database";
const LOADING_DATABASES = "Loading databases...";
const NO_DATABASES = "This server has no databases";
const FAILED_TO_LOAD_DATABASES = "Failed to load databases";

export const TypeDBLoginTab: React.FC = () => {
    const classes = loginStyles({ theme: themeState.use()[0] });

    const [db, setDB] = useState(DATABASE);
    const [dbSelected, setDBSelected] = useState(false);
    const [dbList, setDBList] = useState<Database[]>([]);
    const [currentAddress, setCurrentAddress] = useState("");
    const [addressInvalid, setAddressInvalid] = useState(false);
    const { setSnackbar } = useContext(SnackbarContext);
    const routerHistory = useHistory();

    const submit = () => {
        databaseState.set(db);
        routerHistory.push("/workspace");
    };

    const selectDB = (value: string) => {
        setDB(value);
        setDBSelected(true);
    };

    const onAddressInputChange = async (_e: any, value: string) => {
        loadDatabases(value);
    };

    const onAddressInputBlur = (e: any) => {
        loadDatabases(e.target.value);
    };

    const loadDatabases = async (address: string) => {
        if (!address || address === currentAddress) return;
        setCurrentAddress(address);
        setDBSelected(false);
        setDB(LOADING_DATABASES);
        setDBList([]);
        setAddressInvalid(false);
        try {
            const client = TypeDB.coreClient(address);
            typeDBClientState.set(client);
            const dbs = await client.databases.all();
            setDBList(dbs);
            if (dbs) {
                setDB(dbs[0].name);
                setDBSelected(true);
            } else setDB(NO_DATABASES);
        } catch (e: any) {
            setDB(FAILED_TO_LOAD_DATABASES);
            console.log(e)
            let errorMessage: string = e.toString();
            if (e instanceof TypeDBClientError && e.errorMessage) errorMessage = e.errorMessage.toString();
            if (errorMessage.startsWith("Error: ")) errorMessage = errorMessage.substring(7);
            setAddressInvalid(true);
            setSnackbar({
                open: true,
                variant: "error",
                message: errorMessage,
            });
        }
    }

    return (
        <form className={classes.form}>
            <div className={classes.formRow}>
                <StudioAutocomplete label="Server address" value="" onChange={onAddressInputChange} onBlur={onAddressInputBlur} invalid={addressInvalid} options={["127.0.0.1:1729"]}/>
            </div>

            <div className={classes.formRow}>
                <StudioSelect label={DATABASE} value={db} setValue={selectDB} variant="filled">
                    <option disabled value={DATABASE}>{DATABASE}</option>
                    <option disabled hidden value={LOADING_DATABASES}>{LOADING_DATABASES}</option>
                    <option disabled hidden value={NO_DATABASES}>{NO_DATABASES}</option>
                    <option disabled hidden value={FAILED_TO_LOAD_DATABASES}>{FAILED_TO_LOAD_DATABASES}</option>
                    {dbList.map(db => <option value={db.name}>{db.name}</option>)}
                </StudioSelect>
            </div>

            <div className={classes.actionList}>
                <StudioButton size="small" type="primary" onClick={submit} disabled={!dbSelected}>Connect to TypeDB</StudioButton>
            </div>
        </form>
    );
}

export const TypeDBClusterLoginTab: React.FC = () => {
    return <h4>Coming soon!</h4>;
}
