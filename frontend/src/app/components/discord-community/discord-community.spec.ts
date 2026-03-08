import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiscordCommunity } from './discord-community';

describe('DiscordCommunity', () => {
  let component: DiscordCommunity;
  let fixture: ComponentFixture<DiscordCommunity>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiscordCommunity],
    }).compileComponents();

    fixture = TestBed.createComponent(DiscordCommunity);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
