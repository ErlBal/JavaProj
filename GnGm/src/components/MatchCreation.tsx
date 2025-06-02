import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

const AVAILABLE_MAPS = [
  'Map1',
  'Map2'
];

export default function MatchCreation() {
  const [mapName, setMapName] = useState('Map1');
  const [maxPlayers, setMaxPlayers] = useState(4);
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const handleCreateMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCreating(true);
    setError('');

    try {
      const match = await api.createMatch(mapName, maxPlayers);
      console.log('Match created:', match);
      
      // Navigate directly to the game since player is automatically joined
      navigate('/game', { state: { matchId: match.id, mapName } });
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
      padding: '20px',
      maxWidth: '400px',
      margin: '0 auto',
      marginTop: '50px'
    }}>
      <h2 style={{ color: '#ff4757', marginBottom: '30px' }}>Create New Match</h2>
      
      <form onSubmit={handleCreateMatch} style={{ width: '100%' }}>
        {/* Map Selection */}
        <div style={{ marginBottom: '20px' }}>
          <label htmlFor="map" style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold', color: '#ff4757' }}>Select Map:</label>
          <select
            id="map"
            value={mapName}
            onChange={(e) => setMapName(e.target.value)}
            style={{ 
              width: '100%', 
              padding: '10px', 
              borderRadius: '5px', 
              border: '2px solid #ff4757',
              backgroundColor: '#2f3542',
              color: 'white',
              fontSize: '16px' 
            }}
          >
            {AVAILABLE_MAPS.map(map => (
              <option key={map} value={map}>{map}</option>
            ))}
          </select>
        </div>

        {/* Player Count Selection */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ 
            display: 'block', 
            marginBottom: '8px', 
            color: '#ff4757',
            fontWeight: 'bold'
          }}>
            Max Players: {maxPlayers}
          </label>
          <input
            type="range"
            min="2"
            max="8"
            value={maxPlayers}
            onChange={(e) => setMaxPlayers(parseInt(e.target.value))}
            style={{
              width: '100%',
              marginBottom: '10px'
            }}
          />
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between',
            color: '#a4b0be',
            fontSize: '14px'
          }}>
            <span>2</span>
            <span>3</span>
            <span>4</span>
            <span>5</span>
            <span>6</span>
            <span>7</span>
            <span>8</span>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div style={{ 
            color: '#ff3838', 
            marginBottom: '20px',
            padding: '10px',
            backgroundColor: '#2f1b14',
            borderRadius: '5px',
            border: '1px solid #ff3838'
          }}>
            {error}
          </div>
        )}

        {/* Buttons */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            type="submit"
            disabled={isCreating}
            style={{
              flex: 1,
              padding: '12px 20px',
              backgroundColor: isCreating ? '#a4b0be' : '#ff4757',
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              fontSize: '16px',
              fontWeight: 'bold',
              cursor: isCreating ? 'not-allowed' : 'pointer',
              transition: 'background-color 0.3s'
            }}
          >
            {isCreating ? 'Creating...' : 'Create Match'}
          </button>
          
          <button
            type="button"
            onClick={() => navigate('/menu')}
            style={{
              flex: 1,
              padding: '12px 20px',
              backgroundColor: 'transparent',
              color: '#a4b0be',
              border: '2px solid #a4b0be',
              borderRadius: '5px',
              fontSize: '16px',
              fontWeight: 'bold',
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
            onMouseOver={(e) => {
              e.currentTarget.style.backgroundColor = '#a4b0be';
              e.currentTarget.style.color = '#2f3542';
            }}
            onMouseOut={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
              e.currentTarget.style.color = '#a4b0be';
            }}
          >
            Cancel
          </button>
        </div>
      </form>

      {/* Preview */}
      <div style={{
        marginTop: '30px',
        padding: '15px',
        backgroundColor: '#2f3542',
        borderRadius: '5px',
        border: '1px solid #a4b0be',
        width: '100%'
      }}>
        <h4 style={{ color: '#ff4757', marginBottom: '10px' }}>Match Preview:</h4>
        <p style={{ color: 'white', margin: '5px 0' }}>Map: <strong>{mapName}</strong></p>
        <p style={{ color: 'white', margin: '5px 0' }}>Max Players: <strong>{maxPlayers}</strong></p>
      </div>
    </div>
  );
}
