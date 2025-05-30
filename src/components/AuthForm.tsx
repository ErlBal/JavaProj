import { useNavigate } from 'react-router-dom';

export default function AuthForm() {
  const navigate = useNavigate();

  function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    // TODO: Replace with real API call
    localStorage.setItem('jwtToken', 'dummy');
    localStorage.setItem('playerId', '1');
    navigate('/menu');
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 100 }}>
      <h2>Login / Register</h2>
      <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <input type="text" placeholder="Username" required />
        <input type="password" placeholder="Password" required />
        <button type="submit">Login / Register</button>
      </form>
    </div>
  );
} 