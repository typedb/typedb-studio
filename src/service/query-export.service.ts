/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import {
    ApiResponse, Concept, ConceptDocument, ConceptRowAnswer, isApiErrorResponse, QueryResponse,
} from "@typedb/driver-http";
import { RunOutputState } from "./query-page-state.service";

export interface SerializedOutput {
    text: string;
    mime: string;
    ext: string;
}

@Injectable({ providedIn: "root" })
export class QueryExportService {

    serializeLog(run: RunOutputState, format: "log" | "results-text" | "results-json"): SerializedOutput | null {
        if (format === "log") {
            const text = run.log.control.value;
            if (!text) return null;
            return { text, mime: "text/plain;charset=utf-8", ext: "txt" };
        }
        const res = run.lastResponse;
        if (!res || isApiErrorResponse(res)) return null;
        if (format === "results-text") {
            const text = this.formatResultsText(res);
            if (text == null) return null;
            return { text, mime: "text/plain;charset=utf-8", ext: "txt" };
        }
        const json = this.formatResultsJson(res);
        if (json == null) return null;
        return { text: json, mime: "application/json;charset=utf-8", ext: "json" };
    }

    serializeTable(run: RunOutputState, format: "csv" | "json"): SerializedOutput | null {
        const res = run.lastResponse;
        if (!res || isApiErrorResponse(res)) return null;
        if (res.ok.answerType === "conceptRows") {
            const answers = res.ok.answers;
            if (format === "csv") {
                return { text: this.conceptRowsToCsv(answers), mime: "text/csv;charset=utf-8", ext: "csv" };
            }
            return { text: JSON.stringify(answers.map(a => a.data), null, 2), mime: "application/json;charset=utf-8", ext: "json" };
        }
        if (res.ok.answerType === "conceptDocuments") {
            const answers = res.ok.answers as ConceptDocument[];
            if (format === "csv") {
                return { text: this.documentsToCsv(answers), mime: "text/csv;charset=utf-8", ext: "csv" };
            }
            return { text: JSON.stringify(answers, null, 2), mime: "application/json;charset=utf-8", ext: "json" };
        }
        return null;
    }

    serializeRaw(run: RunOutputState): SerializedOutput | null {
        const text = run.raw.control.value;
        if (!text) return null;
        return { text, mime: "application/json;charset=utf-8", ext: "json" };
    }

    canExportResults(run: RunOutputState | undefined): boolean {
        const res = run?.lastResponse;
        if (!res || isApiErrorResponse(res)) return false;
        if (res.ok.answerType === "conceptRows") return res.ok.answers.length > 0;
        if (res.ok.answerType === "conceptDocuments") return (res.ok.answers as unknown[]).length > 0;
        return false;
    }

    download(run: RunOutputState, suffix: string, payload: SerializedOutput): void {
        const blob = new Blob([payload.text], { type: payload.mime });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = this.makeFilename(run, suffix, payload.ext);
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 0);
    }

    async copy(payload: SerializedOutput): Promise<void> {
        await navigator.clipboard.writeText(payload.text);
    }

    private formatResultsText(res: ApiResponse<QueryResponse>): string | null {
        if (isApiErrorResponse(res)) return null;
        if (res.ok.answerType === "conceptRows") {
            const answers = res.ok.answers;
            if (!answers.length) return "";
            return answers.map(a => {
                const cols = Object.keys(a.data).sort();
                return cols.map(col => `$${col}: ${conceptCellValue(a.data[col])}`).join("\n");
            }).join("\n\n") + "\n";
        }
        if (res.ok.answerType === "conceptDocuments") {
            const answers = res.ok.answers as ConceptDocument[];
            if (!answers.length) return "";
            return answers.map(d => {
                try { return JSON.stringify(d, null, 2); } catch { return String(d); }
            }).join("\n\n") + "\n";
        }
        return null;
    }

    private formatResultsJson(res: ApiResponse<QueryResponse>): string | null {
        if (isApiErrorResponse(res)) return null;
        if (res.ok.answerType === "conceptRows") {
            return JSON.stringify(res.ok.answers.map(a => a.data), null, 2);
        }
        if (res.ok.answerType === "conceptDocuments") {
            return JSON.stringify(res.ok.answers, null, 2);
        }
        return null;
    }

    private conceptRowsToCsv(answers: ConceptRowAnswer[]): string {
        if (!answers.length) return "";
        const columns = this.collectColumns(answers.map(a => a.data));
        const header = columns.map(csvEscape).join(",");
        const rows = answers.map(answer =>
            columns.map(col => csvEscape(conceptCellValue(answer.data[col]))).join(",")
        );
        return [header, ...rows].join("\r\n") + "\r\n";
    }

    private documentsToCsv(answers: ConceptDocument[]): string {
        if (!answers.length) return "";
        const columns = this.collectColumns(answers as Record<string, unknown>[]);
        const header = columns.map(csvEscape).join(",");
        const rows = (answers as Record<string, unknown>[]).map(doc =>
            columns.map(col => csvEscape(stringifyCell(doc[col]))).join(",")
        );
        return [header, ...rows].join("\r\n") + "\r\n";
    }

    private collectColumns(rows: Record<string, unknown>[]): string[] {
        const seen = new Set<string>();
        const columns: string[] = [];
        for (const row of rows) {
            for (const key of Object.keys(row)) {
                if (!seen.has(key)) {
                    seen.add(key);
                    columns.push(key);
                }
            }
        }
        return columns;
    }

    private makeFilename(run: RunOutputState, suffix: string, ext: string): string {
        const stem = sanitize(run.label) || "run";
        const db = run.graph.database ? `-${sanitize(run.graph.database)}` : "";
        const ts = new Date().toISOString().replace(/[:.]/g, "-");
        return `${stem}${db}-${suffix}-${ts}.${ext}`;
    }
}

function sanitize(name: string): string {
    return name.replace(/[\\/:*?"<>|]/g, "_").trim();
}

function csvEscape(value: string): string {
    if (value == null) return "";
    if (/[",\r\n]/.test(value)) {
        return `"${value.replace(/"/g, '""')}"`;
    }
    return value;
}

function stringifyCell(value: unknown): string {
    if (value == null) return "";
    if (typeof value === "string") return value;
    try {
        return JSON.stringify(value);
    } catch {
        return String(value);
    }
}

function conceptCellValue(concept: Concept | undefined): string {
    if (!concept) return "";
    switch (concept.kind) {
        case "entityType":
        case "relationType":
        case "roleType":
        case "attributeType":
            return concept.label;
        case "entity":
        case "relation": {
            const typeLabel = concept.type?.label;
            return typeLabel ? `${typeLabel}:${concept.iid}` : concept.iid;
        }
        case "attribute": {
            const typeLabel = concept.type?.label;
            const value = formatAttributeValue(concept);
            return typeLabel ? `${typeLabel}:${value}` : value;
        }
        case "value":
            return formatAttributeValue(concept);
    }
}

function formatAttributeValue(value: { valueType: string; value: unknown }): string {
    if (value.value == null) return "";
    return String(value.value);
}
