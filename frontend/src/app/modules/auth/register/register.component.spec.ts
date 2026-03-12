import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse, AuthService } from '../../../core/services/auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['register']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      declarations: [RegisterComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should mark satya22@a as invalid email', () => {
    component.registerForm.patchValue({
      firstName: 'Satya',
      lastName: 'Dev',
      email: 'satya22@a',
      password: 'secret123',
      role: 'USER',
    });

    const emailControl = component.registerForm.get('email');

    expect(emailControl?.invalid).toBeTrue();
    expect(emailControl?.errors?.['pattern']).toBeTrue();
  });

  it('should not call register when form is invalid', () => {
    component.registerForm.patchValue({
      firstName: 'S',
      lastName: 'D',
      email: 'satya22@a',
      password: '123',
      role: 'USER',
    });

    component.onSubmit();

    expect(authServiceSpy.register).not.toHaveBeenCalled();
    expect(component.registerForm.get('email')?.touched).toBeTrue();
  });

  it('should trim values, register, and redirect to login', fakeAsync(() => {
    const response: AuthResponse = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      userId: 1,
      email: 'satya22@a.com',
      role: 'USER',
    };
    authServiceSpy.register.and.returnValue(of(response));

    component.registerForm.patchValue({
      firstName: '  Satya  ',
      lastName: '  Dev  ',
      email: '  satya22@a.com  ',
      password: 'secret123',
      role: 'USER',
    });

    component.onSubmit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      firstName: 'Satya',
      lastName: 'Dev',
      email: 'satya22@a.com',
      password: 'secret123',
      role: 'USER',
    });
    expect(component.successMessage).toBe('Registration successful. Redirecting to login...');
    expect(component.loading).toBeFalse();

    tick(2000);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/home/login']);
  }));

  it('should surface backend validation errors', () => {
    authServiceSpy.register.and.returnValue(
      throwError(() => ({
        error: {
          validationErrors: {
            email: 'Email is invalid.',
            password: 'Password is weak.',
          },
        },
      }))
    );

    component.registerForm.patchValue({
      firstName: 'Satya',
      lastName: 'Dev',
      email: 'satya22@a.com',
      password: 'secret123',
      role: 'USER',
    });

    component.onSubmit();

    expect(component.errorMessage).toBe('Email is invalid. Password is weak.');
    expect(component.loading).toBeFalse();
  });
});
