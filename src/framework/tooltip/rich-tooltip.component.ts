import { NgTemplateOutlet } from "@angular/common";
import { Component, Input, OnInit, TemplateRef } from "@angular/core";

@Component({
    selector: 'ts-rich-tooltip',
    templateUrl: './rich-tooltip.component.html',
    styleUrls: ['./rich-tooltip.component.scss'],
    imports: [NgTemplateOutlet]
})
export class RichTooltipComponent implements OnInit {

    @Input({ required: true }) content!: string | TemplateRef<any>;

    constructor() { }

    ngOnInit() {
    }

    contentIsString() {
        return typeof this.content === "string";
    }

    get contentLines(): string[] | null {
        return this.contentIsString() ? (this.content as string).split(`\n`) : null;
    }
}
