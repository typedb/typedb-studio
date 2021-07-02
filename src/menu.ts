import { app, Menu, shell, BrowserWindow, MenuItemConstructorOptions } from "electron";

interface DarwinMenuItemConstructorOptions extends MenuItemConstructorOptions {
    selector?: string;
    submenu?: DarwinMenuItemConstructorOptions[] | Menu;
}

export default class MenuBuilder {
    mainWindow: BrowserWindow;

    constructor(mainWindow: BrowserWindow) {
        this.mainWindow = mainWindow;
    }

    buildMenu(): Menu {
        if (process.env.NODE_ENV === 'development' || process.env.DEBUG_PROD === 'true') {
            this.setupDevelopmentEnvironment();
        }

        const template = process.platform === 'darwin' ? this.buildDarwinTemplate() : this.buildDefaultTemplate();
        const menu = Menu.buildFromTemplate(template as MenuItemConstructorOptions[]);
        Menu.setApplicationMenu(menu);

        return menu;
    }

    setupDevelopmentEnvironment(): void {
        this.mainWindow.webContents.on('context-menu', (_, props) => {
            const { x, y } = props;

            Menu.buildFromTemplate([
                {
                    label: 'Inspect element',
                    click: () => {
                        this.mainWindow.webContents.inspectElement(x, y);
                    },
                },
            ]).popup({ window: this.mainWindow });
        });
    }

    helpSubmenu(): MenuItemConstructorOptions[] {
        return [{
            label: "TypeDB Studio on GitHub",
            click() { shell.openExternal("https://github.com/vaticle/typedb-workbase"); },
        }, {
            label: "Documentation",
            click() { shell.openExternal('https://docs.vaticle.com/docs/workbase/overview'); },
        }, {
            label: "Submit an Issue",
            click() { shell.openExternal('https://github.com/vaticle/typedb-workbase/issues/new/choose'); },
        },];
    }

    buildDarwinTemplate(): DarwinMenuItemConstructorOptions[] {
        const subMenuViewDev: DarwinMenuItemConstructorOptions = {
            label: 'View',
            submenu: [
                {
                    label: 'Reload',
                    accelerator: 'Command+R',
                    click: () => {
                        this.mainWindow.webContents.reload();
                    },
                },
                {
                    label: 'Toggle Full Screen',
                    accelerator: 'Ctrl+Command+F',
                    click: () => {
                        this.mainWindow.setFullScreen(!this.mainWindow.isFullScreen());
                    },
                },
                {
                    label: 'Toggle Developer Tools',
                    accelerator: 'Alt+Command+I',
                    click: () => {
                        this.mainWindow.webContents.toggleDevTools();
                    },
                },
            ],
        };
        const subMenuViewProd: DarwinMenuItemConstructorOptions = {
            label: 'View',
            submenu: [
                {
                    label: 'Toggle Full Screen',
                    accelerator: 'Ctrl+Command+F',
                    click: () => {
                        this.mainWindow.setFullScreen(!this.mainWindow.isFullScreen());
                    },
                },
            ],
        };
        const subMenuView = process.env.NODE_ENV === 'development' || process.env.DEBUG_PROD === 'true' ? [subMenuViewDev] : [subMenuViewProd];

        return [{
            label: 'Application',
            submenu: [
                { label: 'About TypeDB Studio', selector: 'orderFrontStandardAboutPanel:' },
                { type: 'separator' },
                { label: 'Services', submenu: [] },
                { type: "separator" },
                { label: 'Hide TypeDB Studio', accelerator: 'Command+H', selector: 'hide:' },
                { label: 'Hide Others', accelerator: 'Command+Shift+H', selector: 'hideOtherApplications:' },
                { label: 'Show All', selector: 'unhideAllApplications:' },
                { type: 'separator' },
                { label: 'Quit TypeDB Studio', accelerator: 'Command+Q', click() { app.quit(); } }
            ],
        }, {
            label: 'Edit',
            submenu: [
                { label: 'Undo', accelerator: 'Command+Z', selector: 'undo:' },
                { label: 'Redo', accelerator: 'Shift+Command+Z', selector: 'redo:' },
                { type: 'separator' },
                { label: 'Cut', accelerator: 'Command+X', selector: 'cut:' },
                { label: 'Copy', accelerator: 'Command+C', selector: 'copy:' },
                { label: 'Paste', accelerator: 'Command+V', selector: 'paste:' },
                { label: 'Select All', accelerator: 'Command+A', selector: 'selectAll:' },
            ],
        }, {
            label: "View",
            submenu: subMenuView,
        }, {
            label: "Window",
            submenu: [
                { label: 'Minimize', accelerator: 'Command+M', selector: 'performMiniaturize:' },
                { label: 'Close', accelerator: 'Command+W', selector: 'performClose:' },
                { type: 'separator' },
                { label: 'Bring All to Front', selector: 'arrangeInFront:' },
            ],
        }, {
            label: "Help",
            submenu: this.helpSubmenu(),
        }];
    }

    buildDefaultTemplate() {
        return [{
            label: '&File',
            submenu: [
                { label: 'E&xit TypeDB Studio', accelerator: 'Ctrl+X', click() { app.quit(); } }
            ],
        }, {
            label: '&Edit',
            submenu: [
                { label: '&Undo', accelerator: 'Ctrl+Z', selector: 'undo:' },
                { label: '&Redo', accelerator: 'Ctrl+Y', selector: 'redo:' },
                { type: 'separator' },
                { label: '&Cut', accelerator: 'Ctrl+X', selector: 'cut:' },
                { label: 'C&opy', accelerator: 'Ctrl+C', selector: 'copy:' },
                { label: '&Paste', accelerator: 'Ctrl+V', selector: 'paste:' },
                { label: 'Select &All', accelerator: 'Ctrl+A', selector: 'selectAll:' },
            ],
        }, {
            label: '&View',
            submenu: process.env.NODE_ENV === 'development' || process.env.DEBUG_PROD === 'true'
                ? [{
                    label: '&Reload',
                    accelerator: 'Ctrl+R',
                    click: () => { this.mainWindow.webContents.reload(); },
                }, {
                    label: 'Toggle &Full Screen',
                    accelerator: 'F11',
                    click: () => { this.mainWindow.setFullScreen(!this.mainWindow.isFullScreen()); },
                }, {
                    label: 'Toggle &Developer Tools',
                    accelerator: 'Ctrl+Shift+I',
                    click: () => { this.mainWindow.webContents.toggleDevTools(); },
                },]
                : [{
                    label: 'Toggle &Full Screen',
                    accelerator: 'F11',
                    click: () => { this.mainWindow.setFullScreen(!this.mainWindow.isFullScreen()); },
                },],
        }, {
            label: 'Help',
            submenu: this.helpSubmenu(),
        },];
    }
}
