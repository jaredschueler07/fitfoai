
import React, { useState, useCallback } from 'react';
import WelcomeScreen from './components/WelcomeScreen';
import ConnectAppsScreen from './components/ConnectAppsScreen';
import PersonalizeProfileScreen from './components/PersonalizeProfileScreen';
import SetEventGoalScreen from './components/SetEventGoalScreen';
import DashboardScreen from './components/DashboardScreen';
import FitnessGPTScreen from './components/FitnessGPTScreen';
import BottomNavBar from './components/BottomNavBar';
import { UserProfile, AppScreen, ConnectedApp, RaceGoal, Coach } from './types';

const App: React.FC = () => {
  const [screen, setScreen] = useState<AppScreen>(AppScreen.Welcome);
  const [userProfile, setUserProfile] = useState<Partial<UserProfile>>({ name: 'Jane' });

  const handleStartOnboarding = useCallback(() => {
    setScreen(AppScreen.ConnectApps);
  }, []);
  
  const handleAppsConnected = useCallback((apps: ConnectedApp[]) => {
    setUserProfile(prev => ({ ...prev, connectedApps: apps }));
    setScreen(AppScreen.PersonalizeProfile);
  }, []);

  const handleProfilePersonalized = useCallback((data: { age: number; height: string; weight: string }) => {
    setUserProfile(prev => ({ ...prev, ...data }));
    setScreen(AppScreen.SetEventGoal);
  }, []);

  const handleGoalSet = useCallback((goal: RaceGoal) => {
    const finalProfile: UserProfile = {
      name: userProfile.name || 'Runner',
      age: userProfile.age,
      height: userProfile.height,
      weight: userProfile.weight,
      connectedApps: userProfile.connectedApps || [],
      goal: goal,
      coach: { id: 'bennett', name: 'Coach Bennett', style: 'Enthusiastic & philosophical' }, // Assign a default coach
    };
    setUserProfile(finalProfile);
    setScreen(AppScreen.Dashboard);
  }, [userProfile]);


  const screensWithNavBar = [AppScreen.Dashboard, AppScreen.FitnessGPT];

  const renderScreen = () => {
    switch (screen) {
      case AppScreen.Welcome:
        return <WelcomeScreen onGetStarted={handleStartOnboarding} />;
      case AppScreen.ConnectApps:
        return <ConnectAppsScreen onComplete={handleAppsConnected} />;
      case AppScreen.PersonalizeProfile:
        return <PersonalizeProfileScreen onComplete={handleProfilePersonalized} />;
      case AppScreen.SetEventGoal:
        return <SetEventGoalScreen onComplete={handleGoalSet} />;
      case AppScreen.Dashboard:
        return userProfile.goal ? <DashboardScreen userProfile={userProfile as UserProfile} /> : <WelcomeScreen onGetStarted={handleStartOnboarding} />;
      case AppScreen.FitnessGPT:
        return <FitnessGPTScreen />;
      default:
        return <WelcomeScreen onGetStarted={handleStartOnboarding} />;
    }
  };

  return (
    <div className="bg-[#121212] min-h-screen flex items-center justify-center font-sans">
      <div className="relative w-full max-w-sm h-[844px] max-h-[844px] bg-black overflow-hidden shadow-2xl rounded-[40px] border-4 border-neutral-800">
        <div className="absolute top-0 left-0 w-full h-full overflow-y-auto">
          {renderScreen()}
        </div>
        {screensWithNavBar.includes(screen) && (
          <BottomNavBar activeScreen={screen} setScreen={setScreen} />
        )}
      </div>
    </div>
  );
};

export default App;
