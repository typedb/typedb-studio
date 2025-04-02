import { Directive, Input, TemplateRef, ElementRef, OnInit, HostListener, ComponentRef, OnDestroy } from '@angular/core';
import { Overlay, OverlayPositionBuilder, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { RichTooltipComponent } from "./rich-tooltip.component";

@Directive({
    standalone: true,
    selector: '[richTooltip]'
})
export class RichTooltipDirective {

    @Input() showTooltip = true;
    @Input({ required: true }) richTooltipContent!: TemplateRef<any>;

    private _overlayRef!: OverlayRef;

    constructor(private _overlay: Overlay,
                private _overlayPositionBuilder: OverlayPositionBuilder,
                private _elementRef: ElementRef) { }

    ngOnInit() {
        if (!this.showTooltip) {
            return;
        }

        const positionStrategy = this._overlayPositionBuilder
            .flexibleConnectedTo(this._elementRef)
            .withPositions([{
                originX: 'center',
                originY: 'bottom',
                overlayX: 'center',
                overlayY: 'top',
                offsetY: 5,
            }]);

        this._overlayRef = this._overlay.create({ positionStrategy});
    }

    @HostListener('mouseenter')
    show() {
        if (this._overlayRef && !this._overlayRef.hasAttached()) {
            const tooltipRef: ComponentRef<RichTooltipComponent> = this._overlayRef.attach(new ComponentPortal(RichTooltipComponent));
            tooltipRef.instance.content = this.richTooltipContent;
        }
    }

    @HostListener('mouseleave')
    hide() {
        this.closeToolTip();
    }

    ngOnDestroy() {
        this.closeToolTip();
    }

    private closeToolTip() {
        if (this._overlayRef) {
            this._overlayRef.detach();
        }
    }

}
