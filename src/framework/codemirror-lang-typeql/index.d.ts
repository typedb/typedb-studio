import { LRLanguage, LanguageSupport } from "@codemirror/language";
declare const TypeQLLanguage: LRLanguage;
declare function TypeQL(): LanguageSupport;
declare function otherExampleLinter(): import("@codemirror/state").Extension;
export { TypeQLLanguage, TypeQL, otherExampleLinter };
