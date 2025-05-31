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
      marginTop: 100,
      gap: '15px'
    }}>
      <h2 style={{ color: '#ff4757', marginBottom: '20px' }}>GnGm - Main Menu</h2>
      
      {/* Current Match Status */}
      {!loading && currentMatch && (
        <div style={{
          backgroundColor: '#ff4757',
          color: 'white',
          padding: '10px 20px',
          borderRadius: '5px',
          marginBottom: '10px',
          textAlign: 'center'
        }}>
          Currently in match: {currentMatch.mapName}
        </div>
      )}

      <button 
        onClick={() => navigate('/create-match')}
        style={{
          padding: '12px 30px',
          backgroundColor: '#ff4757',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '200px',
          transition: 'background-color 0.3s'
        }}
        onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#ff3838'}
        onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#ff4757'}
      >
        Create Match
      </button>
      
      <button 
        onClick={() => navigate('/lobby')}
        style={{
          padding: '12px 30px',
          backgroundColor: '#5352ed',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '200px',
          transition: 'background-color 0.3s'
        }}
        onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#3742fa'}
        onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#5352ed'}
      >
        Join Match
      </button>
      
      {/* Conditional Leave Match Button */}
      {currentMatch && (
        <button 
          onClick={handleLeaveCurrentMatch}
          style={{
            padding: '12px 30px',
            backgroundColor: '#e74c3c',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontSize: '16px',
            fontWeight: 'bold',
            cursor: 'pointer',
            minWidth: '200px',
            transition: 'background-color 0.3s'
          }}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#c0392b'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#e74c3c'}
        >
          Leave Current Match
        </button>
      )}

      <button 
        onClick={() => alert('Settings will be implemented later.')}
        style={{
          padding: '12px 30px',
          backgroundColor: '#a4b0be',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '200px',
          transition: 'background-color 0.3s'
        }}
        onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#747d8c'}
        onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#a4b0be'}
      >
        Settings
      </button>
      
      <button 
        onClick={() => { logout(); navigate('/'); }}
        style={{
          padding: '12px 30px',
          backgroundColor: 'transparent',
          color: '#ff4757',
          border: '2px solid #ff4757',
          borderRadius: '5px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          minWidth: '200px',
          transition: 'all 0.3s'
        }}
        onMouseOver={(e) => {
          e.currentTarget.style.backgroundColor = '#ff4757';
          e.currentTarget.style.color = 'white';
        }}
        onMouseOut={(e) => {
          e.currentTarget.style.backgroundColor = 'transparent';
          e.currentTarget.style.color = '#ff4757';
        }}
      >
        Exit
      </button>
    </div>
  );
}