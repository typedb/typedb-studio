import { Component, OnInit, HostBinding, Input, ElementRef, ViewEncapsulation, Output, EventEmitter, AfterViewInit } from '@angular/core';
import { NgxResizeableWindowRef } from '../window.service';
import { DragDirective } from "./drag.directive";

@Component({
    selector: 'rsz-layout',
    templateUrl: 'resizable.component.html',
    styleUrls: ['resizable.component.scss'],
    providers: [{ provide: 'Window', useValue: window }],
    standalone: true,
    imports: [
        DragDirective
    ],
    encapsulation: ViewEncapsulation.None
})
export class ResizableComponent implements OnInit, AfterViewInit {

  @HostBinding('class.resizable') resizable = true;
  @HostBinding('class.no-transition') noTransition = false;
  @HostBinding('style.width') width?: string;
  @HostBinding('style.height') height?: string;
  @HostBinding('style.flex-basis') flexBasis: any;

  @Input() directions: any;
  @Input() rFlex = false;

  @Output() resizeStart = new EventEmitter();
  @Output() resizing = new EventEmitter();
  @Output() resizeEnd = new EventEmitter();

  private nativeElement;

  private style?: { getPropertyValue: (arg0: string) => string; };

  private w?: number;
  private h?: number;

  private vx = 1;
  private vy = 1;

  private start?: number;

  private dragDir?: string;

  private axis?: string;

  private info: any = {};

  constructor(private regionElement: ElementRef, private windowRef: NgxResizeableWindowRef) {
    this.nativeElement = this.regionElement.nativeElement;
  }

  ngOnInit() {
    if (!this.rFlex) { this.resizable = false; } // Added to permit use of component for all cells
    this.flexBasis = 'flexBasis' in this.nativeElement.style ? 'flexBasis' :
      'webkitFlexBasis' in this.nativeElement.style ? 'webkitFlexBasis' :
      'msFlexPreferredSize' in this.nativeElement.style ? 'msFlexPreferredSize' : 'flexBasis';
  }

  ngAfterViewInit() {
    this.style = this.windowRef.nativeWindow.getComputedStyle(this.nativeElement);
  }

  private updateInfo(e: any) {
    this.info['width'] = false; this.info['height'] = false;
    if (this.axis === 'x') {
      this.info['width'] = parseInt(this.nativeElement.style[this.rFlex ? this.flexBasis : 'width'], 10);
    } else {
      this.info['height'] = parseInt(this.nativeElement.style[this.rFlex ? this.flexBasis : 'height'], 10);
    }
    this.info['id'] = this.nativeElement.id;
    this.info['evt'] = e;
  }

  public dragStart(e: { originalEvent: any; }, direction: string | undefined) {
    const mouseEvent = e.originalEvent;

    this.dragDir = direction;
    this.axis = (this.dragDir === 'left' || this.dragDir === 'right') ? 'x' : 'y';
    this.start = (this.axis === 'x' ? mouseEvent.clientX : mouseEvent.clientY);
    this.w = parseInt(this.style!.getPropertyValue('width'), 10);
    this.h = parseInt(this.style!.getPropertyValue('height'), 10);

    this.resizeStart.emit({ info: this.info });

    // prevent transition while dragging
    this.noTransition = true;
  }

  public dragEnd(e: { originalEvent: any; }) {
    const mouseEvent = e.originalEvent;

    this.updateInfo(mouseEvent);
    this.resizeEnd.emit({ info: this.info });
    this.noTransition = false;
  }

  public dragging(e: { originalEvent: any; }) {
    const mouseEvent = e.originalEvent;
    const offset = (this.axis === 'x') ? this.start! - mouseEvent.clientX : this.start! - mouseEvent.clientY;

    let operand = 1;
    switch (this.dragDir) {
          // @ts-ignore
      case 'top':
        operand = -1;
        /* falls through */
      case 'bottom':
        const height = (this.h! - offset * this.vy * operand) + 'px';
        if (this.rFlex) {
          this.flexBasis = height;
        } else {
          this.height = height;
        }
        break;
        // @ts-ignore
      case 'left':
        operand = -1;
        /* falls through */
      case 'right':
        const width = (this.w! - offset * this.vx * operand) + 'px';
        if (this.rFlex) {
          this.flexBasis = width;
        } else {
          this.width = width;
        }
        break;
    }
    this.updateInfo(mouseEvent);
    this.resizing.emit({ info: this.info });
  }
}
