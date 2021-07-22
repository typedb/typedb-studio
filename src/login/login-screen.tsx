import { ipcRenderer } from "electron";
import React from "react";
import { useHistory } from "react-router-dom";
import { SnackbarContext } from "../app";
import { StudioAutocomplete } from "../common/autocomplete/autocomplete";
import { StudioButton } from "../common/button/button";
import { StudioIconButton } from "../common/button/icon-button";
import { StudioSelect } from "../common/select/select";
import { StudioTabItem, StudioTabPanel, StudioTabs } from "../common/tabs/tabs";
import { ConnectRequest, LoadDatabasesResponse } from "../ipc/event-args";
import { databaseState, dbServerState, themeState } from "../state/state";
import { studioStyles } from "../styles/studio-styles";
import { loginStyles } from "./login-styles";

export const LoginScreen: React.FC = () => {
    const theme = themeState.use()[0];
    const classes = Object.assign({}, studioStyles({ theme }), loginStyles({ theme }));

    const tabs: StudioTabItem[] = [
        { name: "TypeDB", key: "TypeDB" },
        { name: "TypeDB Cluster", key: "TypeDB Cluster" },
    ];

    const [selectedIndex, setSelectedIndex] = React.useState(0);

    return (
        <div className={classes.backdrop}>
            <StudioTabs selectedIndex={selectedIndex} setSelectedIndex={setSelectedIndex} items={tabs} classes={{ root: classes.tabs, tabGroup: classes.tabGroup, tab: classes.tab }}>
                <StudioTabPanel index={0} selectedIndex={selectedIndex}><TypeDBLoginTab/></StudioTabPanel>
                <StudioTabPanel index={1} selectedIndex={selectedIndex}><TypeDBClusterLoginTab/></StudioTabPanel>
            </StudioTabs>
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

    const [db, setDB] = React.useState(DATABASE);
    const [dbServer, setDBServer] = dbServerState.use();
    const [dbSelected, setDBSelected] = React.useState(false);
    const [addressValidity, setAddressValidity] = React.useState<AddressValidity>("unknown");
    const { setSnackbar } = React.useContext(SnackbarContext);
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
        if (!address || address === dbServer.address) return;
        setDBServer({ address, dbs: [] });
        setDBSelected(false);
        setDB(LOADING_DATABASES);
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
                setDBServer({ ...dbServer, dbs: res.databases });
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
                                    onBlur={onAddressInputBlur} invalid={addressValidity === "invalid"}
                                    options={["127.0.0.1:1729"]}/>
            </div>

            <div className={classes.formRow}>
                <StudioSelect label={DATABASE} value={db} setValue={selectDB} variant="filled">
                    <option disabled value={DATABASE}>{DATABASE}</option>
                    <option disabled hidden value={LOADING_DATABASES}>{LOADING_DATABASES}</option>
                    <option disabled hidden value={NO_DATABASES}>{NO_DATABASES}</option>
                    <option disabled hidden value={FAILED_TO_LOAD_DATABASES}>{FAILED_TO_LOAD_DATABASES}</option>
                    {dbServer.dbs.map(db => <option value={db}>{db}</option>)}
                </StudioSelect>

                <StudioIconButton size="medium" classes={{root: classes.buttonBesideTextField}}
                                  disabled={addressValidity !== "valid"}
                                  onClick={(e) => e.preventDefault()}>+</StudioIconButton>
            </div>

            <div className={classes.actionList}>
                <StudioButton size="small" type="primary" onClick={submit} disabled={!dbSelected}>Connect to
                    TypeDB</StudioButton>
            </div>
        </form>
    );
}

export const TypeDBClusterLoginTab: React.FC = () => {
    return <h4>Coming soon!</h4>;
}
