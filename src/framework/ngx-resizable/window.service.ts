import { Injectable } from '@angular/core';

function getWindow(): any {
  return window;
}

@Injectable({
  providedIn: 'root'
})
export class NgxResizeableWindowRef {
  get nativeWindow(): any {
    return getWindow();
  }
}
