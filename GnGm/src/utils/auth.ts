import { api } from '../services/api';

export function isLoggedIn() {
  return !!localStorage.getItem('jwtToken');
}

export async function logout() {
  try {
    // Leave any active match before logging out
    const playerId = localStorage.getItem('playerId');
    if (playerId) {
      const activeMatch = await api.getCurrentPlayerMatch(parseInt(playerId));
      if (activeMatch) {
        await api.leaveMatch(activeMatch.id, parseInt(playerId));
      }
    }
  } catch (error) {
    console.error('Error leaving match during logout:', error);
  } finally {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('playerId');
  }
}