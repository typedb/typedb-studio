import { Component, inject, Input } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MatMenuModule } from "@angular/material/menu";
import { GraphStyleService, CustomPreset } from "../../service/graph-style.service";
import { GraphVisualiser } from "../graph-visualiser";

@Component({
    selector: "ts-graph-styles-themes-tab",
    templateUrl: "themes-tab.component.html",
    styleUrls: ["graph-styles-pane.component.scss"],
    imports: [FormsModule, MatMenuModule],
})
export class ThemesTabComponent {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);

    get activePreset(): string | null {
        return this.styleService.activePreset;
    }

    appliedPreset: string | null = null;
    private appliedTimer: ReturnType<typeof setTimeout> | null = null;

    applyPreset(preset: "default" | "structure" | "uniform" | "classic" | "grayscale"): void {
        if (preset === "default") {
            this.styleService.applyDefaultPreset();
            this.visualiser?.restoreLabels();
            this.visualiser?.applyEdgeStyleUpdate();
            this.visualiser?.colorEdgesByConstraintIndex(true);
        } else if (preset === "structure") {
            this.styleService.applyStructurePreset();
            this.visualiser?.applyStructureMode();
        } else if (preset === "uniform") {
            this.styleService.applyUniformPreset();
            this.visualiser?.restoreLabels();
        } else if (preset === "classic") {
            this.styleService.applyClassicPreset();
            this.visualiser?.restoreLabels();
        } else if (preset === "grayscale") {
            this.styleService.applyGrayscalePreset();
            this.visualiser?.restoreLabels();
        }
        if (this.appliedTimer) clearTimeout(this.appliedTimer);
        this.appliedPreset = preset;
        this.appliedTimer = setTimeout(() => { this.appliedPreset = null; }, 2000);
    }

    // -- Custom presets --

    get customPresets(): readonly CustomPreset[] {
        return this.styleService.customPresets;
    }

    savingPreset = false;
    newPresetName = "";
    newPresetDescription = "";

    startSavingPreset(): void {
        this.savingPreset = true;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    confirmSavePreset(): void {
        const name = this.newPresetName.trim();
        if (!name) return;
        this.styleService.saveCustomPreset(name, this.newPresetDescription.trim());
        this.savingPreset = false;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    cancelSavePreset(): void {
        this.savingPreset = false;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    applyCustomPreset(name: string): void {
        this.styleService.applyCustomPreset(name);
        this.visualiser?.restoreLabels();
        this.visualiser?.applyEdgeStyleUpdate();
        if (this.appliedTimer) clearTimeout(this.appliedTimer);
        this.appliedPreset = `custom:${name}`;
        this.appliedTimer = setTimeout(() => { this.appliedPreset = null; }, 2000);
    }

    editingPreset: string | null = null;
    editPresetName = "";
    editPresetDescription = "";

    startEditPreset(name: string): void {
        const preset = this.customPresets.find(p => p.name === name);
        if (!preset) return;
        this.editingPreset = name;
        this.editPresetName = preset.name;
        this.editPresetDescription = preset.description;
    }

    confirmEditPreset(originalName: string): void {
        const name = this.editPresetName.trim();
        if (!name) return;
        this.styleService.renameCustomPreset(originalName, name, this.editPresetDescription.trim());
        this.editingPreset = null;
    }

    cancelEditPreset(): void {
        this.editingPreset = null;
    }

    overwrittenPreset: string | null = null;
    private overwrittenTimer: ReturnType<typeof setTimeout> | null = null;

    overwriteCustomPreset(name: string): void {
        const preset = this.customPresets.find(p => p.name === name);
        if (!preset) return;
        this.styleService.saveCustomPreset(name, preset.description);
        if (this.overwrittenTimer) clearTimeout(this.overwrittenTimer);
        this.overwrittenPreset = name;
        this.overwrittenTimer = setTimeout(() => { this.overwrittenPreset = null; }, 2000);
    }

    deleteCustomPreset(name: string): void {
        this.styleService.deleteCustomPreset(name);
    }
}
