import { useSyncExternalStore } from 'react';

const STORAGE_KEY = 'user';
const STORE_EVENT = 'mediatron-user-updated';
let cachedRawUser = null;
let cachedParsedUser = null;

function readUserFromStorage() {
  const raw = window.localStorage.getItem(STORAGE_KEY);

  if (raw === cachedRawUser) {
    return cachedParsedUser;
  }

  cachedRawUser = raw;

  if (!raw) {
    cachedParsedUser = null;
    return cachedParsedUser;
  }

  try {
    cachedParsedUser = JSON.parse(raw);
  } catch {
    cachedParsedUser = null;
  }

  return cachedParsedUser;
}

export function getStoredUser() {
  if (typeof window === 'undefined') {
    return null;
  }

  return readUserFromStorage();
}

export function setStoredUser(user) {
  if (typeof window === 'undefined') {
    return;
  }

  if (user) {
    const raw = JSON.stringify(user);
    cachedRawUser = raw;
    cachedParsedUser = user;
    window.localStorage.setItem(STORAGE_KEY, raw);
  } else {
    cachedRawUser = null;
    cachedParsedUser = null;
    window.localStorage.removeItem(STORAGE_KEY);
  }

  window.dispatchEvent(new CustomEvent(STORE_EVENT, { detail: user }));
}

function subscribe(callback) {
  if (typeof window === 'undefined') {
    return () => {};
  }

  const handler = () => callback();
  window.addEventListener(STORE_EVENT, handler);
  window.addEventListener('storage', handler);

  return () => {
    window.removeEventListener(STORE_EVENT, handler);
    window.removeEventListener('storage', handler);
  };
}

export function useStoredUser() {
  return useSyncExternalStore(subscribe, getStoredUser, () => null);
}

export function getPrimaryRole(user) {
  return user?.roles?.[0] || null;
}

export function hasAdminPrivileges(user) {
  const role = getPrimaryRole(user);
  return role === 'ROLE_ADMIN' || (role === 'ROLE_LAB_HEAD' && Boolean(user?.delegationActive));
}
