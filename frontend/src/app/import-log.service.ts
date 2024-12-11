import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { AdminRequest, ImportLogEntry } from './model';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ImportLogService {
  constructor(private http: HttpClient) {}

  private _model = new BehaviorSubject<ImportLogEntry[]>([]);

  get model() {
    return this._model.asObservable();
  }

  updateAll() {
    this.http
      .get<ImportLogEntry[]>(environment.backendURL + '/api/v1/import/log')
      .subscribe((data) => {
        this._model.next(data);
        console.log(data);
      });
  }
}
