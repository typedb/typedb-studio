/**
 * This module executes inside of electron's main process. You can start
 * electron renderer process from here and communicate with the other processes
 * through IPC.
 *
 * When running `npm build` or `npm build:main`, this file is compiled to
 * `./src/main.prod.js` using webpack. This gives us some performance wins.
 */
import 'core-js/stable';
import 'regenerator-runtime/runtime';
import path from 'path';
import { app, BrowserWindow, protocol, screen, shell, ipcMain } from 'electron';
import { SessionType, TransactionType, TypeDB, TypeDBClient, TypeDBSession } from "typedb-client";
import { ConceptData, ConceptMapData, ConnectRequest, ConnectResponse, LoadDatabasesResponse, MatchQueryRequest, MatchQueryResponse } from "./ipc/event-args";
// import { autoUpdater } from 'electron-updater';
// import log from 'electron-log';
import MenuBuilder from './menu';
import { TypeDBVisualiserData } from "./typedb-visualiser";

// export default class AppUpdater {
//     constructor() {
//         log.transports.file.level = 'info';
//         autoUpdater.logger = log;
//         autoUpdater.checkForUpdatesAndNotify();
//     }
// }

// TODO: do we need this? does it do anything?
// Scheme must be registered before the app is ready
protocol.registerSchemesAsPrivileged([
    { scheme: 'app', privileges: { secure: true, standard: true } }
])

let mainWindow: BrowserWindow = null;

if (process.env.NODE_ENV === 'production') {
    const sourceMapSupport = require('source-map-support');
    sourceMapSupport.install();
}

const isDevelopment = process.env.NODE_ENV === 'development' || process.env.DEBUG_PROD === 'true';

if (isDevelopment) {
    require('electron-debug')();
}

const installExtensions = async () => {
    const installer = require('electron-devtools-installer');
    const forceDownload = !!process.env.UPGRADE_EXTENSIONS;
    const extensions = ['REACT_DEVELOPER_TOOLS'];

    return installer.default(extensions.map((name) => installer[name]), forceDownload)
        .catch(console.log);
};

const createWindow = async () => {
    if (isDevelopment) {
        await installExtensions();
    }

    const RESOURCES_PATH = app.isPackaged
        ? path.join(process.resourcesPath, 'assets')
        : path.join(__dirname, '../assets');

    const getAssetPath = (...paths: string[]): string => {
        return path.join(RESOURCES_PATH, ...paths);
    };

    const { width, height } = screen.getPrimaryDisplay().workAreaSize;

    mainWindow = new BrowserWindow({
        show: false,
        width,
        height,
        icon: getAssetPath('icon.png'),
        webPreferences: {
            nodeIntegration: true,
        },
    });

    mainWindow.loadURL(`file://${__dirname}/index.html`);

    // @TODO: Use 'ready-to-show' event
    //        https://github.com/electron/electron/blob/master/docs/api/browser-window.md#using-ready-to-show-event
    mainWindow.webContents.on('did-finish-load', () => {
        if (!mainWindow) {
            throw new Error('"mainWindow" is not defined');
        }
        if (process.env.START_MINIMIZED) {
            mainWindow.minimize();
        } else {
            mainWindow.show();
            mainWindow.focus();
        }
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    const menuBuilder = new MenuBuilder(mainWindow);
    menuBuilder.buildMenu();

    // Open urls in the user's browser
    mainWindow.webContents.on('new-window', (event, url) => {
        event.preventDefault();
        shell.openExternal(url);
    });

    // Initialise auto updates
    // new AppUpdater();
};

/**
 * Add event listeners...
 */

app.on('window-all-closed', () => {
    // Respect the OSX convention of having the application in memory even after all windows have been closed
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.whenReady().then(createWindow).catch(console.log);

app.on('activate', () => {
    // On macOS it's common to re-create a window in the app when the dock icon is clicked and there are no other windows open.
    if (mainWindow === null) createWindow();
});

// Exit cleanly on request from parent process in development mode.
if (isDevelopment) {
    if (process.platform === 'win32') {
        process.on('message', (data) => {
            if (data === 'graceful-exit') {
                app.quit()
            }
        });
    } else {
        process.on('SIGTERM', () => {
            app.quit()
        });
    }
}

let client: TypeDBClient;

function processError(e: any): string {
    let errorMessage: string = e.toString();
    if (e instanceof Error) errorMessage = e.message;
    if (errorMessage.startsWith("Error: ")) errorMessage = errorMessage.substring(7); // Most gRPC errors
    return errorMessage;
}

// TODO: Concurrent requests may cause issues
ipcMain.on("connect-request", ((event, req: ConnectRequest) => {
    let res: ConnectResponse;
    try {
        client = TypeDB.coreClient(req.address);
        res = { success: true };
    } catch (e: any) {
        const errorMessage = processError(e);
        res = { success: false, error: errorMessage };
    }
    event.sender.send("connect-response", res);
}));

ipcMain.on("load-databases-request", (async (event, _req: {}) => {
    let res: LoadDatabasesResponse;
    try {
        const dbs = await client.databases.all();
        res = { success: true, databases: dbs.map(db => db.name) };
    } catch (e: any) {
        const errorMessage = processError(e);
        res = { success: false, error: errorMessage };
    }
    event.sender.send("load-databases-response", res);
}));

ipcMain.on("match-query-request", (async (event, req: MatchQueryRequest) => {
    let res: MatchQueryResponse;
    let session: TypeDBSession;
    try {
        session = await client.session(req.db, SessionType.DATA);
        const tx = await session.transaction(TransactionType.READ);
        const answers = await tx.query.match(req.query).collect(); // TODO: Add support for streaming responses
        const answersData = answers.map(cm => {
            const answerData: Partial<ConceptMapData> = {};
            for (const [varName, concept] of cm.map.entries()) {
                let encoding: TypeDBVisualiserData.VertexEncoding;
                if (concept.isEntity()) encoding = "entity";
                else if (concept.isRelation()) encoding = "relation";
                else if (concept.isAttribute()) encoding = "attribute";
                else if (concept.isEntityType()) encoding = "entityType";
                else if (concept.isRelationType()) encoding = "relationType";
                else if (concept.isAttributeType()) encoding = "attributeType"; // TODO: RoleType is missing
                else encoding = "thingType";

                const conceptData: ConceptData = { encoding };
                if (concept.isThing()) {
                    const thing = concept.asThing();
                    conceptData.iid = thing.iid;
                    conceptData.type = thing.type.label.name;

                    if (thing.isAttribute()) conceptData.value = thing.asAttribute().value;
                } else {
                    const type = concept.asType();
                    conceptData.label = type.label.name;
                }

                answerData[varName] = conceptData;
            }
            return answerData;
        });
        res = { success: true, answers: answersData };
    } catch (e: any) {
        const errorMessage = processError(e);
        res = { success: false, error: errorMessage };
    } finally {
        session?.close();
    }
    event.sender.send("match-query-response", res);
}));
