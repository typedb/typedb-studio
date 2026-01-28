import { Directive, Input, TemplateRef, ElementRef, HostListener, ComponentRef, OnDestroy } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { RichTooltipComponent } from "./rich-tooltip.component";

@Directive({
    standalone: true,
    selector: '[richTooltip]'
})
export class RichTooltipDirective implements OnDestroy {

    @Input() showTooltip = true;
    @Input({ required: true }) richTooltipContent!: string | TemplateRef<any>;

    private _overlayRef: OverlayRef | null = null;

    constructor(private _overlay: Overlay,
                private _elementRef: ElementRef) { }

    private createOverlay(): OverlayRef {
        // Dispose old overlay if exists
        if (this._overlayRef) {
            this._overlayRef.dispose();
        }

        // Use flexible connected position strategy for automatic viewport handling
        const positionStrategy = this._overlay.position()
            .flexibleConnectedTo(this._elementRef)
            .withPositions([
                // Below, with overlay starting from anchor's left edge
                { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetY: 5 },
                // Above, with overlay starting from anchor's left edge
                { originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', offsetY: -5 },
            ])
            .withPush(true)
            .withViewportMargin(8);

        this._overlayRef = this._overlay.create({
            positionStrategy,
            scrollStrategy: this._overlay.scrollStrategies.reposition(),
            maxWidth: 400
        });
        return this._overlayRef;
    }

    @HostListener('mouseenter')
    show() {
        if (!this.showTooltip) {
            return;
        }

        // Create fresh overlay each time to ensure correct positioning
        const overlayRef = this.createOverlay();
        const tooltipRef: ComponentRef<RichTooltipComponent> = overlayRef.attach(new ComponentPortal(RichTooltipComponent));
        tooltipRef.instance.content = this.richTooltipContent;
    }

    @HostListener('mouseleave')
    hide() {
        this.closeToolTip();
    }

    ngOnDestroy() {
        if (this._overlayRef) {
            this._overlayRef.dispose();
            this._overlayRef = null;
        }
    }

    private closeToolTip() {
        if (this._overlayRef) {
            this._overlayRef.detach();
        }
    }

}
