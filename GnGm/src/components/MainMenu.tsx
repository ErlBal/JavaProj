import { useNavigate } from 'react-router-dom';
import { logout } from '../utils/auth';

export default function MainMenu() {
  const navigate = useNavigate();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 100 }}>
      <h2>Main Menu</h2>
      <button onClick={() => navigate('/lobby')}>Lobby</button>
      <button onClick={() => alert('Settings will be implemented later.')}>Settings</button>
      <button onClick={() => { logout(); navigate('/'); }}>Exit</button>
    </div>
  );
} 