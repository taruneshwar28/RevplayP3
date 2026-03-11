import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpErrorResponse,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(
    req: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    if (req.url.includes('/api/auth/')) {
      return next.handle(req);
    }

    if (!token) {
      return next.handle(req);
    }

    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401) {
          return throwError(() => error);
        }

        const refreshToken = this.authService.getRefreshToken();
        if (!refreshToken) {
          this.authService.logout();
          return throwError(() => error);
        }

        return this.authService.refreshToken(refreshToken).pipe(
          switchMap((refreshResponse) => {
            this.authService.saveAccessToken(refreshResponse.accessToken);
            localStorage.setItem('refreshToken', refreshResponse.refreshToken);

            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${refreshResponse.accessToken}`,
              },
            });

            return next.handle(retryReq);
          }),
          catchError((refreshError) => {
            this.authService.logout();
            return throwError(() => refreshError);
          }),
        );
      }),
    );
  }
}
