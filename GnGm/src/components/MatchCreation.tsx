import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

const AVAILABLE_MAPS = [
  'Map1',
  'Map2'
];

export default function MatchCreation() {
  const [matchName, setMatchName] = useState('');
  const [maxPlayers, setMaxPlayers] = useState(4);
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const handleCreateMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCreating(true);
    setError('');

    try {
      const match = await api.createMatch(matchName, maxPlayers);
      console.log('Match created:', match);
      
      // Navigate directly to the game since player is automatically joined
      navigate('/game', { state: { matchId: match.id, matchName } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create match');
    } finally {
      setIsCreating(false);
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
    }}>
      <div style={{
        background: '#232323',
        border: '2px solid #a12a2a',
        borderRadius: 10,
        padding: '36px 32px',
        boxShadow: '0 4px 24px #111a',
        minWidth: 340,
        maxWidth: 400,
        width: '100%',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
      }}>
        <h2 style={{ color: '#a12a2a', fontWeight: 900, letterSpacing: 2, marginBottom: 24, fontSize: 26, textTransform: 'uppercase' }}>Create New Match</h2>
        <form onSubmit={handleCreateMatch} style={{ width: '100%' }}>
          {/* Match Name Field */}
          <div style={{ marginBottom: '18px' }}>
            <label htmlFor="matchName" style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold', color: '#a12a2a', letterSpacing: 1 }}>Match Name:</label>
            <input
              id="matchName"
              type="text"
              value={matchName}
              onChange={(e) => setMatchName(e.target.value)}
              required
              placeholder="Enter a unique match name"
            />
          </div>
          {/* Player Count Selection */}
          <div style={{ marginBottom: '18px' }}>
            <label style={{ display: 'block', marginBottom: '8px', color: '#a12a2a', fontWeight: 'bold', letterSpacing: 1 }}>
              Max Players: {maxPlayers}
            </label>
            <input
              type="range"
              min="2"
              max="8"
              value={maxPlayers}
              onChange={(e) => setMaxPlayers(parseInt(e.target.value))}
              style={{ width: '100%', marginBottom: '10px' }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#888', fontSize: '14px' }}>
              <span>2</span><span>3</span><span>4</span><span>5</span><span>6</span><span>7</span><span>8</span>
            </div>
          </div>
          {/* Error Message */}
          {error && (
            <div style={{ color: '#f4f4f4', background: '#a12a2a', borderRadius: 4, padding: '8px 12px', marginBottom: 16, fontWeight: 600 }}>
              {error}
            </div>
          )}
          {/* Buttons */}
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="submit"
              disabled={isCreating}
              style={{ flex: 1 }}
            >
              {isCreating ? 'Creating...' : 'Create Match'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/menu')}
              style={{
                flex: 1,
                background: '#444',
                color: '#f4f4f4',
                border: 'none',
              }}
              onMouseOver={e => (e.currentTarget.style.background = '#a12a2a')}
              onMouseOut={e => (e.currentTarget.style.background = '#444')}
            >
              Cancel
            </button>
          </div>
        </form>
        {/* Preview */}
        <div style={{
          marginTop: '28px',
          padding: '12px',
          backgroundColor: '#2d2d2d',
          borderRadius: '6px',
          border: '1.5px solid #444',
          width: '100%'
        }}>
          <h4 style={{ color: '#a12a2a', marginBottom: '8px', fontWeight: 700, letterSpacing: 1 }}>Match Preview:</h4>
          <p style={{ color: '#f4f4f4', margin: '5px 0' }}>Name: <strong>{matchName || '(not set)'}</strong></p>
          <p style={{ color: '#f4f4f4', margin: '5px 0' }}>Max Players: <strong>{maxPlayers}</strong></p>
        </div>
      </div>
    </div>
  );
}
