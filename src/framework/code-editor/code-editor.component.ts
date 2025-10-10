/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { Component, EventEmitter, Input, Output, signal } from "@angular/core";
import { TypeQL, typeqlAutocompleteExtension } from "../codemirror-lang-typeql";
import { basicDark } from "./theme";
import { Extension, Prec } from "@codemirror/state";
import { keymap } from "@codemirror/view";
import { startCompletion } from "@codemirror/autocomplete";
import { indentWithTab } from "@codemirror/commands";
import { FormControl, ReactiveFormsModule } from "@angular/forms";

@Component({
    selector: "tp-code-editor",
    templateUrl: "code-editor.component.html",
    styleUrls: ["code-editor.component.scss"],
    standalone: true,
    imports: [CodeEditor, ReactiveFormsModule],
}) export class CodeEditorComponent {

    @Input() keymap: Extension = Prec.highest(keymap.of([
        { key: "Alt-Space", run: startCompletion, preventDefault: true },
        indentWithTab,
    ]));
    @Input({ required: true }) formControlProp!: FormControl<string>;
    @Input() runOverlayVisible = false;
    @Output() runButtonClick = new EventEmitter<void>();

    readonly codeEditorTheme = basicDark;
    protected readonly TypeQL = TypeQL;
    protected readonly typeqlAutocompleteExtension = typeqlAutocompleteExtension;

    ran = signal(false);

    async onRunButtonClick() {
        this.ran.set(true);

        this.runButtonClick.emit();

        setTimeout(() => {
            this.ran.set(false);
        }, 3000);
    }
}
