import React, { useState, useEffect } from 'react';
import Sockette from 'sockette';

function Watch() {
  const [connected, setConnected] = useState(false);
  const [data, setData] = useState({
    agent_id: '',
    world_id: '',
    position: '',
    can_move: '',
    time: '',
    fov: [],
    pc: 0
  });

  const wid = window.sessionStorage.getItem('world_id');
  const aid = window.sessionStorage.getItem('agent_id');

  let setSocketData = async msg => {
    console.log(msg);
    setData(msg);
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
      <div>{`World ID: ${data.world_id}`}</div>
      <div>{`Agent ID: ${data.agent_id}`}</div>
      <div>{`@ ${data.position}`}</div>
      <div>{`Player C: ${data.pc}`}</div>
      <div>{data.time}</div>
      <div>
        {data.fov.map((v, i) => (
          <div key={i}>
            {v[0]} {v[1]}
          </div>
        ))}
      </div>
      <div>{data.can_move}</div>
    </div>
  );
}

export default Watch;
