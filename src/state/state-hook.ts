import React, { useEffect, useState } from "react";

export class StateHook<TValue> {
    private _value: TValue;
    private _observers: React.Dispatch<React.SetStateAction<TValue>>[];

    constructor(initialValue: TValue = null) {
        this._value = initialValue;
        this._observers = [];
    }

    set(value: TValue) {
        this._value = value;
        this._observers.forEach(update => update(value));
    }

    use(): [TValue, Function] {
        const [state, setState] = useState(this._value);

        useEffect(() => {
            this._observers.push(setState);
            setState(this._value);
            return () => {
                this._observers = this._observers.filter(update => update !== setState);
            };
        }, []);

        return [state, this.set];
    }
}
