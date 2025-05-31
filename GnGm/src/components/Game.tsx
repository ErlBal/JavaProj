import { useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { api } from '../services/api';

export default function Game() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    // Placeholder: clear canvas
    const ctx = canvasRef.current?.getContext('2d');
    if (ctx) {
      ctx.fillStyle = '#222';
      ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    }
  }, []);

  const handleExitMatch = async () => {
    setIsExiting(true);
    try {
      const playerId = localStorage.getItem('playerId');
      if (playerId) {
        // Get current player's active match
        const currentMatch = await api.getCurrentPlayerMatch(parseInt(playerId));
        if (currentMatch) {
          // Leave the match
          await api.leaveMatch(currentMatch.id, parseInt(playerId));
        }
      }
    } catch (error) {
      console.error('Error leaving match:', error);
    } finally {
      setIsExiting(false);
      navigate('/menu');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 40 }}>
      <h2>Game Area</h2>
      <canvas ref={canvasRef} width={800} height={600} style={{ border: '1px solid #444', background: '#333' }} />
      <button 
        onClick={handleExitMatch} 
        disabled={isExiting}
        style={{ 
          marginTop: 16,
          padding: '10px 20px',
          backgroundColor: isExiting ? '#666' : '#ff4757',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          cursor: isExiting ? 'not-allowed' : 'pointer'
        }}
      >
        {isExiting ? 'Exiting...' : 'Back to Menu'}
      </button>
    </div>
  );
}