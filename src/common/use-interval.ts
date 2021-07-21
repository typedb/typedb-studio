import React from 'react';

export function useInterval(callback: Function, delay: number) {
    const savedCallback = React.useRef<Function>();

    // Remember the latest callback.
    React.useEffect(() => {
        savedCallback.current = callback;
    }, [callback]);

    // Set up the interval.
    React.useEffect(() => {
        function tick() {
            savedCallback.current();
        }
        if (delay !== null) {
            const id = setInterval(tick, delay);
            return () => clearInterval(id);
        }
        return null;
    }, [delay]);
}
