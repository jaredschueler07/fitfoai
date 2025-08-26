
import React from 'react';
import { UserProfile } from '../types';
import { PlusIcon } from './icons/PlusIcon';
import ProgressChart from './ProgressChart';

interface DashboardScreenProps {
  userProfile: UserProfile;
}

const DashboardScreen: React.FC<DashboardScreenProps> = ({ userProfile }) => {
  const pastWorkouts = [
    { date: 'May 28', type: 'Speed Run', duration: '25:10' },
    { date: 'May 26', type: 'Recovery Run', duration: '30:05' },
    { date: 'May 25', type: 'Long Run', duration: '45:52' },
  ];

  const weeklyProgressData = [
    { label: 'M', value: 30 },
    { label: 'T', value: 0 },
    { label: 'W', value: 45 },
    { label: 'T', value: 25 },
    { label: 'F', value: 35 },
    { label: 'S', value: 60 },
    { label: 'S', value: 0 },
  ];


  return (
    <div className="w-full h-full bg-black text-white p-6 pb-28">
      <header className="pt-10 flex justify-between items-center">
        <div>
          <p className="text-neutral-400">Welcome back,</p>
          <h1 className="text-3xl font-bold text-white">{userProfile.name}</h1>
        </div>
        <div className="w-12 h-12 bg-neutral-800 rounded-full overflow-hidden">
            <img src={`https://i.pravatar.cc/150?u=${userProfile.name}`} alt="User avatar" className="w-full h-full object-cover"/>
        </div>
      </header>

      <main className="mt-10">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6">
          <h2 className="text-lg font-semibold text-white">Today's Guided Run</h2>
          <p className="text-neutral-400 mt-1">with {userProfile.coach.name}</p>
          <div className="mt-4 flex items-center justify-between">
            <div>
              <p className="text-4xl font-bold text-lime-400">30:00</p>
              <p className="text-neutral-500">Recovery Run</p>
            </div>
            <button className="bg-lime-400 text-black font-bold py-3 px-5 rounded-full flex items-center justify-center text-md hover:bg-lime-300 transition-colors duration-300">
              <PlusIcon className="w-5 h-5 mr-1"/>
              Start
            </button>
          </div>
        </div>

        <div className="mt-8">
            <h3 className="text-lg font-semibold text-white mb-3">Weekly Activity</h3>
            <ProgressChart data={weeklyProgressData} />
        </div>

        <div className="mt-8">
            <h3 className="text-lg font-semibold text-white mb-3">Your Plan: {userProfile.goal?.name || 'General Fitness'}</h3>
            <div className="space-y-3">
                <div className="bg-neutral-900 p-4 rounded-lg flex justify-between items-center">
                    <p>Week 1, Day 2: Speed Run</p>
                    <span className="text-neutral-500">25 min</span>
                </div>
                <div className="bg-neutral-900 p-4 rounded-lg flex justify-between items-center">
                    <p>Week 1, Day 3: Long Run</p>
                    <span className="text-neutral-500">45 min</span>
                </div>
                <div className="bg-neutral-900 p-4 rounded-lg flex justify-between items-center opacity-50">
                    <p>Week 1, Day 4: Rest Day</p>
                </div>
            </div>
        </div>

        <div className="mt-8">
            <h3 className="text-lg font-semibold text-white mb-3">Past Workouts</h3>
            <div className="space-y-3">
                {pastWorkouts.map((workout, index) => (
                    <div key={index} className="bg-neutral-900 p-4 rounded-lg flex justify-between items-center">
                        <div>
                            <p className="font-medium text-white">{workout.date}</p>
                            <p className="text-sm text-neutral-400">{workout.type}</p>
                        </div>
                        <span className="text-white font-mono text-lg">{workout.duration}</span>
                    </div>
                ))}
            </div>
        </div>
      </main>
    </div>
  );
};

export default DashboardScreen;
