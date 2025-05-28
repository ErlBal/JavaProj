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
class Game {
    constructor(playerId) {
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.playerId = playerId;
        this.gameState = {
            playerStates: new Map(),
            projectiles: new Map()
        };
        this.keys = new Set();
        this.mousePosition = { x: 0, y: 0 };
        this.setupWebSocket();
        this.setupInputHandlers();
        this.gameLoop();
    }

    setupWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, frame => {
            console.log('Connected to WebSocket');
            
            // Subscribe to game state updates
            this.stompClient.subscribe('/topic/game/state', message => {
                const state = JSON.parse(message.body);
                this.updateGameState(state);
            });
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
    }

    updateGameState(state) {
        // Update player states
        this.gameState.playerStates = new Map(Object.entries(state.playerStates));
        this.gameState.projectiles = new Map(Object.entries(state.projectiles));
        
        // Update health bar
        if (this.playerId && this.gameState.playerStates.has(this.playerId.toString())) {
            const health = this.gameState.playerStates.get(this.playerId.toString()).health;
            document.getElementById('healthBar').textContent = `Health: ${Math.round(health)}`;
        }
    }

    handleMovement() {
        if (!this.playerId) return;

        let deltaX = 0;
        let deltaY = 0;

        if (this.keys.has('w')) deltaY -= 1;
        if (this.keys.has('s')) deltaY += 1;
        if (this.keys.has('a')) deltaX -= 1;
        if (this.keys.has('d')) deltaX += 1;

        if (deltaX !== 0 || deltaY !== 0) {
            // Calculate rotation based on mouse position
            const playerState = this.gameState.playerStates.get(this.playerId.toString());
            if (playerState) {
                const dx = this.mousePosition.x - playerState.x;
                const dy = this.mousePosition.y - playerState.y;
                const rotation = Math.atan2(dy, dx);

                this.stompClient.send("/app/game/move", {}, JSON.stringify({
                    playerId: this.playerId,
                    deltaX: deltaX,
                    deltaY: deltaY,
                    rotation: rotation
                }));
            }
        }
    }

    shoot() {
        if (!this.playerId) return;

        const playerState = this.gameState.playerStates.get(this.playerId.toString());
        if (playerState) {
            const dx = this.mousePosition.x - playerState.x;
            const dy = this.mousePosition.y - playerState.y;
            const direction = Math.atan2(dy, dx);

            this.stompClient.send("/app/game/shoot", {}, JSON.stringify({
                playerId: this.playerId,
                direction: direction
            }));
        }
    }

    render() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw players
        this.gameState.playerStates.forEach((state, id) => {
            this.ctx.save();
            this.ctx.translate(state.x, state.y);
            this.ctx.rotate(state.rotation);
            
            // Draw player body
            this.ctx.fillStyle = id === this.playerId.toString() ? '#00ff00' : '#ff0000';
            this.ctx.fillRect(-15, -15, 30, 30);
            
            // Draw weapon direction
            this.ctx.strokeStyle = '#ffffff';
            this.ctx.beginPath();
            this.ctx.moveTo(0, 0);
            this.ctx.lineTo(30, 0);
            this.ctx.stroke();
            
            this.ctx.restore();
        });

        // Draw projectiles
        this.gameState.projectiles.forEach(projectile => {
            this.ctx.fillStyle = '#ffff00';
            this.ctx.beginPath();
            this.ctx.arc(projectile.x, projectile.y, 3, 0, Math.PI * 2);
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
    new Game(playerId);
} 