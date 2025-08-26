
import React, { useState } from 'react';
import { RaceGoal } from '../types';
import { ChevronRightIcon } from './icons/ChevronRightIcon';

interface SetEventGoalScreenProps {
  onComplete: (goal: RaceGoal) => void;
}

const popularRaces: Omit<RaceGoal, 'targetTime'>[] = [
    { name: 'New York City Marathon', date: 'Nov 5, 2024', distance: '26.2 mi' },
    { name: 'Chicago Marathon', date: 'Oct 8, 2024', distance: '26.2 mi' },
    { name: 'Berlin Marathon', date: 'Sep 24, 2024', distance: '26.2 mi' },
    { name: 'London Marathon', date: 'Apr 21, 2025', distance: '26.2 mi' },
];

const SetEventGoalScreen: React.FC<SetEventGoalScreenProps> = ({ onComplete }) => {
  const [goalType, setGoalType] = useState<'popular' | 'custom'>('popular');
  const [customGoal, setCustomGoal] = useState<Omit<RaceGoal, 'targetTime'>>({
      name: '',
      date: '',
      distance: ''
  });
  const [targetTime, setTargetTime] = useState('');

  const handleSelectPopular = (race: Omit<RaceGoal, 'targetTime'>) => {
    onComplete(race);
  };
  
  const handleCustomSubmit = () => {
    if (customGoal.name && customGoal.date && customGoal.distance) {
      onComplete({ ...customGoal, targetTime: targetTime || undefined });
    }
  };

  const isCustomFormValid = customGoal.name && customGoal.date && customGoal.distance;

  return (
    <div className="w-full h-full bg-black text-white p-6 flex flex-col">
      <div className="pt-16 text-center">
        <h1 className="text-3xl font-bold text-white">What are you training for?</h1>
        <p className="text-neutral-400 mt-2">Select a race or create a custom goal.</p>
      </div>

      <div className="my-8 flex justify-center bg-neutral-900 rounded-full p-1 border border-neutral-800">
        <button onClick={() => setGoalType('popular')} className={`px-8 py-2 rounded-full font-semibold transition-colors ${goalType === 'popular' ? 'bg-lime-400 text-black' : 'text-neutral-400'}`}>Popular</button>
        <button onClick={() => setGoalType('custom')} className={`px-8 py-2 rounded-full font-semibold transition-colors ${goalType === 'custom' ? 'bg-lime-400 text-black' : 'text-neutral-400'}`}>Custom</button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {goalType === 'popular' ? (
          <div className="space-y-3">
            {popularRaces.map(race => (
              <button key={race.name} onClick={() => handleSelectPopular(race)} className="w-full bg-neutral-900 border border-neutral-800 rounded-2xl p-4 flex items-center justify-between text-left hover:border-lime-400 transition-colors">
                <div>
                  <p className="font-semibold text-white">{race.name}</p>
                  <p className="text-sm text-neutral-400">{race.date} &middot; {race.distance}</p>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-neutral-500" />
              </button>
            ))}
          </div>
        ) : (
          <div className="space-y-4">
            <input type="text" placeholder="Race Name" value={customGoal.name} onChange={e => setCustomGoal({...customGoal, name: e.target.value})} className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 px-4 focus:outline-none focus:ring-2 focus:ring-lime-400" />
            <input type="date" placeholder="Date" value={customGoal.date} onChange={e => setCustomGoal({...customGoal, date: e.target.value})} className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 px-4 focus:outline-none focus:ring-2 focus:ring-lime-400" />
            <input type="text" placeholder="Distance (e.g., 10k, 13.1 mi)" value={customGoal.distance} onChange={e => setCustomGoal({...customGoal, distance: e.target.value})} className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 px-4 focus:outline-none focus:ring-2 focus:ring-lime-400" />
            <input type="text" placeholder="Target Time (optional)" value={targetTime} onChange={e => setTargetTime(e.target.value)} className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 px-4 focus:outline-none focus:ring-2 focus:ring-lime-400" />
          </div>
        )}
      </div>

      {goalType === 'custom' && (
        <div className="py-4">
          <button 
            onClick={handleCustomSubmit}
            disabled={!isCustomFormValid}
            className="w-full bg-lime-400 text-black font-bold py-4 px-6 rounded-full flex items-center justify-center text-lg hover:bg-lime-300 transition-colors duration-300 disabled:bg-neutral-600 disabled:cursor-not-allowed"
          >
            Set Goal
          </button>
        </div>
      )}
    </div>
  );
};

export default SetEventGoalScreen;
