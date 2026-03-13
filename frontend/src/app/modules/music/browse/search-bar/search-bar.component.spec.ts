import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { SearchBarComponent } from './search-bar.component';

describe('SearchBarComponent', () => {
  let component: SearchBarComponent;
  let fixture: ComponentFixture<SearchBarComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SearchBarComponent],
      imports: [FormsModule],
    });

    fixture = TestBed.createComponent(SearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit trimmed search text', () => {
    spyOn(component.searchEvent, 'emit');

    component.keyword = '  hello  ';
    component.onSearch();

    expect(component.searchEvent.emit).toHaveBeenCalledWith('hello');
  });

  it('should emit empty string when keyword is blank', () => {
    spyOn(component.searchEvent, 'emit');

    component.keyword = '   ';
    component.onSearch();

    expect(component.searchEvent.emit).toHaveBeenCalledWith('');
  });
});
