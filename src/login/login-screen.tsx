import clsx from "clsx";
import { ipcRenderer } from "electron";
import React, { useContext, useState } from "react";
import { SnackbarContext } from "../app";
import { StudioIconButton } from "../common/button/icon-button";
import { ConnectRequest, LoadDatabasesResponse } from "../ipc/event-args";
import { loginStyles } from "./login-styles";
import { StudioButton } from "../common/button/button";
import { StudioTabItem, StudioTabs } from "../common/tabs/tabs";
import { StudioAutocomplete } from "../common/autocomplete/autocomplete";
import { StudioSelect } from "../common/select/select";
import { databaseState, themeState } from "../state/typedb-client";
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

type AddressValidity = "valid" | "invalid" | "unknown";

export const TypeDBLoginTab: React.FC = () => {
    const classes = loginStyles({ theme: themeState.use()[0] });

    const [db, setDB] = useState(DATABASE);
    const [dbSelected, setDBSelected] = useState(false);
    const [dbList, setDBList] = React.useState<string[]>([]);

    const [currentAddress, setCurrentAddress] = useState("");
    const [addressValidity, setAddressValidity] = useState<AddressValidity>("unknown");
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

    const onAddressInputChange = (_e: object, value: string) => {
        loadDatabases(value);
    };

    const onAddressInputBlur = (e: React.FocusEvent<any>) => {
        loadDatabases(e.target.value);
    };

    const loadDatabases = (address: string) => {
        if (!address || address === currentAddress) return;
        setCurrentAddress(address);
        setDBSelected(false);
        setDB(LOADING_DATABASES);
        setDBList([]);
        setAddressValidity("unknown");
        const req: ConnectRequest = { address };
        ipcRenderer.send("connect-request", req);
    }

    React.useEffect(() => {
        ipcRenderer.on("connect-response", () => {
            ipcRenderer.send("load-databases-request", {});
        });

        ipcRenderer.on("load-databases-response", ((_event, res: LoadDatabasesResponse) => {
            if (res.success) {
                setDBList(res.databases);
                if (res.databases) {
                    setDB(res.databases[0]);
                    setDBSelected(true);
                } else setDB(NO_DATABASES);
                setAddressValidity("valid");
            } else {
                setDB(FAILED_TO_LOAD_DATABASES);
                setAddressValidity("invalid");
                setSnackbar({ open: true, variant: "error", message: res.error });
            }
        }));
    }, []);

    return (
        <form className={classes.form}>
            <div className={classes.formRow}>
                <StudioAutocomplete label="Server address" value="" onChange={onAddressInputChange}
                                    onBlur={onAddressInputBlur} invalid={addressValidity === "invalid"} options={["127.0.0.1:1729"]}/>
            </div>

            <div className={classes.formRow}>
                <StudioSelect label={DATABASE} value={db} setValue={selectDB} variant="filled">
                    <option disabled value={DATABASE}>{DATABASE}</option>
                    <option disabled hidden value={LOADING_DATABASES}>{LOADING_DATABASES}</option>
                    <option disabled hidden value={NO_DATABASES}>{NO_DATABASES}</option>
                    <option disabled hidden value={FAILED_TO_LOAD_DATABASES}>{FAILED_TO_LOAD_DATABASES}</option>
                    {dbList.map(db => <option value={db}>{db}</option>)}
                </StudioSelect>

                <StudioIconButton size="small" className={classes.buttonBesideTextField}
                                  disabled={addressValidity !== "valid"} onClick={(e) => e.preventDefault()}>+</StudioIconButton>
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
