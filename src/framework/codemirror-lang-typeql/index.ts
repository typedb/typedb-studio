
import { parser } from "./generated/typeql.grammar.generated"
import { LRLanguage, LanguageSupport, indentNodeProp, foldNodeProp, foldInside, delimitedIndent, syntaxTree } from "@codemirror/language"
import { styleTags, tags as t } from "@lezer/highlight"
import { Diagnostic } from "@codemirror/lint";
import { EditorView } from "@codemirror/view";
import { linter } from '@codemirror/lint'
import { autocompletion, CompletionContext } from "@codemirror/autocomplete";
import { NodePrefixAutoComplete } from "./complete"
import { Schema, SchemaImpl } from "./schema";
import { SUGGESTION_MAP } from "./typeql_suggestions";

export const TypeQLLanguage = LRLanguage.define({
  parser: parser.configure({
    props: [
      indentNodeProp.add({

      }),
      foldNodeProp.add({
        QueryStage: foldInside
      }),
      styleTags({
        // See: https://lezer.codemirror.net/docs/ref/#highlight.tags

        VAR: t.variableName,

        // Literals
        STRINGLITERAL: t.string,
        INTEGERLITERAL: t.number,
        DOUBLELITERAL: t.number,
        BOOLEANLITERAL: t.bool,

        // Types
        LABEL: t.typeName,

        BOOLEAN: t.typeName,
        INTEGER: t.typeName,
        DOUBLE: t.typeName,
        DECIMAL: t.typeName,
        DATETIMETZ: t.typeName,
        DATETIME: t.typeName,
        DATE: t.typeName,
        DURATION: t.typeName,
        STRING: t.typeName,


        // Keywords
        ISA: t.keyword,
        HAS: t.keyword,
        LINKS: t.keyword,
        OWNS: t.keyword,
        RELATES: t.keyword,
        PLAYS: t.keyword,

        FUN: t.keyword,
        LET: t.keyword,
        FIRST: t.keyword,
        LAST: t.keyword,

        // Value type names?

        // Stages
        DEFINE: t.heading1,
        UNDEFINE: t.heading1,
        REDEFINE: t.heading1,

        MATCH: t.heading1,
        INSERT: t.heading1,
        DELETE: t.heading1,
        UPDATE: t.heading1,
        PUT: t.heading1,
        END: t.heading1,

        SELECT: t.heading1,
        REDUCE: t.heading1,
        SORT: t.heading1,
        OFFSET: t.heading1,
        LIMIT: t.heading1,
        REQUIRE: t.heading1,
        DISTINCT: t.heading1,
        GROUPBY: t.heading1,

        // SubPattern
        OR: t.controlOperator,
        NOT: t.controlOperator,
        TRY: t.controlOperator,

        // Misc
        Annotation: t.meta,
        LINECOMMENT: t.lineComment,
      })
    ]
  }),
  languageData: {
    commentTokens: { line: "#" }
  }
})


export function TypeQL() {
  return new LanguageSupport(TypeQLLanguage, [])
}


export function typeqlAutocompleteExtension() {
  let typeqlAutocomplete = new NodePrefixAutoComplete(SUGGESTION_MAP, new Schema());
  let autocomplete_fn = (context: CompletionContext) => typeqlAutocomplete.autocomplete(context);
  return autocompletion({ activateOnTypingDelay: 100, override: [autocomplete_fn] });
}

// A Linter which flags syntax errors from: https://discuss.codemirror.net/t/showing-syntax-errors/3111/6
export function otherExampleLinter() {
  return linter((view: EditorView) => {
    const diagnostics: Diagnostic[] = [];
    syntaxTree(view.state).iterate({
      enter: n => {
        if (n.type.isError) {
          diagnostics.push({
            from: n.from,
            to: n.to,
            severity: "error",
            message: "Syntax error.",
          });
        }
      },
    });
    return diagnostics;
  });
}

export function typeqlSchemaFromText(text: string): SchemaImpl {
  return SchemaImpl.fromTypeQL(text, parser.parse(text));
}
