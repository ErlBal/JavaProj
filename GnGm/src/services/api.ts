export type LoginRequest = {
  username: string;
  password: string;
};

const API_URL = 'http://localhost:9090/api';

export interface LoginResponse {
  token: string;
  id: number;
}

export interface Match {
  id: number;
  mapName: string;
  maxPlayers: number;
  isActive: boolean;
  createdAt: string;
  players: Player[];
}

export interface Player {
  id: number;
  username: string;
  kills: number;
  deaths: number;
  wins: number;
}

export const api = {
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error('Login failed');
    }

    const result = await response.json();
    
    // Store both token and player ID
    localStorage.setItem('jwtToken', result.token);
    localStorage.setItem('playerId', result.id.toString());
    
    return result;
  },

  async register(data: LoginRequest): Promise<LoginResponse> {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error('Registration failed');
    }

    const result = await response.json();
    
    // Store both token and player ID
    localStorage.setItem('jwtToken', result.token);
    localStorage.setItem('playerId', result.id.toString());
      return result;
  },

  // Add auth token to requests
  getAuthHeader(): Record<string, string> {
    const token = localStorage.getItem('jwtToken');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  },

  // Match-related API calls
  async createMatch(mapName: string, maxPlayers: number): Promise<Match> {
    const response = await fetch(`${API_URL}/matches?mapName=${encodeURIComponent(mapName)}&maxPlayers=${maxPlayers}`, {
      method: 'POST',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (!response.ok) {
      throw new Error('Failed to create match');
    }

    return response.json();
  },

  async getActiveMatches(): Promise<Match[]> {
    const response = await fetch(`${API_URL}/matches/active`, {
      method: 'GET',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (!response.ok) {
      throw new Error('Failed to get active matches');
    }

    return response.json();
  },

  async joinMatch(matchId: number, playerId: number): Promise<Match> {
    const response = await fetch(`${API_URL}/matches/${matchId}/join?playerId=${playerId}`, {
      method: 'POST',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (!response.ok) {
      throw new Error('Failed to join match');
    }

    return response.json();
  },
  async getCurrentPlayerMatch(playerId: number): Promise<Match | null> {
    const response = await fetch(`${API_URL}/matches/player/${playerId}/current`, {
      method: 'GET',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (response.status === 404) {
      return null; // No active match
    }

    if (!response.ok) {
      throw new Error('Failed to get current player match');
    }

    return response.json();
  },

  async leaveMatch(matchId: number, playerId: number): Promise<Match> {
    const response = await fetch(`${API_URL}/matches/${matchId}/leave?playerId=${playerId}`, {
      method: 'POST',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (!response.ok) {
      throw new Error('Failed to leave match');
    }

    return response.json();
  }
};