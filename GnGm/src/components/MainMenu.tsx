import { useNavigate } from 'react-router-dom';
import { logout } from '../utils/auth';
import { useState, useEffect } from 'react';
import { api, type Match } from '../services/api';

export default function MainMenu() {
  const navigate = useNavigate();
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkCurrentMatch();
  }, []);

  const checkCurrentMatch = async () => {
    try {
      const playerId = localStorage.getItem('playerId');
      if (playerId) {
        const match = await api.getCurrentPlayerMatch(parseInt(playerId));
        setCurrentMatch(match);
      }
    } catch (error) {
      console.error('Error checking current match:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleLeaveCurrentMatch = async () => {
    if (!currentMatch) return;
    
    try {
      const playerId = localStorage.getItem('playerId');
      if (playerId) {
        await api.leaveMatch(currentMatch.id, parseInt(playerId));
        setCurrentMatch(null);
      }
    } catch (error) {
      console.error('Error leaving match:', error);
      alert('Failed to leave match. Please try again.');
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      background: '#232323',
      gap: '18px',
    }}>
      <h2 style={{ color: '#a12a2a', marginBottom: '24px', fontWeight: 900, letterSpacing: 2, textTransform: 'uppercase' }}>Menu</h2>
      {/* Current Match Status */}
      {!loading && currentMatch && (
        <div style={{
          backgroundColor: '#a12a2a',
          color: '#f4f4f4',
          padding: '10px 20px',
          borderRadius: '6px',
          marginBottom: '10px',
          textAlign: 'center',
          fontWeight: 700,
          letterSpacing: 1
        }}>
          Currently in match: {currentMatch.mapName}
        </div>
      )}
      <button
        onClick={() => navigate('/create-match')}
        style={{ minWidth: '220px' }}
      >
        Create Match
      </button>
      <button
        onClick={() => navigate('/lobby')}
        style={{ minWidth: '220px', background: '#444', color: '#f4f4f4' }}
        onMouseOver={e => (e.currentTarget.style.background = '#a12a2a')}
        onMouseOut={e => (e.currentTarget.style.background = '#444')}
      >
        Join Match
      </button>
      {/* Conditional Leave Match Button */}
      {currentMatch && (
        <button
          onClick={handleLeaveCurrentMatch}
          style={{ minWidth: '220px', background: '#7a1c1c', color: '#f4f4f4' }}
          onMouseOver={e => (e.currentTarget.style.background = '#a12a2a')}
          onMouseOut={e => (e.currentTarget.style.background = '#7a1c1c')}
        >
          Leave Current Match
        </button>
      )}
      <button
        onClick={() => { logout(); navigate('/'); }}
        style={{
          minWidth: '220px',
          background: 'transparent',
          color: '#a12a2a',
          border: '2px solid #a12a2a',
          fontWeight: 700,
        }}
        onMouseOver={e => {
          e.currentTarget.style.background = '#a12a2a';
          e.currentTarget.style.color = '#fff';
        }}
        onMouseOut={e => {
          e.currentTarget.style.background = 'transparent';
          e.currentTarget.style.color = '#a12a2a';
        }}
      >
        Exit
      </button>
    </div>
  );
}