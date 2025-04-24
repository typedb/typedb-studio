import { Component } from '@angular/core';
import { MatMenuModule } from "@angular/material/menu";

@Component({
    selector: 'ts-hover-menu',
    template: `
      <div class="app-nav-item" [matMenuTriggerFor]="menu" #menuTrigger="matMenuTrigger"
           (mouseenter)="mouseEnter(menuTrigger)" (mouseleave)="mouseLeave(menuTrigger)">
        <ng-content select="[trigger]"></ng-content>
      </div>
      <mat-menu #menu="matMenu" [hasBackdrop]="false">
        <div (mouseenter)="mouseEnter(menuTrigger)" (mouseleave)="mouseLeave(menuTrigger)">
          <ng-content select="[content]"></ng-content>
        </div>
      </mat-menu>
    `,
    standalone: true,
    imports: [
        MatMenuModule
    ]
})
export class HoverMenuComponent {
    timedOutCloser: number | undefined;

    constructor() { }

    mouseEnter(trigger: { openMenu: () => void; }) {
        if (this.timedOutCloser) {
            clearTimeout(this.timedOutCloser);
        }
        trigger.openMenu();
    }

    mouseLeave(trigger: { closeMenu: () => void; }) {
        this.timedOutCloser = setTimeout(() => {
            trigger.closeMenu();
        }, 50);
    }
}
