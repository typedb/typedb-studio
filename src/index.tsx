import React from "react";
import ReactDOM from "react-dom";
import "./assets/css/fonts.css";
import "./assets/css/reset.css";
import "./assets/css/base.scss";
// import "./assets/css/prism.scss"; // TODO: grab these from typedb-visualiser
// import {installPrismTypeQL} from "../common/typeql/prism-typeql";
import {StudioApp} from "./app";

// installPrismTypeQL();

ReactDOM.render(<StudioApp/>, document.getElementById('root'));
