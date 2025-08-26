
import React from 'react';
import { AppScreen } from '../types';
import { HomeIcon } from './icons/HomeIcon';
import { ChartIcon } from './icons/ChartIcon';
import { ProfileIcon } from './icons/ProfileIcon';
import { ChatIcon } from './icons/ChatIcon';

interface BottomNavBarProps {
    activeScreen: AppScreen;
    setScreen: (screen: AppScreen) => void;
}

const BottomNavBar: React.FC<BottomNavBarProps> = ({ activeScreen, setScreen }) => {
    const navItems = [
        { screen: AppScreen.Dashboard, icon: <HomeIcon className="w-7 h-7" />, label: "Home" },
        { screen: AppScreen.FitnessGPT, icon: <ChatIcon className="w-7 h-7" />, label: "AI Coach" },
        { screen: 'PROGRESS' as any, icon: <ChartIcon className="w-7 h-7" />, label: "Progress" },
        { screen: 'PROFILE' as any, icon: <ProfileIcon className="w-7 h-7" />, label: "Profile" }
    ];
    
    return (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-sm bg-neutral-900/80 backdrop-blur-sm border-t border-neutral-800">
            <div className="flex justify-around items-center h-20">
                {navItems.map((item) => (
                    <button 
                        key={item.label} 
                        onClick={() => setScreen(item.screen)}
                        className={`flex flex-col items-center gap-1 ${activeScreen === item.screen ? 'text-lime-400' : 'text-neutral-500 hover:text-lime-300 transition-colors'}`}
                        disabled={!Object.values(AppScreen).includes(item.screen)} // Disable placeholder buttons
                    >
                        {item.icon}
                        <span className="text-xs font-medium">{item.label}</span>
                    </button>
                ))}
            </div>
        </div>
    );
};

export default BottomNavBar;
