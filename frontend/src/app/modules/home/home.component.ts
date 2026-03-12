import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  constructor(public readonly router: Router) {}

  get showWelcomeBanner(): boolean {
    return this.router.url === '/home/login' || this.router.url === '/home';
  }
}
