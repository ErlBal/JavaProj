// --- AUTH HANDLING ---
function showAuthError(msg) {
    document.getElementById('authError').textContent = msg;
}

function hideAuthModal() {
    document.getElementById('authModal').style.display = 'none';
}

function showAuthModal() {
    document.getElementById('authModal').style.display = 'flex';
}

function doLogin(username, password, onSuccess) {
    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    .then(res => {
        if (!res.ok) throw new Error('Login failed');
        return res.json();
    })
    .then(data => onSuccess(data))
    .catch(() => showAuthError('Login failed. Please check your credentials.'));
}

function doRegister(username, password, onSuccess) {
    fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    .then(res => {
        if (!res.ok) throw new Error('Register failed');
        return res.json();
    })
    .then(data => onSuccess(data))
    .catch(() => showAuthError('Registration failed. Try a different username.'));
}

window.addEventListener('DOMContentLoaded', () => {
    showAuthModal();
    document.getElementById('loginBtn').onclick = () => {
        const username = document.getElementById('authUsername').value.trim();
        const password = document.getElementById('authPassword').value;
        if (!username || !password) return showAuthError('Please enter username and password.');
        doLogin(username, password, data => {
            hideAuthModal();
            startGame(data.id);
        });
    };
    document.getElementById('registerBtn').onclick = () => {
        const username = document.getElementById('authUsername').value.trim();
        const password = document.getElementById('authPassword').value;
        if (!username || !password) return showAuthError('Please enter username and password.');
        doRegister(username, password, data => {
            hideAuthModal();
            startGame(data.id);
        });
    };
});

// --- GAME LOGIC ---
class Game {    constructor(playerId) {
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.playerId = playerId;
        this.gameState = {
            playerStates: new Map(),
            projectiles: new Map()
        };
        this.keys = new Set();
        this.mousePosition = { x: 0, y: 0 };
        
        // Debug flags to prevent console spam
        this.debugLogged = false;
        this.playerDebugLogged = false;
        this.colorDebugLogged = false;
        
        // Resize canvas initially and on window resize
        this.resizeCanvas();
        window.addEventListener('resize', () => this.resizeCanvas());

        this.setupWebSocket();
        this.setupInputHandlers();
        this.setupBotControls();
        this.gameLoop();
    }

    resizeCanvas() {
        const container = document.getElementById('gameContainer');
        this.canvas.width = container.clientWidth;
        this.canvas.height = container.clientHeight;
        
        // Add debug logging for canvas resize
        console.log(`Canvas resized to: ${this.canvas.width}x${this.canvas.height}`);
        
        // Ensure minimum canvas size
        if (this.canvas.width === 0 || this.canvas.height === 0) {
            this.canvas.width = 800;
            this.canvas.height = 600;
            console.log(`Canvas had zero dimensions, set to default: ${this.canvas.width}x${this.canvas.height}`);
        }
    }

    setupBotControls() {
        // Add bot button handler
        document.querySelector('#addBotBtn').onclick = () => {
            if (this.stompClient && this.stompClient.connected) {
                console.log("Sending add bot request...");
                this.stompClient.send("/app/game/addBot", {}, {});
            } else {
                console.error("WebSocket not connected");
            }
        };

        document.querySelector('#removeBotsBtn').onclick = () => {
            if (this.stompClient && this.stompClient.connected) {
                // Get all bot IDs from the game state and remove them
                this.gameState.playerStates.forEach((state, id) => {
                    if (state.username && state.username.startsWith('Bot')) {
                        console.log("Removing bot:", id);
                        this.stompClient.send("/app/game/removeBot", {}, JSON.stringify({ botId: parseInt(id) }));
                    }
                });
            } else {
                console.error("WebSocket not connected");
            }
        };

        // Add debug button handler
        document.querySelector('#debugBtn').onclick = () => {
            console.log("=== MANUAL DEBUG INFO ===");
            console.log(`Canvas dimensions: ${this.canvas.width}x${this.canvas.height}`);
            console.log(`Canvas style: ${this.canvas.style.cssText}`);
            console.log(`Game container style: ${document.getElementById('gameContainer').style.cssText}`);
            console.log(`Player ID: ${this.playerId}`);
            console.log(`Game state players:`, this.gameState.playerStates);
            
            const currentPlayer = this.gameState.playerStates.get(this.playerId.toString());
            if (currentPlayer) {
                console.log(`Current player state:`, currentPlayer);
                const canvasX = (currentPlayer.x / 1000) * this.canvas.width;
                const canvasY = (currentPlayer.y / 1000) * this.canvas.height;
                console.log(`Canvas position: (${canvasX}, ${canvasY})`);
                
                // Draw a test rectangle to verify canvas is working
                this.ctx.fillStyle = '#00ff00';
                this.ctx.fillRect(canvasX - 25, canvasY - 25, 50, 50);
                console.log("Drew green test rectangle at player position");
            } else {
                console.log("❌ Current player not found in game state!");
            }
        };
    }

    setupWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // Disable debug logging
        // this.stompClient.debug = null;
          this.stompClient.connect({}, frame => {
            console.log('Connected to WebSocket');
            
            // Subscribe to game state updates
            this.stompClient.subscribe('/topic/game/state', message => {
                const state = JSON.parse(message.body);
                this.updateGameState(state);
            });

            // Send join message to initialize player in game engine
            console.log('Sending join message for player ID:', this.playerId);
            this.stompClient.send("/app/game/join", {}, JSON.stringify({
                playerId: this.playerId,
                username: "Player" + this.playerId
            }));
        });
    }

    setupInputHandlers() {
        // Keyboard controls
        window.addEventListener('keydown', e => this.keys.add(e.key));
        window.addEventListener('keyup', e => this.keys.delete(e.key));
        
        // Mouse position for aiming
        this.canvas.addEventListener('mousemove', e => {
            const rect = this.canvas.getBoundingClientRect();
            this.mousePosition = {
                x: e.clientX - rect.left,
                y: e.clientY - rect.top
            };
        });

        // Mouse click for shooting
        this.canvas.addEventListener('click', () => this.shoot());
    }    updateGameState(state) {
        // Update player states
        this.gameState.playerStates = new Map(Object.entries(state.playerStates));
        this.gameState.projectiles = new Map(Object.entries(state.projectiles));
        
        console.log('Updated game state. Players:', this.gameState.playerStates.size, 'Projectiles:', this.gameState.projectiles.size);
        console.log('Player states:', this.gameState.playerStates);
        
        // Update health bar
        if (this.playerId && this.gameState.playerStates.has(this.playerId.toString())) {
            const health = this.gameState.playerStates.get(this.playerId.toString()).health;
            document.getElementById('healthBar').textContent = `Health: ${Math.round(health)}`;
        }

        // Debug log for bot states
        this.gameState.playerStates.forEach((state, id) => {
            if (state.username && state.username.startsWith('Bot')) {
                console.log(`Bot ${id} state:`, state);
            }
        });
    }

    handleMovement() {
        if (!this.playerId) return;

        // Check if player state exists before processing movement
        const playerState = this.gameState.playerStates.get(this.playerId.toString());
        if (!playerState) {
            // console.log('Player state not found in game state.'); // Optional debug log
            return;
        }

        let deltaX = 0;
        let deltaY = 0;

        if (this.keys.has('w')) deltaY -= 1;
        if (this.keys.has('s')) deltaY += 1;
        if (this.keys.has('a')) deltaX -= 1;
        if (this.keys.has('d')) deltaX += 1;

        if (deltaX !== 0 || deltaY !== 0) {
            // Calculate rotation based on mouse position
            const mouseGameX = (this.mousePosition.x / this.canvas.width) * 1000;
            const mouseGameY = (this.mousePosition.y / this.canvas.height) * 1000;

            // Calculate rotation from player's game world position to mouse game world position
            const dx = mouseGameX - playerState.x;
            const dy = mouseGameY - playerState.y;
            const rotation = Math.atan2(dy, dx);

            this.stompClient.send("/app/game/move", {}, JSON.stringify({
                playerId: this.playerId,
                deltaX: deltaX,
                deltaY: deltaY,
                rotation: rotation
            }));
        }
    }

    shoot() {
        if (!this.playerId) return;

        const playerState = this.gameState.playerStates.get(this.playerId.toString());
        if (playerState) {
            // Convert mouse position from canvas pixels to game world coordinates
            const mouseGameX = (this.mousePosition.x / this.canvas.width) * 1000;
            const mouseGameY = (this.mousePosition.y / this.canvas.height) * 1000;

            // Calculate direction from player's game world position to mouse game world position
            const dx = mouseGameX - playerState.x;
            const dy = mouseGameY - playerState.y;
            const direction = Math.atan2(dy, dx);

            this.stompClient.send("/app/game/shoot", {}, JSON.stringify({
                playerId: this.playerId,
                direction: direction
            }));
        }
    }    render() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Draw a subtle background grid or border to confirm canvas is visible
        this.ctx.strokeStyle = '#444';
        this.ctx.lineWidth = 1;
        this.ctx.strokeRect(0, 0, this.canvas.width, this.canvas.height);

        // Calculate player size based on canvas size
        const minDim = Math.min(this.canvas.width, this.canvas.height);
        const playerSize = Math.max(30, Math.min(80, minDim * 0.02)); // Adjusted calculation again        // Add one-time debug logging to avoid console spam
        if (!this.debugLogged) {
            console.log("=== DETAILED RENDER DEBUG (ONE TIME) ===");
            console.log(`Canvas size: ${this.canvas.width}x${this.canvas.height}`);
            console.log(`Players count: ${this.gameState.playerStates.size}`);
            console.log(`Current player ID: ${this.playerId} (type: ${typeof this.playerId})`);
            this.debugLogged = true;
        }

        // Draw players
        this.gameState.playerStates.forEach((state, id) => {
            // Only log debug info once per player to avoid spam
            if (!this.playerDebugLogged) {
                console.log("=== PLAYER RENDER DEBUG ===");
                console.log(`Player ID from state: ${id} (type: ${typeof id})`);
                console.log(`Player state:`, state);
                console.log(`ID comparison: ${id} === ${this.playerId.toString()} = ${id === this.playerId.toString()}`);
                
                // Scale position to canvas size
                const x = (state.x / 1000) * this.canvas.width;
                const y = (state.y / 1000) * this.canvas.height;
                
                console.log(`Game position: (${state.x}, ${state.y})`);
                console.log(`Canvas position: (${x}, ${y})`);
                console.log(`Canvas bounds: width=${this.canvas.width}, height=${this.canvas.height}`);
                
                this.playerDebugLogged = true;
            }
            
            // Scale position to canvas size
            const x = (state.x / 1000) * this.canvas.width;
            const y = (state.y / 1000) * this.canvas.height;
            
            this.ctx.save();
            this.ctx.translate(x, y);
            this.ctx.rotate(state.rotation);
            
            // Draw player body - make larger and more visible for debugging
            if (id === this.playerId.toString()) {
                this.ctx.fillStyle = '#ff0000'; // Changed to bright red for visibility
                if (!this.colorDebugLogged) {
                    console.log('🔴 Drawing current player in BRIGHT RED');
                    this.colorDebugLogged = true;
                }
            } else if (state.username && state.username.startsWith('Bot')) {
                this.ctx.fillStyle = '#ff9900'; // Bots are orange
                if (!this.colorDebugLogged) {
                    console.log('🟠 Drawing bot in ORANGE');
                    this.colorDebugLogged = true;
                }
            } else {
                this.ctx.fillStyle = '#0000ff'; // Changed to bright blue for visibility
                if (!this.colorDebugLogged) {
                    console.log('🔵 Drawing other player in BLUE');
                    this.colorDebugLogged = true;
                }
            }
            
            // Make player larger for debugging
            const debugPlayerSize = Math.max(50, playerSize);
            this.ctx.fillRect(-debugPlayerSize/2, -debugPlayerSize/2, debugPlayerSize, debugPlayerSize);            
            // Draw weapon direction  
            this.ctx.fillStyle = '#000';
            this.ctx.fillRect(0, -debugPlayerSize/6, debugPlayerSize * 0.7, debugPlayerSize/3);
            
            this.ctx.restore();
            
            // Draw player name
            this.ctx.fillStyle = '#fff';
            this.ctx.textAlign = 'center';
            this.ctx.fillText(state.username || 'Player ' + id, x, y - debugPlayerSize);
        });

        // Draw projectiles
        this.gameState.projectiles.forEach(projectile => {
            // Scale position to canvas size
            const x = (projectile.x / 1000) * this.canvas.width;
            const y = (projectile.y / 1000) * this.canvas.height;
            
            this.ctx.fillStyle = '#ffff00';
            this.ctx.beginPath();
            this.ctx.arc(x, y, Math.max(3, playerSize * 0.15), 0, Math.PI * 2);
            this.ctx.fill();
        });
    }

    gameLoop() {
        this.handleMovement();
        this.render();
        requestAnimationFrame(() => this.gameLoop());
    }
}

function startGame(playerId) {
    // Show the game container
    document.getElementById('gameContainer').style.display = 'block';
    new Game(playerId);
}