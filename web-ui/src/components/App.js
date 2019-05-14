import React from 'react';
import { Router, Route } from 'react-router-dom';

import history from '../core/history';
import Login from './Login';

function App() {
  return (
    <Router history={history}>
      <div className="container justify-content-center text-center">
        <Route exact path="/" component={Login} />
      </div>
    </Router>
  );
}

export default App;
