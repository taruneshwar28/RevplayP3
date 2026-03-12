import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse, AuthService } from '../../../core/services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'login',
      'saveSession',
      'getDefaultRouteForCurrentRole',
    ]);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should mark satya22@a as invalid email', () => {
    component.loginForm.patchValue({
      email: 'satya22@a',
      password: 'secret123',
    });

    const emailControl = component.loginForm.get('email');

    expect(emailControl?.invalid).toBeTrue();
    expect(emailControl?.errors?.['pattern']).toBeTrue();
  });

  it('should not call login when form is invalid', () => {
    component.loginForm.patchValue({
      email: 'satya22@a',
      password: '',
    });

    component.onSubmit();

    expect(authServiceSpy.login).not.toHaveBeenCalled();
    expect(component.loginForm.get('email')?.touched).toBeTrue();
    expect(component.loginForm.get('password')?.touched).toBeTrue();
  });

  it('should login and navigate on valid submit', () => {
    const response: AuthResponse = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      userId: 1,
      email: 'satya22@a.com',
      role: 'USER',
    };
    authServiceSpy.login.and.returnValue(of(response));
    authServiceSpy.getDefaultRouteForCurrentRole.and.returnValue('/browse');

    component.loginForm.patchValue({
      email: '  satya22@a.com  ',
      password: 'secret123',
    });

    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: '  satya22@a.com  ',
      password: 'secret123',
    });
    expect(authServiceSpy.saveSession).toHaveBeenCalledWith(response);
    expect(component.successMessage).toBe('Login successful');
    expect(component.loading).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/browse']);
  });

  it('should show backend error when login fails', () => {
    authServiceSpy.login.and.returnValue(
      throwError(() => ({ error: { message: 'Invalid credentials' } }))
    );

    component.loginForm.patchValue({
      email: 'satya22@a.com',
      password: 'wrongpass',
    });

    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid credentials');
    expect(component.loading).toBeFalse();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
