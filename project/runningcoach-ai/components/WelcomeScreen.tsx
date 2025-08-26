
import React from 'react';

interface WelcomeScreenProps {
  onGetStarted: () => void;
}

const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onGetStarted }) => {
  return (
    <div className="w-full h-full bg-black text-white flex flex-col items-center justify-between p-8 pt-24">
      <div className="text-center">
        <div className="w-48 h-48 mx-auto rounded-full overflow-hidden border-2 border-neutral-700">
          <img src="https://picsum.photos/seed/runner/400/400" alt="Runner" className="w-full h-full object-cover" />
        </div>
        <h1 className="text-4xl font-bold mt-8">RunningCoach</h1>
        <p className="text-neutral-400 mt-2">Your Personal Running Companion</p>
      </div>
      
      <div className="w-full text-center">
        <p className="text-neutral-300 mb-6">Start your fitness journey today!</p>
        <button 
          onClick={onGetStarted}
          className="w-full bg-lime-400 text-black font-bold py-4 rounded-full text-lg hover:bg-lime-300 transition-colors duration-300"
        >
          Get Started
        </button>
      </div>
    </div>
  );
};

export default WelcomeScreen;
