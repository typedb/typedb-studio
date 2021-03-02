export declare class BlockingQueue<T> {
    private readonly _promises;
    private readonly _resolvers;
    constructor();
    private addPromise;
    add(t: T): void;
    take(): Promise<T>;
}
