ace.define("ace/theme/studio-dark", ["require", "exports", "module", "ace/lib/dom"], function(require, exports, module) {

    exports.isDark = true;
    exports.cssClass = "ace-studio-dark";
    exports.cssText = `.ace-studio-dark .ace_gutter {
background: #2F3129;
color: #8F908A
}
.ace-studio-dark .ace_print-margin {
width: 1px;
background: #555651
}
.ace-studio-dark {
background-color: #272822;
color: #F8F8F2
}
.ace-studio-dark .ace_cursor {
color: #F8F8F0
}
.ace-studio-dark .ace_marker-layer .ace_selection {
background: #49483E
}
.ace-studio-dark.ace_multiselect .ace_selection.ace_start {
box-shadow: 0 0 3px 0px #272822;
}
.ace-studio-dark .ace_marker-layer .ace_step {
background: rgb(102, 82, 0)
}
.ace-studio-dark .ace_marker-layer .ace_bracket {
margin: -1px 0 0 -1px;
border: 1px solid #49483E
}
.ace-studio-dark .ace_marker-layer .ace_active-line {
background: #202020
}
.ace-studio-dark .ace_gutter-active-line {
background-color: #272727
}
.ace-studio-dark .ace_marker-layer .ace_selected-word {
border: 1px solid #49483E
}
.ace-studio-dark .ace_invisible {
color: #52524d
}
.ace-studio-dark .ace_entity.ace_name.ace_tag,
.ace-studio-dark .ace_keyword,
.ace-studio-dark .ace_meta.ace_tag,
.ace-studio-dark .ace_storage {
color: #F92672
}
.ace-studio-dark .ace_punctuation,
.ace-studio-dark .ace_punctuation.ace_tag {
color: #fff
}
.ace-studio-dark .ace_constant.ace_character,
.ace-studio-dark .ace_constant.ace_language,
.ace-studio-dark .ace_constant.ace_numeric,
.ace-studio-dark .ace_constant.ace_other {
color: #AE81FF
}
.ace-studio-dark .ace_invalid {
color: #F8F8F0;
background-color: #F92672
}
.ace-studio-dark .ace_invalid.ace_deprecated {
color: #F8F8F0;
background-color: #AE81FF
}
.ace-studio-dark .ace_support.ace_constant,
.ace-studio-dark .ace_support.ace_function {
color: #66D9EF
}
.ace-studio-dark .ace_fold {
background-color: #A6E22E;
border-color: #F8F8F2
}
.ace-studio-dark .ace_storage.ace_type,
.ace-studio-dark .ace_support.ace_class,
.ace-studio-dark .ace_support.ace_type {
font-style: italic;
color: #66D9EF
}
.ace-studio-dark .ace_entity.ace_name.ace_function,
.ace-studio-dark .ace_entity.ace_other,
.ace-studio-dark .ace_entity.ace_other.ace_attribute-name,
.ace-studio-dark .ace_variable {
color: #A6E22E
}
.ace-studio-dark .ace_variable.ace_parameter {
font-style: italic;
color: #FD971F
}
.ace-studio-dark .ace_string {
color: #E6DB74
}
.ace-studio-dark .ace_comment {
color: #75715E
}
.ace-studio-dark .ace_indent-guide {
background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAEklEQVQImWPQ0FD0ZXBzd/wPAAjVAoxeSgNeAAAAAElFTkSuQmCC) right repeat-y
}`;

    var dom = require("../lib/dom");
    dom.importCssString(exports.cssText, exports.cssClass);
});

(function() {
    ace.require(["ace/theme/studio-dark"], function(m) {
        if (typeof module == "object" && typeof exports == "object" && module) {
            module.exports = m;
        }
    });
})();
