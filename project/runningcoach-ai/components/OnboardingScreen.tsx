
import React, { useState } from 'react';
import { UserProfile, FitnessLevel, RunningGoal, Coach, RaceGoal } from '../types';
import { UserIcon } from './icons/UserIcon';
import { CalendarIcon } from './icons/CalendarIcon';
import { GoalIcon } from './icons/GoalIcon';
import { CoachIcon } from './icons/CoachIcon';
import { PlusIcon } from './icons/PlusIcon';

interface OnboardingScreenProps {
  onComplete: (profile: UserProfile) => void;
}

const coaches: Coach[] = [
  { id: 'bennett', name: 'Coach Bennett', style: 'Enthusiastic & philosophical' },
  { id: 'mariana', name: 'Mariana Fernández', style: 'Calming & empowering' },
  { id: 'becs', name: 'Becs Gentry', style: 'High-energy & assertive' },
];

const OnboardingScreen: React.FC<OnboardingScreenProps> = ({ onComplete }) => {
  const [name, setName] = useState('');
  const [fitnessLevel, setFitnessLevel] = useState<FitnessLevel>(FitnessLevel.Beginner);
  const [runningGoal, setRunningGoal] = useState<RunningGoal>(RunningGoal.FiveK);
  const [selectedCoachId, setSelectedCoachId] = useState<string>(coaches[0].id);

  const handleSubmit = () => {
    const selectedCoach = coaches.find(c => c.id === selectedCoachId);
    if (name && selectedCoach) {
      // FIX: Construct a valid UserProfile object.
      // - The 'fitnessLevel' property is now part of the UserProfile type.
      // - Convert 'runningGoal' enum to a 'goal' object of type RaceGoal.
      // - Add the required 'connectedApps' property.
      onComplete({
        name,
        fitnessLevel,
        goal: { name: runningGoal, date: 'TBD', distance: runningGoal },
        coach: selectedCoach,
        connectedApps: [],
      });
    }
  };

  const InputGroup: React.FC<{ label: string; description: string; children: React.ReactNode }> = ({ label, description, children }) => (
    <div className="mb-6">
      <h2 className="text-xl font-bold text-white">{label}</h2>
      <p className="text-neutral-400 text-sm mb-3">{description}</p>
      {children}
    </div>
  );

  const CustomSelect: React.FC<{ icon: React.ReactNode; value: string; onChange: (e: React.ChangeEvent<HTMLSelectElement>) => void; children: React.ReactNode }> = ({ icon, value, onChange, children }) => (
    <div className="relative">
      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
        {icon}
      </div>
      <select 
        value={value}
        onChange={onChange}
        className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 pl-12 pr-4 appearance-none focus:outline-none focus:ring-2 focus:ring-lime-400"
      >
        {children}
      </select>
    </div>
  );


  return (
    <div className="w-full h-full bg-black text-white p-6 pb-28">
      <div className="pt-10">
        <InputGroup label="Name" description="Enter your name">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
              <UserIcon className="w-5 h-5 text-neutral-400" />
            </div>
            <input 
              type="text" 
              placeholder="Name" 
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-lime-400"
            />
          </div>
        </InputGroup>

        <InputGroup label="Fitness Level" description="Select your current fitness level">
          <CustomSelect icon={<CalendarIcon className="w-5 h-5 text-neutral-400" />} value={fitnessLevel} onChange={(e) => setFitnessLevel(e.target.value as FitnessLevel)}>
            {Object.values(FitnessLevel).map(level => <option key={level} value={level}>{level}</option>)}
          </CustomSelect>
        </InputGroup>

        <InputGroup label="Running Goals" description="Set your running goals">
          <CustomSelect icon={<GoalIcon className="w-5 h-5 text-neutral-400" />} value={runningGoal} onChange={(e) => setRunningGoal(e.target.value as RunningGoal)}>
             {Object.values(RunningGoal).map(goal => <option key={goal} value={goal}>{goal}</option>)}
          </CustomSelect>
        </InputGroup>

        <InputGroup label="Coach Selection" description="Choose your coach">
           <CustomSelect icon={<CoachIcon className="w-5 h-5 text-neutral-400" />} value={selectedCoachId} onChange={(e) => setSelectedCoachId(e.target.value)}>
             {coaches.map(coach => <option key={coach.id} value={coach.id}>{coach.name}</option>)}
          </CustomSelect>
        </InputGroup>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-sm p-4 bg-black border-t border-neutral-800">
        <button 
          onClick={handleSubmit}
          disabled={!name}
          className="w-full bg-lime-400 text-black font-bold py-4 px-6 rounded-full flex items-center justify-center text-lg hover:bg-lime-300 transition-colors duration-300 disabled:bg-neutral-600 disabled:cursor-not-allowed"
        >
          <PlusIcon className="w-6 h-6 mr-2"/>
          Start Run
        </button>
      </div>
    </div>
  );
};

export default OnboardingScreen;
