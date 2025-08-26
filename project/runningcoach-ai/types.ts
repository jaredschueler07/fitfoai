
export enum AppScreen {
  Welcome = 'WELCOME',
  ConnectApps = 'CONNECT_APPS',
  PersonalizeProfile = 'PERSONALIZE_PROFILE',
  SetEventGoal = 'SET_EVENT_GOAL',
  Dashboard = 'DASHBOARD',
  FitnessGPT = 'FITNESS_GPT',
}

export enum FitnessLevel {
  Beginner = 'Beginner',
  Intermediate = 'Intermediate',
  Advanced = 'Advanced',
}

export enum RunningGoal {
  FiveK = '5K',
  TenK = '10K',
  HalfMarathon = 'Half Marathon',
  Marathon = 'Marathon',
}

export interface Coach {
  id: string;
  name: string;
  style: string;
}

export interface RaceGoal {
  name: string;
  date: string;
  distance: string;
  targetTime?: string;
}

export type ConnectedApp = 'fitbit' | 'google_fit' | 'spotify';

export interface UserProfile {
  name: string;
  age?: number;
  height?: string;
  weight?: string;
  // FIX: Add optional fitnessLevel to UserProfile type
  fitnessLevel?: FitnessLevel;
  connectedApps: ConnectedApp[];
  goal?: RaceGoal;
  coach: Coach;
}
