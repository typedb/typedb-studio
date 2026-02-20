/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output } from "@angular/core";
import { TypeQL, typeqlAutocompleteExtension } from "../codemirror-lang-typeql";
import { basicDark } from "./theme";
import { Extension, Prec } from "@codemirror/state";
import { EditorView, keymap } from "@codemirror/view";
import { startCompletion } from "@codemirror/autocomplete";
import { indentWithTab } from "@codemirror/commands";
import { FormControl, ReactiveFormsModule } from "@angular/forms";

@Component({
    selector: "tp-code-editor",
    templateUrl: "code-editor.component.html",
    styleUrls: ["code-editor.component.scss"],
    standalone: true,
    imports: [CodeEditor, ReactiveFormsModule],
    host: {
        '[class.has-scrollbar]': 'hasScrollbar'
    }
}) export class CodeEditorComponent implements AfterViewInit, OnDestroy {

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

    // Workaround for WebView IME input issues on macOS and Linux (Tauri)
    // Only applied when running in Tauri to avoid breaking dead keys/IME in browsers
    private readonly webViewInputFix = EditorView.domEventHandlers({
        keydown: (event, view) => {
            // Only handle printable characters that WKWebView fails to input
            if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
                const transaction = view.state.replaceSelection(event.key);
                view.dispatch(transaction);
                event.preventDefault();
                return true;
            }
            return false;
        }
    });

    private readonly isTauriMacOrLinux = !!(window as any).__TAURI_INTERNALS__ && (navigator.platform.startsWith('Mac') || navigator.platform.startsWith('Linux'));

    get extensions(): Extension[] {
        const baseExtensions = [this.codeEditorTheme, TypeQL(), typeqlAutocompleteExtension(), this.keymap];
        return this.isTauriMacOrLinux ? [...baseExtensions, this.webViewInputFix] : baseExtensions;
    }

    ran = false;
    copied = false;
    hasScrollbar = false;
    private resizeObserver?: ResizeObserver;

    constructor(private elementRef: ElementRef<HTMLElement>) {}

    ngAfterViewInit() {
        const scroller = this.elementRef.nativeElement.querySelector('.cm-scroller');
        if (scroller) {
            this.resizeObserver = new ResizeObserver(() => {
                this.hasScrollbar = scroller.scrollHeight > scroller.clientHeight;
            });
            this.resizeObserver.observe(scroller);
        }
    }

    ngOnDestroy() {
        this.resizeObserver?.disconnect();
    }

    async onRunButtonClick() {
        this.ran = true;

        this.runButtonClick.emit();

        setTimeout(() => {
            this.ran = false;
        }, 3000);
    }

    async onCopyButtonClick() {
        try {
            await navigator.clipboard.writeText(this.formControlProp.value);
            this.copied = true;
            setTimeout(() => {
                this.copied = false;
            }, 3000);
        } catch (err) {
            console.error('Failed to copy code:', err);
        }
    }
}
