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

  const x_own = parseInt(window.sessionStorage.getItem('agent_x'));
  const y_own = parseInt(window.sessionStorage.getItem('agent_y'));

  let dx = Math.floor(data.fov_size / 2) - x_own;
  let dy = Math.floor(data.fov_size / 2) - y_own;

  useImperativeHandle(ref, () => ({
    pass: d => setData(d)
  }));

  useEffect(() => {
    dx = Math.floor(data.fov_size / 2) - x_own;
    dy = Math.floor(data.fov_size / 2) - y_own;

    let _grid = new Array(data.fov_size);
    for (let i = 0; i < data.fov_size; i++) {
      _grid[i] = new Array(data.fov_size).fill('');
    }

    let fov = data.fov;
    if (fov && fov.length > 0 && _grid) {
      fov.map(value => {
        const xy = value[1].split(':');
        const x = parseInt(xy[0]);
        const y = parseInt(xy[1]);

        _grid[y + dy][x + dx] = value;
      });
    }

    setGrid(_grid);
  }, [data]);

  // todo colors
  // todo positional alignments

  const fov_center = Math.floor(data.fov_size / 2);

  return (
    <div className="col">
      <table className="table-bordered mx-auto g-display">
        <tbody>
          {grid &&
            grid.map((row, y) => (
              <tr key={`rows-${y}`}>
                {row.map((v, x) => {
                  const aid = v && v[0].split(':')[1];
                  const loc = v && v[1];

                  const ox = x_own - fov_center + x;
                  const oy = y_own - fov_center + y;

                  const is_out = ox < 0 || oy < 0;

                  return (
                    <td
                      key={`cell-${x}:${y}`}
                      className={is_out && 'bg-secondary'}
                    >
                      {v && (
                        <OverlayTrigger
                          placement="bottom"
                          overlay={overlay(`${aid}@${loc}`)}
                        >
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
