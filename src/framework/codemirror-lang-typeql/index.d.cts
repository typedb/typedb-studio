import { LRLanguage, LanguageSupport } from "@codemirror/language";
declare const TypeQLLanguage: LRLanguage;
declare function TypeQL(): LanguageSupport;
declare function typeqlAutocompleteExtension(): import("@codemirror/state").Extension;
declare function otherExampleLinter(): import("@codemirror/state").Extension;
export { TypeQLLanguage, TypeQL, typeqlAutocompleteExtension, otherExampleLinter };
