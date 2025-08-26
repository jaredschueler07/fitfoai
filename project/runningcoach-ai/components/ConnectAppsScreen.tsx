
import React, { useState } from 'react';
import { ConnectedApp } from '../types';
import { FitbitIcon } from './icons/FitbitIcon';
import { GoogleFitIcon } from './icons/GoogleFitIcon';
import { SpotifyIcon } from './icons/SpotifyIcon';
import { PlusIcon } from './icons/PlusIcon';

interface ConnectAppsScreenProps {
  onComplete: (apps: ConnectedApp[]) => void;
}

type AppId = 'fitbit' | 'google_fit' | 'spotify';

const apps: { id: AppId; name: string; icon: React.ReactNode; category: string }[] = [
  { id: 'fitbit', name: 'Fitbit', icon: <FitbitIcon />, category: 'Health' },
  { id: 'google_fit', name: 'Google Fit', icon: <GoogleFitIcon />, category: 'Health' },
  { id: 'spotify', name: 'Spotify', icon: <SpotifyIcon />, category: 'Music' },
];

const ConnectAppsScreen: React.FC<ConnectAppsScreenProps> = ({ onComplete }) => {
  const [connectedApps, setConnectedApps] = useState<Set<AppId>>(new Set());

  const toggleConnection = (appId: AppId) => {
    setConnectedApps(prev => {
      const newSet = new Set(prev);
      if (newSet.has(appId)) {
        newSet.delete(appId);
      } else {
        newSet.add(appId);
      }
      return newSet;
    });
  };

  const handleSubmit = () => {
    onComplete(Array.from(connectedApps));
  };

  return (
    <div className="w-full h-full bg-black text-white p-6 flex flex-col">
      <div className="pt-16 text-center">
        <h1 className="text-3xl font-bold text-white">Connect Your Apps</h1>
        <p className="text-neutral-400 mt-2">Get a personalized experience by connecting your health and music services.</p>
      </div>

      <div className="flex-1 mt-10 space-y-4">
        {apps.map(app => (
          <div key={app.id} className="bg-neutral-900 border border-neutral-800 rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 flex items-center justify-center bg-black rounded-lg">
                {app.icon}
              </div>
              <div>
                <p className="font-semibold text-white">{app.name}</p>
                <p className="text-sm text-neutral-500">{app.category}</p>
              </div>
            </div>
            <button
              onClick={() => toggleConnection(app.id)}
              className={`font-semibold py-2 px-4 rounded-full text-sm transition-colors ${
                connectedApps.has(app.id)
                  ? 'bg-neutral-700 text-neutral-300'
                  : 'bg-lime-400 text-black'
              }`}
            >
              {connectedApps.has(app.id) ? 'Connected' : 'Connect'}
            </button>
          </div>
        ))}
      </div>

      <div className="py-4">
        <button 
          onClick={handleSubmit}
          className="w-full bg-lime-400 text-black font-bold py-4 px-6 rounded-full flex items-center justify-center text-lg hover:bg-lime-300 transition-colors duration-300"
        >
          Continue
        </button>
         <button 
          onClick={handleSubmit}
          className="w-full text-neutral-400 font-medium py-4 text-center text-sm mt-2 hover:text-white transition-colors"
        >
          Skip for now
        </button>
      </div>
    </div>
  );
};

export default ConnectAppsScreen;
