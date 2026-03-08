import { TestBed } from '@angular/core/testing';

import { Mod } from './mod';

describe('Mod', () => {
  let service: Mod;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Mod);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
