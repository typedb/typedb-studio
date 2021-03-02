export declare type Merge<L, R> = L & Pick<R, Exclude<keyof R, keyof L>>;
export declare function uuidv4(): string;
export declare type Stringable = {
    toString: () => string;
};
