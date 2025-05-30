import { useNavigate } from 'react-router-dom';
import { useEffect, useRef } from 'react';

export default function Game() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    // Placeholder: clear canvas
    const ctx = canvasRef.current?.getContext('2d');
    if (ctx) {
      ctx.fillStyle = '#222';
      ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    }
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 40 }}>
      <h2>Game Area</h2>
      <canvas ref={canvasRef} width={800} height={600} style={{ border: '1px solid #444', background: '#333' }} />
      <button onClick={() => navigate('/menu')} style={{ marginTop: 16 }}>Back to Menu</button>
    </div>
  );
} 