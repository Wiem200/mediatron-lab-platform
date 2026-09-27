import api from './axios';

const normalizeParams = (params = {}) => {
  const normalized = {};

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      normalized[key] = value;
    }
  });

  return normalized;
};

const resolveApiBaseUrl = () => {
  const configuredBaseUrl = (api.defaults.baseURL || 'http://localhost:8080/api').replace(/\/$/, '');

  if (configuredBaseUrl.startsWith('http://') || configuredBaseUrl.startsWith('https://')) {
    return configuredBaseUrl;
  }

  const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:5173';
  return `${origin}${configuredBaseUrl.startsWith('/') ? configuredBaseUrl : `/${configuredBaseUrl}`}`;
};

const buildExportUrl = (format, params = {}, memberId = null) => {
  const baseUrl = resolveApiBaseUrl();
  const endpoint = memberId
    ? `${baseUrl}/publications/export/member/${memberId}/${format}`
    : `${baseUrl}/publications/export/${format}`;

  const url = new URL(endpoint);

  Object.entries(normalizeParams(params)).forEach(([key, value]) => {
    url.searchParams.append(key, value);
  });

  return url.toString();
};

export const publicationService = {
  // Public publications page
  getPublications: (params = {}) =>
    api.get('/publications', { params: normalizeParams(params) }),

  getPublicationsByMember: (memberId, params = {}) =>
    api.get(`/publications/member/${memberId}`, { params: normalizeParams(params) }),

  getPublicationTypes: () => api.get('/publications/types'),

  getPublicationYears: () => api.get('/publications/years'),

  getPublicationById: (id) => api.get(`/publications/${id}`),

  buildPublicCsvExportUrl: (params = {}) => buildExportUrl('csv', params),

  buildPublicPdfExportUrl: (params = {}) => buildExportUrl('pdf', params),

  buildMemberCsvExportUrl: (memberId, params = {}) => buildExportUrl('csv', params, memberId),

  buildMemberPdfExportUrl: (memberId, params = {}) => buildExportUrl('pdf', params, memberId),

  // Logged-in member publication validation page
  getMyPublications: (params = {}) =>
    api.get('/members/me/publications', { params: normalizeParams(params) }),

  getMyPendingPublications: () => api.get('/publications/my-pending'),

  validateMyPublication: (publicationId, payload) =>
    api.patch(`/publications/${publicationId}/validate`, payload),

  approveMyPublication: (publicationId) =>
    api.post(`/publications/${publicationId}/approve`),

  rejectMyPublication: (publicationId) =>
    api.post(`/publications/${publicationId}/reject`),

  getMemberDashboardPublications: (memberId, params = {}) =>
    api.get(`/member/publications/member/${memberId}`, { params: normalizeParams(params) }),

  // Lab head publication validation page
  getValidationQueue: (params = {}) =>
    api.get('/lab-head/publications/validation-queue', { params: normalizeParams(params) }),

  updateValidationStatus: (publicationId, payload) =>
    api.patch(`/lab-head/publications/${publicationId}/status`, payload),

  getValidationHistory: (publicationId) =>
    api.get(`/lab-head/publications/${publicationId}/history`),
};