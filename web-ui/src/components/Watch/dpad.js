import React from 'react';

import '@fortawesome/fontawesome-free/svgs/solid/arrow-up.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-down.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-left.svg';
import '@fortawesome/fontawesome-free/svgs/solid/arrow-right.svg';
import '@fortawesome/fontawesome-free/svgs/solid/circle.svg';

import './dpad.css';

function Dpad() {
  return (
    <div className="col-6 dpad">
      <table className="table-bordered">
        <tbody>
        <tr>
          <td/>
          <td className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100">
            <i className="fas fa-arrow-up"/>
          </td>
          <td/>
        </tr>
        <tr>
          <td className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100">
            <i className="fas fa-arrow-left"/>
          </td>
          <td>
            <i className="fas fa-circle"/>
          </td>
          <td className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100">
            <i className="fas fa-arrow-right"/>
          </td>
        </tr>
        <tr>
          <td/>
          <td className="btn btn-outline-dark border-0 rounded-0 m-0 p-0 w-100">
            <i className="fas fa-arrow-down"/>
          </td>
          <td/>
        </tr>
        </tbody>
      </table>
    </div>
  );
}

export default Dpad;
