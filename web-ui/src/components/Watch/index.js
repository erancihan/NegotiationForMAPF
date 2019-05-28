import React, { useState, useEffect, useRef } from 'react';
import Sockette from 'sockette';

import Dpad from './dpad';
import Raw from './raw';
import Display from './display';

function Watch() {
  const rawEl = useRef(null);
  const displayEl = useRef(null);

  const [connected, setConnected] = useState(false);

  const wid = window.sessionStorage.getItem('world_id');
  const aid = window.sessionStorage.getItem('agent_id');

  let setSocketData = async msg => {
    rawEl.current.pass(msg);
    displayEl.current.pass(msg);
  };

  let connect = () => {
    const ws = new Sockette(`ws://localhost:3001/world/${wid}/${aid}`, {
      timeout: 5e3,
      maxAttempts: 10,
      onopen: ev => setConnected(true),
      onmessage: ev => {
        if (ev.data !== 'ping' || ev.data !== 'PING') {
          const msg = JSON.parse(ev.data);
          setSocketData(msg);
        }
      },
      onreconnect: ev => setConnected(true),
      onmaximum: ev => console.log('Stop Attempting!', ev),
      onclose: ev => setConnected(false),
      onerror: ev => {
        setConnected(false);
        console.log(ev);
      }
    });

    setInterval(() => {
      try {
        ws.send('ping');
      } catch (ev) {
        console.log(ev);
        setConnected(false);
      }
    }, 250);
  };

  useEffect(() => {
    console.log('start watching...');

    connect();
  }, []);

  return (
    <div>
      <div className="row">
        <Raw ref={rawEl} />
        <Dpad />
      </div>
      <div className="row">
        <Display ref={displayEl}/>
      </div>
    </div>
  );
}

export default Watch;
