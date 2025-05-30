export function isLoggedIn() {
  return !!localStorage.getItem('jwtToken');
}

export function logout() {
  localStorage.removeItem('jwtToken');
  localStorage.removeItem('playerId');
} 