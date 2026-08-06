import axios from 'axios';

const client = axios.create({ baseURL: process.env.REACT_APP_API_URL || '/api' });
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
client.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
    localStorage.removeItem('token'); localStorage.removeItem('user'); window.location.assign('/login');
  }
  return Promise.reject(error);
});
export const errorMessage = (error) => error.response?.data?.message || 'Something went wrong. Please try again.';
export default client;
