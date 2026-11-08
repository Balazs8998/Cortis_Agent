import { TestBed } from '@angular/core/testing';
import { Panel } from './panel';

describe('Workspace panel', () => {
  it('restores a minimized panel and resets maximized geometry', () => {
    const fixture = TestBed.createComponent(Panel);
    fixture.componentRef.setInput('title', 'Test');
    const panel = fixture.componentInstance;
    panel.minimized.set(true);
    panel.restore();
    expect(panel.minimized()).toBe(false);
    panel.maximized.set(true);
    panel.arrange(20, 30, 400, 500);
    expect([panel.x(), panel.y(), panel.width(), panel.height()]).toEqual([20,30,400,500]);
    expect(panel.maximized()).toBe(false);
  });
  it('clamps dragging and resizing to the workspace', () => {
    const fixture = TestBed.createComponent(Panel);
    fixture.componentRef.setInput('title', 'Test');
    const parent = document.createElement('div');
    Object.defineProperties(parent, {clientWidth:{value:1000},clientHeight:{value:800}});
    parent.appendChild(fixture.nativeElement);
    const panel=fixture.componentInstance;
    panel.arrange(0,0,400,400);
    const event={button:0,clientX:0,clientY:0,pointerId:1,preventDefault:()=>{},currentTarget:{setPointerCapture:()=>{}}} as unknown as PointerEvent;
    panel.start(event,'move'); panel.move({clientX:2000,clientY:2000} as PointerEvent);
    expect([panel.x(),panel.y()]).toEqual([600,400]);
    panel.end();panel.arrange(0,0,400,400);panel.start(event,'resize');
    panel.move({clientX:-2000,clientY:-2000} as PointerEvent);
    expect([panel.width(),panel.height()]).toEqual([300,260]);
    panel.cancel();
  });
});
