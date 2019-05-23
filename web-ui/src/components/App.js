import React from 'react';
import { Router, Route } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.css';

import history from '../core/history';
import Login from './Login';
import Worlds from "./Worlds";

function App() {
  return (
    <Router history={history}>
      <div className="container justify-content-center text-center">
        <Route path="/:agentID?" component={Login} />
        <Route path="/worlds" component={Worlds} />
      </div>
    </Router>
  );
}

export default App;
