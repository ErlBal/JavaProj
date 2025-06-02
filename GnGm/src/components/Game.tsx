import React, { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useLocation } from 'react-router-dom';
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
  const [gameState, setGameState] = useState<GameState>({ players: {}, projectiles: {} });
  const [currentPlayer, setCurrentPlayer] = useState<Player | null>(null);
  const [connected, setConnected] = useState(false);
  const [walls, setWalls] = useState<Wall[]>([]);
    // Input handling
  const keysPressed = useRef<{ [key: string]: boolean }>({});
  const mousePos = useRef({ x: 0, y: 0 });
  const currentPlayerRef = useRef<Player | null>(null);
  const connectedRef = useRef<boolean>(false);
  
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
  const CANVAS_W = 1000;
  const CANVAS_H = 700;
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
          console.log('🔍 Looking for player ID:', playerId, 'in state players:', Object.keys(state.players));
          if (state.players[playerId]) {
            console.log('✅ Found current player:', state.players[playerId]);
            setCurrentPlayer(state.players[playerId]);
          } else {
            console.log('❌ Current player not found in game state');
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
  }, [matchId]);
  
  useEffect(() => {
    axios.get(`/api/walls/${selectedMap}`)
      .then(res => setWalls(res.data))
      .catch(() => setWalls([]));
  }, [selectedMap]);
  
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
      }
    };
    
    const keyUp = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      if (['w', 'a', 's', 'd'].includes(key)) {
        keysPressed.current[key] = false;
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
    
    // Calculate shoot direction
    const playerScreenX = (currentPlayer.x / WORLD_W) * CANVAS_W;
    const playerScreenY = (currentPlayer.y / WORLD_H) * CANVAS_H;
    
    const dx = mousePos.current.x - playerScreenX;
    const dy = mousePos.current.y - playerScreenY;
    const direction = Math.atan2(dy, dx);
    
    // Send shoot command
    clientRef.current.publish({
      destination: '/app/game/shoot',
      body: JSON.stringify({
        matchId,
        playerId: currentPlayer.id,
        direction
      })
    });
    
    console.log('💥 Shooting!');
  };
    // Movement loop
  useEffect(() => {    const moveLoop = setInterval(() => {
      // Use refs to get current values
      const player = currentPlayerRef.current;
      const isConnected = connectedRef.current;
      const client = clientRef.current;
      
      // Debug: Check movement loop conditions
      if (!player) {
        console.log('❌ No current player');
        return;
      }
      if (!client) {
        console.log('❌ No WebSocket client');
        return;
      }      if (!isConnected) {
        console.log('❌ Not connected');
        return;
      }
      
      const keys = keysPressed.current;
      let deltaX = 0;
      let deltaY = 0;

      // WASD movement
      if (keys['w']) deltaY -= 5;
      if (keys['s']) deltaY += 5;
      if (keys['a']) deltaX -= 5;
      if (keys['d']) deltaX += 5;
      
      // Debug: Log when movement is detected
      if (deltaX !== 0 || deltaY !== 0) {
        console.log('🎮 Moving:', { deltaX, deltaY });
      }
        // Calculate rotation towards mouse
      const playerScreenX = (player.x / WORLD_W) * CANVAS_W;
      const playerScreenY = (player.y / WORLD_H) * CANVAS_H;
      const dx = mousePos.current.x - playerScreenX;
      const dy = mousePos.current.y - playerScreenY;
      const rotation = Math.atan2(dy, dx);      // Send movement
      if (deltaX !== 0 || deltaY !== 0) {
        console.log('🚶 Sending movement:', { playerId: player.id, deltaX, deltaY, rotation });
        client.publish({
          destination: '/app/game/move',
          body: JSON.stringify({
            matchId,
            playerId: player.id,
            deltaX,
            deltaY,
            rotation
          })        });
      }
    }, 16); // 60 FPS for smoother movement
      return () => clearInterval(moveLoop);
  }, []); // Remove dependencies to prevent constant recreation
  
  // Render game
  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;
    
    // Clear screen
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);
    
    // Draw grid
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    for (let x = 0; x < CANVAS_W; x += 50) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, CANVAS_H);
      ctx.stroke();
    }
    for (let y = 0; y < CANVAS_H; y += 50) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(CANVAS_W, y);
      ctx.stroke();
    }
    
    // Draw players
    Object.values(gameState.players).forEach(player => {
      const screenX = (player.x / WORLD_W) * CANVAS_W;
      const screenY = (player.y / WORLD_H) * CANVAS_H;
      
      // Player circle
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
        ctx.lineTo(
          screenX + Math.cos(player.rotation) * 25,
          screenY + Math.sin(player.rotation) * 25
        );
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
      
      // Username
      ctx.fillStyle = '#fff';
      ctx.font = '12px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(player.username, screenX, screenY - 35);
    });
      // Draw projectiles
    Object.values(gameState.projectiles).forEach(proj => {
      const screenX = (proj.x / WORLD_W) * CANVAS_W;
      const screenY = (proj.y / WORLD_H) * CANVAS_H;
      
      ctx.fillStyle = '#ff6';
      ctx.beginPath();
      ctx.arc(screenX, screenY, 3, 0, Math.PI * 2);
      ctx.fill();
    });
      // Draw walls
    ctx.fillStyle = '#555';
    if (Array.isArray(walls)) {
      walls.forEach(wall => {
        ctx.fillRect(wall.x, wall.y, wall.width, wall.height);
      });
    }
    
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
  
  return (
    <div style={{ textAlign: 'center', padding: '20px' }}>
      <h2>Test Area</h2>
      
      <div style={{ marginBottom: '10px' }}>
        <span style={{ color: connected ? '#4a4' : '#f44', fontWeight: 'bold' }}>
          {connected ? '🟢 ONLINE' : '🔴 OFFLINE'}
        </span>
        
        {currentPlayer && (
          <span style={{ marginLeft: '20px', color: '#ccc' }}>
            {currentPlayer.username} | HP: {currentPlayer.health} | 
            {currentPlayer.alive ? ' 🟢 ALIVE' : ' 💀 DEAD'}
          </span>
        )}
      </div>
      
      <canvas
        ref={canvasRef}
        width={CANVAS_W}
        height={CANVAS_H}        style={{ 
          border: '2px solid #333',
          backgroundColor: '#1a1a1a',
          cursor: 'crosshair',
          outline: 'none'
        }}
        tabIndex={0}
        onMouseMove={handleMouseMove}
        onClick={handleClick}
        onFocus={() => console.log('🎯 Canvas focused - keyboard ready!')}
      />
      
      <div style={{ marginTop: '10px', color: '#ccc' }}>
        <p><strong>Controls:</strong> WASD = Move | Mouse = Aim | Click = Shoot</p>
        
        {currentPlayer && !currentPlayer.alive && (
          <button 
            onClick={respawn}
            style={{
              padding: '10px 20px',
              fontSize: '16px',
              backgroundColor: '#f44',
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: 'pointer',
              marginTop: '10px'
            }}
          >
            🔄 RESPAWN
          </button>
        )}
        
        <div style={{ marginTop: '10px', fontSize: '14px', opacity: 0.7 }}>
          Players: {Object.keys(gameState.players).length} | 
          Bullets: {Object.keys(gameState.projectiles).length}
        </div>
      </div>
    </div>
  );
};

export default Game;