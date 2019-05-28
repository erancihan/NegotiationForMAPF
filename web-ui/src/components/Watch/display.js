import React, { useState, forwardRef, useImperativeHandle } from 'react';

let Display = function(props, ref) {
  const [data, setData] = useState({
    fov_size: 0,
    fov: []
  });

  useImperativeHandle(ref, () => ({
    pass: d => {
      setData(d);
    }
  }));

  return <div className="col">{}</div>;
};

Display = forwardRef(Display);

export default Display;
