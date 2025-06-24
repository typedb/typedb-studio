import { Directive, HostBinding, HostListener } from "@angular/core";

@Directive({
    selector: "[detectScroll]"
})
export class DetectScrollDirective {
    @HostBinding("class.scrolled") scrolled = false;

    @HostListener("scroll", ["$event.target"]) onScroll(target: any) {
        this.scrolled = target.scrollTop > 0;
    }
}
