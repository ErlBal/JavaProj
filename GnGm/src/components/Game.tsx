import React, { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';

// Simple types
interface Player {
  id: number;
  username: string;
  x: number;
  y: number;
  rotation: number;
  health: number;
  alive: boolean;
}

interface Projectile {
  id: string;
  x: number;
  y: number;
  direction: number;
  playerId: number;
}

interface GameState {
  players: { [key: number]: Player };
  projectiles: { [key: string]: Projectile };
  gameOver: boolean;
  winnerName: string | null;
}

interface Wall {
  x: number;
  y: number;
  width: number;
  height: number;
}

const Game: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const clientRef = useRef<Client | null>(null);
  const location = useLocation();
  const mapNameFromState = location.state?.mapName as string | undefined;
  const selectedMap = mapNameFromState || 'Map1';
  const matchId = location.state?.matchId || localStorage.getItem('matchId');
  
  // Game state
  const [gameState, setGameState] = useState<GameState>({ players: {}, projectiles: {}, gameOver: false, winnerName: null });
  const [currentPlayer, setCurrentPlayer] = useState<Player | null>(null);
  const [connected, setConnected] = useState(false);
    // Input handling
  const keysPressed = useRef<{ [key: string]: boolean }>({});
  const mousePos = useRef({ x: 0, y: 0 });
  const currentPlayerRef = useRef<Player | null>(null);
  const connectedRef = useRef<boolean>(false);
  const [walls, setWalls] = useState<Wall[]>([]);
  const [gameOver, setGameOver] = useState(false);
  const [winnerName, setWinnerName] = useState<string | null>(null);
  const navigate = useNavigate();
  
  // Update refs when state changes
  useEffect(() => {
    currentPlayerRef.current = currentPlayer;
  }, [currentPlayer]);
  
  useEffect(() => {
    connectedRef.current = connected;
  }, [connected]);
  
  useEffect(() => {
    if (matchId) {
      localStorage.setItem('matchId', matchId);
    }
  }, [matchId]);
  
  // Game constants
  const WORLD_W = 1600;
  const WORLD_H = 1200;
  const PLAYER_SIZE = 40;
    // WebSocket connection
  useEffect(() => {    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:9090/ws'),
      onConnect: () => {
        console.log('✅ Connected to game server');
        setConnected(true);
          // Listen for game updates
        client.subscribe(`/topic/game/state/${matchId}`, (message) => {
          const state: GameState = JSON.parse(message.body);
          setGameState(state);
          
          // Update current player
          const playerId = parseInt(localStorage.getItem('playerId') || '1');
          if (state.players[playerId]) {
            setCurrentPlayer(state.players[playerId]);
          } else {
            console.log('❌ Current player not found in game state');
          }
          
          if ('gameOver' in state) setGameOver(state.gameOver);
          if ('winnerName' in state) setWinnerName(state.winnerName);
          if (state.gameOver) {
            setTimeout(() => {
              navigate('/menu');
            }, 4000);
          }
        });
        
        // Join the game
        const playerId = Date.now() % 10000; // Simple ID
        const username = `Player${playerId}`;
        localStorage.setItem('playerId', playerId.toString());
        
        client.publish({
          destination: '/app/game/join',
          body: JSON.stringify({ matchId, playerId, username })
        });
        
        console.log(`🎮 Joined as ${username} (ID: ${playerId})`);
      },
      onDisconnect: () => {
        console.log('❌ Disconnected from server');
        setConnected(false);
      },
      onStompError: (error) => {
        console.error('WebSocket error:', error);
      }
    });
    
    client.activate();
    clientRef.current = client;
      return () => {
      client.deactivate();
    };
  }, [matchId, navigate]);
  
  // Cleanup on page refresh or component unmount
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (currentPlayer && clientRef.current && connected) {
        // Send a leave message to remove the player from the server
        clientRef.current.publish({
          destination: '/app/game/leave',
          body: JSON.stringify({ playerId: currentPlayer.id })
        });
      }
    };
    
    window.addEventListener('beforeunload', handleBeforeUnload);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      // Also cleanup when component unmounts
      handleBeforeUnload();
    };
  }, [currentPlayer, connected]);  // Keyboard input
  useEffect(() => {
    const keyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      if (['w', 'a', 's', 'd'].includes(key)) {
        keysPressed.current[key] = true;
        console.log('Key down:', key, keysPressed.current);
      }
    };
    
    const keyUp = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      if (['w', 'a', 's', 'd'].includes(key)) {
        keysPressed.current[key] = false;
        console.log('Key up:', key, keysPressed.current);
      }
    };
    
    window.addEventListener('keydown', keyDown);
    window.addEventListener('keyup', keyUp);
    
    return () => {
      window.removeEventListener('keydown', keyDown);
      window.removeEventListener('keyup', keyUp);
    };
  }, []);
  
  // Mouse tracking
  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    mousePos.current = {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
  };
  
  // Shooting
  const handleClick = () => {
    if (!currentPlayer || !clientRef.current || !connected) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    // Convert mouse position from canvas to world coordinates
    const mouseWorldX = (mousePos.current.x / canvas.width) * WORLD_W;
    const mouseWorldY = (mousePos.current.y / canvas.height) * WORLD_H;
    // Use game world coordinates for the player as well
    const dx = mouseWorldX - currentPlayer.x;
    const dy = mouseWorldY - currentPlayer.y;
    const direction = Math.atan2(dy, dx);
    clientRef.current.publish({
      destination: '/app/game/shoot',
      body: JSON.stringify({
        matchId,
        playerId: currentPlayer.id,
        direction
      })
    });
    console.log('�� Shooting!');
  };
    // Movement loop
  useEffect(() => {
    const moveLoop = setInterval(() => {
      console.log('[MoveLoop] Tick');
      const player = currentPlayerRef.current;
      const isConnected = connectedRef.current;
      const client = clientRef.current;
      const canvas = canvasRef.current;
      console.log('[MoveLoop] Player:', player);
      console.log('[MoveLoop] Canvas:', canvas);
      if (!player || !client || !isConnected || !canvas) return;
      const keys = keysPressed.current;
      let vx = 0;
      let vy = 0;
      if (keys['w']) vy -= 5;
      if (keys['s']) vy += 5;
      if (keys['a']) vx -= 5;
      if (keys['d']) vx += 5;
      // Always send movement, even if vx/vy are zero
      const mouseWorldX = (mousePos.current.x / canvas.width) * WORLD_W;
      const mouseWorldY = (mousePos.current.y / canvas.height) * WORLD_H;
      const dx = mouseWorldX - player.x;
      const dy = mouseWorldY - player.y;
      const rotation = Math.atan2(dy, dx);
      const newRotation = rotation;
      console.log('[MoveLoop] Sending movement:', { matchId, playerId: player.id, vx, vy, rotation: newRotation });
      client.publish({
        destination: '/app/game/move',
        body: JSON.stringify({ matchId, playerId: player.id, vx, vy, rotation: newRotation })
      });
    }, 1000 / 60);
    return () => clearInterval(moveLoop);
  }, [matchId, currentPlayer]);
  
  // Fetch wall data on mount
  useEffect(() => {
    fetch('http://localhost:9090/api/map/walls')
      .then(res => res.json())
      .then(data => {
        setWalls(data);
        console.log('Fetched walls:', data);
      })
      .catch(err => console.error('Failed to fetch walls:', err));
  }, []);
  
  // Render game
  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;
    // Clear screen
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    // Draw grid using canvas size
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    for (let x = 0; x < canvas.width; x += 50) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }
    for (let y = 0; y < canvas.height; y += 50) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvas.width, y);
      ctx.stroke();
    }
    // Draw walls first
    walls.forEach(wall => {
      const x = (wall.x / WORLD_W) * canvas.width;
      const y = (wall.y / WORLD_H) * canvas.height;
      const w = (wall.width / WORLD_W) * canvas.width;
      const h = (wall.height / WORLD_H) * canvas.height;
      ctx.fillStyle = '#444';
      ctx.fillRect(x, y, w, h);
    });
    // Draw players
    Object.values(gameState.players).forEach(player => {
      const screenX = (player.x / WORLD_W) * canvas.width;
      const screenY = (player.y / WORLD_H) * canvas.height;
      ctx.fillStyle = player.alive ? '#ff4444' : '#666';
      ctx.beginPath();
      ctx.arc(screenX, screenY, PLAYER_SIZE / 2, 0, Math.PI * 2);
      ctx.fill();
      // Direction line
      if (player.alive) {
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(screenX, screenY);
        if (currentPlayer && player.id === currentPlayer.id) {
          const dx = mousePos.current.x - screenX;
          const dy = mousePos.current.y - screenY;
          const length = Math.sqrt(dx * dx + dy * dy);
          const stickLength = 25;
          const dirX = dx / (length || 1);
          const dirY = dy / (length || 1);
          ctx.lineTo(
            screenX + dirX * stickLength,
            screenY + dirY * stickLength
          );
        } else {
          ctx.lineTo(
            screenX + Math.cos(player.rotation) * 25,
            screenY + Math.sin(player.rotation) * 25
          );
        }
        ctx.stroke();
      }
      // Health bar
      const barW = 40;
      const barH = 6;
      const healthPct = player.health / 100;
      ctx.fillStyle = '#333';
      ctx.fillRect(screenX - barW/2, screenY - 30, barW, barH);
      ctx.fillStyle = healthPct > 0.3 ? '#4a4' : '#f44';
      ctx.fillRect(screenX - barW/2, screenY - 30, barW * healthPct, barH);
      ctx.fillStyle = '#fff';
      ctx.font = '12px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(player.username, screenX, screenY - 35);
    });
    // Draw projectiles
    Object.values(gameState.projectiles).forEach(proj => {
      const screenX = (proj.x / WORLD_W) * canvas.width;
      const screenY = (proj.y / WORLD_H) * canvas.height;
      ctx.fillStyle = '#ff6';
      ctx.beginPath();
      ctx.arc(screenX, screenY, 3, 0, Math.PI * 2);
      ctx.fill();
    });
  }, [gameState, walls]);
  
  // Respawn
  const respawn = () => {
    if (!currentPlayer || !clientRef.current) return;
    
    clientRef.current.publish({
      destination: '/app/game/respawn',
      body: JSON.stringify({
        matchId,
        playerId: currentPlayer.id
      })
    });
  };
  
  // Make canvas full screen and responsive
  const [canvasSize, setCanvasSize] = useState({ width: window.innerWidth, height: window.innerHeight });
  useEffect(() => {
    const handleResize = () => {
      setCanvasSize({ width: window.innerWidth, height: window.innerHeight });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
  
  return (
    <>
      <style>{`
        html, body {
          margin: 0;
          padding: 0;
          overflow: hidden;
          height: 100%;
        }
        #game-canvas {
          position: fixed;
          left: 0;
          top: 0;
          width: 100vw;
          height: 100vh;
          z-index: 0;
          display: block;
          border: none;
        }
        .hud-corner {
          position: fixed;
          z-index: 2;
          color: #ccc;
          font-family: Arial, sans-serif;
          font-size: 16px;
          pointer-events: none;
        }
        .hud-top-left { left: 20px; top: 20px; text-align: left; }
        .hud-top-right { right: 20px; top: 20px; text-align: right; }
        .hud-bottom-left { left: 20px; bottom: 20px; text-align: left; }
        .hud-bottom-right { right: 20px; bottom: 20px; text-align: right; }
        .hud-btn {
          pointer-events: auto;
          padding: 10px 20px;
          font-size: 16px;
          background-color: #f44;
          color: white;
          border: none;
          border-radius: 5px;
          cursor: pointer;
          margin-top: 10px;
        }
      `}</style>
      {/* Top Left: Status */}
      <div className="hud-corner hud-top-left">
        <span style={{ color: connected ? '#4a4' : '#f44', fontWeight: 'bold' }}>
          {connected ? '🟢 ONLINE' : '🔴 OFFLINE'}
        </span>
      </div>
      {/* Top Right: Player Info */}
      <div className="hud-corner hud-top-right">
        {currentPlayer && (
          <span>
            {currentPlayer.username} | HP: {currentPlayer.health} | 
            {currentPlayer.alive ? <span style={{ color: '#4a4' }}>🟢 ALIVE</span> : <span style={{ color: '#f44' }}>💀 DEAD</span>}
          </span>
        )}
      </div>
      {/* Bottom Left: Controls */}
      <div className="hud-corner hud-bottom-left">
        <strong>Controls:</strong> WASD = Move<br />Mouse = Aim<br />Click = Shoot
      </div>
      {/* Bottom Right: Stats and Respawn */}
      <div className="hud-corner hud-bottom-right">
        <div style={{ fontSize: '14px', opacity: 0.8 }}>
          Players: {Object.keys(gameState.players).length} | Bullets: {Object.keys(gameState.projectiles).length}
        </div>
        {currentPlayer && !currentPlayer.alive && (
          <button className="hud-btn" onClick={respawn}>
            🔄 RESPAWN
          </button>
        )}
      </div>
      <canvas
        id="game-canvas"
        ref={canvasRef}
        width={canvasSize.width}
        height={canvasSize.height}
        style={{
          position: 'fixed',
          left: 0,
          top: 0,
          width: '100vw',
          height: '100vh',
          backgroundColor: '#1a1a1a',
          cursor: 'crosshair',
          outline: 'none',
          zIndex: 0,
          border: 'none',
          display: 'block'
        }}
        tabIndex={0}
        onMouseMove={handleMouseMove}
        onClick={handleClick}
        onFocus={() => console.log('🎯 Canvas focused - keyboard ready!')}
      />
      {gameOver && winnerName && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          background: 'rgba(0,0,0,0.7)',
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '3rem',
          zIndex: 1000
        }}>
          Winner: {winnerName}
        </div>
      )}
    </>
  );
};

export default Game;