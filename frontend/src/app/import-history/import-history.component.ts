import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { WebSocketService } from '../web-socket.service';
import { HttpModelService } from '../http-model.service';
import { ImportLogEntry, UserProfile } from '../model';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { Observable, map } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { UserService } from '../user.service';
import { MatIconModule } from '@angular/material/icon';
import { ImportLogService } from '../import-log.service';

@Component({
  selector: 'app-main',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './import-history.component.html',
  styleUrl: './import-history.component.css',
})
export class ImportHistoryComponent {
  dataSource = new MatTableDataSource<ImportLogEntry>();

  adminRqMaterialDataSource$: Observable<MatTableDataSource<ImportLogEntry>>;

  currentUser: UserProfile;

  constructor(
    private router: Router,
    private webSocketService: WebSocketService,
    private httpModelService: HttpModelService,
    private adminRqService: ImportLogService,
    private userService: UserService,
    private http: HttpClient
  ) {
    this.currentUser = userService.authenticatedUserSubject.value;
    this.adminRqMaterialDataSource$ = this.adminRqService.model.pipe(
      map((things) => {
        const dataSource = this.dataSource;
        dataSource.data = things;
        return dataSource;
      })
    );
    this.userService.authenticatedUser.subscribe((user) => {
      this.currentUser = user;
    });
  }

  time = new Date();
  intervalId: any;

  displayedColumns = [
    'id',
    'importedCount',
    'isSuccessful',
    'requestDate',
    'owner',
    'download',
  ];

  ngOnInit() {
    this.intervalId = setInterval(() => {
      this.time = new Date();
    }, 1000);
  }

  openMainPanel() {
    this.router.navigate(['main']);
  }

  updateAll() {
    this.adminRqService.updateAll();
  }

  applyFilter($event: Event) {
    const filterValue = ($event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  logout() {
    localStorage.removeItem('token');
    this.http.delete(environment.backendURL + '/api/v1/logout');
    this.router.navigate(['']);
    this.webSocketService.disconnectWs();
    window.location.reload();
  }

  downloadFile(entry: ImportLogEntry) {
    this.http
      .get(environment.backendURL + '/api/v1/file/' + entry.filename, {
        responseType: 'blob',
      })
      .subscribe((data) => {
        const blob = new Blob([data]);
        const anchor = document.createElement('a');
        anchor.download = entry.filename;
        anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
        anchor.click();
      });
  }
}
