import axios from 'axios';

// todo constants
const world_port = 'http://localhost:3001';

const world_api = axios.create({
  baseURL: world_port
});

export default world_api;