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
  matchName: string;
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
  async createMatch(matchName: string, maxPlayers: number): Promise<Match> {
    const headers = {
      'Content-Type': 'application/json',
      ...this.getAuthHeader(),
    };
    console.log('[API] POST /matches', { headers });
    const response = await fetch(`${API_URL}/matches`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ matchName, maxPlayers })
    });
    if (!response.ok) {
      throw new Error('Failed to create match');
    }
    return response.json();
  },

  async listMatches(search?: string): Promise<Match[]> {
    const url = search ? `${API_URL}/matches?search=${encodeURIComponent(search)}` : `${API_URL}/matches`;
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        ...this.getAuthHeader(),
      },
    });
    if (!response.ok) {
      throw new Error('Failed to list matches');
    }
    return response.json();
  },

  async joinMatch(matchName: string, playerId: number): Promise<Match> {
    const headers = {
      'Content-Type': 'application/json',
      ...this.getAuthHeader(),
    };
    console.log('[API] POST /matches/join', { headers });
    const response = await fetch(`${API_URL}/matches/join`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ matchName, playerId })
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
  },

  async getMatchDetails(matchId: number): Promise<{ matchName: string }> {
    const response = await fetch(`${API_URL}/matches/${matchId}`, {
      method: 'GET',
      headers: {
        ...this.getAuthHeader(),
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch match details');
    }

    return response.json();
  },
};