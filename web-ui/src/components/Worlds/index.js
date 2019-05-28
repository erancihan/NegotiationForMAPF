import React, { useState, useEffect } from 'react';
import { servlet, world_api } from '../../api/client';
import '@fortawesome/fontawesome-free/svgs/solid/plus.svg';

function Worlds() {
  const [worldList, setWorldList] = useState([]);

  const aid = window.sessionStorage.getItem('agent_id');

  useEffect(() => {
    const getWorldList = async () => {
      world_api.get('/worlds').then(response => {
        setWorldList(response.data.worlds);
      });
    };

    getWorldList();
  }, []);

  const join = async wid => {
    window.sessionStorage.setItem('world_id', wid);

    // get agent info from java-end
    /*
    await servlet.post('/agent', { aid, wid }).then(response => {
      window.sessionStorage.setItem('agent_x', response.data.x);
      window.sessionStorage.setItem('agent_y', response.data.y);
    });
*/

    const x = window.sessionStorage.getItem('agent_x');
    const y = window.sessionStorage.getItem('agent_y');

    await world_api
      .post('/join/' + wid, {
        agent_id: aid,
        agent_x: x,
        agent_y: y
      })
      .then(response => {
        window.location.href = '/watch';
      });
  };

  const joinNew = async () => {
    const wid = worldList.length + 1;

    await join(wid);
  };

  return (
    <div>
      <div className="row mt-2 justify-content-center">
        <div>
          {'id:'} {aid}
        </div>
      </div>
      <div className="row mt-2 justify-content-center">
        {worldList.length > 0 ? (
          worldList.map((value, i) => {
            const wid = value.split(':')[1];

            return (
              <div className="col-4 my-1" key={i}>
                <div className="card border-dark">
                  <div className="card-header">
                    <h5 className="mb-0">
                      {'World'} {wid}
                    </h5>
                  </div>
                  <div className="card-body">
                    <div className="w-100 text-center">
                      <div
                        className="btn btn-outline-primary"
                        onClick={() => join(wid)}
                      >
                        {'Join'}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div>{'No Worlds present'}</div>
        )}
      </div>
      <div className="row mt-2 justify-content-center">
        <div className="btn btn-outline-info btn-lg" onClick={joinNew}>
          <i className="fas fa-plus" /> {'New World'}
        </div>
      </div>
    </div>
  );
}

export default Worlds;
