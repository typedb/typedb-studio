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

        const rect = this._elementRef.nativeElement.getBoundingClientRect();

        // Use global position strategy with manually calculated position
        const positionStrategy = this._overlay.position()
            .global()
            .left(`${rect.left + rect.width / 2}px`)
            .top(`${rect.bottom + 5}px`);

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
