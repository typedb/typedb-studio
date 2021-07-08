import React from "react";
import ReactDOM from "react-dom";
import "./assets/css/fonts.css";
import "./assets/css/reset.css";
import "./assets/css/base.scss";
import "./typedb-visualiser/assets/prism.scss";
import {StudioApp} from "./app";
import { installPrismTypeQL } from "./typedb-visualiser";

installPrismTypeQL();

ReactDOM.render(<StudioApp/>, document.getElementById('root'));
