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
      minHeight: '100vh',
      background: '#232323',
      padding: '0',
    }}>
      <h2 style={{ color: '#a12a2a', marginBottom: '28px', fontWeight: 900, letterSpacing: 2, textTransform: 'uppercase' }}>Game Lobby</h2>
      {createdMatchId && (
        <div style={{
          backgroundColor: '#444',
          color: '#f4f4f4',
          padding: '10px 20px',
          borderRadius: '6px',
          marginBottom: '18px',
          fontWeight: 700,
          letterSpacing: 1
        }}>
          Match created successfully! ID: {createdMatchId}
        </div>
      )}
      {error && (
        <div style={{
          backgroundColor: '#a12a2a',
          color: '#f4f4f4',
          padding: '10px 20px',
          borderRadius: '6px',
          marginBottom: '18px',
          fontWeight: 700,
        }}>
          {error}
        </div>
      )}
      <div style={{
        display: 'flex',
        gap: '18px',
        marginBottom: '28px',
      }}>
        <button onClick={() => navigate('/create-match')} style={{ minWidth: '180px' }}>Create New Match</button>
        <button onClick={() => fetchMatches()} style={{ minWidth: '140px', background: '#444', color: '#f4f4f4' }} onMouseOver={e => (e.currentTarget.style.background = '#a12a2a')} onMouseOut={e => (e.currentTarget.style.background = '#444')}>Refresh</button>
        <button onClick={handleExitMatch} style={{ minWidth: '140px', background: 'transparent', color: '#a12a2a', border: '2px solid #a12a2a', fontWeight: 700 }} onMouseOver={e => { e.currentTarget.style.background = '#a12a2a'; e.currentTarget.style.color = '#fff'; }} onMouseOut={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#a12a2a'; }}>Exit Match</button>
      </div>
      <div style={{ width: '100%', maxWidth: '800px' }}>
        <h3 style={{ color: '#a12a2a', marginBottom: '18px', fontWeight: 800, letterSpacing: 1 }}>Active Matches</h3>
        {/* Search Field */}
        <form onSubmit={handleSearch} style={{ marginBottom: '18px', display: 'flex', gap: '10px' }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by match name (case-sensitive)"
          />
          <button type="submit" style={{ minWidth: '110px' }}>Search</button>
        </form>
        {loading ? (
          <div style={{ color: '#f4f4f4', textAlign: 'center', padding: '20px' }}>
            Loading matches...
          </div>
        ) : matches.length === 0 ? (
          <div style={{
            color: '#888',
            textAlign: 'center',
            padding: '36px',
            backgroundColor: '#2d2d2d',
            borderRadius: '6px',
            border: '1.5px solid #444',
          }}>
            No active matches found. Create one to get started!
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {matches.map((match) => (
              <div
                key={match.id}
                style={{
                  backgroundColor: '#2d2d2d',
                  border: createdMatchId === match.id ? '2px solid #a12a2a' : '1.5px solid #444',
                  borderRadius: '8px',
                  padding: '18px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  boxShadow: '0 2px 8px #1116',
                }}
              >
                <div>
                  <h4 style={{ color: '#a12a2a', margin: '0 0 5px 0', fontWeight: 700, letterSpacing: 1 }}>
                    {match.matchName}
                  </h4>
                  <p style={{ color: '#f4f4f4', margin: '0 0 5px 0', fontWeight: 500 }}>
                    Players: {match.players?.length || 0}/{match.maxPlayers}
                  </p>
                  <p style={{ color: '#888', margin: '0', fontSize: '14px' }}>
                    Match ID: {match.id}
                  </p>
                </div>
                <button
                  onClick={() => handleJoinMatch(match.matchName)}
                  disabled={match.players?.length >= match.maxPlayers}
                  style={{
                    minWidth: '100px',
                    background: match.players?.length >= match.maxPlayers ? '#444' : '#a12a2a',
                    color: '#f4f4f4',
                    fontWeight: 700,
                    cursor: match.players?.length >= match.maxPlayers ? 'not-allowed' : 'pointer',
                  }}
                  onMouseOver={e => {
                    if (match.players?.length < match.maxPlayers) e.currentTarget.style.background = '#7a1c1c';
                  }}
                  onMouseOut={e => {
                    if (match.players?.length < match.maxPlayers) e.currentTarget.style.background = '#a12a2a';
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