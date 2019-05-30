import React, { forwardRef, useState, useImperativeHandle } from 'react';

const readableTime = t => {
  const date = new Date(0);
  date.setUTCMilliseconds(t / 1e6);

  return date.toString().substr(0, 33);
};

let Raw = function(props, ref) {
  const [data, setData] = useState({
    agent_id: '',
    world_id: '',
    position: '',
    can_move: '',
    fov_size: 0,
    time: '',
    exec_time: '',
    fov: [],
    pc: 0
  });

  useImperativeHandle(ref, () => ({
    pass: d => setData(d)
  }));

  return (
    <div className="col-6">
      <pre className="text-left">
        <div>{`${readableTime(data.time)}`}</div>
        <div>{`World ID: ${data.world_id}`}</div>
        <div>{`Agent ID: ${data.agent_id}`}</div>
        <div>{`@ ${data.position}`}</div>
        <div>{`Player C: ${data.pc}`}</div>
        <div>{`can move: ${data.can_move}`}</div>
        <div>{`FoV size: ${data.fov_size}`}</div>
        <div>{`timestamp: ${data.time}`}</div>
        <div>{`exec time: ${parseFloat(data.exec_time).toFixed(3)} ms`}</div>
      </pre>
      <div className="text-left">
        {data.fov ? (
          data.fov.map((v, i) => (
            <div key={i} className="d-inline-block ml-3">
              {`${v[0].split(':')[1]}@${v[1]}`}
            </div>
          ))
        ) : (
          <div>{'No Data'}</div>
        )}
      </div>
    </div>
  );
};

Raw = forwardRef(Raw);

export default Raw;
