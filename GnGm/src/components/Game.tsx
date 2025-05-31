import { useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState, useCallback } from 'react';
import { api } from '../services/api';
import SockJS from 'sockjs-client';
import { Stomp, Client } from '@stomp/stompjs';

interface PlayerState {
  x: number;
  y: number;
  rotation: number;
  health: number;
  username: string;
  isAlive: boolean;
  currentWeapon?: {
    name: string;
    damage: number;
    fireRate: number;
  };
}

interface Projectile {
  x: number;
  y: number;
  direction: number;
  speed: number;
  damage: number;
  playerId: number;
}

interface GameState {
  playerStates: Record<string, PlayerState>;
  projectiles: Record<string, Projectile>;
}

export default function Game() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stompClientRef = useRef<Client | null>(null);
  const keysRef = useRef<Set<string>>(new Set());
  const mousePositionRef = useRef({ x: 0, y: 0 });
  const gameLoopRef = useRef<number | undefined>(undefined);
  
  const [isExiting, setIsExiting] = useState(false);
  const [gameState, setGameState] = useState<GameState>({
    playerStates: {},
    projectiles: {}
  });
  const [isConnected, setIsConnected] = useState(false);
  const [playerId] = useState(() => localStorage.getItem('playerId'));

  // Game constants
  const CANVAS_WIDTH = 1000;
  const CANVAS_HEIGHT = 800;
  const PLAYER_SIZE = 30;
  const PROJECTILE_SIZE = 4;
  const setupWebSocket = useCallback(() => {
    if (!playerId) return;

    const socket = new SockJS('http://localhost:9090/ws');
    const client = Stomp.over(socket);
    
    client.configure({
      debug: (str) => console.log('STOMP Debug:', str),
      onConnect: () => {
        console.log('Connected to WebSocket');
        setIsConnected(true);
          // Subscribe to game state updates
        client.subscribe('/topic/game/state', (message) => {
          const gameState = JSON.parse(message.body);
          setGameState(gameState);
        });

        // Send join message to initialize player in game engine
        const username = localStorage.getItem('username') || `Player${playerId}`;
        client.publish({
          destination: '/app/game/join',
          body: JSON.stringify({
            playerId: parseInt(playerId),
            username: username
          })
        });

        // Initialize player in game engine
        api.getCurrentPlayerMatch(parseInt(playerId))
          .then(match => {
            if (match) {
              // Player is in a match, initialize their state
              console.log('Player in match:', match.id);
            }
          })
          .catch(err => console.error('Error getting current match:', err));
      },
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setIsConnected(false);
      }
    });

    client.activate();
    stompClientRef.current = client;
  }, [playerId]);

  const sendMovement = useCallback(() => {
    if (!stompClientRef.current?.connected || !playerId) return;

    const keys = keysRef.current;
    let deltaX = 0;
    let deltaY = 0;

    if (keys.has('w') || keys.has('W')) deltaY -= 1;
    if (keys.has('s') || keys.has('S')) deltaY += 1;
    if (keys.has('a') || keys.has('A')) deltaX -= 1;
    if (keys.has('d') || keys.has('D')) deltaX += 1;

    if (deltaX !== 0 || deltaY !== 0) {
      // Calculate rotation based on mouse position
      const canvas = canvasRef.current;
      if (canvas) {
        const rect = canvas.getBoundingClientRect();
        const mouseGameX = ((mousePositionRef.current.x - rect.left) / canvas.clientWidth) * CANVAS_WIDTH;
        const mouseGameY = ((mousePositionRef.current.y - rect.top) / canvas.clientHeight) * CANVAS_HEIGHT;
        
        const playerState = gameState.playerStates[playerId];
        if (playerState) {
          const dx = mouseGameX - playerState.x;
          const dy = mouseGameY - playerState.y;
          const rotation = Math.atan2(dy, dx);

          stompClientRef.current.publish({
            destination: '/app/game/move',
            body: JSON.stringify({
              playerId: parseInt(playerId),
              deltaX,
              deltaY,
              rotation
            })
          });
        }
      }
    }
  }, [playerId, gameState.playerStates]);

  const handleShoot = useCallback(() => {
    if (!stompClientRef.current?.connected || !playerId) return;

    const canvas = canvasRef.current;
    if (canvas) {
      const rect = canvas.getBoundingClientRect();
      const mouseGameX = ((mousePositionRef.current.x - rect.left) / canvas.clientWidth) * CANVAS_WIDTH;
      const mouseGameY = ((mousePositionRef.current.y - rect.top) / canvas.clientHeight) * CANVAS_HEIGHT;
      
      const playerState = gameState.playerStates[playerId];
      if (playerState) {
        const dx = mouseGameX - playerState.x;
        const dy = mouseGameY - playerState.y;
        const direction = Math.atan2(dy, dx);

        stompClientRef.current.publish({
          destination: '/app/game/shoot',
          body: JSON.stringify({
            playerId: parseInt(playerId),
            direction
          })
        });
      }
    }
  }, [playerId, gameState.playerStates]);

  useEffect(() => {
    setupWebSocket();

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [setupWebSocket]);
  // Input handlers
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysRef.current.add(e.key.toLowerCase());
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysRef.current.delete(e.key.toLowerCase());
    };

    const handleMouseMove = (e: MouseEvent) => {
      mousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const handleMouseClick = (e: MouseEvent) => {
      if (e.button === 0) { // Left click
        handleShoot();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keyup', handleKeyUp);
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mousedown', handleMouseClick);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('keyup', handleKeyUp);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mousedown', handleMouseClick);
    };
  }, [handleShoot]);

  // Game loop for movement
  useEffect(() => {
    gameLoopRef.current = setInterval(() => {
      sendMovement();
    }, 1000 / 60); // 60 FPS

    return () => {
      if (gameLoopRef.current) {
        clearInterval(gameLoopRef.current);
      }
    };
  }, [sendMovement]);

  // Canvas rendering
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.fillStyle = '#2c3e50'; // Dark blue-gray background
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw grid pattern for the map
    ctx.strokeStyle = '#34495e';
    ctx.lineWidth = 1;
    const gridSize = 50;
    
    for (let x = 0; x <= canvas.width; x += gridSize) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }
    
    for (let y = 0; y <= canvas.height; y += gridSize) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvas.width, y);
      ctx.stroke();
    }

    // Draw map boundaries
    ctx.strokeStyle = '#e74c3c';
    ctx.lineWidth = 3;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);

    // Draw players
    Object.entries(gameState.playerStates).forEach(([id, state]) => {
      if (!state.isAlive) return;

      // Scale position to canvas size
      const x = (state.x / CANVAS_WIDTH) * canvas.width;
      const y = (state.y / CANVAS_HEIGHT) * canvas.height;
      
      ctx.save();
      ctx.translate(x, y);
      ctx.rotate(state.rotation);
      
      // Draw player body
      if (id === playerId) {
        ctx.fillStyle = '#27ae60'; // Player is green
      } else if (state.username?.startsWith('Bot')) {
        ctx.fillStyle = '#f39c12'; // Bots are orange
      } else {
        ctx.fillStyle = '#3498db'; // Other players are blue
      }
      
      ctx.fillRect(-PLAYER_SIZE/2, -PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);
      
      // Draw weapon direction (gun barrel)
      ctx.fillStyle = '#2c3e50';
      ctx.fillRect(0, -PLAYER_SIZE/6, PLAYER_SIZE * 0.7, PLAYER_SIZE/3);
      
      ctx.restore();
      
      // Draw player name
      ctx.fillStyle = '#ffffff';
      ctx.font = '12px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(state.username || `Player ${id}`, x, y - PLAYER_SIZE - 5);
      
      // Draw health bar
      const healthBarWidth = PLAYER_SIZE;
      const healthBarHeight = 4;
      const healthPercentage = state.health / 100;
      
      ctx.fillStyle = '#e74c3c';
      ctx.fillRect(x - healthBarWidth/2, y + PLAYER_SIZE/2 + 5, healthBarWidth, healthBarHeight);
      
      ctx.fillStyle = '#27ae60';
      ctx.fillRect(x - healthBarWidth/2, y + PLAYER_SIZE/2 + 5, healthBarWidth * healthPercentage, healthBarHeight);
    });

    // Draw projectiles
    Object.values(gameState.projectiles).forEach(projectile => {
      // Scale position to canvas size
      const x = (projectile.x / CANVAS_WIDTH) * canvas.width;
      const y = (projectile.y / CANVAS_HEIGHT) * canvas.height;
      
      ctx.fillStyle = '#f1c40f'; // Yellow projectiles
      ctx.beginPath();
      ctx.arc(x, y, PROJECTILE_SIZE, 0, 2 * Math.PI);
      ctx.fill();
    });

  }, [gameState, playerId]);

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

  if (!playerId) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 40 }}>
        <h2>Error: Not logged in</h2>
        <button onClick={() => navigate('/login')}>Go to Login</button>
      </div>
    );
  }

  const currentPlayer = gameState.playerStates[playerId];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: 20 }}>
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        width: '100%', 
        maxWidth: CANVAS_WIDTH,
        marginBottom: 10 
      }}>
        <h2 style={{ margin: 0, color: '#2c3e50' }}>GnGm Arena</h2>
        <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
          <div style={{ 
            padding: '5px 10px', 
            backgroundColor: isConnected ? '#27ae60' : '#e74c3c',
            color: 'white',
            borderRadius: 5,
            fontSize: 12
          }}>
            {isConnected ? 'Connected' : 'Disconnected'}
          </div>
          {currentPlayer && (
            <div style={{ 
              display: 'flex', 
              gap: 15,
              color: '#2c3e50',
              fontSize: 14
            }}>
              <span>Health: {Math.round(currentPlayer.health)}</span>
              <span>Weapon: {currentPlayer.currentWeapon?.name || 'None'}</span>
            </div>
          )}
        </div>
      </div>

      <canvas 
        ref={canvasRef} 
        width={CANVAS_WIDTH} 
        height={CANVAS_HEIGHT} 
        style={{ 
          border: '2px solid #2c3e50', 
          background: '#34495e',
          cursor: 'crosshair'
        }} 
      />

      <div style={{ marginTop: 15, display: 'flex', gap: 20, alignItems: 'center' }}>
        <div style={{ 
          fontSize: 12, 
          color: '#7f8c8d',
          textAlign: 'center'
        }}>
          <div>Controls: WASD to move, Mouse to aim, Click to shoot</div>
          <div>Players: {Object.keys(gameState.playerStates).length} online</div>
        </div>
        
        <button 
          onClick={handleExitMatch} 
          disabled={isExiting}
          style={{ 
            padding: '10px 20px',
            backgroundColor: isExiting ? '#95a5a6' : '#e74c3c',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            cursor: isExiting ? 'not-allowed' : 'pointer',
            fontSize: 14
          }}
        >
          {isExiting ? 'Exiting...' : 'Exit Match'}
        </button>
      </div>
    </div>
  );
}