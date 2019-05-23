import React, { useRef, useEffect } from 'react';

import 'bootstrap/dist/css/bootstrap.css';

function Login({ match }) {
  const inputID = useRef(null);

  const login = event => {
    event.preventDefault();

    let agentID = inputID.current.value;
    if (agentID.length <= 0) {
      return;
    }
    console.log('>', agentID);

    window.sessionStorage.setItem('agent_id', agentID);
    window.location.href = '/worlds';
  };

  useEffect(() => {
    console.log('login mounted');

    if (match.params.aid) {
      window.sessionStorage.setItem('agent_id', match.params.aid);
      window.location.href = '/worlds';
    }
  }, [match]);

  return (
    <div className="card col-4 border-dark mt-3 mx-auto">
      <div className="card-body">
        <div className="input-group mb-3">
          <div className="input-group-prepend">
            <span className="input-group-text">{'ID'}</span>
          </div>
          <input
            type="text"
            name="agent_id"
            className="form-control"
            ref={inputID}
            autoComplete="on"
          />
        </div>
        <button className="btn btn-primary" onClick={login}>
          {'Enter'}
        </button>
      </div>
    </div>
  );
}

export default Login;
