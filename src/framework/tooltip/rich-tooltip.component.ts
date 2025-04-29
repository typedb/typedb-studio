import { NgTemplateOutlet } from "@angular/common";
import { Component, Input, OnInit, TemplateRef } from "@angular/core";

@Component({
    selector: 'ts-rich-tooltip',
    templateUrl: './rich-tooltip.component.html',
    standalone: true,
    styleUrls: ['./rich-tooltip.component.scss'],
    imports: [NgTemplateOutlet],
})
export class RichTooltipComponent implements OnInit {

    @Input({ required: true }) content!: TemplateRef<any>;

    constructor() { }

    ngOnInit() {
    }

}
