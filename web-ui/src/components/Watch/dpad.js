import React from 'react';

import '@fortawesome/fontawesome-free/svgs/solid/arrow-up.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-down.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-left.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-right.svg';
import '@fortawesome/fontawesome-free/svgs/solid/circle.svg';

import './dpad.css';
import { world_api } from '../../api/client';

function Dpad() {
  const world_id = window.sessionStorage.getItem('world_id');
  const agent_id = window.sessionStorage.getItem('agent_id');

  const move = async direction => {
    const agent_x = window.sessionStorage.getItem('agent_x');
    const agent_y = window.sessionStorage.getItem('agent_y');

    await world_api
      .post('/move', {
        world_id,
        agent_id,
        agent_x,
        agent_y,
        direction
      })
      .then(response => {
        console.log(response);

        window.sessionStorage.setItem('agent_x', response.data.agent_x);
        window.sessionStorage.setItem('agent_y', response.data.agent_y);
      });
  };

  return (
    <div className="col-6 dpad">
      <table className="table-bordered">
        <tbody>
          <tr>
            <td />
            <td
              className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100"
              onClick={() => move('N')}
            >
              <i className="fas fa-arrow-up" />
            </td>
            <td />
          </tr>
          <tr>
            <td
              className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100"
              onClick={() => move('W')}
            >
              <i className="fas fa-arrow-left" />
            </td>
            <td>
              <i className="fas fa-circle" />
            </td>
            <td
              className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100"
              onClick={() => move('E')}
            >
              <i className="fas fa-arrow-right" />
            </td>
          </tr>
          <tr>
            <td />
            <td
              className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100"
              onClick={() => move('S')}
            >
              <i className="fas fa-arrow-down" />
            </td>
            <td />
          </tr>
        </tbody>
      </table>
    </div>
  );
}

export default Dpad;
