import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { api, type Match } from '../services/api';

export default function Lobby() {
  const navigate = useNavigate();
  const location = useLocation();
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [search, setSearch] = useState('');

  // Check if we were redirected here after creating a match
  const createdMatchId = location.state?.matchId;

  useEffect(() => {
    fetchMatches();
    fetchCurrentPlayerMatch();
  }, []);

  const fetchCurrentPlayerMatch = async () => {
    try {
      const playerId = localStorage.getItem('playerId');
      if (playerId) {
        const match = await api.getCurrentPlayerMatch(parseInt(playerId));
        setCurrentMatch(match);
      }
    } catch (err) {
      console.error('Failed to fetch current match:', err);
    }
  };

  const fetchMatches = async (searchTerm?: string) => {
    try {
      setLoading(true);
      const result = await api.listMatches(searchTerm);
      setMatches(result);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch matches');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchMatches(search);
  };

  const handleJoinMatch = async (matchName: string) => {
    try {
      const playerId = localStorage.getItem('playerId');
      if (!playerId) {
        setError('Player ID not found. Please log in again.');
        return;
      }
      await api.joinMatch(matchName, parseInt(playerId));
      navigate('/game', { state: { matchName } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join match');
    }
  };

  const handleExitMatch = async () => {
    // Exit current match if player is in one
    if (currentMatch) {
      try {
        const playerId = localStorage.getItem('playerId');
        if (playerId) {
          await api.leaveMatch(currentMatch.id, parseInt(playerId));
          setCurrentMatch(null);
          await fetchMatches();
        }
      } catch (error) {
        console.error('Error leaving match:', error);
        setError('Failed to exit match. Please try again.');
      }
    }
    navigate('/menu');
  };

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      padding: '20px',
      minHeight: '100vh',
      backgroundColor: '#2f3542'
    }}>
      <h2 style={{ color: '#ff4757', marginBottom: '30px' }}>Game Lobby</h2>
      
      {createdMatchId && (
        <div style={{
          backgroundColor: '#27ae60',
          color: 'white',
          padding: '10px 20px',
          borderRadius: '5px',
          marginBottom: '20px'
        }}>
          Match created successfully! ID: {createdMatchId}
        </div>
      )}      {error && (
        <div style={{
          backgroundColor: '#e74c3c',
          color: 'white',
          padding: '10px 20px',
          borderRadius: '5px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      <div style={{
        display: 'flex',
        gap: '20px',
        marginBottom: '30px'
      }}>
        <button 
          onClick={() => navigate('/create-match')}
          style={{
            padding: '12px 24px',
            backgroundColor: '#ff4757',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontSize: '16px',
            fontWeight: 'bold',
            cursor: 'pointer',
            transition: 'background-color 0.3s'
          }}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#ff3838'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#ff4757'}
        >
          Create New Match
        </button>
        
        <button 
          onClick={() => fetchMatches()}
          style={{
            padding: '12px 24px',
            backgroundColor: '#5352ed',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontSize: '16px',
            fontWeight: 'bold',
            cursor: 'pointer',
            transition: 'background-color 0.3s'
          }}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#3742fa'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#5352ed'}
        >
          Refresh
        </button>        <button 
          onClick={handleExitMatch}
          style={{
            padding: '12px 24px',
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
          Exit Match
        </button>
      </div>

      <div style={{ width: '100%', maxWidth: '800px' }}>
        <h3 style={{ color: '#ff4757', marginBottom: '20px' }}>Active Matches</h3>
        
        {/* Search Field */}
        <form onSubmit={handleSearch} style={{ marginBottom: '20px', display: 'flex', gap: '10px' }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by match name (case-sensitive)"
            style={{ flex: 1, padding: '10px', borderRadius: '5px', border: '1px solid #ff4757', fontSize: '16px' }}
          />
          <button type="submit" style={{ padding: '10px 20px', backgroundColor: '#ff4757', color: 'white', border: 'none', borderRadius: '5px', fontWeight: 'bold', fontSize: '16px', cursor: 'pointer' }}>Search</button>
        </form>
        
        {loading ? (
          <div style={{ color: 'white', textAlign: 'center', padding: '20px' }}>
            Loading matches...
          </div>
        ) : matches.length === 0 ? (
          <div style={{ 
            color: '#a4b0be', 
            textAlign: 'center', 
            padding: '40px',
            backgroundColor: '#40495a',
            borderRadius: '5px'
          }}>
            No active matches found. Create one to get started!
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {matches.map((match) => (
              <div 
                key={match.id}
                style={{
                  backgroundColor: '#40495a',
                  border: createdMatchId === match.id ? '2px solid #ff4757' : '1px solid #57606f',
                  borderRadius: '8px',
                  padding: '20px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
              >
                <div>
                  <h4 style={{ color: '#ff4757', margin: '0 0 5px 0' }}>
                    {match.matchName}
                  </h4>
                  <p style={{ color: '#a4b0be', margin: '0 0 5px 0' }}>
                    Players: {match.players?.length || 0}/{match.maxPlayers}
                  </p>
                  <p style={{ color: '#747d8c', margin: '0', fontSize: '14px' }}>
                    Match ID: {match.id}
                  </p>
                </div>
                
                <button
                  onClick={() => handleJoinMatch(match.matchName)}
                  disabled={match.players?.length >= match.maxPlayers}
                  style={{
                    padding: '10px 20px',
                    backgroundColor: match.players?.length >= match.maxPlayers ? '#57606f' : '#27ae60',
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    fontSize: '14px',
                    fontWeight: 'bold',
                    cursor: match.players?.length >= match.maxPlayers ? 'not-allowed' : 'pointer',
                    transition: 'background-color 0.3s'
                  }}
                  onMouseOver={(e) => {
                    if (match.players?.length < match.maxPlayers) {
                      e.currentTarget.style.backgroundColor = '#2ed573';
                    }
                  }}
                  onMouseOut={(e) => {
                    if (match.players?.length < match.maxPlayers) {
                      e.currentTarget.style.backgroundColor = '#27ae60';
                    }
                  }}
                >
                  {match.players?.length >= match.maxPlayers ? 'Full' : 'Join'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}