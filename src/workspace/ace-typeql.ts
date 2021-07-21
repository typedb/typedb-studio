import 'brace/mode/java';
import { getAceInstance } from "react-ace/lib/editorOptions";

export class CustomHighlightRules extends getAceInstance().require("ace/mode/text_highlight_rules").TextHighlightRules {
    constructor() {
        super();
        this.$rules = {
            "start": [{
                regex: /#.*/,
                token: 'comment'
            }, {
                regex: /ERROR:.*/,
                token: 'error'
            }, {
                regex: /".*?"/,
                token: 'string'
            }, {
                regex: /((?:(?![-a-zA-Z_0-9]|\$).)|^|\s)(as|sub|sub!|has|owns|@key|abstract|relates|plays|value|match|isa|isa!|contains|regex|val|via|iid|label|define|undefine|get|insert|delete|aggregate|std|median|mean|max|min|sum|count|group|where|from|to|in|of|limit|offset|sort|asc|desc|when|then|commit)(?![-a-zA-Z_0-9])/,
                token: 'keyword',
                lookbehind: true
            }, {
                regex: /((?:(?![-a-zA-Z_0-9]|\$).)|^|\s)(entity|relation|attribute|rule|thing|boolean|double|long|string|datetime)(?![-a-zA-Z_0-9])/,
                token: 'super',
                lookbehind: true
            }, {
                regex: /typeql>>|answers>>|\.\.\./
            }, {
                regex: /\$[-a-zA-Z_0-9]+/,
                token: 'variable'
            }, {
                regex: /[0-9]+(\.[0-9][0-9]*)?/,
                token: 'number'
            }, {
                regex: /=|;|\.|\+|\*|,|\(|\)|:|{|}|!=|>|<|>=|<=/,
                token: 'operator'
            }]
        }
    }
}

export class AceTypeQL extends getAceInstance().require('ace/mode/java').Mode {
    constructor() {
        super();
        this.HighlightRules = CustomHighlightRules;
    }
}
