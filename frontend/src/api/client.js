import axios from 'axios';

const configuredApiUrl = process.env.REACT_APP_API_URL?.trim().replace(/\/+$/, '');
const apiBaseUrl = configuredApiUrl || '/api';
const isProductionBuild = process.env.NODE_ENV === 'production';
const usesNgrok = configuredApiUrl?.includes('.ngrok-free.dev');

const client = axios.create({ baseURL: apiBaseUrl });
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (usesNgrok) config.headers['ngrok-skip-browser-warning'] = 'true';
  // Let the browser set multipart boundary; a manual Content-Type breaks uploads.
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    if (config.headers && typeof config.headers.delete === 'function') {
      config.headers.delete('Content-Type');
    } else if (config.headers) {
      delete config.headers['Content-Type'];
      delete config.headers['content-type'];
    }
  }
  return config;
});
client.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
    localStorage.removeItem('token'); localStorage.removeItem('user'); window.location.assign('/login');
  }
  return Promise.reject(error);
});
export const errorMessage = (error) => {
  const status = error.response?.status;
  const apiMessage = error.response?.data?.message;
  if (apiMessage) return apiMessage;
  if (!configuredApiUrl && isProductionBuild && (!error.response || status === 404 || status >= 500)) {
    return 'The registration service is not connected yet. Configure REACT_APP_API_URL in Vercel with your deployed backend URL, then redeploy.';
  }
  if (!error.response) return 'We could not reach the WordWise service. Check that the backend is running and try again.';
  return `The WordWise service returned an unexpected response (${status}). Please try again.`;
};
export default client;
