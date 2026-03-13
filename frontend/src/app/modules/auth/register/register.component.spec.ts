import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
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
      schemas: [NO_ERRORS_SCHEMA],
    });

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should mark invalid email as invalid', () => {
    component.registerForm.patchValue({
      firstName: 'Sample',
      lastName: 'Dev',
      email: 'invalid-email',
      password: 'secret123',
      role: 'USER',
    });

    const emailControl = component.registerForm.get('email');

    expect(emailControl?.invalid).toBeTrue();
    expect(emailControl?.errors?.['email']).toBeTrue();
  });

  it('should not call register when form is invalid', () => {
    component.registerForm.patchValue({
      firstName: 'S',
      lastName: 'D',
      email: 'invalid-email',
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
      email: 'sample.user@example.com',
      role: 'USER',
    };
    authServiceSpy.register.and.returnValue(of(response));

    component.registerForm.patchValue({
      firstName: 'Sample',
      lastName: 'Dev',
      email: 'sample.user@example.com',
      password: 'secret123',
      role: 'USER',
    });

    component.onSubmit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      firstName: 'Sample',
      lastName: 'Dev',
      email: 'sample.user@example.com',
      password: 'secret123',
      role: 'USER',
    });
    expect(component.successMessage).toBe('Registration successful. Please login to continue.');
    expect(component.loading).toBeFalse();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/home/login']);
  }));

  it('should surface backend validation errors', () => {
    authServiceSpy.register.and.returnValue(
      throwError(() => ({
        error: {
          message: 'Email is invalid. Password is weak.',
        },
      }))
    );

    component.registerForm.patchValue({
      firstName: 'Sample',
      lastName: 'Dev',
      email: 'sample.user@example.com',
      password: 'secret123',
      role: 'USER',
    });

    component.onSubmit();

    expect(component.errorMessage).toBe('Email is invalid. Password is weak.');
    expect(component.loading).toBeFalse();
  });
});
