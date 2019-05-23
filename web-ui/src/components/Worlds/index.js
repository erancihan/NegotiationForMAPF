import React, { useState, useEffect } from 'react';
import world_api from '../../api/client';

function Worlds() {
  const [worldList, setWorldList] = useState([]);

  useEffect(() => {
    const getWorldList = async () => {
      world_api.get('/worlds').then(response => {
        setWorldList(response.data.worlds);
      });
    };

    getWorldList();
  }, []);

  const join = wid => {
    const aid = window.sessionStorage.getItem('agent_id');

    console.log('> join', wid, aid);
  };

  return (
    <div>
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
        <div>{'Create new World'}</div>
      </div>
    </div>
  );
}

export default Worlds;
