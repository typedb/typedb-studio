import Prism from "prismjs";

export function installPrismTypeQL() {
    Prism.languages.typeql = {
        comment: {
            pattern: /#.*/,
            alias: 'comment'
        },
        error: {
            pattern: /ERROR:.*/,
            alias: 'error'
        },
        string: {
            pattern: /".*?"/,
            alias: 'string'
        },
        keyword: {
            pattern: /((?:(?![-a-zA-Z_0-9]|\$).)|^|\s)(as|sub|sub!|has|owns|@key|abstract|relates|plays|value|match|isa|isa!|contains|regex|val|via|iid|label|define|undefine|get|insert|delete|aggregate|std|median|mean|max|min|sum|count|group|where|from|to|in|of|limit|offset|sort|asc|desc|when|then|commit)(?![-a-zA-Z_0-9])/,
            alias: 'typeql-keyword',
            lookbehind: true
        },
        super: {
            pattern: /((?:(?![-a-zA-Z_0-9]|\$).)|^|\s)(entity|relation|attribute|rule|thing|boolean|double|long|string|datetime)(?![-a-zA-Z_0-9])/,
            alias: 'type',
            lookbehind: true
        },
        special: {
            pattern: /typeql>>|answers>>|\.\.\./
        },
        variable: {
            pattern: /\$[-a-zA-Z_0-9]+/,
            alias: 'variable'
        },
        number: {
            pattern: /[0-9]+(\.[0-9][0-9]*)?/,
            alias: 'number'
        },
        operator: {
            pattern: /=|;|\.|\+|\*|,|\(|\)|:|{|}|!=|>|<|>=|<=/,
            alias: 'operator'
        }
    };
}
