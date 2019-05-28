import React, { useEffect } from 'react';
import { Router, Route } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.css';
import '@fortawesome/fontawesome-free/css/all.css';

import history from '../core/history';
import Login from './Login';
import Worlds from './Worlds';
import Watch from './Watch';

function App() {
  useEffect(() => {
    const aid = window.sessionStorage.getItem('agent_id');
    if (aid) {
      document.title = `Agent ${aid}`;
    }
  }, []);

  return (
    <Router history={history}>
      <div className="container justify-content-center text-center">
        <Route exact path="/" component={Login} />
        <Route exact path="/login/:aid" component={Login} />
        <Route exact path="/worlds" component={Worlds} />
        <Route exact path="/watch" component={Watch} />
      </div>
    </Router>
  );
}

export default App;
