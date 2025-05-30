import { useNavigate } from 'react-router-dom';

export default function Lobby() {
  const navigate = useNavigate();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 100 }}>
      <h2>Lobbies</h2>
      <div style={{ margin: 16 }}>Match list will go here.</div>
      <button onClick={() => alert('Create Match (to be implemented)')}>Create New Match</button>
      <button onClick={() => navigate('/menu')}>Back to Menu</button>
    </div>
  );
} 