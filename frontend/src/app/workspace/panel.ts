import { Component, ElementRef, HostListener, inject, input, signal } from '@angular/core';

@Component({
  selector: 'app-panel',
  template: ` <section
    [style.left.px]="maximized() ? 0 : x()"
    [style.top.px]="maximized() ? 0 : y()"
    [style.width]="maximized() ? '100%' : width() + 'px'"
    [style.height]="maximized() ? '100%' : height() + 'px'"
    [style.z-index]="layer()"
    [hidden]="minimized()"
    (pointerdown)="focus()"
  >
    <header (pointerdown)="start($event, 'move')">
      <span class="grip">⠿</span><strong>{{ title() }}</strong
      ><span class="tag">{{ tag() }}</span>
      <nav (pointerdown)="$event.stopPropagation()">
        <button (click)="minimized.set(true)" [attr.aria-label]="title() + ' minimalizálása'">
          −
        </button>
        <button
          (click)="maximized.set(!maximized())"
          [attr.aria-label]="title() + ' nagyítása / visszaállítása'"
        >
          □
        </button>
      </nav>
    </header>
    <div class="body"><ng-content /></div>
    <button
      class="resize"
      (pointerdown)="start($event, 'resize')"
      (keydown)="resizeKey($event)"
      aria-label="Panel méretezése; nyílbillentyűkkel is"
    >
      ◢
    </button>
  </section>`,
  styles: `
    :host {
      display: contents;
    }
    section {
      position: absolute;
      display: flex;
      flex-direction: column;
      background: #101622;
      border: 1px solid #303c50;
      border-radius: 14px;
      box-shadow: 0 24px 70px #0005;
      overflow: hidden;
    }
    section[hidden] {
      display: none;
    }
    header {
      display: flex;
      align-items: center;
      gap: 10px;
      height: 50px;
      flex-shrink: 0;
      padding: 0 16px;
      border-bottom: 1px solid #293345;
      background: #151e2c;
      cursor: grab;
      touch-action: none;
      user-select: none;
    }
    strong {
      font-size: 13px;
      font-weight: 600;
    }
    .grip {
      color: #71839d;
    }
    .tag {
      font: 10px monospace;
      letter-spacing: 1px;
      color: #8ca4bd;
    }
    nav {
      margin-left: auto;
      display: flex;
      gap: 5px;
    }
    button {
      color: #bac9db;
      background: transparent;
      border: 0;
      border-radius: 5px;
      cursor: pointer;
      font-size: 19px;
      width: 28px;
      height: 28px;
    }
    button:hover {
      background: #2b3950;
      color: #fff;
    }
    button:focus-visible {
      outline: 2px solid #78d5fc;
    }
    .body {
      flex: 1;
      min-height: 0;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }
    .resize {
      position: absolute;
      bottom: 0;
      right: 0;
      cursor: nwse-resize;
      font-size: 13px;
      touch-action: none;
      color: #7590ab;
    }
    @media (max-width: 760px) {
      section {
        position: relative !important;
        left: 0 !important;
        top: 0 !important;
        width: 100% !important;
        height: 620px !important;
        margin-bottom: 16px;
      }
      header {
        cursor: default;
      }
      .resize {
        display: none;
      }
    }
  `,
})
export class Panel {
  readonly title = input.required<string>();
  readonly tag = input('');
  readonly x = signal(0);
  readonly y = signal(0);
  readonly width = signal(500);
  readonly height = signal(600);
  readonly minimized = signal(false);
  readonly maximized = signal(false);
  readonly layer = signal(1);
  private static topLayer = 1;
  private host = inject(ElementRef<HTMLElement>);
  private gesture?: {
    mode: string;
    px: number;
    py: number;
    x: number;
    y: number;
    w: number;
    h: number;
  };
  focus() {
    this.layer.set(++Panel.topLayer);
  }
  restore() {
    this.minimized.set(false);
    this.focus();
  }
  arrange(x: number, y: number, w: number, h: number) {
    this.x.set(x);
    this.y.set(y);
    this.width.set(w);
    this.height.set(h);
    this.maximized.set(false);
    this.minimized.set(false);
  }
  start(e: PointerEvent, mode: string) {
    if (e.button !== 0 || this.maximized() || window.innerWidth <= 760) return;
    e.preventDefault();
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    this.gesture = {
      mode,
      px: e.clientX,
      py: e.clientY,
      x: this.x(),
      y: this.y(),
      w: this.width(),
      h: this.height(),
    };
  }
  @HostListener('document:pointermove', ['$event']) move(e: PointerEvent) {
    const g = this.gesture;
    if (!g) return;
    const parent = this.host.nativeElement.parentElement!;
    const dx = e.clientX - g.px,
      dy = e.clientY - g.py;
    if (g.mode === 'move') {
      this.x.set(Math.max(0, Math.min(parent.clientWidth - this.width(), g.x + dx)));
      this.y.set(Math.max(0, Math.min(parent.clientHeight - this.height(), g.y + dy)));
    } else {
      this.width.set(Math.max(300, Math.min(parent.clientWidth - this.x(), g.w + dx)));
      this.height.set(Math.max(260, Math.min(parent.clientHeight - this.y(), g.h + dy)));
    }
  }
  @HostListener('document:pointerup') end() {
    this.gesture = undefined;
  }
  @HostListener('document:pointercancel') cancel() {
    this.gesture = undefined;
  }
  resizeKey(e: KeyboardEvent) {
    if (!['ArrowRight', 'ArrowLeft', 'ArrowUp', 'ArrowDown'].includes(e.key)) return;
    e.preventDefault();
    const p = this.host.nativeElement.parentElement!;
    this.width.set(
      Math.max(
        300,
        Math.min(
          p.clientWidth - this.x(),
          this.width() + (e.key === 'ArrowRight' ? 20 : e.key === 'ArrowLeft' ? -20 : 0),
        ),
      ),
    );
    this.height.set(
      Math.max(
        260,
        Math.min(
          p.clientHeight - this.y(),
          this.height() + (e.key === 'ArrowDown' ? 20 : e.key === 'ArrowUp' ? -20 : 0),
        ),
      ),
    );
  }
}
