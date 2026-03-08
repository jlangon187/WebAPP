import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PoliticaDevoluciones } from './politica-devoluciones';

describe('PoliticaDevoluciones', () => {
  let component: PoliticaDevoluciones;
  let fixture: ComponentFixture<PoliticaDevoluciones>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PoliticaDevoluciones],
    }).compileComponents();

    fixture = TestBed.createComponent(PoliticaDevoluciones);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
