import Constants from 'expo-constants';
import { Platform } from 'react-native';

/**
 * Resolve a host that can reach the backend from the current runtime.
 * - Web/iOS simulator: `localhost` works.
 * - Android emulator: the host machine is `10.0.2.2`.
 * - Physical device / Expo Go: use the dev-server LAN IP so the device can reach the host.
 */
function defaultHost(): string {
  const hostUri = Constants.expoConfig?.hostUri;
  const host = hostUri?.split(':')[0];
  if (host && host !== 'localhost' && host !== '127.0.0.1') {
    return `http://${host}:9090`;
  }
  return Platform.OS === 'android' ? 'http://10.0.2.2:9090' : 'http://localhost:9090';
}

/** Override via `EXPO_PUBLIC_API_BASE_URL` (e.g. a real server on the LAN). */
export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? defaultHost();

/** aqua-admin GraphQL endpoint (port 9090). */
export const GRAPHQL_URL = `${API_BASE_URL}/graphql`;

/** Delivery-photo upload endpoint (multipart `file`). */
export const UPLOAD_URL = `${API_BASE_URL}/api/v1/storage/files`;
