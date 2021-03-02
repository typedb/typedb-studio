export declare class ServerAddress {
    private readonly _externalHost;
    private readonly _externalPort;
    private readonly _internalHost;
    private readonly _internalPort;
    constructor(externalHost: string, externalPort: number, internalHost: string, internalPort: number);
    static parse(address: string): ServerAddress;
    external(): string;
    externalHost(): string;
    externalPort(): number;
    internal(): string;
    internalHost(): string;
    internalPort(): number;
    toString(): string;
}
