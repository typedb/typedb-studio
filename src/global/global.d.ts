import { StudioTheme } from "../styles/theme";

declare global {
    interface Window {
        studio: {
            theme: StudioTheme;
        }
    }
}
