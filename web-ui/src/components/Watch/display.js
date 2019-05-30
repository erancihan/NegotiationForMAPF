import React, {
  useEffect,
  useState,
  forwardRef,
  useImperativeHandle
} from 'react';
import '@fortawesome/fontawesome-free/svgs/solid/circle.svg';
import { Tooltip, OverlayTrigger } from 'react-bootstrap';

import './display.css';

const overlay = text => <Tooltip id={`tooltip-${text}`}>{text}</Tooltip>;

let Display = function(props, ref) {
  const [data, setData] = useState({
    agent_id: '',
    position: '',
    fov_size: 0,
    fov: []
  });
  const [grid, setGrid] = useState([]);

  useImperativeHandle(ref, () => ({
    pass: d => setData(d)
  }));

  useEffect(() => {
    let _grid = new Array(data.fov_size);
    for (let i = 0; i < data.fov_size; i++) {
      _grid[i] = new Array(data.fov_size).fill('');
    }

    let fov = data.fov;
    if (fov && fov.length > 0) {
      fov.map(value => {
        const xy = value[1].split(':');
        const x = xy[0];
        const y = xy[1];

        _grid[x][y] = value[0];
      });
    }

    setGrid(_grid);
  }, [data]);

  // todo colors
  // todo positional alignments

  return (
    <div className="col">
      <table className="table-bordered mx-auto g-display">
        <tbody>
          {grid &&
            grid.map((col, i) => (
              <tr key={i}>
                {col.map((v, j) => {
                  v = v.split(':')[1];
                  return (
                    <td key={`${i}:${j}`}>
                      {v && (
                        <OverlayTrigger placement="bottom" overlay={overlay(v)}>
                          <i className="fas fa-circle" />
                        </OverlayTrigger>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
};

Display = forwardRef(Display);

export default Display;
